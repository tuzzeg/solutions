'''
Created on Oct 10, 2012

@author: kingsfield
'''
import os, sys
_root_dir = os.path.dirname(os.path.dirname(os.path.realpath(__file__)))
if not _root_dir in sys.path:
    sys.path.insert(0, _root_dir)

from nltk.stem import WordNetLemmatizer
from constants import train_file, test_file, new_train_file, new_test_file, st_date
from util import readfile, writefile, get_words
from datetime import datetime

def parse_datetime(click_time):
    try:
        t = datetime.strptime(click_time, '%Y-%m-%d %H:%M:%S.%f')
    except:
        t = datetime.strptime(click_time, '%Y-%m-%d %H:%M:%S')
    return t

def get_new_time(t):
    new_time = str((parse_datetime(t) - st_date).days)
    return new_time 

def split_word_num(word):
    if word.isdigit():
        return word
    if word[-1].isalpha():
        return word
    for idx in xrange(len(word) - 1, 0, -1):
        if not (word[idx].isdigit() and word[idx - 1].isdigit()):
            break
    if idx <= 1:
        return word
    return word[:idx], word[idx:]

def get_split(w):
    if not w.isdigit() and not w.isalpha():
        split = split_word_num(w)
        return split
    else:
        return w

def correct_query(raw_query, lemmatizer, local_cache):
    raw_query = raw_query.lower().strip()
    if raw_query in local_cache:
        return local_cache[raw_query]
    words = get_words(raw_query)
    new_words = list()
    for w in words:
        split = get_split(w)
        if type(split) == type(()):
            new_words.extend(list(split))
        else:
            new_words.append(split)
    new_query = ''
    for w in new_words:
        lemma = lemmatizer.lemmatize(w)
        if len(lemma) >= 4 and not lemma.isdigit() and not lemma.isalpha():
            split = split_word_num(w)
            if type(split) == type(()):
                w, num = split
                lemma = ' '.join([w, num])
        new_query += lemma + ' '
    new_query = new_query[0:-1]
    local_cache[raw_query] = new_query
    return new_query

def make_query_correct(target_file, out_file, tp):
    local_cache = dict()
    lemmatizer = WordNetLemmatizer()
    reader = readfile(target_file)
    with open(out_file, 'w') as writer:
        writer.write('data:\n')
        if tp == 'train':
            'we do not use query_time here'
            for (user, sku, category, raw_query, click_time, __query_time) in reader:
                new_query = correct_query(raw_query, lemmatizer, local_cache)
                new_click_time = get_new_time(click_time) 
                outline = ','.join([user, sku, category, new_query, new_click_time])
                writer.write(outline + '\n')
        elif tp == 'test':
            'we do not use query_time here'
            for (user, category, raw_query, click_time, __query_time) in reader:
                new_query = correct_query(raw_query, lemmatizer, local_cache)
                new_click_time = get_new_time(click_time)
                outline = ','.join([user, category, new_query, new_click_time])
                writer.write(outline + '\n')
        else:
            raise Exception('Error Query Correction Request!!!')


if __name__ == '__main__':
    make_query_correct(train_file, new_train_file, 'train')
    make_query_correct(test_file, new_test_file, 'test')
    print 'done'
