import json
import logging
from sets import Set
from datetime import datetime
from v1.config_and_pickle import generated_loc, trainPosts_loc
from v2.train_feature_util import train_feature_util

lines_to_read = -1
max_post_to_compare = 500

wk_5_start_date = '2012-04-23'
wk_5_start_date__struct = datetime.strptime(wk_5_start_date + ' 00:00:00', '%Y-%m-%d %H:%M:%S')

################################################################################################################################
logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)
logger = logging.getLogger('training_csv_generator')
################################################################################################################################
def get_day_since_week_5_start_date(date_struct):
    d = (date_struct - wk_5_start_date__struct).days + 1
    if d <= 1:
        return 1
    elif d >= 7:
        return 7
    else:
        return d

#build training CSV
logger.info('started building training CSV...')

populate_for_first_four_weeks = True
print 'populate_for_first_four_weeks = ', populate_for_first_four_weeks
tfu = train_feature_util(populate_for_first_four_weeks)

output_file = '/training__more_features1__max-posts-' + str(max_post_to_compare) + '.csv'
print 'output_file = ', output_file
print 'lines_to_read = ', lines_to_read

with open(generated_loc + output_file, 'w') as f_training:
    f_training.write('did_user_like_the_blog_post,blog_post_day,blog_like_fraction,blog_author_like_fraction,max_cosine_similarity,avg_cosine_similarity' +
                     ',max_tag_like_fraction,avg_tag_like_fraction,max_category_like_fraction,avg_category_like_fraction\n')

    with open(trainPosts_loc, 'r') as f:
        #for each blog-post with (date_gmt >= 2012-04-23)
        for line_number, line_text in enumerate(f):
            if lines_to_read != -1 and line_number >= lines_to_read:
                break

            blog_json = json.loads(line_text)

            blog        = blog_json['blog']
            post_id     = blog_json['post_id']
            author      = blog_json['author']
            tags        = blog_json['tags']
            categories  = blog_json['categories']
            date_struct = datetime.strptime(blog_json['date_gmt'], '%Y-%m-%d %H:%M:%S')
            date_string = date_struct.strftime('%Y-%m-%d')

            #for each blog-post with (date_gmt >= 2012-04-23)
            if date_string < wk_5_start_date:
                continue

            users_who_liked_this_blog_post = Set(like['uid'] for like in blog_json['likes'])
            topic_distribution = tfu.find_topic_distribution(blog, post_id)

            #for each user who has liked at least one post from this blog before [in the 4 weeks before date_gmt:2012-04-23]
            for uid in tfu.users_who_have_liked_this_blog_before(blog):
                if tfu.like_count(uid) < 4:
                    #"test users have at least 5 posts in the train period."
                    #so for our training we put a restriction of 4 weeks [because our training data is for 4 weeks (for test, training data is for 5 weeks)]
                    continue

                did_user_like_the_blog_post = int(uid in users_who_liked_this_blog_post)
                blog_post_day = get_day_since_week_5_start_date(date_struct)
                blog_like_fraction = tfu.get_blog_like_fraction(uid, blog)
                blog_author_like_fraction = tfu.get_blog_author_like_fraction(uid, blog, author)
                max_cosine_similarity, avg_cosine_similarity = tfu.cosine_similarity(uid, max_post_to_compare, topic_distribution)
                max_tag_like_fraction, avg_tag_like_fraction = tfu.get_tag_like_fractions(uid, tags)
                max_category_like_fraction, avg_category_like_fraction = tfu.get_category_like_fractions(uid, categories)

                #write to training CSV
                f_training.write(','.join([str(did_user_like_the_blog_post), str(blog_post_day),
                                           str(blog_like_fraction), str(blog_author_like_fraction),
                                           str(max_cosine_similarity), str(avg_cosine_similarity),
                                           str(max_tag_like_fraction), str(avg_tag_like_fraction),
                                           str(max_category_like_fraction), str(avg_category_like_fraction)]) + '\n')

            if line_number % 100 == 0:
                logger.info('processed %d lines' % (line_number))

logger.info('finished building training CSV')
