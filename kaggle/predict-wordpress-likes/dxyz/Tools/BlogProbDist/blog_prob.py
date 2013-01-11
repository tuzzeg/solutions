from __future__ import division
import sys
#sys.path.append('../Benchmarks/wordpress-LDA')


from collections import defaultdict,Counter
#from corp import stop_words, Files, Corp
from gensim import corpora, models, similarities
import logging
import json
import cPickle
import random
import time
import csv
import subprocess


DEFAULT_NUMBER_OF_POSTS = 1000 # this number is used for blogs tha have likes but no observed posts
WEIGHT_OLDER_POSTS = 1.0

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

DataSet = 'my'

infolder = '../../'+DataSet+'Data/'
outfolder = '../../'+DataSet+'Submissions/'

blogprobdist_savedfolder = DataSet+'Saved/'

# autodetect here
x= subprocess.check_output(["ls", "../../Data/"]).split()

kaggle_user_stats = x[1] #'kaggle-stats-users-20111123-20120423.json'
kaggle_blog_stat = x[0] #'kaggle-stats-blogs-20111123-20120423.json'

kaggle_user_stats_file = open("../../Data/"+kaggle_user_stats)
kaggle_blog_stats_file = open("../../Data/"+kaggle_blog_stat)

stats_end_date_string = kaggle_user_stats.split('-')
stats_end_date_string = stats_end_date_string[4].split('.')[0]+' 00:00:00'

STATS_END_DATE = time.strptime(stats_end_date_string,"%Y%m%d %H:%M:%S")

trainPostsFile = infolder+DataSet+"trainPosts.json"
trainUsersFile = infolder+DataSet+"trainUsers.json"


try:
    user_liked_blog_dict = cPickle.load(open(blogprobdist_savedfolder+"BlogProbDist.saved", "r"))
except:
    #blog probs from kaggle_stats
    blogs_number_of_posts = Counter()
    for line in kaggle_blog_stats_file:
        blog = json.loads(line)
        blogs_number_of_posts[str(blog['blog_id'])] = blog['num_posts']

    # now we have to add the blogs in the days starting after the stats end before the training period end
    blogs_not_in_blogs_number_of_posts = Counter()

    with open(trainPostsFile) as posts:
        for line in posts:
            post = json.loads(line)
            #TODO Note we will maintain an additional dict for blogs that are missed from the stats

            if blogs_number_of_posts[post['blog']] == 0:
                #TODO so these are the blogs that have zero count in the stats for posts that appear in train
                blogs_not_in_blogs_number_of_posts[post['blog']] += 1
            else:
                # here the blog that the post appears in is in the stats and we assume the count is correct
                dt = time.strptime(post['date_gmt'],"%Y-%m-%d %H:%M:%S")
                if dt > STATS_END_DATE:
                    blogs_number_of_posts[post['blog']] +=1
        # now put them together
        for blog in blogs_not_in_blogs_number_of_posts.keys():
            blogs_number_of_posts[blog] = blogs_not_in_blogs_number_of_posts[blog]

    user_liked_blog_dict = defaultdict(lambda: defaultdict(float))
    for line in kaggle_user_stats_file:
        user = json.loads(line)
        for like in user['like_blog_dist']:
            user_liked_blog_dict[str(user["user_id"])][like['blog_id']] += float(like['likes'])*WEIGHT_OLDER_POSTS #TODO can weight older posts here

    with open(trainUsersFile) as users:
        for line in users:
            user = json.loads(line)
            #TODO we maintain a dict of posts whose blogs do not appear in blogs_number_of_posts

            for like in user['likes']:
                date_of_like = time.strptime(like['like_dt'],"%Y-%m-%d %H:%M:%S")
                blog_of_like = like['blog']
                if blogs_number_of_posts[blog_of_like] == 0:
                    #TODO so these are the blogs that have zero count in the stats but a user liked a post
                    user_liked_blog_dict[user["uid"]][like['blog']] += 1
                else:
                    # we have the blog and have to assume the count from stats is correct
                    # add one if it occurs after the stats period
                    if date_of_like > STATS_END_DATE:
                        user_liked_blog_dict[user["uid"]][like['blog']] += 1

    # now calculate the probability of a user liking a post in a given blog
    # 1000 is our default prob
    for user in user_liked_blog_dict.keys():
        for blog in user_liked_blog_dict[user].keys():
            if blogs_number_of_posts[blog] == 0:
                user_liked_blog_dict[user][blog] /= DEFAULT_NUMBER_OF_POSTS
            else:
                user_liked_blog_dict[user][blog] /= blogs_number_of_posts[blog]
    x = dict(user_liked_blog_dict)
    cPickle.dump(x, open(blogprobdist_savedfolder+"BlogProbDist.saved", "w"))

    logging.info("Done calculating blog like probability for each user")

