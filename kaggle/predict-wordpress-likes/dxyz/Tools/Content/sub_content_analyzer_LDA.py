from __future__ import division
import sys
#sys.path.append('../Benchmarks/wordpress-LDA')


from collections import defaultdict
from corp import stop_words, Files, Corp
from gensim import corpora, models, similarities
import logging
import json
import cPickle
import random
LDA_NUM_TOPICS = 200

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

DataSet = ''

infolder = '../../'+DataSet+'Data/'
outfolder = '../../'+DataSet+'Submissions/'
savedfolder = DataSet+"Saved/"

testPostsFile = infolder+DataSet+"testPosts.json"
trainPostsFile = infolder+DataSet+"trainPosts.json"

# First, we make a dictionary of words used in the posts
with Files([open(trainPostsFile), open(testPostsFile)]) as myFiles:
    try:
        dictionary = corpora.dictionary.Dictionary.load(savedfolder+"dictionary.saved")
    except:
        dictionary = corpora.Dictionary(doc for doc in myFiles)
        stop_ids = [dictionary.token2id[stopword] for stopword in stop_words if stopword in dictionary.token2id]
        infreq_ids = [tokenid for tokenid, docfreq in dictionary.dfs.iteritems() if docfreq < 50]
        dictionary.filter_tokens(stop_ids + infreq_ids) # remove stop words and words that appear infrequently
        dictionary.compactify() # remove gaps in id sequence after words that were removed

        dictionary.save(savedfolder+"dictionary.saved")


    # Next, we train the LDA model with the blog posts, estimating the topics
    try:
        lda = models.ldamodel.LdaModel.load(savedfolder+"lda.saved")
    except:
        lda = models.ldamodel.LdaModel(corpus=Corp(myFiles, dictionary), id2word=dictionary, num_topics=LDA_NUM_TOPICS, update_every=1, chunksize=10000, passes=1)
        lda.save(savedfolder+"lda.saved")

try:
    testVecs = cPickle.load(open(savedfolder+"TestVecs.saved", "r"))
    #testIndex = similarities.Similarity.load(savedfolder+"TestIndex.saved")
except:
    with Files([open(infolder+DataSet+"testPosts.json")]) as myFilesTest:
        myCorpTest = Corp(myFilesTest, dictionary)
        testVecs = [vec for vec in lda[myCorpTest]]
        #testIndex = similarities.Similarity("./simDump/", testVecs, num_features=LDA_NUM_TOPICS)
        #testIndex.num_best = 100
    cPickle.dump(testVecs, open(savedfolder+"TestVecs.saved", "w"))
    #testIndex.save(savedfolder+"TestIndex.saved")

logging.info("Done making the test lookup index")

# We estimate the training topics, which we can hold in memory since they are sparsely coded in gensim
try:
    trainVecs = cPickle.load(open(savedfolder+"TrainVecs.saved", "r"))
except:
    with Files([open(trainPostsFile)]) as myFilesTrain:
        myCorpTrain = Corp(myFilesTrain, dictionary)
        trainVecs = [vec for vec in lda[myCorpTrain]]
    cPickle.dump(trainVecs, open(savedfolder+"TrainVecs.saved", "w"))

logging.info("Done estimating the training topics")
exit()
