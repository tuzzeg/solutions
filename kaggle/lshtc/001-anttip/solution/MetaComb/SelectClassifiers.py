import sys
import re
import commands
import random

#1 10 11 18 25 27 28 30 42 46 49 56 57 59 62 64 65 66 68 0.535366401240325 0.5353589423841909
#start_list= "results_40x8_mnb_mafs2_s6_lp_u_jm2_bm18ti_pct0_ps5_thr16.results results_40x8_mnb_mafs3_s2_u_lp_jm2_bm18tib_pct0_ps7_iw0_thr16.results results_40x8_mnb_mafs3_s2_uc1_jm2_bm18tid_pci7_pct0_ps8_iw1_thr16.results results_40x8_mnb_mafs3_s4_u_jm2_bm18tib_mc0_pci6_pct0_ps7_cs0_iw0_thr16.results results_40x8_mnb_mafs_s0_lp_u_jm2_bm18tib_mc0_pct0_ps5_thr16.results results_40x8_mnb_mafs_s1_kd_nobo_bm25c2_mc0_mlc0_ps2_lt5_mr0_tk2_thr16.results results_40x8_mnb_mafs_s1_lp_u_jm4_pd2_tXiX2_fb2_mc0_pct0_ps0_thr16.results results_40x8_mnb_mafs_s2_lp_u_jm4_bm18ti_mc0_pct0_ps2_thr16.results results_40x8_mnb_mifs_s1_lp_u_jm2_bm18tib_mc0_pct0_ps5_thr16.results results_40x8_mnb_mifs_s2_lp_u_jm2_bm18tib_fb3_mc0_pct0_ps5_thr16.results results_40x8_mnb_mjac_s0_lp_u_jm2_pd2_tXiX3_fb2_mc0_pci1_pct0_ps0_thr16.results results_40x8_mnb_mjac_s0_lp_u_jm2_tiX3_mc0_pct0_ps0_thr16.results results_40x8_mnb_mjac_s0_lp_u_jm4_bm15ti_mc0_pct0_ps0_thr16.results results_40x8_mnb_mjac_s1_u_jm2_tiX1_mc0_pct0_mlc0_ps2_lt2_mr0_tk0_thr16.results"
start_list= "results_40x8_mnb_mafs2_s6_lp_u_jm2_bm18ti_pct0_ps5_thr16.results results_40x8_mnb_mafs3_s2_u_lp_jm2_bm18tib_pct0_ps7_iw0_thr16.results results_40x8_mnb_mafs3_s2_uc1_jm2_bm18tid_pci7_pct0_ps8_iw1_thr16.results results_40x8_mnb_mafs3_s4_u_jm2_bm18tib_mc0_pci6_pct0_ps7_cs0_iw0_thr16.results results_40x8_mnb_mafs_s0_lp_u_jm2_bm18tib_mc0_pct0_ps5_thr16.results results_40x8_mnb_mafs_s1_kd_nobo_bm25c2_mc0_mlc0_ps2_lt5_mr0_tk2_thr16.results results_40x8_mnb_mafs_s1_lp_u_jm4_pd2_tXiX2_fb2_mc0_pct0_ps0_thr16.results results_40x8_mnb_mafs_s2_lp_u_jm4_bm18ti_mc0_pct0_ps2_thr16.results results_40x8_mnb_mifs_s0_uc2_jm7_tXiX2_ld3_thr16.results results_40x8_mnb_mifs_s1_lp_u_jm2_bm18tib_mc0_pct0_ps5_thr16.results results_40x8_mnb_mifs_s1_uc2_jm7_qidfX2_ld3_thr16.results results_40x8_mnb_mifs_s2_lp_u_jm2_bm18tib_fb3_mc0_pct0_ps5_thr16.results results_40x8_mnb_mifs_s2_uc2_jm5_bm18tif_ld2_thr16.results results_40x8_mnb_mifs_s2_uc_jm7_qidfX2_fb4_ld4_thr16.results results_40x8_mnb_mifs_s2_uc_pd_jm7_ld4_thr16.results results_40x8_mnb_mjac_s0_lp_u_jm2_pd2_tXiX3_fb2_mc0_pci1_pct0_ps0_thr16.results results_40x8_mnb_mjac_s0_lp_u_jm2_tiX3_mc0_pct0_ps0_thr16.results results_40x8_mnb_mjac_s0_lp_u_jm4_bm15ti_mc0_pct0_ps0_thr16.results results_40x8_mnb_mjac_s1_u_jm2_tiX1_mc0_pct0_mlc0_ps2_lt2_mr0_tk0_thr16.results"

search= 1
random.seed(0)
#z= random.randint(0, train_count-1)

num_classifiers= int(commands.getoutput("ls comb_dev_results3/*.results | wc -l"))
best_classifier= []
classifiers= {}
frontier= [""]
#frontier= ["0 1 10 22 24 25 27 39 4 52 54 63"]
classifier_names= commands.getoutput("ls comb_dev_results3/*.results").replace("comb_dev_results3/", "").split("\n")
classifier_names.sort()
z= 0
for x in classifier_names:
	print z, x
	if start_list.__contains__(x):
		frontier[0]+=" "+str(z)
	z+=1
print frontier
scores= {}
max_score= 0
max_setting= ""
#print commands.getoutput("/usr/bin/javac MetaComb.java -cp .:*")
#command= "/usr/bin/java -Xmx15600M -cp .:* MetaComb"
print commands.getoutput("/usr/bin/javac MetaComb2.java -cp .:*")
command= "/usr/bin/java -Xmx15600M -cp .:* MetaComb2"
while len(frontier)>0:
	setting= frontier.pop(0)
	args= " "+setting
	#args+= " 10 11 12 13 14 15 16 17 18 19 20 21"
	#print command+args
	score= commands.getoutput(command+args) #.split(":")[-1]
	#print score
	score= score.split(":")[-1]
	scores[setting]= score 
	print command+args, score, max_score
	if score>max_score:
		diff= {}
		max_setting= setting
		max_score= score
		if search<-1:
			frontier= [""]
		if search>0:
			for x in xrange(0, num_classifiers):
				if (setting.split(" ").__contains__(str(x)) or len(setting.split(" "))==num_classifiers-2):
					continue
				setting2= " ".join(sorted((setting+" "+str(x)).strip().split(" ")))
				#print scores.has_key(setting2), frontier.__contains__(setting2)
				if not(scores.has_key(setting2)or frontier.__contains__(setting2)):
					#frontier.append(setting2)
					frontier.insert(random.randint(0, len(frontier)+1), setting2)
		if search<0:
			continue
		s= setting.split(" ")
		for x in xrange(0, len(s)):
			setting2= (" ".join(s[0:x])+" "+" ".join(s[x+1:len(s)])).strip()
			#print scores.has_key(setting2), frontier.__contains__(setting2)
			if not(scores.has_key(setting2) or frontier.__contains__(setting2)):
				#frontier.append(setting2)
				frontier.insert(random.randint(0, len(frontier)+1), setting2)
	print setting, score, frontier
print max_setting, max_score
#for x in xrange(0, num_classifiers-1):

