from __future__ import division
from collections import defaultdict
from category_corp import stop_words, Files, Corp
from gensim import corpora, models, similarities
import logging
import json
import cPickle
import random

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

DataSet = ''

infolder = '../../'+DataSet+'Data/'
outfolder = '../../'+DataSet+'Submissions/'
savedfolder = DataSet+"Saved/"

testPostsFile = infolder+DataSet+"testPosts.json"
trainPostsFile = infolder+DataSet+"trainPosts.json"

# First, we make a dictionary of words used in the titles
with Files([open(trainPostsFile), open(testPostsFile)]) as myFiles:
    try:
        dictionary = corpora.dictionary.Dictionary.load(savedfolder+"dictionary.saved")

    except:
        dictionary = corpora.Dictionary(doc for doc in myFiles)
        stop_ids = [dictionary.token2id[stopword] for stopword in stop_words if stopword in dictionary.token2id]
        #infreq_ids = [tokenid for tokenid, docfreq in dictionary.dfs.iteritems() if docfreq < 50]
        #dictionary.filter_tokens(stop_ids + infreq_ids) # remove stop words and words that appear infrequently
        dictionary.filter_tokens(stop_ids)
        dictionary.compactify() # remove gaps in id sequence after words that were removed

        dictionary.save(savedfolder+"dictionary.saved")

    # Next, we train the LDA model with the blog posts, estimating the topics
    try:
        tfidf = models.tfidfmodel.TfidfModel.load(savedfolder+"tfidf.saved")
    except:
        tfidf = models.tfidfmodel.TfidfModel(corpus=Corp(myFiles, dictionary), id2word=dictionary, normalize = True)

        tfidf.save(savedfolder+"tfidf.saved")


try:
    testVecs = cPickle.load(open(savedfolder+"TestVecs.saved", "r"))

except:
    with Files([open(testPostsFile)]) as myFilesTest:
        myCorpTest = Corp(myFilesTest, dictionary)
        testVecs = [vec for vec in tfidf[myCorpTest]]

    cPickle.dump(testVecs, open(savedfolder+"TestVecs.saved", "w"))


logging.info("Done making the test lookup index")

# We estimate the training topics, which we can hold in memory since they are sparsely coded in gensim
try:
    trainVecs = cPickle.load(open(savedfolder+"TrainVecs.saved", "r"))
except:
    with Files([open(trainPostsFile)]) as myFilesTrain:
        myCorpTrain = Corp(myFilesTrain, dictionary)
        trainVecs = [vec for vec in tfidf[myCorpTrain]]
    cPickle.dump(trainVecs, open(savedfolder+"TrainVecs.saved", "w"))

logging.info("Done estimating the training topics")
