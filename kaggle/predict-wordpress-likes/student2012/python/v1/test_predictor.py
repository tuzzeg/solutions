from collections import defaultdict
import csv
from v1.config_and_pickle import sub_loc
from v1.test_util import load_test_users

def load_user_post_like_probability_tuple_map(test_prediction_file):
    user_post_like_probability_tuple_map = defaultdict(list)
    with open(test_prediction_file, 'r') as f:
        f.readline()#skip header
        for row in csv.reader(f):
            uid              = row[0]
            test_post        = row[1]
            like_probability = row[2]
            user_post_like_probability_tuple_map[uid] += [(test_post, like_probability)]

    return user_post_like_probability_tuple_map

def find_like_posts(uid, user_post_like_probability_tuple_map):
    post_like_probability_tuples = sorted(user_post_like_probability_tuple_map[uid], key = lambda post_like_probability_tuple : post_like_probability_tuple[1], reverse=True)
    posts = [test_post for test_post, like_probability in post_like_probability_tuples]
    if len(posts) > 100:
        posts = posts[0:100]
    return posts

def build_sub_file():
    R_predictions_file_name = '~/gom/predictions/rf_AND_gbm_1200_trees_AND_lr_sub_009.csv'

    print 'R_predictions_file_name = ', R_predictions_file_name
    user_post_like_probability_tuple_map = load_user_post_like_probability_tuple_map(R_predictions_file_name)
    count = 0
    no_recommendation_count = 0

    output_file = '/011_gbm_rf_lr.csv'
    print 'output_file = ', output_file
    with open(sub_loc + output_file, 'w') as f:
        f.write('"posts"\n')
        users = load_test_users()
        for uid in users:
            posts = find_like_posts(uid, user_post_like_probability_tuple_map)
            if len(posts) == 0:
                no_recommendation_count += 1
                print '    no recommended posts: no_recommendation_count = ', no_recommendation_count

            f.write(' '.join(posts) + '\n')

            count += 1

    print 'Total no_recommendation_count = ', no_recommendation_count

build_sub_file()
