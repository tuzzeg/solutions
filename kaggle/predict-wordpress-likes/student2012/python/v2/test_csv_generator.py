from datetime import datetime
import logging
from v1.config_and_pickle import  generated_loc, unpickle
from v2.train_feature_util import train_feature_util
from v1.test_util import load_test_users

lines_to_read = -1
max_post_to_compare = 2

test_start_date = '2012-04-30'
test_start_date__struct = datetime.strptime(test_start_date + ' 00:00:00', '%Y-%m-%d %H:%M:%S')

################################################################################################################################
logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)
logger = logging.getLogger('test_csv_generator')
################################################################################################################################
test_blog_post_tuples_map = unpickle('test_blog_post_tuples_map')
################################################################################################################################
def get_day_since_test_start_date(date_struct):
    d = (date_struct - test_start_date__struct).days + 1
    if d <= 1:
        return 1
    elif d >= 7:
        return 7
    else:
        return d

################################################################################################################################
#build test CSV
logger.info('started building test CSV...')

populate_for_first_four_weeks = False
print 'populate_for_first_four_weeks = ', populate_for_first_four_weeks
tfu = train_feature_util(populate_for_first_four_weeks)

output_file = '/test__more_features1__max-posts-' + str(max_post_to_compare) + '.csv'
print 'output_file = ', output_file
print 'lines_to_read = ', lines_to_read

with open(generated_loc + output_file, 'w') as f_test:
    f_test.write('uid,post_id,blog_post_day,blog_like_fraction,blog_author_like_fraction,max_cosine_similarity,avg_cosine_similarity' +
                 ',max_tag_like_fraction,avg_tag_like_fraction,max_category_like_fraction,avg_category_like_fraction\n')

    users = load_test_users()

    count = 0
    for uid in users:
        liked_blogs = tfu.liked_blogs(uid)

        for blog in liked_blogs:
            for post_id, author, tags, categories, date_struct in test_blog_post_tuples_map[blog]:
                topic_distribution = tfu.find_topic_distribution(blog, post_id)

                blog_post_day = get_day_since_test_start_date(date_struct)
                blog_like_fraction = tfu.get_blog_like_fraction(uid, blog)
                blog_author_like_fraction = tfu.get_blog_author_like_fraction(uid, blog, author)
                max_cosine_similarity, avg_cosine_similarity = tfu.cosine_similarity(uid, max_post_to_compare, topic_distribution)
                max_tag_like_fraction, avg_tag_like_fraction = tfu.get_tag_like_fractions(uid, tags)
                max_category_like_fraction, avg_category_like_fraction = tfu.get_category_like_fractions(uid, categories)


                #write to test CSV
                f_test.write(','.join([str(uid), str(post_id), str(blog_post_day),
                                       str(blog_like_fraction), str(blog_author_like_fraction),
                                       str(max_cosine_similarity), str(avg_cosine_similarity),
                                       str(max_tag_like_fraction), str(avg_tag_like_fraction),
                                       str(max_category_like_fraction), str(avg_category_like_fraction)]) + '\n')


        count += 1
        if count % 100 == 0:
            logger.info('processed %d users' % (count))

logger.info('finished building test CSV')


