import os
import commands
import sys
import random

def print_help():
    print "   Arguments:"
    print "   1) input file"
    print "   2) sampling ratio of output files"
    print "   3) output file 1"
    print "   4) output file 2"
    sys.exit()
    
if __name__ == '__main__':
    random.seed(0)
    #Partitions, samples file into two partitions.
    #Note: the output is still ordered, not ramdomized
    if len(sys.argv)!=5:
        print_help()
    file=open(sys.argv[1], 'r')
    file_len=int(commands.getoutput('cat '+file.name+"| wc -l"))
    unused=[]
    out_part_1={}
    line_on=0
    while line_on<file_len:
        unused.append(line_on)
        line_on=line_on+1
         
    out_part_1_size=int(float(file_len)*float(sys.argv[2]))
    t=out_part_1_size
    while t>0:
        z=random.randint(1, len(unused))-1
        out_part_1[(unused[z])]=''
        unused.pop(z)
        t=t-1

    print "Input size: "+str(file_len)
    print "File 1 size: "+str(out_part_1_size)
    print "File 2 size: "+str(file_len-out_part_1_size)
    
    out_file_1=open(sys.argv[3], 'w')
    out_file_2=open(sys.argv[4], 'w')
    line_on=0
    while line_on<file_len:
        if out_part_1.has_key(line_on):
            out_file_1.writelines(file.readline())
        else:
            out_file_2.writelines(file.readline())
        line_on=line_on+1
