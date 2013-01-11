from collections import defaultdict

import json
import logging
from datetime import datetime
from v1.config_and_pickle import trainPosts_loc, pickle
from v2.defaultdict_util import increment_by_one
from v2.util import get_pickle_file_suffix

lines_to_read = -1

wk_5_start_date = '2012-04-23'

################################################################################################################################
logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)
logger = logging.getLogger('training_data_structure_populator')
################################################################################################################################


def populate_data_structures(populate_for_first_four_weeks):
    #populate data structures required for building training CSV
    logger.info('started populating data structures...')

    #key = blog
    #value = number of posts
    blog_post_count_map = defaultdict(int)

    #key = blog_author
    #value = number of posts
    blog_author__post_count_map = defaultdict(int)

    #key   = uid (user id)
    #value = list of (blog, post_id, author) tuples
    user_likes_map = defaultdict(list)

    #key   = uid (user id)
    #value = set of blogs liked by this user
    user_liked_blogs_map = defaultdict(set)

    #key   = blog
    #value = set of users (uid) who have liked at least one post from the blog 
    blog_liked_users_map = defaultdict(set)
    
    #key = tag
    #value = dict:
    #             key = blog
    #             value = count [# of posts in this blog for this tag]
    #tag_blog_count_map = defaultdict(lambda : defaultdict(int))
    tag_blog_count_map = {}

    #key = category
    #value = dict:
    #             key = blog
    #             value = count [# of posts in this blog for this category]
    #category_blog_count_map = defaultdict(lambda : defaultdict(int))
    category_blog_count_map = {}

    #key = tag
    #value = dict:
    #             key = user [uid]
    #             value = count [# of posts for this tag that this user has liked]
    #tag_user_count_map = defaultdict(lambda : defaultdict(int))
    tag_user_count_map = {}

    #key = category
    #value = dict:
    #             key = user [uid]
    #             value = count [# of posts for this category that this user has liked]
    #category_user_count_map = defaultdict(lambda : defaultdict(int))
    category_user_count_map = {}

    with open(trainPosts_loc, 'r') as f:
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

            if populate_for_first_four_weeks and date_string >= wk_5_start_date:
                continue

            blog_post_count_map[blog] += 1
            blog_author__post_count_map[blog + '_' + author] += 1

            for tag in tags:
                #tag_blog_count_map[tag][blog] += 1
                increment_by_one(tag_blog_count_map, tag, blog)

            for category in categories:
                #category_blog_count_map[category][blog] += 1
                increment_by_one(category_blog_count_map, category, blog)

            for like in blog_json['likes']:
                uid = like['uid']
                user_likes_map[uid] += [(blog, post_id, author)]
                user_liked_blogs_map[uid].add(blog)
                blog_liked_users_map[blog].add(uid)
                for tag in tags:
                    #tag_user_count_map[tag][uid] += 1
                    increment_by_one(tag_user_count_map, tag, uid)
                for category in categories:
                    #category_user_count_map[category][uid] += 1
                    increment_by_one(category_user_count_map, category, uid)

    logger.info('finished populating data structures')

    pickle(blog_post_count_map, 'blog_post_count_map' + get_pickle_file_suffix(populate_for_first_four_weeks))
    pickle(blog_author__post_count_map, 'blog_author__post_count_map' + get_pickle_file_suffix(populate_for_first_four_weeks))
    pickle(user_likes_map, 'user_likes_map' + get_pickle_file_suffix(populate_for_first_four_weeks))
    pickle(user_liked_blogs_map, 'user_liked_blogs_map' + get_pickle_file_suffix(populate_for_first_four_weeks))
    pickle(blog_liked_users_map, 'blog_liked_users_map' + get_pickle_file_suffix(populate_for_first_four_weeks))
    pickle(tag_blog_count_map, 'tag_blog_count_map' + get_pickle_file_suffix(populate_for_first_four_weeks))
    pickle(category_blog_count_map, 'category_blog_count_map' + get_pickle_file_suffix(populate_for_first_four_weeks))
    pickle(tag_user_count_map, 'tag_user_count_map' + get_pickle_file_suffix(populate_for_first_four_weeks))
    pickle(category_user_count_map, 'category_user_count_map' + get_pickle_file_suffix(populate_for_first_four_weeks))

populate_data_structures(True)
