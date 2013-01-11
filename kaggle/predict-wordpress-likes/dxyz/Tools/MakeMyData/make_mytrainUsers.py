#! /usr/bin/python

import json
import csv
import json
import sys
from collections import defaultdict,Counter
import time

#Min date:  Mon, 26 Mar 2012 00:00:00 +0000
#Max date:  Sun, 29 Apr 2012 23:59:59 +0000

#TEST_USERS = 16262
#CUTOFF_DATE = "2012-04-22 22:59:59" # <= cutoff date is train period


if __name__ == '__main__':

    f = open('cutoff_time.saved','r')
    CUTOFF_DATE = f.readline()
    print CUTOFF_DATE.rstrip()
    f.close()


    cutoff_dt = time.strptime(CUTOFF_DATE,"%Y-%m-%d %H:%M:%S")

    folderin = "../../Data/"
    folderout = "../../MyData/"
    trainUsers = open(folderin + "trainUsers.json", 'r')
    trainPosts = open(folderin + "trainPosts.json", 'r')
    mytrainUsers = open(folderout + "mytrainUsers.json", 'w')
    # We do not consider likes made in the test period for posts
    # that are published in the train period. Omitted posts reflects this
    myomittedPosts = open(folderout+"myomittedPosts.json",'w')

    # need to make post_id to post_date dict
    post_date_dict ={}

    for line in trainPosts:
        post = json.loads(line)
        post_date_dict[int(post['post_id'])] = time.strptime(post['date_gmt'],"%Y-%m-%d %H:%M:%S")

    found = 0

    for line in trainUsers:
        try:
            user = json.loads(line)
        except:
            print line
            exit()
        filtered_likes = []
        omitted_posts = []
        for liked in user['likes']:
            date = liked['like_dt']
            #print date
            dt = time.strptime(date,"%Y-%m-%d %H:%M:%S")

            if dt <= cutoff_dt:
                filtered_likes.append(liked)
            else:
                if int(liked['post_id']) in post_date_dict:
                    if post_date_dict[int(liked['post_id'])] > cutoff_dt:
                        omitted_posts.append(liked['post_id'])

        user['likes'] = filtered_likes


        mytrainUsers.write(json.dumps(user)+'\n')

        if user['inTestSet']:
            myomittedPosts.write(' '.join(omitted_posts)+'\n')

        found += 1

