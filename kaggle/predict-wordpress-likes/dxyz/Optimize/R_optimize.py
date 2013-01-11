import csv
from collections import defaultdict
import json

import time
import socket
import sys
from rpy2.robjects.vectors import FloatVector
from rpy2.robjects.packages import importr
import rpy2.robjects as robjects
import rpy2.rinterface as ri

from multiprocessing import Process
from multiprocessing import Queue

LIKED_POST_SAMPLE_SIZE = 50
CUTOFF = 100
NUMBER_OF_WORKERS = 6
features_list = []
index_list = []
users_list = []
omitted_list = []

def AP(predicted,actual,CUTOFF):
    if len(actual) == 0: return 0.0
    if len(predicted) == 0: return 0.0

    recall = 0
    running_precision = 0.0

    for z in xrange(min(len(predicted), CUTOFF)):
        if actual.count(predicted[z]) > 0:
            recall += 1
            running_precision += float(recall)/float(z + 1)

    AP = running_precision/float(min(len(actual), CUTOFF))

    return AP

def F_test(x):
    return (x[0] - 2.34)**2 + (x[1] - 5.67)**2

def F_worker(x,start,end,result):

    blog_weight = 1.0
    time_weight = x[0]
    #lang_weight = x[1]
    content_weight = x[1]
    title_weight = x[2]
    tags_weight = x[3]
    category_weight= x[4]


    scores_list = []

    for j in xrange(start,end):
        row = index_list[j]
        user = row[0]
        candidate = row[1]
        features = features_list[j]
        blog_prob = features[0]
        time_feature = features[1]
        #lang_feature = features[2]
        SEMANTIC_FEATURES_OFFSET = 2
        if len(features) == SEMANTIC_FEATURES_OFFSET + LIKED_POST_SAMPLE_SIZE*4: #final feature is label
            content_similar = features[SEMANTIC_FEATURES_OFFSET:SEMANTIC_FEATURES_OFFSET+LIKED_POST_SAMPLE_SIZE]
            title_similar = features[SEMANTIC_FEATURES_OFFSET+LIKED_POST_SAMPLE_SIZE:SEMANTIC_FEATURES_OFFSET+2*LIKED_POST_SAMPLE_SIZE]
            tags_similar= features[SEMANTIC_FEATURES_OFFSET+2*LIKED_POST_SAMPLE_SIZE:SEMANTIC_FEATURES_OFFSET+3*LIKED_POST_SAMPLE_SIZE]
            categories_similar = features[SEMANTIC_FEATURES_OFFSET+3*LIKED_POST_SAMPLE_SIZE:SEMANTIC_FEATURES_OFFSET+4*LIKED_POST_SAMPLE_SIZE]
            #    print len(features),len(content_similar),len(title_similar),len(tags_similar),len(categories_similar)
            #    if len(categories_similar) == 0:
            #        print row,features

            similar = [content_weight*rho_content + title_weight*rho_title + tags_weight*rho_tags + category_weight*rho_categories for (rho_content,rho_title,rho_tags,rho_categories) in zip(content_similar,title_similar,tags_similar,categories_similar)]

            #score = blog_prob + time_weight*time_feature + lang_weight*lang_feature + max([rho for rho in similar])
            score = blog_prob + time_weight*time_feature + max([rho for rho in similar])
        else:
            print "WARNING: wrong number of features."
            #score = blog_prob + time_weight*time_feature + lang_weight*lang_feature
            score = blog_prob + time_weight*time_feature
        scores_list.append((user,candidate,score))

    result.put(scores_list)
    return

def F(x):
    print "Evaluating function at:",x[0],x[1],x[2],x[3],x[4]

    threads = []
    result = Queue()
    candidates = defaultdict(list)
    slice_size = int(len(index_list)/NUMBER_OF_WORKERS)

    for n in xrange(NUMBER_OF_WORKERS):
        slice_start = n*slice_size

        if n == NUMBER_OF_WORKERS - 1:
            slice_end = len(index_list)
        else:
            slice_end = (n+1)*slice_size

        #print "Starting worker with ",slice_start,slice_end
        t = Process(target= F_worker, args=(x,slice_start,slice_end,result))
        threads.append(t)
        t.start()

    results_read = 0

    while True:
        y = result.get()
        results_read += 1
        for row in y:
            candidates[row[0]].append((row[1],row[2]))
        if results_read == NUMBER_OF_WORKERS:
            break

    number_of_predictions = 0
    running_AP = 0.0

    for k,user in enumerate(users_list):

        actual = omitted_list[k]

        candidate_list = candidates[user]
        candidate_list.sort(key = lambda (k,v):v, reverse = True)

        predicted = [int(k) for (k,v) in candidate_list[:100]]

        p = AP(predicted,actual,CUTOFF)
        running_AP += p
        number_of_predictions += 1

    MAP = running_AP/float(number_of_predictions)
    print 'Function Eval Complete. 1 - MAP =',1.0 - MAP
    return 1.0 - MAP

def display_current(y):
    print y

# put everything in memory for fast evaluation

def initialize():

    #TODO try passing slices to processes rather than use global obj to save memory
    DataSet = 'my'
    inputfolder = '../'+DataSet+'Features/'
    inputIndex = inputfolder+'linear_kaggle_index.csv'
    featuresFile = inputfolder+'linear_kaggle_features.csv'


    index_file = open(inputIndex,'r')
    reader = csv.reader(index_file)
    features_file = open(featuresFile,'r')
    features_reader = csv.reader(features_file)



    print "Building features lists"

    for row in reader:
        user = row[0]
        candidate = row[1]
        index_list.append((user,candidate))

        features = features_reader.next()
        features_list.append([float(x) for x in features[:-1]])

    print "Done"

    print "Building users/omitted lists"

    infolder = '../'+DataSet+'Data/'
    omitted_file = '../MyData/myomittedPosts.json'

    trainUsersFile = infolder+DataSet+"trainUsers.json"

    number_of_predictions = 0
    running_AP = 0.0

    with open(trainUsersFile, "r") as users, open(omitted_file,'r') as f_omitted:

        for user_total, line in enumerate(users):
            user = json.loads(line)
            if not user["inTestSet"]:
                continue
            users_list.append(user["uid"])

            actual = f_omitted.readline()
            actual = actual.strip()
            actual = actual.split()
            actual = [int(n) for n in actual]
            omitted_list.append(actual)

    print "Done"


TEST = False

nloptr = importr('nloptr')

if TEST:

    # wrap the function f so it can be exposed to R
    cost_Fr = ri.rternalize(F_test)

    # starting parameters
    #start_params = FloatVector((.1, .1, .1, .1, .1))
    start_params = FloatVector((.1, .1))

    lower_bound = FloatVector((0, 0))
    upper_bound = FloatVector((10, 10))


    test ={'algorithm':'NLOPT_GN_DIRECT',"ftol_abs":1.0e-7,'maxeval':1000000}
    rlist = robjects.ListVector(test)

    print 'Starting opt'
    res = nloptr.nloptr(x0=start_params, eval_f=cost_Fr, opts = rlist,lb=lower_bound,ub=upper_bound)
    print "Opt finished"
    result = str(res).split()
    best_point = [result[-2],result[-1]]
    print "Best Point:",best_point
    f = open('BEST_WEIGHTS','w')
    f.write(' '.join([str(z) for z in best_point]))
    f.close()
    exit()

initialize()
nloptr = importr('nloptr')

# wrap the function f so it can be exposed to R
cost_Fr = ri.rternalize(F)

# starting parameters
#start_params = FloatVector((0.050911813998, 0.0293639525993, 0.0405870554642 ,0.0735468126597, 0.0313789144358))
start_params = FloatVector((0.0,0.0,0.0,0.0,0.0))

lower_bound = FloatVector((0,0,0,0,0))
upper_bound = FloatVector((.25,.25,.25,.25,.25))

# use 1.0e-6
test ={'algorithm':'NLOPT_GN_DIRECT_L',"ftol_abs":1.0e-6,'maxeval':10000}
rlist = robjects.ListVector(test)

res = nloptr.nloptr(x0=start_params, eval_f=cost_Fr, opts = rlist,lb=lower_bound,ub=upper_bound)
result = str(res).split()
best_point = [result[-5],result[-4],result[-3],result[-2],result[-1]]
print "Best Point:",best_point
f = open('BEST_WEIGHTS','w')
f.write(' '.join([str(z) for z in best_point]))
f.close()
exit()
