from __future__ import division
import sys
#sys.path.append('../Benchmarks/wordpress-LDA')


from collections import defaultdict,Counter
#from corp import stop_words, Files, Corp
from gensim import corpora, models, similarities
import logging
import json
import cPickle
import random
import time
import csv

LIKED_POST_SAMPLE_SIZE = 50

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

DataSet = 'my'

infolder = '../../'+DataSet+'Data/'
outfolder = DataSet+'Saved/'


testPostsFile = infolder+DataSet+"testPosts.json"
trainPostsFile = infolder+DataSet+"trainPosts.json"
trainUsersFile = infolder+DataSet+"trainUsers.json"


try:
    open(outfolder+'StandingQueries.saved','r')
except:
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
    number_of_users = 0
    total_number_of_likes = 0

    with open(trainUsersFile, "r") as users, open(outfolder+'StandingQueries.saved',"w") as outfile:
        csv_writer = csv.writer(outfile)
        #TODO for final submission think of including posts from test set as well (for 'new' topics that occur in test set)
        for user_total, line in enumerate(users):
            user = json.loads(line)
            if not user["inTestSet"]:
                continue
            number_of_users += 1

            liked_post_indices = []
            for like in user["likes"]:
                try: # For whatever reason, there is a slight mismatch between posts liked by users in trainUsers.json, and posts appearing in trainPosts.json
                    liked_post_indices.append(trainPostIndices[like["post_id"]])
                except:
                    logging.warning("Bad index!")


            total_likes = len(liked_post_indices)
            total_number_of_likes += total_likes

            sample_size = min(LIKED_POST_SAMPLE_SIZE, total_likes)
            liked_post_indices = random.sample(liked_post_indices, sample_size) # to cut down computation time

            csv_writer.writerow(liked_post_indices)

    print "Average number of likes:",float(total_number_of_likes)/float(number_of_users)

