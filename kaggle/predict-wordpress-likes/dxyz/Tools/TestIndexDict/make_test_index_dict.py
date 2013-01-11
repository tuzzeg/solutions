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

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

DataSet = 'my'

infolder = '../../'+DataSet+'Data/'

testindexdict_savedfolder = DataSet+'Saved/'

testPostsFile = infolder+DataSet+"testPosts.json"

try:
    open(testindexdict_savedfolder+'TestIndexDict.saved','r')
    open(testindexdict_savedfolder+'TestPostList.saved','r')

except:
    testPostIds = []
    testPostIndices = {}
    with open(testPostsFile) as f:
        for i, line in enumerate(f):
            post = json.loads(line)
            blog_id = post["blog"]
            post_id = post["post_id"]
            testPostIds.append(post_id)
            # testPostIndices is the dict that maps post_id into the index in vectorized LDA/tfidf already constructed
            testPostIndices[post_id] = i

    cPickle.dump(testPostIndices, open(testindexdict_savedfolder+'TestIndexDict.saved', "w"))
    cPickle.dump(testPostIds, open(testindexdict_savedfolder+'TestPostList.saved', "w"))

