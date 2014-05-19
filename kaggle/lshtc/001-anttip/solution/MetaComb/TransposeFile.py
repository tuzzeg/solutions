import sys
import re
import commands

source_file= sys.argv[1]
#column_count= int(commands.getoutput("wc -l "+sys.argv[1]).split(" ")[0])
column_count= int(sys.argv[2])
transpose= {}

in_file= open(source_file,"r")
x= -1
for line in in_file:
	#print x, line
	x+= 1
	line= line.strip().split(",")
	if line[0]=="":
		continue
	for entry in line:
		#print line
		entry= int(entry.strip())
		if transpose.has_key(entry):
			transpose.get(entry).append(x)
		else:
			transpose[entry]= [x]

for x in xrange(0, column_count):
	entries= []
	if transpose.has_key(x):
		entries= transpose[x]
	print str(entries).replace("[", "").replace("]", "")
	
