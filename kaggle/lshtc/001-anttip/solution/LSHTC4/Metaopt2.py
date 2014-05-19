# -*- coding: iso-8859-1 -*-
import commands
import os
import sys
import subprocess
import time
import string
import re
import random
import copy
import math

def parallize_processes(max_procs, tasks, std_outs, std_errs, task_check):
	procs={}
	#print task_check
	num_procs= 0
	for x in sorted(tasks.keys()):
		std_outs[x]= open(std_outs[x], 'w')
		std_errs[x]= open(std_errs[x], 'w')
		#if not(x.startswith("trec_la")):
		#	continue
		procs[x]= subprocess.Popen(tasks[x], shell=True, stdin=subprocess.PIPE, stdout=std_outs[x], stderr=std_errs[x])
		#print tasks[x]
		print "Added task",x,"to process list"
		num_procs+=1
		if num_procs < max_procs:
			continue
		num_procs= max_procs
		while num_procs==max_procs:
			time.sleep(3)
			if task_check!="":
				output= commands.getoutput(task_check)
				try:
					num_procs= int(output)
					if num_procs==0:
						break
				except:
					continue
			else:
				num_procs= 0
				for y in procs.keys(): 
					if procs[y].poll()==None:
						num_procs+= 1
					else:
						#procs[y].kill()
						procs[y]= None
						procs.pop(y)
	num_procs= 1
	while num_procs>0:
		time.sleep(3)
		if task_check!="":
			output= commands.getoutput(task_check)
			try:
				num_procs= int(output)
				if num_procs==0:
					break
			except:
				continue
		else:
			num_procs= 0
			for y in procs.keys():
				if procs[y].poll()==None:
					num_procs+= 1
					#procs[y].kill()
				else:
					procs[y]= None
					procs.pop(y)
	for x in std_outs:
		std_outs[x].close()
	for x in std_errs:
		std_errs[x].close()

def read_params(params_file):
	params= [{},{},{},{}]
	file= open(params_file)
	lines= 0
	for x in file:
		lines+= 1
	file= open(params_file)
	param_type= 0
	while lines>0:
		line= re.sub(r'\s\s*', ' ', file.readline()).split(" ", 1)
		line[1]= line[1].rstrip()
		lines-= 1
		if line[0]=="#":
			param_type+=1
			continue
		if param_type> 1:
			line[1]= line[1].split(" ")
			for x in range(0, len(line[1])-1):
				entry= float(line[1][x])
				if x in [0, 1, 2]:
					entry= transform(entry, line[1][-1], 1)
				line[1][x]= entry
		params[param_type][line[0]]= line[1]
	file.close()
	return params

def transform(variable, transformation, direction):
	#print "XX", variable, transformation, direction
	if (direction>0):
		#print transformation,  variable, direction
		for y in transformation:
			if y=="1":
				variable= 1.0-variable
			if y=="/":
				variable= 1.0/variable
			if y=="-":
				variable= -variable
			if y=="l":
				variable= math.log(1.0+variable)
	else:
		for y in transformation[::-1]:
			if y=="1":
				variable= 1.0-variable
			if y=="/":
				variable= 1.0/variable
                        if y=="-":
                                variable= -variable
                        if y=="l":
                                variable= math.exp(variable)-1.0
	#print "->", variable
	return variable

def write_params(params, params_file):
	file= open(params_file, 'w')
	for x in sorted(params[0].keys()):
		file.writelines(x+" "+ params[0][x]+"\n")
	file.writelines("#"+"\n")
	for x in sorted(params[1].keys()):
		file.writelines(x+" "+ params[1][x]+"\n")
	file.writelines("#"+"\n")
	for z in [2,3]:
		for x in sorted(params[z].keys()):
			t=""
			for y in range(0, len(params[z][x])):
				entry= params[z][x][y]
				if y in [0, 1, 2]:
					entry= transform(float(entry), params[z][x][-1], -1)
				t+= str(entry)+" "
			file.writelines(x+" "+ t.strip()+"\n")
		if z==2:
			file.writelines("#"+"\n")

def sample_point(param_set, perbs, perb_index):
	z= perb_index
	for x in param_set:
		values= param_set[x]
		if values[-1].__contains__("="):
			z+= 1
			continue
		values[0]= min(values[2], max(values[1], values[0] + perbs[z]*values[3]*random.normalvariate(0, 1.0)))
		#values[0]= min(values[2], max(values[1], values[0] + perbs[z]*values[3]*random.lognormvariate(0, 2.0)))
		param_set[x]= values
		z+= 1
	return param_set

def EQ(f1, f2):
	if abs(f1-f2)< 0.000000001:
		return True
	return False

def GE(f1, f2):
	if EQ(f1, f2):
		return True
	return GT(f1, f2)

def GT(f1, f2):
	if f1> f2 + 0.000000001:
		return True
	return False


def combine_params(old_params, points, type, old_score):
	max_point= ""
	#old_score= float(old_params[0][0]["score"])
	max_score= -10000000000000.0
	for y in points:
		if GT(points[y][2], max_score):
			max_score= round(points[y][2], 10)
			if GE(max_score,  old_score):
				max_point= y
	for x in old_params[0][type]:
		values= old_params[0][type][x]
		if values[-1].__contains__("="):
			continue
		value= values[0]
		update= 0.0
		for y in points:
			if y!=max_point:
				continue
			value= points[y][0][type][x][0]
			update= value- values[0]
		plus_stepsize= values[3]
		#if GT(max_score, old_score):
		#	plus_stepsize*= 1.2
		#	print "+",
		#if GE(old_score, max_score):
		#	plus_stepsize*= 0.8
		#	print "-",
		plus_stepsize*= 0.9
		
		#print max_score, x, value, "\t", update
		print max_score, x, transform(value, old_params[0][type][x][-1], -1), "\t", update
		
		old_params[0][type][x][0]= value
		for y in old_params:
			#old_params[y][type][x][0]= value
			#old_params[y][type][x][3]= min((values[2]-values[1])*0.5, plus_stepsize)
			old_params[y][type][x][3]= min((values[2]-values[1]), plus_stepsize)
	return old_params

if __name__ == '__main__':
   	params_file= sys.argv[1]
	train_iters= -1
	eval_iters= 1
	sample_size= 1
	if len(sys.argv)!=2:
		train_iters= int(sys.argv[2])
		eval_iters= int(sys.argv[3])
		sample_size= int(sys.argv[4])
	
	init_params= {}
	params_list= {}
	file= open(params_file)
        if file.readline().startswith("#params_list"):
		params_file= line= file.readline().strip()
		init_params= read_params(line)
		params_list[line]= init_params
		#params_list[line][1]["test_file"]= "test_ir_eval.txt"
		#params_list[line][1]["train_file"]= "test_ir_train.txt"
		for line in file:
			line= line.strip()
			params_list[line]= read_params(line)
			#params_list[line][1]["test_file"]= "test_ir_eval.txt"
			#params_list[line][1]["train_file"]= "test_ir_train.txt"
	else:
		init_params= read_params(params_file)
		params_list[params_file]= init_params
	old_params= {}
	old_params[0]= copy.deepcopy(init_params)

	iter_count= train_iters*eval_iters
	points= {}
	task_check= init_params[0]["task_check"].replace("#IDD#", params_file[0:16]).replace("#USERNAME#", init_params[0]["username"]) 
	nfold= int(init_params[0]["nfold"])
	max_procs= int(init_params[0]["max_procs"])
	score_cmd= init_params[0]["score_cmd"]
	time_cmd= init_params[0]["time_cmd"]
	scheduler= init_params[0]["scheduler"]
	
	param_count= len(old_params[0][2])+len(old_params[0][3])
	random.seed(0)
	init_score= float(old_params[0][0]["score"])
	if 1:
		old_params[0][0]["train_time"]= "0.0"
		old_params[0][0]["test_time"]= "0.0"
		old_params[0][0]["score"]= "0.0"
		for x in old_params[0][2]:
			old_params[0][2][x][3]= (old_params[0][2][x][2]-old_params[0][2][x][1])
			#old_params[0][2][x][3]= (old_params[0][2][x][2]-old_params[0][2][x][1])*0.5
			#old_params[0][2][x][3]= (old_params[0][2][x][2]-old_params[0][2][x][1])*0.1
		for x in old_params[0][3]:
			old_params[0][3][x][3]= (old_params[0][3][x][2]-old_params[0][3][x][1])
			#old_params[0][3][x][3]= (old_params[0][3][x][2]-old_params[0][3][x][1])*0.5
			#old_params[0][3][x][3]= (old_params[0][3][x][2]-old_params[0][3][x][1])*0.1
	old_score= float(old_params[0][0]["score"])
	train_iter= 0
	old_point= ""
	locale= 0
	while (train_iter< train_iters or train_iters==-1):
		eval_iter= 0
		print "Train iteration:", train_iter
		while (eval_iter< eval_iters):
			total_iters= eval_iter+eval_iters*train_iter
			tasks= {}; std_outs= {}; std_errs= {}; taskcheck= "";
			for dataset in params_list:
				tasks[dataset]= {}
				std_outs[dataset]= {}
				std_errs[dataset]= {}
			print "Eval iteration:", eval_iter, "total iters:", total_iters
			if train_iters!=-1:
				perbs= [[0 for i in range(param_count)] for j in range(sample_size)]
			else:
				perbs= [[0 for i in range(len(init_params[3]))] for j in range(sample_size)] 
			for x in range(0, len(perbs)):
				if (x%2==0):
					for y in range(0, len(perbs[x])):
						perbs[x][y]= float(random.randint(0, 1)*2-1)
						#perbs[x][y]= float(random.randint(0, 2)-1)
					if (perbs[:x].__contains__(perbs[x])):
						x-= 1
				else:
					for y in range(0, len(perbs[x])):
						perbs[x][y]= -perbs[x-1][y]
			point= 0
			new_params= {}
			while (point<sample_size):
				new_params_file= params_file+"_"+str(train_iter)+"_"+str(eval_iter)+"_"+str(point)
				new_params[new_params_file]= copy.deepcopy(old_params[locale])
				locale= (locale+1)%len(old_params)
				if train_iters!=-1:
					new_params[new_params_file][2]= sample_point(new_params[new_params_file][2], perbs[point], 0)
					new_params[new_params_file][3]= sample_point(new_params[new_params_file][3], perbs[point], len(old_params[0][2]))
				else:
					new_params[new_params_file][3]= sample_point(new_params[new_params_file][3], perbs[point], 0)
					
				for dataset in params_list:
					new_params_file2= dataset+"_"+str(train_iter)+"_"+str(eval_iter)+"_"+str(point)
					if new_params_file2!= new_params_file:
						new_params[new_params_file2]= copy.deepcopy(params_list[dataset])
						new_params[new_params_file2][0]= copy.deepcopy(new_params[new_params_file][0])
						new_params[new_params_file2][2]= copy.deepcopy(new_params[new_params_file][2])
						new_params[new_params_file2][3]= copy.deepcopy(new_params[new_params_file][3])
					write_params(new_params[new_params_file2], new_params_file2)
					run= ""
					#if train_iters!=-1:
					run+= init_params[0]["run_cmd"]
					#else:
					#	run+= init_params[0]["eval_cmd"]
					#new_params[new_params_file2][1]["model_file"]= params_file.split(".txt")[0]+".mdl"
					#for x in ["save_model", "load_model", "entailment", "model_use", "binary_model", "powerset_model"]:
					#	if init_params[0].has_key(x):
					#		run+= " -"+x+" "+ init_params[0][x]

					for x in new_params[new_params_file2][1]:
						line= new_params[new_params_file2][1][x]
						if x.endswith("_file") and not(line.startswith("/")):
							line= (new_params[new_params_file2][1]["workdir"]+"/"+line).replace("//", "/")
						run+= " -"+ x+(" "+line).replace(" -", " \\\-")
						#run+= " -"+ x+" "+new_params[new_params_file2][1][x]
					for x in new_params[new_params_file2][2]:
						entry= transform(float(new_params[new_params_file2][2][x][0]), new_params[new_params_file2][2][x][-1], -1)
						run+= " -"+ x+(" "+str(entry)).replace(" -", " \\\-")
					for x in new_params[new_params_file2][3]:
					#print x, new_params[new_params_file2][3][x]
						entry= transform(float(new_params[new_params_file2][3][x][0]), new_params[new_params_file2][3][x][-1], -1)
						run+= " -"+ x+(" "+str(entry)).replace(" -", " \\\-")
				
					run_file= open(new_params_file2+".run", 'w')
					#run_file.writelines("cd "+ init_params[1]["workdir"]+"\n")
					for x in range(0, nfold):
						run2= run
						new_params_file3= new_params_file2
						if nfold>1:
							new_params_file3= new_params_file2 + "_" + str(x)
							run2= run2.replace(new_params[new_params_file2][1]["train_file"],
									   new_params[new_params_file2][1]["train_file"]+"_"+str(x))
							run2= run2.replace(new_params[new_params_file2][1]["test_file"],
									   new_params[new_params_file2][1]["test_file"]+"_"+str(x))
						run2+= " > "+ new_params_file3+".log"
						run_file.writelines(run2+"\n")
				#print run2
					run_file.close()
					commands.getoutput("chmod u+x "+ new_params_file2+".run")
					tasks[dataset][new_params_file2]= scheduler+" ./"+new_params_file2+".run"
					std_outs[dataset][new_params_file2]= '/dev/null'
					std_errs[dataset][new_params_file2]= '/dev/null'

				point+= 1
			if total_iters>= 0:
				for dataset in params_list:
					x= 1
					parallize_processes(max_procs, tasks[dataset], std_outs[dataset], std_errs[dataset], task_check)
			max_score= -10000000000000.0
			
			for z in sorted(tasks.keys()):
				for x in sorted(tasks[z].keys()):
					x3= x
					ndx= len(x)
					ndx= x.rfind("_", 0, ndx)
					ndx= x.rfind("_", 0, ndx)
					ndx= x.rfind("_", 0, ndx)
					x= x[ndx:len(x)]
					for y in range(0, nfold):
						if nfold>1:
							x3= z+x+"_" + str(y)
						if(not(os.path.exists(x3+".log"))):
							score= 0.000001
						else:
							score= commands.getoutput(score_cmd.replace("#OUTFILE#", x3+".log"))
						if score=='':
							score= 0.000001
						score2= score= float(score)
						train_time= test_time= 0
						times= commands.getoutput(time_cmd.replace("#OUTFILE#", x3+".log")).split("\n")
						t1= float(times[0].split(":")[1])
						train_time= t1/1000
						t2= times[1].split(":")[1].split(" ")
						test_time+= (float(t2[0])-t1)/float(t2[1])
						if not(points.has_key(x)):
							points[x]= [copy.deepcopy(new_params[params_file+x]), 0, 0, 0, 0]
						points[x][1]+= score/nfold/len(params_list)
						points[x][2]+= score2/nfold/len(params_list)
						points[x][3]+= test_time/nfold/len(params_list)
						points[x][4]+= train_time/nfold/len(params_list)
			max_point= ""
			for x in points:
			  	score= points[x][2]
				print x, score
				if GE(score, max_score):
					if GE(score, old_score):
						if GT(score, max_score):
							#old_point= points[x][-1]
							old_point= params_file+x
							locale= 0
							for y in range(1, len(old_params)):
								old_params.pop(y)
						else:
							#print "#", score, x, len(old_params)
							old_params[len(old_params)]= copy.deepcopy(points[x][0])
					max_score= score
					max_point= x

			if eval_iter==0:
				old_params= combine_params(old_params, points, 2, old_score)
				old_params= combine_params(old_params, points, 3, old_score)
			else:
				old_params= combine_params(old_params, points, 3, old_score)

			test_time= 0
			train_time= 0
			if GE(max_score, old_score):
				test_time= points[max_point][3]
				train_time= points[max_point][4]
			print "Iter results:", old_score, max_score, train_time, test_time, old_point, len(old_params)
			if GE(max_score, old_score):
				old_score= max_score
				old_params[0][0]["score"]= str(max_score)
				old_params[0][0]["train_time"]= str(train_time)
				old_params[0][0]["test_time"]= str(test_time)
			points= {}
			#write_params(old_params[0], params_file+"_"+str(train_iter)+"_"+str(eval_iter))
			for dataset in params_list:
				new_params_file= dataset+"_"+str(train_iter)+"_"+str(eval_iter)
				tmp_params= copy.deepcopy(params_list[dataset])
				tmp_params[0]["score"]= old_params[0][0]["score"]
				tmp_params[0]["train_time"]= old_params[0][0]["train_time"]
				tmp_params[0]["test_time"]= old_params[0][0]["test_time"]
				tmp_params[0]= copy.deepcopy(old_params[0][0])
				tmp_params[2]= copy.deepcopy(old_params[0][2])
				tmp_params[3]= copy.deepcopy(old_params[0][3])
				write_params(tmp_params, new_params_file)
			eval_iter+= 1
		if train_iters==-1:
			break
		train_iter+= 1
	commands.getoutput("rm *.run.e*")
	commands.getoutput("rm *.run.o*")
