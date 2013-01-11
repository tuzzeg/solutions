'''
Created on Sep 15, 2012

@author: guocong
'''
import os
import pandas as pd
import redis
import csv
import time
import re
from train_redis import str_to_datetime, text_process, get_head_query
from pprint import pprint
import cPickle as pickle


r = redis.StrictRedis(host='localhost', port=6379, db=0)


def c2s(r, user, category, query, click_time, query_time):
    return r.zrevrange(''.join(['c2s:', category]), start=0, num=4)


def qc2s(r, user, category, query, click_time, query_time):
    skus = r.zrevrange(''.join(['qc2s:', query, '::', category]),
                       start=0, num=4)
    if len(skus) == 5:
        return skus
    skus2 = r.zrevrange(''.join(['c2s:', category]), start=0, num=4)
    for s in skus2:
        if s not in skus:
            skus.append(s)
    return skus[0:5]


def qc_uc_to_s(r, skus, user, category, query, click_time, query_time):
    uc2s = set(r.zrevrange(''.join(['uc2s:', user, '::', category]),
                       start=0, num= -1))
    skus1 = r.zrevrange(''.join(['qc2s:', query, '::', category]),
                       start=0, num=len(uc2s) + 4)
    for s in skus1:
        if s not in skus and s not in uc2s:
            skus.append(s)
    return skus[0:5]


def c_uc_to_s(r, skus, user, category, query, click_time, query_time):
    uc2s = set(r.zrevrange(''.join(['uc2s:', user, '::', category]),
                       start=0, num= -1))
    skus1 = r.zrevrange(''.join(['c2s:', category]),
                        start=0, num=len(uc2s) + 4)
    for s in skus1:
        if s not in skus and s not in uc2s:
            skus.append(s)
    return skus[0:5]


def similarity_query_text(w1, w2):
    a = len(w1.intersection(w2))
    b = max(len(w1), len(w2))
    return float(a) / float(b)


def find_nn_query(r, q, c, length=2):
    if (q, c) in find_nn_query.qc_to_q:
        pair = find_nn_query.qc_to_q[(q, c)]
        return [x for x, score in pair if x != q][0:length]

    qw = set(q.split(' '))
    cq = r.zrevrange(':'.join(['c2q', c]), 0, -1, withscores=True)
    pair = []
    for x, score in cq:
        xw = set(x.split(' '))
        sim = similarity_query_text(qw, xw)
        pair.append((x, sim))
    pair = sorted(pair, key=lambda x: x[1], reverse=True)
    find_nn_query.qc_to_q[(q, c)] = pair[0:3]
    return [x for x, score in pair if x != q][0:length]


def sku_query(skus, query):
    hasSku = re.findall('sku[ #:]', query)
    if re.findall('[A-Za-z]', query) and not hasSku:
        return skus
    if not hasSku:
        if len(query) != 7:
            return skus

    ss = re.findall('\d{7}', query)
    if len(ss) != 1:
        return skus
    s = ss[0]
    if s not in skus:
        skus.append(s)
    return skus[0:5]


def qcw_qc_uc2s_match(r, skus, user, category, query, click_time, query_time):
    q = r.get(''.join(['spell:', query]))
    if not q:
        q = query
    c = category
    w = query_time.isocalendar()[1]
            # print q, c, tp, w
    if q in qcw_qc_uc2s_match.QCW:
        if c in qcw_qc_uc2s_match.QCW[q]:
            key = ''.join(['qcw2s:', q, '::', c, '::', str(w)])
        else:
            key = ''.join(['qc()2s:', q, '::', c])
        uc2s = set(r.zrevrange(''.join(['uc2s:', user, '::', category]),
                       start=0, num= -1))
        skus1 = r.zrevrange(key, start=0, num=len(uc2s) + 4)
        for s in skus1:
            if s not in skus and s not in uc2s:
                skus.append(s)

    skus = qc_uc2s_match(r, skus, user, category, query, click_time, query_time)
    return skus[0:5]


def qc_uc2s_match(r, skus, user, category, query, click_time, query_time):
    if len(skus) >= 5:
        skus = order_multi_click(skus, user, category, query,
                                 click_time, query_time)
        return skus[0:5]

    matched_query = r.get(''.join(['spell:', query]))
    if not matched_query:
        matched_query = query

    skus = qc_uc_to_s(r, skus, user, category, matched_query,
                      click_time, query_time)
    if matched_query != query and len(skus) == 0:
        skus = qc_uc_to_s(r, skus, user, category, query,
                          click_time, query_time)

    skus = order_multi_click(skus, user, category, query,
                                 click_time, query_time)

    if len(skus) >= 5:
        return skus[0:5]

    if len(skus) == 0:
        skus = sku_query(skus, query)

    if len(skus) <= 3:
        rec = find_nn_query(r, matched_query, category)
        for q in rec:
            if q != None and q != matched_query:
                skus = qc_uc_to_s(r, skus, user, category, q,
                                  click_time, query_time)
            if len(skus) >= 5:
                return skus[0:5]

    skus = c_uc_to_s(r, skus, user, category, query,
                     click_time, query_time)
    return skus


def order_multi_click(skus, user, category, query, click_time, query_time):
    key = (user, category, query, query_time)
    if key not in order_multi_click.multi_click:
        raise "multi_click error"
    k = order_multi_click.multi_click[key]
    if k == 0:
        return skus
    if len(skus) > k:
        tmp = skus[0:k]
        tmp = [skus[k]] + tmp
        skus[0:k + 1] = tmp
    return skus


def make_predictions(path, out, func):
    print out

    with open(path + "test.csv") as infile:
        reader = csv.reader(infile, delimiter=",")
        reader.next()  # burn the header
        cnt = 0
        with open(outPath + out, "w") as outfile:
            writer = csv.writer(outfile, delimiter=",")
            writer.writerow(["sku"])
            for (user, category, query, click_time, query_time) in reader:
                query = text_process(query)
                click_time = str_to_datetime(click_time)
                query_time = str_to_datetime(query_time)

                key = (user, category, query, query_time)
                if key in order_multi_click.multi_click:
                    order_multi_click.multi_click[key] += 1
                else:
                    order_multi_click.multi_click[key] = 0

                skus = []
                skus = func(r, skus, user, category, query,
                            click_time, query_time)
                skus = skus[0:5]
                if skus:
                    writer.writerow([" ".join(skus)])
                else:
                    writer.writerow(["0"])

                cnt += 1
                if cnt % 100000 == 0:
                    print cnt


if __name__ == '__main__':
    start = time.clock()

    path = '../data-big/'
    outPath = '../submit/'
    submitFileName = 'qcw_qc_uc2s_match_3_sq2_spell_sortq_v12_2k.csv'

    if not os.path.exists(outPath):
        os.makedirs(outPath)
    order_multi_click.multi_click = {}

    qcw_qc_uc2s_match.QCW = get_head_query(top=2000)
    print 'QCW is loaded', len(qcw_qc_uc2s_match.QCW)

    find_nn_query.qc_to_q = {}
    try:
        f = open('qc_to_q.pkl', 'rb')
        find_nn_query.qc_to_q = pickle.load(f)
        f.close()
        print 'Cached qc_to_q found.'
    except:
        print 'No cached qc_to_q. It would take longer time...'
    print 'qc_to_q loaded:', len(find_nn_query.qc_to_q)

    make_predictions(path, submitFileName, qcw_qc_uc2s_match)
    with open('qc_to_q.pkl', 'wb') as f:
        pickle.dump(find_nn_query.qc_to_q, f)
    print (time.clock() - start) / 60, 'min'
