'''
Created on Sep 8, 2012

@author: guocong
'''

import pandas as pd
import datetime
import redis
import re
import random
import csv
import pickle
import time
from pprint import pprint

PORT = 6379
DB = 0


def str_to_datetime(x):
    return datetime.datetime.strptime(x.split('.')[0], '%Y-%m-%d %H:%M:%S')


def text_process(s):
    s = s.lower()
    s = re.sub(r'[^\w]', ' ', s)
    s = re.sub('[ \t]+', ' ', s)
    s = s.strip()
    s = stopwordRemove.process(s)
    return text_sort(s)


def text_sort(s):
    wl = s.split(' ')
    d = {}
    for x in wl:
        d[x] = 1
    wl = list(d.keys())
    wl.sort()
    return ' '.join(wl)


class StopwordRemove(object):
    def __init__(self):
        self.stopwords = set(pickle.load(open('stopwords.pkl', 'r')))
        print 'loaded', len(self.stopwords), 'stopwords'

    def process(self, s):
        wl = s.split(' ')
        nwl = []
        for w in wl:
            if w not in self.stopwords:
                nwl.append(w)
        return ' '.join(nwl)


stopwordRemove = StopwordRemove()


def make_redis_db(path, randomfactor=False):
    if randomfactor:
        random.seed(11)
    EPS = 1e-4
    r = redis.StrictRedis(host='localhost', port=PORT, db=DB)
    pipe = r.pipeline()
    tp = pd.read_csv(path + 'train.csv', iterator=True,
                        chunksize=2000)

    T1 = {'c2s': 'category'}

    T2 = {'qc2s': ('query', 'category'),
          'uc2s': ('user', 'category')}

    T4 = {'N(q)': ('query',),
          'N(q,c)': ('query', 'category')}

    for chunk in tp:
        chunk['query'] = chunk['query'].apply(text_process)
        for i in xrange(len(chunk)):
            row = chunk.ix[i]
            for key in T1:
                delta = random.uniform(-EPS, EPS) if randomfactor else 0
                pipe.zincrby(''.join([key, ':', row[T1[key]]]), row['sku'],
                             amount=1 + delta)
            for key in T2:
                k1, k2 = T2[key]
                delta = random.uniform(-EPS, EPS) if randomfactor else 0
                pipe.zincrby(''.join([key, ':', row[k1], '::', row[k2]]),
                             row['sku'], amount=1 + delta)

            for key in T4:
                member = [row[x] for x in T4[key]]
                pipe.zincrby(key, '::'.join(member), amount=1)

            key = ':'.join(['c2q', row['category']])
            pipe.zincrby(key, row['query'], amount=1)
        pipe.execute()


def make_bigq(path, QCW, randomfactor=False):
    if randomfactor:
        random.seed(11)
    EPS = 1e-4
    r = redis.StrictRedis(host='localhost', port=PORT, db=DB)
    pipe = r.pipeline()
    tp = pd.read_csv(path + 'train.csv', iterator=True,
                        chunksize=2000)
    for chunk in tp:
        chunk['query'] = chunk['query'].apply(text_process)
        chunk['query_time'] = chunk['query_time'].apply(str_to_datetime)

        for i in xrange(len(chunk)):
            row = chunk.ix[i]
            q = str(row['query'])
            c = str(row['category'])
            tp = row['query_time']
            w = tp.isocalendar()[1]
            if q in QCW:
                if c in QCW[q]:
                    key = ''.join(['qcw2s:', q, '::', c, '::', str(w)])
                else:
                    key = ''.join(['qc()2s:', q, '::', c])
                pipe.zincrby(key, row['sku'], amount=1)
        pipe.execute()


def make_spell_dict(path):
    r = redis.StrictRedis(host='localhost', port=PORT, db=DB)
    with open(path, 'r') as f:
        reader = csv.reader(f, delimiter=',')
        reader.next()
        cnt = 0
        for q, s in reader:
            key = ''.join(['spell:', q])
            r.set(key, s)
            cnt += 1
        print cnt


def get_head_query(top=1500):
    r = redis.StrictRedis(host='localhost', port=PORT, db=DB)
    data = r.zrevrange('N(q,c)', start=0, num=top - 1, withscores=True)
    df = pd.DataFrame(data, columns=['key', 'score'])
    q_c = df.key.tolist()
    q_c = [tuple(x.split('::')) for x in q_c]
    qcw = {}
    for q, c in q_c:
        if q in qcw:
            qcw[q].add(c)
        else:
            qcw[q] = set([c])
    return qcw


if __name__ == '__main__':
    path = '../data-big/'
    start = time.clock()

    r = redis.StrictRedis(host='localhost', port=PORT, db=DB)
    if r.dbsize() > 0:
        print "Redis DB is not empty! Please flash DB!"
        exit(0)

    print 'step 1'
    make_redis_db(path, randomfactor=True)

    print 'step 2'
    make_spell_dict('spell_dict_sort_2.csv')

    print 'step 3'
    QCW = get_head_query(2500)
    print 'loaded QCW:', len(QCW)
    make_bigq(path, QCW, randomfactor=False)

    print (time.clock() - start) / 60, 'min'
