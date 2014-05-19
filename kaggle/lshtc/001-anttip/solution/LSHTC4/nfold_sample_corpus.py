import os
import commands
import sys
import random

def print_help():
    print "   Arguments:"
    print "   1) input file"
    print "   2) number of folds"
    print "   3) sum of test file counts"
    print "   4) train files name"
    print "   5) test files name"
    sys.exit()
    
if __name__ == '__main__':
    if len(sys.argv)!=6:
        print_help()
    random.seed(0)
    file=open(sys.argv[1], 'r')
    file_len=int(commands.getoutput('cat '+file.name+"| wc -l"))
    nfold= int(sys.argv[2])
    test_size= int(sys.argv[3])
    unused= []
    test_part= {}
    line_on= 0
    while line_on<file_len:
        unused.append(line_on)
        line_on+=1
         
    t= test_size
    while t>0:
        z= random.randint(1, len(unused))-1
        test_part[(unused[z])]=''
        unused.pop(z)
        t= t-1

    train_files= {}
    test_files= {}
    for x in range(0, nfold):
        train_files[x]= open(sys.argv[4] + "_" + str(x), 'w')
        test_files[x]= open(sys.argv[5] + "_" + str(x), 'w')
    line_on=0
    y= 0
    while line_on<file_len:
        line= file.readline()
        for x in range(0, nfold):
            if test_part.has_key(line_on) and y%nfold==x:
                test_files[x].writelines(line)
            else:
                train_files[x].writelines(line)
        if test_part.has_key(line_on):
            y+= 1
        line_on= line_on+1
    for x in range(0, nfold):
        train_files[x].close()
        test_files[x].close()
