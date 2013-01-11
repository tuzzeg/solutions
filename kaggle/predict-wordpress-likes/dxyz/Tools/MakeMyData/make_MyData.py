#! /usr/bin/python

import json
import csv
import json
import sys
from collections import defaultdict,Counter
import time

#TEST_USERS = 16262
#CUTOFF_DATE = "2012-04-22 22:59:59"


if __name__ == '__main__':

    f = open('cutoff_time.saved','r')
    CUTOFF_DATE = f.readline()
    print CUTOFF_DATE.rstrip()
    f.close()


    cutoff_dt = time.strptime(CUTOFF_DATE,"%Y-%m-%d %H:%M:%S")

    folderin = "../../Data/"
    folderout = "../../myData/"
    trainPosts = open(folderin + "trainPosts.json", 'r')
    mytrainPosts = open(folderout + "mytrainPosts.json", 'w')
    mytestPosts = open(folderout + "mytestPosts.json", 'w')

    found = 0

    for line in trainPosts:
        post = json.loads(line)
        date = post['date_gmt']
        #print date
        dt = time.strptime(date,"%Y-%m-%d %H:%M:%S")

        if dt <= cutoff_dt:
            mytrainPosts.write(json.dumps(post)+'\n')

        else:
            mytestPosts.write(json.dumps(post)+'\n')

        found += 1


