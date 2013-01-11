#! /usr/bin/python

import json
import csv
import json
import sys
from collections import defaultdict,Counter
import time


if __name__ == '__main__':

    #cutoff_dt = time.strptime(CUTOFF_DATE,"%Y-%m-%d %H:%M:%S")

    folderin = "../../Data/"
    trainPosts = open(folderin + "trainPosts.json", 'r')

    found = 0

    for line in trainPosts:
        post = json.loads(line)
        date = post['date_gmt']
        #print date
        dt = time.strptime(date,"%Y-%m-%d %H:%M:%S")
        if found == 0:
            min_dt = dt
            max_dt = dt
        else:
            if min_dt > dt:
                min_dt = dt
            if max_dt < dt:
                max_dt = dt

        found += 1

        #        if found > 1000:
        #            break



    print 'Min date: ',time.strftime("%a, %d %b %Y %H:%M:%S +0000", min_dt)
    print 'Max date: ',time.strftime("%a, %d %b %Y %H:%M:%S +0000", max_dt)

    # the training set is the week at the end

    x = time.mktime(max_dt)
    y = x - 7*24*60*60

    cutoff_dt = time.localtime(y)
    cutoff_file = open('cutoff_time.saved','w')
    cutoff_file.write(time.strftime("%Y-%m-%d %H:%M:%S", cutoff_dt))
    cutoff_file.close()


