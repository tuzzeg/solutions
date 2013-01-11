from collections import defaultdict
import json
import logging
from gensim.models.ldamodel import LdaModel
from v1.config_and_pickle import testPosts_loc, MyFilesIterator, trainPosts_loc, MyCorpus, build_word_id_map, normalize_content_stats, topic_count, pickle, trainPostsThin_loc

################################################################################################################################################
logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)
logger = logging.getLogger('LDA_model_builder')
################################################################################################################################################
logger.info('building word_id_map...')
word_id_map = build_word_id_map([trainPosts_loc, testPosts_loc])
pickle(word_id_map, 'word_id_map')
normalize_content_stats()

train_and_test_corpus = MyCorpus([trainPosts_loc, testPosts_loc], word_id_map)
logger.info('training LDA model...')
#id2word is a mapping from word ids (integers) to words (strings). It is used to determine the vocabulary size, as well as for debugging and topic printing.
lda = LdaModel(train_and_test_corpus, id2word=word_id_map, num_topics=topic_count, update_every=1, chunksize=10000, passes=1)
pickle(lda, 'lda')

#Print the 'topn' most probable words for (randomly selected) 'topics' number of topics. Set topics=-1 to print all topics.
lda.show_topics(topics=topic_count, topn=10)
################################################################################################################################################
#key = blog + '_' + post_id
#value = a list of (topic_id, topic_probability) 2-tuples
blog_topic_distribution_map = {}

#key = uid (user id)
#value = list of (blog, post_id) tuples
train_user_likes_map = defaultdict(list)

#key = blog
#value = list of post_ids
test_blog_post_map = defaultdict(list)

logger.info('starting LDA prediction for training data...')
for blog, post_id, likes, blog_content_as_list_of_words in MyFilesIterator([trainPosts_loc]).iterate_fields():
    blog_as_bow = word_id_map.doc2bow(blog_content_as_list_of_words)

    #blog_topic_distribution: a list of (topic_id, topic_probability) 2-tuples
    blog_topic_distribution = lda[blog_as_bow]
    blog_topic_distribution_map['' + blog + '_' + post_id] = blog_topic_distribution
    for like in likes:
        train_user_likes_map[like['uid']] += [(blog, post_id)]
#        print 'c'
#    print 'd'

logger.info('starting LDA prediction for test data...')
for blog, post_id, blog_content_as_list_of_words in MyFilesIterator([testPosts_loc]).iterate_fields_no_likes():
    blog_as_bow = word_id_map.doc2bow(blog_content_as_list_of_words)

    #blog_topic_distribution: a list of (topic_id, topic_probability) 2-tuples
    blog_topic_distribution = lda[blog_as_bow]
    blog_topic_distribution_map['' + blog + '_' + post_id] = blog_topic_distribution

    test_blog_post_map[blog] += [post_id]
#    print 'b'

pickle(blog_topic_distribution_map, 'blog_topic_distribution_map')
pickle(train_user_likes_map, 'train_user_likes_map')
pickle(test_blog_post_map, 'test_blog_post_map')

#print 'a'
################################################################################################################################################
#key = blog
#value = number of posts
train_blog_post_count_map = defaultdict(int)
for line_text in MyFilesIterator([trainPostsThin_loc])._iterate_through_files():
    blog_json = json.loads(line_text)
    blog = blog_json['blog']
    train_blog_post_count_map[blog] += 1

pickle(train_blog_post_count_map, 'train_blog_post_count_map')
