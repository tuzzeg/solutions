import sys
import re
import commands

source_dir= "/research/antti/multi_label/MetaComb/comb_dev_results2"
reference_file= "comb_dev_reference2.txt"
result_files= commands.getoutput("ls "+source_dir+"/*.results").split("\n")

in_file= open(reference_file, "r")
references= {}
x= 0
out_file= open("comb_dev_reference3.txt", "w")
for line in in_file:
	if line[:-1]!="":
		references[x]= 1
		out_file.write(line)
	x+= 1
in_file.close()
out_file.close()

for result_file in result_files:
	in_file= open(result_file, "r")
	#out_file= open(result_file.replace("/c/", "/d/"), "w")
	out_file= open(result_file.replace("/comb_dev_results2/", "/comb_dev_results3/"), "w")
	print result_file
	x= 0
	for line in in_file:
		if references.has_key(x):
			out_file.write(line);
		x+= 1 
	in_file.close()
	out_file.close()

