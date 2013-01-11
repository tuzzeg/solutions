import json
import logging
import string
import cPickle
from gensim import corpora
from Corp import strip_tags, stop_words

logger = logging.getLogger('config_and_pickle')

base_dir = '~/gom'

pickle_dir = base_dir + '/pickle'

trainPostsThin_loc = base_dir + '/download/trainPostsThin.json'
trainPosts_loc = base_dir + '/download/trainPosts.json'

testPosts_loc = base_dir + '/download/testPosts.json'

test_loc = base_dir + '/download/test.csv'
sub_loc = base_dir + '/sub'
generated_loc = base_dir + '/generated'

rare_term_document_frequency_threshold = 50
#lines_to_read = 1 * 1000
lines_to_read = -1
topic_count = 100

count_normalize_content_none      = 0
count_normalize_content_ok        = 0
count_normalize_content_exception = 0

def pickle(obj, file_name):
    logger.info('pickling %s...' % (file_name))
    # 'b' for binary, needed on Windows
    with open(pickle_dir + '/' + file_name, "wb") as fout:
        cPickle.dump(obj, fout, protocol=-1)
    logger.info('pickled %s' % (file_name))

def unpickle(file_name):
    logger.info('unpickling %s...' % (file_name))
    with open(pickle_dir + '/' + file_name, "rb") as fin:
        obj = cPickle.load(fin)
    logger.info('unpickled %s' % (file_name))
    return obj

def normalize_content_stats():
    print '________________________________________________________________________'
    print 'normalize_content_stats()'
    print '    none = ', count_normalize_content_none
    print '    ok = ', count_normalize_content_ok
    print '    exception = ', count_normalize_content_exception
    print '________________________________________________________________________'

def normalize_content(content):
    global count_normalize_content_none
    global count_normalize_content_ok
    global count_normalize_content_exception

    if count_normalize_content_ok % (100 * 1000) == 0:
        normalize_content_stats()

    try:
        if content is None:
            count_normalize_content_none += 1
            return 'none'

        count_normalize_content_ok += 1
        return strip_tags(content).encode('ascii', 'ignore').translate(None, string.punctuation).lower()
    except:
        count_normalize_content_exception += 1
        print 'normalize_content error: content = ', content
        return 'abc def'

def build_word_id_map(file_names):
    #http://radimrehurek.com/gensim/corpora/dictionary.html
    #print Dictionary(['abc def ghi'.split(), 'ew fjker fdfd'.split()])
    my_files_iterator = MyFilesIterator(file_names)
    word_id_map = corpora.dictionary.Dictionary(my_files_iterator)
    print 'len(word_id_map.keys()) = ', int(len(word_id_map.keys()))

    #remove rare terms
    #Dictionary.dfs = {} # document frequencies: tokenId -> in how many documents this token appeared
    rare_token_ids = [token_id for token_id, document_frequency in word_id_map.dfs.iteritems() if (document_frequency < rare_term_document_frequency_threshold)]
    word_id_map.filter_tokens(rare_token_ids)
    print 'len(rare_token_ids) = ', len(rare_token_ids)

    #remove stop words
    #self.id2token = {} # reverse mapping for token2id; only formed on request, to save memory
    stop_word_token_ids = [token_id for token_id in word_id_map.keys() if (word_id_map[token_id] in stop_words)]
    word_id_map.filter_tokens(stop_word_token_ids)
    print 'len(stop_word_token_ids) = ', len(stop_word_token_ids)

    #Assign new word ids to all words.
    #This is done to make the ids more compact, e.g. after some tokens have been removed via filter_tokens() and there are gaps in the id series. Calling this method will remove the gaps.
    word_id_map.compactify()
    return word_id_map


class MyFilesIterator:
    def __init__(self, file_names):
        self.file_names = file_names

    def __iter__(self):
        for line_text in self._iterate_through_files():
        #            print '\n'
        #            print 'line_text   = ', line_text

            blog_json = json.loads(line_text)
            yield normalize_content(blog_json['content']).split()

    def iterate_fields(self):
        for line_text in self._iterate_through_files():
            blog_json = json.loads(line_text)
            yield blog_json['blog'], blog_json['post_id'], blog_json['likes'], normalize_content(blog_json['content']).split()

    def iterate_fields_no_likes(self):
        for line_text in self._iterate_through_files():
            blog_json = json.loads(line_text)
            yield blog_json['blog'], blog_json['post_id'], normalize_content(blog_json['content']).split()

    def _iterate_through_files(self):
        for file_name in self.file_names:
            with open(file_name, 'r') as f:
                for line_number, line_text in enumerate(f):
                    if lines_to_read != -1 and line_number >= lines_to_read:
                        break

                    yield line_text

#http://radimrehurek.com/gensim/interfaces.html
#A corpus is simply an iterable, where each iteration step yields one document:
#A document is a sequence [Python list] of (fieldId, fieldValue) 2-tuples:
class MyCorpus:
    def __init__(self, file_names, word_id_map):
        """
        word_id_map: gensim.corpora.dictionary.Dictionary
        """
        self.file_names = file_names
        self.word_id_map = word_id_map

    def __iter__(self):
        my_files_iterator = MyFilesIterator(self.file_names)
        for blog_content_as_list_of_words in my_files_iterator:
            #Convert document (a list of words) into the bag-of-words format = list of (token_id, token_count) 2-tuples.
            yield self.word_id_map.doc2bow(blog_content_as_list_of_words)

    #http://radimrehurek.com/gensim/interfaces.html
    #Note that although a default len() method is provided, it is very inefficient (performs a linear scan through the corpus to determine its length).
    def __len__(self):
        n = 0
        my_files_iterator = MyFilesIterator(self.file_names)
        for blog_content_as_list_of_words in my_files_iterator._iterate_through_files():
            n += 1

        return n





