import os
import commands
import sys
import math

if __name__ == '__main__':
    train_file= open(sys.argv[1], 'r')
    labelsets= {}
    for line in train_file:
        line= line.strip().split(" ")
        labelset= []
        for word in line:
            word= word.split(",")[0]
            word= word.split(":")
            if len(word)!=2:
                labelset.append(word[0])
        #labelset.sort()
        labelset= " ".join(sorted(labelset))
        if not(labelsets.has_key(labelset)):
            labelsets[labelset]= 1
        else:
            labelsets[labelset]+= 1
        #labelsets[",".join(labelset)]= 1
    for x in labelsets:
        print labelsets[x], x
    #print len(labelsets)
