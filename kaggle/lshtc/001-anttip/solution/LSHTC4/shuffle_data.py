import os
import commands
import sys
import math
import random

#Fisher-yates Shuffle

if __name__ == '__main__':
    train_file= open(sys.argv[1], 'r')
    lines= {}
    random.seed(0)
    train_count= 0
    for line in train_file:
        lines[train_count]= line[:-1]
        train_count+= 1
    
    for x in range(0, train_count):
        z= random.randint(0, train_count-1)
        line= lines[x]
        lines[x]= lines[z]
        lines[z]= line

    for line in lines:
        print lines[line]
