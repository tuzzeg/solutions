from __future__ import division
import sys
sys.path.append('../Benchmarks/wordpress-LDA')


from collections import defaultdict,Counter
#from corp import stop_words, Files, Corp
from gensim import corpora, models, similarities
import logging
import json
import cPickle
import random
import time
import csv

LDA_NUM_TOPICS = 200
GENERATE_FEATURES = True
LIKED_POST_SAMPLE_SIZE = 50
DEFAULT_NUMBER_OF_POSTS = 1000 # this number is used for blogs tha have likes but no observed posts
WEIGHT_OLDER_POSTS = 1.0

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

DataSet = 'my'
SPLIT = ''

infolder = '../'+DataSet+'Data/'
outfolder = '../'+DataSet+'Submissions/'

content_savedfolder = '../Tools/Content/'+DataSet+'Saved/'
title_savedfolder = '../Tools/Titles/'+DataSet+'Saved/'
tags_savedfolder = '../Tools/Tags/'+DataSet+'Saved/'
categories_savedfolder = '../Tools/Categories/'+DataSet+'Saved/'
blogprobdist_savedfolder = '../Tools/BlogProbDist/'+DataSet+'Saved/'
standingqueries_savedfolder = '../Tools/StandingQueries/'+DataSet+'Saved/'
testindexdict_savedfolder = '../Tools/TestIndexDict/'+DataSet+'Saved/'

if False:
    kaggle_user_stats = 'kaggle-stats-users-20111123-20120423.json'
    kaggle_blog_stat = 'kaggle-stats-blogs-20111123-20120423.json'

    kaggle_user_stats_file = open("../Data/kaggle-stats-users-20111123-20120423.json")
    kaggle_blog_stats_file = open("../Data/kaggle-stats-blogs-20111123-20120423.json")

    stats_end_date_string = kaggle_user_stats.split('-')
    stats_end_date_string = stats_end_date_string[4].split('.')[0]+' 00:00:00'

    STATS_END_DATE = time.strptime(stats_end_date_string,"%Y%m%d %H:%M:%S")


testPostsFile = infolder+DataSet+"test"+SPLIT+"Posts.json"
trainPostsFile = infolder+DataSet+"trainPosts.json"
trainUsersFile = infolder+DataSet+"trainUsers.json"
omittedPostsFile = infolder+DataSet+"omittedPosts.json"

featuresFile = '../'+DataSet+'Features/linear'+SPLIT+'_kaggle_features.csv'
indexFile = '../'+DataSet+'Features/linear'+SPLIT+'_kaggle_index.csv'

try:
    x = cPickle.load(open(blogprobdist_savedfolder+"BlogProbDist.saved", "r"))
    user_liked_blog_dict = defaultdict(lambda: defaultdict(float))
    user_liked_blog_dict.update(x)

except:
    print "Cannot find Blog Prob Dist."
    exit()

logging.info("Done calculating blog like probability for each user")

#print user_liked_blog_dict['5']
#print user_liked_blog_dict['102']


# Now, we do some quick preliminary work to determine which blogs have which posts, and to map post_id's to a zero-based index, or vice versa
testPostIds = []
testPostIndices = {}

try:
    testPostIndices = cPickle.load(open(testindexdict_savedfolder+'TestIndexDict.saved', "r"))
    testPostIds = cPickle.load(open(testindexdict_savedfolder+'TestPostList.saved', "r"))
except:
    print "Cannot find test post index dict"
    exit()

testPostTimeWeights = {}
blogTestPosts = defaultdict(list)
with open(testPostsFile) as f:
    for i, line in enumerate(f):
        post = json.loads(line)
        blog_id = post["blog"]
        post_id = post["post_id"]
        blogTestPosts[blog_id].append(post_id)
        date = post['date_gmt']
        dt = time.strptime(date,"%Y-%m-%d %H:%M:%S")
        testPostTimeWeights[post_id] = time.mktime(dt)

dates = testPostTimeWeights.items()
dates.sort(key = lambda (k,v):v)
min_time = dates[0][1]
max_time = dates[-1][1]
# TODO try different weighting function here
for post_id in testPostTimeWeights:
    testPostTimeWeights[post_id] = 1.0 - (testPostTimeWeights[post_id] - min_time)/(max_time-min_time)

logging.info("Done doing preliminary test data processing")

if False:
    trainPostIndices = {}
    blogTrainPosts = defaultdict(list)
    with open(trainPostsFile) as f:
        for i, line in enumerate(f):
            post = json.loads(line)
            blog_id = post["blog"]
            post_id = post["post_id"]
            trainPostIndices[post_id] = i
            blogTrainPosts[blog_id].append(post_id)

    logging.info("Done doing preliminary training data processing")

# CONTENT

try:
    content_testVecs = cPickle.load(open(content_savedfolder+"TestVecs.saved", "r"))
    #content_testIndex = similarities.Similarity.load(content_savedfolder+"TestIndex.saved") #TODO dont need this w/o the padding
except:
    print 'Cannot find TestVec.saved'
    exit()
logging.info("Done making the test lookup index")


try:
    content_trainVecs = cPickle.load(open(content_savedfolder+"TrainVecs.saved", "r"))
except:
    print 'Cannot find TrainVecs.saved'
    exit()

logging.info("Done estimating the training topics")

# TITLES
try:
    title_dictionary = corpora.dictionary.Dictionary.load(title_savedfolder+"dictionary.saved")
except:
    print 'Cannot find dictionary.saved'
    exit()

try:
    title_testVecs = cPickle.load(open(title_savedfolder+"TestVecs.saved", "r"))
    #title_testIndex = similarities.Similarity.load(title_savedfolder+"TestIndex.saved")
except:
    print 'Cannot find TestVec.saved'
    exit()

logging.info("Done making the test lookup index")

try:
    title_trainVecs = cPickle.load(open(title_savedfolder+"TrainVecs.saved", "r"))
except:
    print 'Cannot find TrainVecs.saved'
    exit()

# TAGS
try:
    tags_dictionary = corpora.dictionary.Dictionary.load(tags_savedfolder+"dictionary.saved")
except:
    print 'Cannot find dictionary.saved'
    exit()

try:
    tags_testVecs = cPickle.load(open(tags_savedfolder+"TestVecs.saved", "r"))
    #tags_testIndex = similarities.Similarity.load(tags_savedfolder+"TestIndex.saved")
except:
    print 'Cannot find TestVec.saved'
    exit()

logging.info("Done making the test lookup index")


try:
    tags_trainVecs = cPickle.load(open(tags_savedfolder+"TrainVecs.saved", "r"))
except:
    print 'Cannot find TrainVecs.saved'
    exit()
# CATEGORIES
try:
    categories_dictionary = corpora.dictionary.Dictionary.load(categories_savedfolder+"dictionary.saved")
except:
    print 'Cannot find dictionary.saved'
    exit()

try:
    categories_testVecs = cPickle.load(open(categories_savedfolder+"TestVecs.saved", "r"))
    #categories_testIndex = similarities.Similarity.load(categories_savedfolder+"TestIndex.saved")
except:
    print 'Cannot find TestVec.saved'
    exit()

logging.info("Done making the test lookup index")


try:
    categories_trainVecs = cPickle.load(open(categories_savedfolder+"TrainVecs.saved", "r"))
except:
    print 'Cannot find TrainVecs.saved'
    exit()

# Now we begin making submissions
#weights = [1.0/16.31076201,0.8378268/16.31076201,1.40504561/16.31076201,1.05145758/16.31076201,1.01224388/16.31076201]
weights = [0.050911813998, 0.0293639525993, 0.0405870554642 ,0.0735468126597, 0.0313789144358]

blog_weight = 1.0

time_weight = weights[0]
content_weight = weights[1]
title_weight = weights[2]
tags_weight = weights[3]
category_weight= weights[4]


logging.info("Beginning to make submissions")
#file_out = open(outfolder+'output.txt','w')
#file_out_submissions = open(outfolder+'linearsubmission.csv','w')

if DataSet == 'my':
    omitted = open(omittedPostsFile, "r")

if GENERATE_FEATURES:
    features_file = open(featuresFile,"w")
    csv_writer = csv.writer(features_file)
    index_file = open(indexFile,"w")
    csv_index_writer = csv.writer(index_file)


with open(trainUsersFile, "r") as users, open(standingqueries_savedfolder+"StandingQueries.saved", "r") as standing_queries ,open(outfolder+'linear_kaggle'+SPLIT+'_submission.csv','w') as file_out_submissions:
    liked_post_reader = csv.reader(standing_queries)
    file_out_submissions.write("\"posts\"\n")
    for user_total, line in enumerate(users):
        user = json.loads(line)
        if not user["inTestSet"]:
            continue
        if True and DataSet == 'my':
            omitted_posts = omitted.readline()
            omitted_posts = omitted_posts.strip()
            omitted_posts = omitted_posts.split()
            #omitted_posts = [int(n) for n in omitted_posts] # keys are strings
            omitted_posts_indices = [testPostIndices[post_id] for post_id in omitted_posts]
            omitted_vecs = [content_testVecs[i] for i in omitted_posts_indices]


        candidate_posts = defaultdict(int) # The potential posts to recommend and their scores
        if GENERATE_FEATURES:
            candidate_posts_features = defaultdict(list)

        #candidate generation
        user_likes_dict = defaultdict(float)
        if False:

            for likes in user['likes']:
                try: # try block is needed as there are a few blogs where the user likes them in the train set but the likes do not appear
                    user_likes_dict[likes['blog']] += 1/float(len(blogTrainPosts[likes['blog']]))
                except:
                    user_likes_dict[likes['blog']] += 1 #TODO maybe try .5?

        for blog in user_liked_blog_dict[user["uid"]].keys():
            user_likes_dict[blog] = user_liked_blog_dict[user["uid"]][blog]

        for blog,weight in user_likes_dict.items():

            for post_id in blogTestPosts[blog]:
                if GENERATE_FEATURES:
                    candidate_posts_features[post_id].extend([weight,testPostTimeWeights[post_id]])
                candidate_posts[post_id] = weight*blog_weight + time_weight*testPostTimeWeights[post_id]

        candidate_posts_indices = [testPostIndices[post_id] for post_id in candidate_posts]

        content_candidate_posts_vecs = [content_testVecs[i] for i in candidate_posts_indices]
        title_candidate_posts_vecs = [title_testVecs[i] for i in candidate_posts_indices]
        tags_candidate_posts_vecs = [tags_testVecs[i] for i in candidate_posts_indices]
        categories_candidate_posts_vecs = [categories_testVecs[i] for i in candidate_posts_indices]

        row = liked_post_reader.next()
        liked_post_indices = [int(k) for k in row]

        if False:
            for like in user["likes"]:
                try: # For whatever reason, there is a slight mismatch between posts liked by users in trainUsers.json, and posts appearing in trainPosts.json
                    liked_post_indices.append(trainPostIndices[like["post_id"]])
                except:
                    logging.warning("Bad index!")


            total_likes = len(liked_post_indices)
            sample_size = min(LIKED_POST_SAMPLE_SIZE, total_likes)
            liked_post_indices = random.sample(liked_post_indices, sample_size) # to cut down computation time
            #print liked_post_indices

        if True and len(liked_post_indices) > 0:

            content_liked_post_vecs = [content_trainVecs[i] for i in liked_post_indices]
            content_likedPostIndex = similarities.SparseMatrixSimilarity(content_liked_post_vecs, num_terms=LDA_NUM_TOPICS)

            title_liked_post_vecs = [title_trainVecs[i] for i in liked_post_indices]
            title_likedPostIndex = similarities.SparseMatrixSimilarity(title_liked_post_vecs, num_terms=len(title_dictionary.keys()))

            tags_liked_post_vecs = [tags_trainVecs[i] for i in liked_post_indices]
            tags_likedPostIndex = similarities.SparseMatrixSimilarity(tags_liked_post_vecs, num_terms=len(tags_dictionary.keys()))

            categories_liked_post_vecs = [categories_trainVecs[i] for i in liked_post_indices]
            categories_likedPostIndex = similarities.SparseMatrixSimilarity(categories_liked_post_vecs, num_terms=len(categories_dictionary.keys()))


            for posts_index, content_similar,title_similar,tags_similar,categories_similar in zip(candidate_posts_indices, content_likedPostIndex[content_candidate_posts_vecs],title_likedPostIndex[title_candidate_posts_vecs],tags_likedPostIndex[tags_candidate_posts_vecs],categories_likedPostIndex[categories_candidate_posts_vecs]):
                # the length of similarity will be the length of liked_post_indices
                #print testPostIds[posts_index],content_similar,title_similar
                similar = [content_weight*rho_content + title_weight*rho_title + tags_weight*rho_tags + category_weight*rho_categories for (rho_content,rho_title,rho_tags,rho_categories) in zip(content_similar,title_similar,tags_similar,categories_similar)]
                #print similar
                # have to pad similarities to length LIKED_POST_SAMPLE_SIZE
                if GENERATE_FEATURES:
                    content_similar_list = list(content_similar)
                    title_similar_list = list(title_similar)
                    tags_similar_list = list(tags_similar)
                    categories_similar_list = list(categories_similar)

                if GENERATE_FEATURES and len(liked_post_indices) < LIKED_POST_SAMPLE_SIZE:
                    pad = [0.0 for z in xrange(LIKED_POST_SAMPLE_SIZE-len(liked_post_indices))]
                    content_similar_list.extend(pad)
                    title_similar_list.extend(pad)
                    tags_similar_list.extend(pad)
                    categories_similar_list.extend(pad)

                if GENERATE_FEATURES:
                    candidate_posts_features[testPostIds[posts_index]].extend(content_similar_list)
                    candidate_posts_features[testPostIds[posts_index]].extend(title_similar_list)
                    candidate_posts_features[testPostIds[posts_index]].extend(tags_similar_list)
                    candidate_posts_features[testPostIds[posts_index]].extend(categories_similar_list)

                candidate_posts[testPostIds[posts_index]] += max([rho for rho in similar])
        else:
            if GENERATE_FEATURES:
                pad = [0.0 for z in xrange(LIKED_POST_SAMPLE_SIZE)]
                for posts_index in candidate_posts_indices:
                    candidate_posts_features[testPostIds[posts_index]].extend(pad)
                    candidate_posts_features[testPostIds[posts_index]].extend(pad)
                    candidate_posts_features[testPostIds[posts_index]].extend(pad)
                    candidate_posts_features[testPostIds[posts_index]].extend(pad)

        recommendedPosts = list(sorted(candidate_posts, key=candidate_posts.__getitem__, reverse=True))


        if DataSet == 'my':
            print "Recall: ", len(set(omitted_posts) & set(recommendedPosts[:100])),'/',len(set(omitted_posts))

        if GENERATE_FEATURES:
            #for post_id in candidate_posts_features:
            for post_id in recommendedPosts[:125]:
                if DataSet == 'my':
                    if post_id in set(omitted_posts):
                        candidate_posts_features[post_id].append('g')
                    else:
                        candidate_posts_features[post_id].append('b')
                csv_writer.writerow(candidate_posts_features[post_id])
                csv_index_writer.writerow([user["uid"],post_id])


        #scores = [(p,candidate_posts[p]) for p in recommendedPosts[:100]]
        output = " ".join(recommendedPosts[:100]) + "\n"
        #print output
        #file_scores_dump.write(','.join([str(n) for n in scores])+'\n')
        #print " ".join([str(i) for i in user_predictions])
        file_out_submissions.write(output)




        if user_total % 100 == 0:
            logging.info("User " + str(user_total) + " out of 16262")