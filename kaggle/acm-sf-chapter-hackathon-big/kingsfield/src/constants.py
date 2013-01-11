'''
Created on Oct 10, 2012

@author: kingsfield
'''
from datetime import datetime
import os

data_fold = '/your fold' 
out_buffer_path = '/your fold'

train_file = os.path.join(data_fold, 'train.csv')
test_file = os.path.join(data_fold, 'test.csv')
out_file = os.path.join(data_fold, 'prediction.csv')

new_train_file = os.path.join(data_fold, 'new_train.csv')
new_test_file = os.path.join(data_fold, 'new_test.csv')

st_date = datetime.strptime('2011-08-11 04:00:17', '%Y-%m-%d %H:%M:%S')
ed_date = datetime.strptime('2011-10-31 10:17:42', '%Y-%m-%d %H:%M:%S')

'unigram and bigram parameters'
GLOBAL_QUERY = 6
GLOBAL_BIGRAM_QUERY = 6
w1 = 0.7
w2 = 0.3

'time features parameters'
duration = (ed_date - st_date).days
block_size = 12
MAX_BLOCK = block_size - 1
block = duration / block_size

PREDICT_HOT_SIZE = 'PREDICT_HOT_SIZE'
HOT = 'HOT'
BIGRAM_HOT = 'BIGRAM_HOT'
HOT_SIZE = 'HOT_SIZE'
SUM = 'SUM'
SUM_SIZE = 'SUM_SIZE'

magic_num = 100000   # this constant let all skus can be predictable in my code setting

proc_size = 2

MAX_TEST_LINE = 28242  
TEST_STEP = MAX_TEST_LINE / (100 * proc_size) 

