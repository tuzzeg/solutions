'''
Created on Oct 10, 2012

@author: kingsfield
'''
import os, sys
_root_dir = os.path.dirname(os.path.dirname(os.path.realpath(__file__)))
if not _root_dir in sys.path:
    sys.path.insert(0, _root_dir)
    
import time
from collections import defaultdict
from util import readfile, writefile, get_words
from constants import *
import multiprocessing

clicked_map = dict()

def init():
    with open(new_train_file) as fr:
        raw_data = fr.readlines()
    raw_data = raw_data[1:]
    for line in raw_data:
        user, sku, __category, raw_query, __click_time = line.split(',')
        if user not in clicked_map:
            clicked_map[user] = dict()
        if raw_query not in clicked_map[user]:
            clicked_map[user][raw_query] = list()
        clicked_map[user][raw_query].append(sku)

def rerank_guess(guesses, user, query):
    if user in clicked_map and query in clicked_map[user]:
        clicked_skus = clicked_map[user][query]
        clicked = []
        res = []
        for sku in guesses:
            if sku in clicked_skus:
                clicked.append(sku)
            else:
                res.append(sku)
        res.extend(clicked)
        return res
    else:
        return guesses

def get_time_feature(t):
    return min(int(t) / block, MAX_BLOCK)

def get_pair(lt):
    'lt must be sorted before'
    size = len(lt)
    return [str(lt[i]) + '_' + str(lt[j]) for i in xrange(size) for j in xrange(i + 1, size)]

def get_bigram_word(raw_query, hot_words, cat):
    words = get_words(raw_query)
    words = [w for w in words if w in hot_words[cat]]
    words.sort()
    bigram = get_pair(words)
    return bigram

'***********************************************************************'
'******************************training*******************************'
'***********************************************************************'
def get_item_count():
    reader = readfile(new_train_file)
    item_count = defaultdict(lambda: defaultdict(int))
    time_item_count = defaultdict(lambda:defaultdict(lambda: defaultdict(int)))
    idx = 0
    for (__user, sku, category, __query, click_time) in reader:
        time_block = get_time_feature(click_time)
        idx += 1
        item_count[category][sku] += magic_num
        time_item_count[time_block][category][sku] += magic_num
    item_sort = dict()
    for category in item_count:
        item_sort[category] = sorted(item_count[category].items(), \
                                      key=lambda x: x[1], reverse=True)
    smooth_time_item_count = defaultdict(lambda:defaultdict(lambda: defaultdict(int)))
    for time_block in time_item_count:
        for cat in time_item_count[time_block]:
            for sku in time_item_count[time_block][cat]:
                smooth_time_item_count[time_block][cat][sku] = item_count[cat][sku] * 3.0 / block_size
    for time_block in time_item_count:
        for cat in time_item_count[time_block]:
            for sku in time_item_count[time_block][cat]:
                smooth_time_item_count[time_block][cat][sku] = time_item_count[time_block][cat][sku]
                if time_block == 0 or time_block == MAX_BLOCK:
                    smooth_time_item_count[time_block][cat][sku] += time_item_count[time_block][cat][sku]
                if time_block >= 1:
                    smooth_time_item_count[time_block][cat][sku] += time_item_count[time_block - 1][cat][sku]
                if time_block < MAX_BLOCK:
                    smooth_time_item_count[time_block][cat][sku] += time_item_count[time_block + 1][cat][sku]
    return item_count, item_sort, smooth_time_item_count
    
def get_cat_statistic(item_sort, time_item_count):
    cat_count = defaultdict(lambda: defaultdict(int))
    for cat in item_sort:
        sum_query = sum([i[1] for i in item_sort[cat]])
        hot_query = 0
        idx = 0
        __jdx = 0
        sum_size = len(item_sort[cat])
        while True:
            if idx >= sum_size or item_sort[cat][idx][1] < GLOBAL_QUERY:
                break
            hot_query += item_sort[cat][idx][1]
            idx += 1
        if idx < 5:
            idx = 5 
        print '--hot size=%d' % idx
        cat_count[cat][HOT_SIZE] = idx
        cat_count[cat][SUM_SIZE] = sum_size
        cat_count[cat][HOT] = hot_query
        cat_count[cat][SUM] = sum_query
        cat_count[cat][PREDICT_HOT_SIZE] = idx
    for t in xrange(block_size):
        for cat in item_sort:
            '''TODO: pay attention, this is not correct in logic, but I write this for saving time'''
            cat_count[cat][t] = cat_count[cat][SUM]
    return cat_count    

def get_unigram_model(item_sort, cat_count):
    reader = readfile(new_train_file)
    item_word = defaultdict(lambda: defaultdict(lambda: defaultdict(int)))
    cat_word = defaultdict(lambda: defaultdict(int))
    idx = 0
    for (__user, sku, category, raw_query, ___click_time) in reader:
        idx += 1
        bound = cat_count[category][HOT_SIZE]
        popular = [i[0] for i in item_sort[category][0:bound]]
        if sku in popular:
            words = get_words(raw_query)
            for w in words:
                item_word[category][sku][w] += magic_num
                cat_word[category][w] += magic_num
    return item_word, cat_word

def get_bigram_model(item_word, item_sort, cat_count):
    hot_sku_words = defaultdict(lambda: defaultdict(set))
    for cat in item_word:
        for sku in item_word[cat]:
            hots = item_word[cat][sku].items()
            hot_sku_words[cat][sku] = set([i[0] for i in hots if i[1] >= GLOBAL_BIGRAM_QUERY])
    
    hot_words = dict() 
    for cat in hot_sku_words:
        hot_words[cat] = set()
        for sku in hot_sku_words[cat]:
            hot_words[cat] = hot_words[cat].union(hot_sku_words[cat][sku])
            
    reader = readfile(new_train_file)
    bigram_item_word = defaultdict(lambda: defaultdict(lambda: defaultdict(int)))
    idx = 0
    for (__user, sku, category, raw_query, ___click_time) in reader:
        idx += 1
        bound = cat_count[category][HOT_SIZE]
        popular = [i[0] for i in item_sort[category][0:bound]]
        if sku in popular:
            bigram = get_bigram_word(raw_query, hot_words, category)
            for w in bigram:
                bigram_item_word[category][sku][w] += magic_num
                cat_count[category][BIGRAM_HOT] += magic_num
            
    return bigram_item_word, cat_count, hot_words

'***********************************************************************'
'******************************prediction*******************************'
'***********************************************************************'
def plain_bayes_query_prediction(words, cat, sku, alpha, beta, item_word, item_count, cat_count):
    """predict the probability of click under this raw_query"""
    cat_c = cat_count[cat][HOT]
    p_i = (item_count[cat][sku] + alpha) * 1.0 / (cat_c + beta)
    p = p_i
    for w in words:
        p_wi = (item_word[cat][sku].get(w, 0) + alpha) * 1.0 / (cat_c + beta)
        p *= p_wi / p_i
    return p 

def time_bayes_bigram_prediction(bigram, cat, sku, alpha, beta, bigram_item_word, item_count, cat_count, month_cat_item_dict, t):
    """predict the probability of click under this raw_query"""
    cat_m = cat_count[cat][t]
    p_m = (month_cat_item_dict[t][cat][sku] + alpha) * 1.0 / (cat_m + beta)
    cat_c = cat_count[cat][HOT]
    cat_bigram_c = cat_count[cat][BIGRAM_HOT]
    p_i = (item_count[cat][sku] + alpha) * 1.0 / (cat_c + beta)
    p = p_m
    for w in bigram:
        p_wi = (bigram_item_word[cat][sku].get(w, 0) + alpha) * 1.0 / (cat_bigram_c + beta)
        p *= p_wi / p_i
    return p 

def time_bayes_query_prediction(words, cat, sku, alpha, beta, item_word, item_count, cat_count, month_cat_item_dict, t):
    """predict the probability of click under this raw_query"""
    cat_m = cat_count[cat][t]
    cat_c = cat_count[cat][HOT]
    p_m = (month_cat_item_dict[t][cat][sku] + alpha) * 1.0 / (cat_m + beta)
    p_i = (item_count[cat][sku] + alpha) * 1.0 / (cat_c + beta) 
    p = p_m
    for w in words:
        p_wi = (item_word[cat][sku].get(w, 0) + alpha) * 1.0 / (cat_c + beta)
        p *= p_wi / p_i
    return p 

def boosting_bayes(bigram, words, cat, sku, alpha, beta, item_word, bigram_item_word, item_count, cat_count, month_cat_item_dict, t):
    p1 = time_bayes_query_prediction(words, cat, sku, alpha, beta, item_word, item_count, cat_count, month_cat_item_dict, t)
    p2 = time_bayes_bigram_prediction(bigram, cat, sku, alpha, beta, bigram_item_word, item_count, cat_count, month_cat_item_dict, t)
    return w1 * p1 + w2 * p2

def make_predictions(st_line, ed_line, out_file, pname, models):
    cat_count, item_count, item_sort, alpha, beta, item_word, bigram_item_word, time_cat_item_dict, cat_word, hot_words = models[0]
    reader = readfile(new_test_file)
    writer = writefile(out_file)
    line_idx = 0
    for (user, category, raw_query, click_time) in reader:
        line_idx += 1
        if line_idx < st_line:
            continue
        if line_idx > ed_line:
            break
        if line_idx % TEST_STEP == 0:
            print '%s--%d' % (pname, line_idx / TEST_STEP)
        time_block = get_time_feature(click_time)
        try:
            bound = cat_count[category][PREDICT_HOT_SIZE]
            hots = [x[0] for x in item_sort[category][0:bound]]
        except:
            writer.writerow(["0"])
            continue
        try:
            bigram = get_bigram_word(raw_query, hot_words, category)
            words = get_words(raw_query)
            query_size = sum([cat_word[category][w] for w in words])
            if query_size >= 100 and len(bigram) > 0:
                'only queries hot enough and can generate bigram features can be predicted by boosting model'
                rank = [[sku, boosting_bayes(bigram, words, category, sku, alpha, beta, item_word, bigram_item_word, item_count, cat_count, time_cat_item_dict, time_block)] for sku in hots]
            elif query_size >= 100 and len(bigram) == 0:
                'if hot enough but can not generate bigram features then use naive bayes with time information'
                rank = [[sku, time_bayes_query_prediction(words, category, sku, alpha, beta, item_word, item_count, cat_count, time_cat_item_dict, time_block)] for sku in hots]
            else:
                'otherwise use plain naive bayes'
                rank = [[sku, plain_bayes_query_prediction(words, category, sku, alpha, beta, item_word, item_count, cat_count)] for sku in hots]
            rank = sorted(rank, key=lambda x:x[1], reverse=True)
            guesses = [i[0] for i in rank[0:5]]
            guesses = rerank_guess(guesses, user, raw_query)
            
            writer.writerow([" ".join(guesses)])
        except (TypeError, KeyError): # a category we haven't seen before
            writer.writerow([" ".join(hots[0:5])])

def mulproc_param(buffer_path, file_name, proc_size):
    sum_line = MAX_TEST_LINE
    block = int(sum_line / proc_size)
    ret = list()
    for idx in xrange(proc_size):
        filename = os.path.join(buffer_path, '%s_%d.csv' % (file_name, idx)) 
        pname = '--p_%d' % idx
        st = idx * block + 1
        ed = (idx + 1) * block if idx != (proc_size - 1) else sum_line
        ret.append([filename, st, ed, pname])
    return ret          

def synout(outfile, inbasepath, file_name, proc_size):
    params = mulproc_param(inbasepath, file_name, proc_size)
    with open(outfile, 'w') as  fw:
        fw.write('sku\n')
        for idx in xrange(proc_size):
            filename, __st, __ed, __pname = params[idx]
            with open(filename) as fr:
                line = fr.readline()
                while True:
                    line = fr.readline()
                    if not line:
                        break
                    fw.write(line[0:-2] + '\n')

def main():
    params = mulproc_param(out_buffer_path, 'buffer', proc_size)
    init()
    st_time = time.time()
    models = list()
    item_count, item_sort, month_item_count = get_item_count()
    cat_count = get_cat_statistic(item_sort, month_item_count)
    item_word, cat_word = get_unigram_model(item_sort, cat_count)
    bigram_item_word, cat_count, hot_words = get_bigram_model(item_word, item_sort, cat_count)
    models.append([cat_count, item_count, item_sort, 1, 100, item_word, bigram_item_word, month_item_count, cat_word, hot_words])
    ed_time = time.time()
    print 'train cost=%f' % (ed_time - st_time)
    
    st_time = time.time()    
    pool = list()
    for pdx in xrange(proc_size):
        filename, st, ed, pname = params[pdx]
        p = multiprocessing.Process(target=make_predictions, args=(st, ed, filename, pname, models))
        p.start()
        pool.append(p)
        
    for pdx in xrange(proc_size):
        p = pool[pdx]
        p.join()
    ed_time = time.time()
    print 'predict cost=%f' % (ed_time - st_time)
    synout(out_file, out_buffer_path, 'buffer', proc_size)

if __name__ == '__main__':
    main()
    print 'done'
