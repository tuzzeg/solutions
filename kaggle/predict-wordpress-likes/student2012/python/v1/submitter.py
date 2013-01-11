from __future__ import division
from collections import defaultdict
from Corp import stop_words, Files, Corp
from gensim import corpora, models, similarities
import logging
import json
import cPickle
import random

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

# First, we make a dictionary of words used in the posts
with Files([open("../trainPosts.json"), open("../testPosts.json")]) as myFiles:
    try:
        dictionary = corpora.dictionary.Dictionary.load("dictionary.saved")
    except:
        dictionary = corpora.Dictionary(doc for doc in myFiles)
        stop_ids = [dictionary.token2id[stopword] for stopword in stop_words if stopword in dictionary.token2id]
        infreq_ids = [tokenid for tokenid, docfreq in dictionary.dfs.iteritems() if docfreq < 50]
        dictionary.filter_tokens(stop_ids + infreq_ids) # remove stop words and words that appear infrequently
        dictionary.compactify() # remove gaps in id sequence after words that were removed

        dictionary.save("dictionary.saved")

    # Next, we train the LDA model with the blog posts, estimating the topics
    try:
        lda = models.ldamodel.LdaModel.load("lda.saved")
    except:
        lda = models.ldamodel.LdaModel(corpus=Corp(myFiles, dictionary), id2word=dictionary, num_topics=100, update_every=1, chunksize=10000, passes=1)

        lda.save("lda.saved")

# Now, we do some quick preliminary work to determine which blogs have which posts, and to map post_id's to a zero-based index, or vice versa

trainPostIndices = {}
blogTrainPosts = defaultdict(list)
with open("../trainPostsThin.json") as f:
    for i, line in enumerate(f):
        post = json.loads(line)
        blog_id = post["blog"]
        post_id = post["post_id"]
        trainPostIndices[post_id] = i
        blogTrainPosts[blog_id].append(post_id)

logging.info("Done doing preliminary training data processing")

testPostIds = []
testPostIndices = {}
blogTestPosts = defaultdict(list)
with open("../testPostsThin.json") as f:
    for i, line in enumerate(f):
        post = json.loads(line)
        blog_id = post["blog"]
        post_id = post["post_id"]
        testPostIds.append(post_id)
        testPostIndices[post_id] = i
        blogTestPosts[blog_id].append(post_id)

logging.info("Done doing preliminary test data processing")

# We build a lookup-index of test posts, for quick answers to questions about what test posts are similar to a given training post

try:
    testVecs = cPickle.load(open("TestVecs.saved", "r"))
    testIndex = similarities.Similarity.load("TestIndex.saved")
except:
    with Files([open("../testPosts.json")]) as myFilesTest:
        myCorpTest = Corp(myFilesTest, dictionary)
        testVecs = [vec for vec in lda[myCorpTest]]
        testIndex = similarities.Similarity("./simDump/", testVecs, num_features=100)
        testIndex.num_best = 100
    cPickle.dump(testVecs, open("TestVecs.saved", "w"))
    testIndex.save("TestIndex.saved")

logging.info("Done making the test lookup index")

# We estimate the training topics, which we can hold in memory since they are sparsely coded in gensim
try:
    TrainVecs = cPickle.load(open("TrainVecs.saved", "r"))
except:
    with Files([open("../trainPosts.json")]) as myFilesTrain:
        myCorpTrain = Corp(myFilesTrain, dictionary)
        trainVecs = [vec for vec in lda[myCorpTrain]]
    cPickle.dump(trainVecs, open("TrainVecs.saved", "w"))

logging.info("Done estimating the training topics")

# Now we begin making submissions
logging.info("Beginning to make submissions")
with open("../trainUsers.json", "r") as users, open("submissions.csv", "w") as submissions:
    submissions.write("\"posts\"\n")
    for user_total, line in enumerate(users):
        user = json.loads(line)
        if not user["inTestSet"]:
            continue

        blog_weight = 2.0
        posts = defaultdict(int) # The potential posts to recommend and their scores

        liked_blogs = [like["blog"] for like in user["likes"]]
        for blog_id in liked_blogs:
            for post_id in blogTestPosts[blog_id]:
                posts[post_id] += blog_weight / len(blogTestPosts[blog_id])
            # After this, posts[post_id] = (# times blog of post_id was liked by user in training) / (# posts from blog of post_id in training)
        posts_indices = [testPostIndices[post_id] for post_id in posts]
        posts_vecs = [testVecs[i] for i in posts_indices]

        liked_post_indices = []
        for like in user["likes"]:
            try: # For whatever reason, there is a slight mismatch between posts liked by users in trainUsers.json, and posts appearing in trainPosts.json
                liked_post_indices.append(trainPostIndices[like["post_id"]])
            except:
                logging.warning("Bad index!")

        total_likes = len(liked_post_indices)
        sample_size = min(10, total_likes)
        liked_post_indices = random.sample(liked_post_indices, sample_size) # to cut down computation time
        liked_post_vecs = [trainVecs[i] for i in liked_post_indices]
        likedPostIndex = similarities.SparseMatrixSimilarity(liked_post_vecs, num_terms=100)

        for posts_index, similar in zip(posts_indices, likedPostIndex[posts_vecs]):
            posts[testPostIds[posts_index]] += max([rho for rho in similar])
            # ie, posts[post_id] += max(semantic similarities to sample of previously liked posts)

        if len(posts) < 100: # Fill up remaining spaces with posts semantically similar to previously liked posts, (almost always from different blogs)
            similar_posts_ids  = [(testPostIds[i], rho) for similar100 in testIndex[liked_post_vecs] for i, rho in similar100]
            for post_id, rho in similar_posts_ids:
                posts[post_id] += rho / sample_size
                # dividing by the sample size ensures that the biggest additional score a post could get from this is 1.0

        # Now pick the top 100 blogs, (or less if that's the case)
        recommendedPosts = list(sorted(posts, key=posts.__getitem__, reverse=True))
        output = " ".join(recommendedPosts[:100]) + "\n"
        submissions.write(output)

        if user_total % 100 == 0:
            logging.info("User " + str(user_total) + " out of 16262")