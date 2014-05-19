import copy
import re

params_file= "templates/mnb_c_jm.template"
prefix= "templates/mnb_"
#0           
#configs= "mjac_lp_u_jm2_tiX3_mc0_pct0_ps0_thr16".split(" ")
#configs= "mjac_lp_u_jm4_bm15ti_mc0_pct0_ps0_thr16".split(" ")
#configs= "mjac_lp_u_jm_pd_dp_tXiX2_mc0_pct0_ps0_thr16".split(" ")
#configs= "mjac_lp_u_jm_pd_tXiX2_mc0_pct0_ps0_thr16".split(" ")
#configs= "mjac_lp_u_jm3_pd_tXiX2_mc0_pct0_ps0_thr16".split(" ")
#configs= "mjac_lp_bm25c1_mc0_mlc0_ps3_thr16".split(" ")
#configs= "mafs_s0_lp_u_jm3_pd2_bm19ti_mc0_pct0_ps2_thr16".split(" ")
#configs= "mafs_s0_lp_u_jm2_bm18tib_mc0_pct0_ps5_thr16".split(" ")
#configs= "mafs_s0_lp_u_jm2_bm18tic_fb3_mc0_pct0_ps6_thr16".split(" ")
#configs= "mafs3_s0_kd_nobo_bm25c2_mi2_ps2_iw0_thr16".split(" ")
#configs= "mifs_s0_uc2_jm5_bm18tif_ld2_thr16".split(" ") 
#configs= "mifs_s0_uc2_jm5_bm18tif_fb4_ld2_thr16".split(" ") 
#configs= "mjac_s0_u_jm3_bm18ti_pct0_ps5_je_thr16".split(" ")
#configs= "mifs_s0_uc2_jm7_qidfX2_ld3_thr16".split(" ") 
#configs= "mifs_s0_uc_pd_jm7_ld4_thr16".split(" ") 
#configs= "mifs_s0_uc_jm7_bm18tig_ld4_thr16".split(" ")
#1
#configs= "mjac_u_jm2_tiX1_mc0_pct0_mlc0_ps2_lt2_mr0_tk0_thr16".split(" ")
#configs= "mafs_s1_lp_u_jm6_tiX5_mc0_pct0_ps0_thr16".split(" ")
#configs= "mafs_s1_lp_u_jm4_pd2_tXiX2_fb2_mc0_pct0_ps0_thr16".split(" ")
#configs= "mafs_s1_kd_nobo_bm25c2_mc0_mlc0_ps2_lt5_mr0_tk2_thr16".split(" ")
#configs= "mafs_s1_lp_u_jm2_bm18tib_vn_mc0_pct0_ps5_thr16".split(" ")
#configs= "mifs_s1_lp_u_jm2_bm18tib_mc0_pct0_ps5_thr16".split(" ")
#configs= "mafs3_s1_u_jm3_bm18ti_pct0_ps7_iw0_thr16".split(" ")
#configs= "mifs_s1_uc2_jm5_bm18tif_ld2_thr16".split(" ") 
#configs= "mifs_s1_uc2_jm7_qidfX2_ld3_thr16".split(" ") 
#configs= "mifs_s1_uc_jm7_qidfX2_fb4_ld4_thr16".split(" ") 
#configs= "mifs_s1_uc_jm7_tXiX2_ld4_thr16".split(" ") 
#configs= "mifs_s1_uc_jm7_bm18tig_ld4_thr16".split(" ")
#configs= "mifs_s1_uc_pd_jm7_ld4_thr16".split(" ")
configs= "mifs_s1_uc_jm7_ti_ld4_thr16".split(" ")
#2
#configs= "mjac_s2_kd_nobo_bm25c2_mc0_mlc0_ps2_lt5_mr0_tk1_thr16".split(" ")
#configs= "mifs_s2_lp_u_jm6_tiX5_mc0_pct0_ps0_thr16".split(" ")
#configs= "mjac_kd_u_jm3_kdp_tiX2_mc0_pci5_pct0_mlc0_ps5_lt5_mr0_tk0_thr16".split(" ")
#configs= "mafs_s2_lp_u_jm4_bm18ti_mc0_pct0_ps2_thr16".split(" ")
#configs= "mifs_s2_lp_u_jm2_bm18tib_fb3_mc0_pct0_ps5_thr16".split(" ")
#configs= "mafs2_s2_lp_u_jm2_bm18tib_mc0_pct0_ps5_thr16".split(" ")
#configs= "mafs3_s2_uc1_jm2_bm18tid_pci7_pct0_ps8_iw1_thr16".split(" ")
#configs= "mifs_s2_uc2_jm5_bm18tif_ld2_thr16".split(" ") 
#configs= "mifs_s2_uc2_jm7_qidfX2_ld3_thr16".split(" ") 
#configs= "mifs_s2_uc_jm7_qidfX2_fb4_ld4_thr16".split(" ") 
#configs= "mifs_s2_uc_jm7_tXiX2_ld4_thr16".split(" ") 
#configs= "mifs_s2_uc_pd_jm7_ld4_thr16".split(" ") 
#configs= "mifs_s2_uc_jm7_ti_ld4_thr16".split(" ")
#3
#configs= "ndcg5b_s3_kd_u_jm2_kdp5_bm18tib_mc0_pci0_pct0_mlc0_ps6_tk0_thr16".split(" ")
#configs= "mafs3_s5_u_jm2_bm18tib_mc0_pci6_pct0_ps7_cs0_iw0_thr16".split(" ")
#4 
#configs= "mafs_s4_kd_u_jm3_kdp5_tXiX2_mc0_pci0_pct0_mlc0_ps5_lt5_mr0_tk2_thr16".split(" ")
#configs= "mafs3_s4_kd_u_jm3_kdp5_bm18ti_pct0_ps7_iw0_thr16".split(" ")
#5
#configs= "mafs_s5_kd_u_jm3_kdp1_bm18ti_mc0_pci0_pct0_mlc0_ps5_lt5_mr0_tk2_thr16".split(" ")
#configs= "mafs3_s5_u_jm2_bm18tib_pct0_ps6_iw0_thr16".split(" ")
#configs= "mafs3_s5_kd_uc1_jm2_kdp5_bm18tid_pct0_ps8_iw1_thr16".split(" ")
#6
#configs= "mafs2_s6_lp_u_jm2_bm18ti_pct0_ps5_thr16".split(" ")
#configs= "mafs_s6_kd_u_jm3_kdp1_bm18ti_mc0_pci1_pct0_ps5_lt5_mr1_tk2_ch80_thr16".split(" "
#configs= "mafs3_s6_kd_uc1_jm2_kdp5_bm18tid_mc0_pci1_pct0_ps8_iw1_ch80_thr16".split(" ")
#7
#configs= "mafs2_s7_lp_u_jm2_bm18ti_pct0_ps5_thr16".split(" ")
#configs= "mafs_s7_kd_u_jm3_kdp1_bm18ti_mc0_pci1_pct0_ps5_lt5_mr1_tk2_ch80_thr16".split(" ")
#configs= "mafs3_s7_kd_uc1_jm2_kdp5_bm18tid_mc0_pci1_pct0_ps8_iw1_ch80_thr16".split(" ")
#8
#configs= "mafs2_s8_lp_u_jm2_bm18ti_pct0_ps5_thr16".split(" ")
#configs= "mafs_s8_kd_u_jm3_kdp1_bm18ti_mc0_pci1_pct0_ps5_lt5_mr1_tk2_ch80_thr16".split(" ")
#9
#configs= "mafs2_s9_lp_u_jm2_bm18ti_pct0_ps5_thr16".split(" ")
#configs= "mafs_s9_kd_u_jm3_kdp1_bm18ti_mc0_pci1_pct0_ps5_lt5_mr1_tk2_ch80_thr16".split(" ")

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
        return variable

def write_params(params, params_file):
        file= open(params_file, 'w')
        for x in sorted(params[0].keys()):
                file.writelines(x+" "+ params[0][x]+"\n")
        file.writelines("#"+"\n")
        for x in sorted(params[1].keys()):
                file.writelines(x+" "+ params[1][x]+"\n")
        file.writelines("#"+"\n")
        for x in sorted(params[2].keys()):
                t=""
                for y in range(0, len(params[2][x])):
                        entry= params[2][x][y]
                        t+= str(entry)+" "
                file.writelines(x+" "+ t.strip()+"\n")
        file.writelines("#"+"\n")
        for x in sorted(params[3].keys()):
                t=""
                for y in range(0, len(params[3][x])):
                        entry= params[3][x][y]
                        t+= str(entry)+" "
                file.writelines(x+" "+ t.strip()+"\n")

init_params= read_params(params_file)
for config in configs:
    new_params= copy.deepcopy(init_params)
    param_file= prefix+config+".template"
    print param_file
    config= config.replace("u_c", "uc")
    config= config.replace("vsm_ti", "vsmti")
    new_params[3]["jelinek_mercer"]= "0.0 0.0 1.0 0.0 F=".split(" ")
    new_params[3]["kernel_jelinek_mercer"]= "0.0 0.0 1.0 0.0 F=".split(" ")
    
    mods= config.split("_")
    for mod in mods:
        if mod=="uc":
            new_params[3]["bg_unif_smooth"]= "0.0 0.0 1.0 0.0 F".split(" ")
        if mod=="c":
            new_params[3]["bg_unif_smooth"]= "0.0 0.0 1.0 0.0 F=".split(" ")
        if mod=="u":
            new_params[3]["bg_unif_smooth"]= "1.0 0.0 1.0 0.0 F=".split(" ")
        if mod=="c0":
            new_params[3]["bg_unif_smooth"]= "-1.0 0.0 1.0 0.0 F=".split(" ")
        if mod=="jm":
            new_params[3]["jelinek_mercer"]= "0.0 0.0 1.0 0.0 F".split(" ")
        if mod=="ad":
            new_params[3]["absolute_discount"]= "0.0 0.0 1.0 0.0 F".split(" ")
        if mod=="pd":
            new_params[3]["powerlaw_discount"]= "0.0 0.0 1.0 0.0 F".split(" ")
        if mod=="pd0":
            new_params[3]["powerlaw_discount"]= "-1.0 0.0 1.0 0.0 F=".split(" ")
        if mod=="pd-":
            new_params[3]["powerlaw_discount"]= "0.0 -20.0 0.0 0.0 F".split(" ")
        if mod=="dp":
            new_params[3]["dirichlet_prior"]= "0.0 0.0 1.0 0.0 F".split(" ")
        if mod=="dp0":
            new_params[3]["dirichlet_prior"]= "-1.0 0.0 1.0 0.0 F=".split(" ")
        if mod=="dp-":
            new_params[3]["dirichlet_prior"]= "0.0 -2.0 0.0 0.0 F".split(" ")
        if mod=="qidf":
            new_params[1]["use_tfidf"] = "8"
        if mod=="qicf":
            new_params[1]["use_tfidf"] = "-8"
        if mod=="ti":
            new_params[1]["use_tfidf"] = "1"
        if mod=="ps":
            new_params[3]["prior_scale"] = "1.0 0.0 2.0 0.0 F".split(" ")
	if mod=="kpd":
            new_params[3]["kernel_powerlaw_discount"]= "0.0 0.0 1.0 0.0 F".split(" ")
	if mod=="kpd0":
            new_params[3]["kernel_powerlaw_discount"]= "-1.0 0.0 1.0 0.0 F=".split(" ")
        if mod=="kjm":
            new_params[3]["kernel_jelinek_mercer"]= "0.0 0.0 1.0 0.0 F".split(" ")
        if mod=="kjm2":
            new_params[3]["kernel_jelinek_mercer"]= "0.998 0.995 0.999 0.0 1/l".split(" ")
	if mod=="kdp":
            new_params[3]["kernel_dirichlet_prior"]= "0.0 0.0 1.0 0.0 F".split(" ")
	if mod=="kdp0":
            new_params[3]["kernel_dirichlet_prior"]= "-1.0 0.0 1.0 0.0 F=".split(" ") 
	if mod=="kdp5":
            new_params[3]["kernel_dirichlet_prior"]= "0.01 0.0 0.1 0.0 F".split(" ")
	if mod=="kdp1":
            new_params[3]["kernel_dirichlet_prior"]= "0.01 0.0 0.1 0.0 l/".split(" ")
	if mod=="qidfX":
            new_params[1]["use_tfidf"] = "8"
	    new_params[2]["idf_lift"] = "0.0 -1.0 50.0 49.0 F".split(" ")
        if mod=="tiX":
            new_params[1]["use_tfidf"] = "1"
	    new_params[2]["idf_lift"] = "0.0 -1.0 50.0 49.0 F".split(" ")
        if mod=="tXi":
            new_params[1]["use_tfidf"] = "1"
	    new_params[2]["length_scale"] = "0.0 -1.0 2.0 49.0 F".split(" ")
        if mod=="tXiX":
            new_params[1]["use_tfidf"] = "1"
	    new_params[2]["idf_lift"] = "0.0 -1.0 50.0 49.0 F".split(" ")
	    new_params[2]["length_scale"] = "0.0 -1.0 2.0 49.0 F".split(" ")
        if mod=="tXiX2":
            new_params[1]["use_tfidf"] = "1"
	    new_params[2]["idf_lift"] = "1000.0 100.0 2000.0 49.0 F".split(" ")
	    new_params[2]["length_scale"] = "0.3 0.0 0.5 49.0 F".split(" ")
	if mod=="tXiX3":
            new_params[1]["use_tfidf"] = "1"
	    new_params[2]["idf_lift"] = "1000.0 100.0 5000.0 49.0 F".split(" ")
	    new_params[2]["length_scale"] = "0.0 -1.0 2.0 49.0 F".split(" ")
	if mod=="tXiX4":
            new_params[1]["use_tfidf"] = "1"
	    new_params[2]["idf_lift"] = "-1.0 100.0 5000.0 49.0 F=".split(" ")
	    new_params[2]["length_scale"] = "0.0 -1.0 2.0 49.0 F".split(" ")
        if mod=="lt":
            new_params[3]["label_threshold"]= "-0.01 -0.5 0.0 0.0 F".split(" ")
        if mod=="mr":
            new_params[3]["max_retrieved"]= "5.0 1.0 10.0 0.0 F".split(" ")
        if mod=="ap":
            new_params[3]["add_prune"]= "-1.0 -60.0 -0.05 0.0 F".split(" ")
        if mod=="nq":
            new_params[3]["neg_ql"]= "0.0 0.0 0.5 0.0 F".split(" ")
	if mod=="fb":
		new_params[1]["norm_posteriors"]= ""
		new_params[3]["feedback"]= "0.0 0.0 1.0 0.0 F".split(" ")
	if mod=="fb-":
		new_params[1]["norm_posteriors"]= ""
		new_params[3]["feedback"]= "0.0 -1.0 0.0 0.0 F".split(" ")
	if mod=="mjac":
		new_params[0]["score_cmd"]= "tail #OUTFILE# | grep Results: | tr ' ' '\\n' | grep meanJaccard: | tr ':' '\\n' | tail -n 1"
	if mod=="map":
		new_params[1]["constrain_labels"]= "1"
		new_params[1]["full_posteriors"]= ""
		new_params[1]["debug"]= "-1"
		new_params[1]["max_retrieved"]= "1000000"
		new_params[3]["top_k"]= "10000000.0 0.0 1.0 0.0 F=".split(" ")
		new_params[0]["score_cmd"]= "tail #OUTFILE# | grep Results: | tr ' ' '\\n' | grep MAP: | tr ':' '\\n' | tail -n 1"
	if mod=="map50":
		new_params[1]["constrain_labels"]= "1"
		new_params[1]["max_retrieved"]= "50"
		new_params[3]["top_k"]= "50.0 0.0 1.0 0.0 F=".split(" ")
		new_params[0]["score_cmd"]= "tail #OUTFILE# | grep Results: | tr ' ' '\\n' | grep MAP@k: | tr ':' '\\n' | tail -n 1"
	if mod=="ndcg":
		new_params[1]["constrain_labels"]= "1"
		new_params[1]["full_posteriors"]= ""
		new_params[1]["debug"]= "-1"
		new_params[1]["max_retrieved"]= "50"
		new_params[3]["top_k"]= "50.0 0.0 1.0 0.0 F=".split(" ")
		new_params[0]["score_cmd"]= "tail #OUTFILE# | grep Results: | tr ' ' '\\n' | grep NDCG: | tr ':' '\\n' | tail -n 1"
	if mod=="ndcg5":
		new_params[1]["constrain_labels"]= "1"
		new_params[1]["full_posteriors"]= ""
		new_params[1]["max_retrieved"]= "5"
		new_params[3]["top_k"]= "5.0 0.0 1.0 0.0 F=".split(" ")
		new_params[0]["score_cmd"]= "tail #OUTFILE# | grep Results: | tr ' ' '\\n' | grep NDCG@k: | tr ':' '\\n' | tail -n 1"
	if mod=="ndcg10":
		new_params[1]["constrain_labels"]= "1"
		new_params[1]["full_posteriors"]= ""
		new_params[1]["max_retrieved"]= "10"
		new_params[3]["top_k"]= "10.0 0.0 1.0 0.0 F=".split(" ")
		new_params[0]["score_cmd"]= "tail #OUTFILE# | grep Results: | tr ' ' '\\n' | grep NDCG@k: | tr ':' '\\n' | tail -n 1"
	if mod=="ndcg20":
		new_params[1]["constrain_labels"]= "1"
		new_params[1]["full_posteriors"]= ""
		new_params[1]["max_retrieved"]= "20"
		new_params[3]["top_k"]= "20.0 0.0 1.0 0.0 F=".split(" ")
		new_params[0]["score_cmd"]= "tail #OUTFILE# | grep Results: | tr ' ' '\\n' | grep NDCG@k: | tr ':' '\\n' | tail -n 1"
	if mod=="ndcg50":
		new_params[1]["constrain_labels"]= "1"
		new_params[1]["full_posteriors"]= ""
		new_params[1]["max_retrieved"]= "50"
		new_params[3]["top_k"]= "50.0 0.0 1.0 0.0 F=".split(" ")
		new_params[0]["score_cmd"]= "tail #OUTFILE# | grep Results: | tr ' ' '\\n' | grep NDCG@k: | tr ':' '\\n' | tail -n 1"
	if mod=="ndcg100":
		new_params[1]["constrain_labels"]= "1"
		new_params[1]["full_posteriors"]= ""
		new_params[1]["max_retrieved"]= "100"
		new_params[3]["top_k"]= "100.0 0.0 1.0 0.0 F=".split(" ")
		new_params[0]["score_cmd"]= "tail #OUTFILE# | grep Results: | tr ' ' '\\n' | grep NDCG@k: | tr ':' '\\n' | tail -n 1"
	if mod=="ndcg200":
		new_params[1]["constrain_labels"]= "1"
		new_params[1]["full_posteriors"]= ""
		new_params[1]["max_retrieved"]= "200"
		new_params[3]["top_k"]= "200.0 0.0 1.0 0.0 F=".split(" ")
		new_params[0]["score_cmd"]= "tail #OUTFILE# | grep Results: | tr ' ' '\\n' | grep NDCG@k: | tr ':' '\\n' | tail -n 1"
	if mod=="prec10":
		new_params[1]["constrain_labels"]= "1"
		new_params[1]["max_retrieved"]= "10"
		new_params[3]["top_k"]= "10.0 0.0 1.0 0.0 F=".split(" ")
		new_params[0]["score_cmd"]= "tail #OUTFILE# | grep Results: | tr ' ' '\\n' | grep Prec@k: | tr ':' '\\n' | tail -n 1"
	if mod=="prec50":
		new_params[1]["constrain_labels"]= "1"
		new_params[1]["max_retrieved"]= "50"
		new_params[3]["top_k"]= "50.0 0.0 1.0 0.0 F=".split(" ")
		new_params[0]["score_cmd"]= "tail #OUTFILE# | grep Results: | tr ' ' '\\n' | grep Prec@k: | tr ':' '\\n' | tail -n 1"
	if mod=="bm25a":
		new_params[1]["use_tfidf"] = "11"
		new_params[2]["length_scale"] = "0.7 0.1 1.0 0.0 F".split(" ")
		new_params[2]["length_norm"] = "2.0 0.0 6.0 0.0 F".split(" ")
	if mod=="bm25b":
		new_params[1]["use_tfidf"] = "9"
		new_params[2]["length_scale"] = "0.7 0.1 1.0 0.0 F".split(" ")
		new_params[2]["length_norm"] = "2.0 0.0 6.0 0.0 F".split(" ")
		new_params[2]["idf_lift"] = "500.0 0.0 1000.0 0.0 F".split(" ")
	if mod=="bm25c":
		new_params[1]["use_tfidf"] = "10"
		new_params[2]["length_scale"] = "0.7 0.1 1.0 0.0 F".split(" ")
		new_params[2]["length_norm"] = "2.0 0.0 6.0 0.0 F".split(" ")
		new_params[2]["idf_lift"] = "500.0 0.0 1000.0 0.0 F".split(" ")
	if mod=="bm25c1":
		new_params[1]["use_tfidf"] = "10"
		new_params[2]["length_scale"] = "1.0 0.7 1.0 0.0 F".split(" ")
		new_params[2]["length_norm"] = "4.0 2.0 6.0 0.0 F".split(" ")
		new_params[2]["idf_lift"] = "4.0 2.0 6.0 0.0 F".split(" ")
	if mod=="bm25c2":
		new_params[1]["use_tfidf"] = "10"
		new_params[2]["length_scale"] = "1.0 0.7 1.0 0.0 F".split(" ")
		new_params[2]["length_norm"] = "2.0 1.0 5.0 0.0 F".split(" ")
		new_params[2]["idf_lift"] = "2.0 1.0 5.0 0.0 F".split(" ")
	if mod=="bm25d":
		new_params[1]["use_tfidf"] = "10"
		new_params[2]["length_scale"] = "1.0 0.5 1.0 0.0 F".split(" ")
		new_params[2]["length_norm"] = "4.0 2.0 6.0 0.0 F".split(" ")
		new_params[2]["idf_lift"] = "0.0 0.0 5.0 0.0 F".split(" ")
	if mod=="bm15ti":
		new_params[1]["use_tfidf"] = "15"
		new_params[2]["length_scale"] = "0.3 -0.5 1.0 0.0 F".split(" ")
		new_params[2]["length_norm"] = "2.0 0.0 6.0 0.0 F".split(" ")
		new_params[2]["idf_lift"] = "1500.0 1000.0 2000.0 0.0 F".split(" ")
	if mod=="bm18ti":
		new_params[1]["use_tfidf"] = "18"
		new_params[2]["length_scale"] = "0.5 0.0 1.0 0.0 F".split(" ")
		new_params[2]["length_norm"] = "4.0 1.0 6.0 0.0 F".split(" ")
		new_params[2]["idf_lift"] = "1500.0 1000.0 2000.0 0.0 F".split(" ")
	if mod=="bm18tib":
		new_params[1]["use_tfidf"] = "18"
		new_params[2]["length_scale"] = "0.1 0.0 0.5 0.0 F".split(" ")
		new_params[2]["length_norm"] = "1.0 0.0 3.0 0.0 F".split(" ")
		new_params[2]["idf_lift"] = "2000.0 1500.0 4000.0 0.0 F".split(" ")
	if mod=="bm18tic":
		new_params[1]["use_tfidf"] = "18"
		new_params[2]["length_scale"] = "0.1 0.0 0.4 0.0 F".split(" ")
		new_params[2]["length_norm"] = "3.0 1.0 5.0 0.0 F".split(" ")
		new_params[2]["idf_lift"] = "4000.0 2000.0 10000.0 0.0 F".split(" ")
	if mod=="bm18tid":
		new_params[1]["use_tfidf"] = "18"
		new_params[2]["length_scale"] = "1.0 0.6 1.0 0.0 F".split(" ")
		new_params[2]["length_norm"] = "6.0 4.0 8.0 0.0 F".split(" ")
		new_params[2]["idf_lift"] = "1500.0 1000.0 2000.0 0.0 F".split(" ")
	if mod=="bm18tie":
		new_params[1]["use_tfidf"] = "18"
		new_params[2]["length_scale"] = "0.0 -1.0 0.0 0.0 F".split(" ")
		new_params[2]["length_norm"] = "2.0 1.0 4.0 0.0 F".split(" ")
		new_params[2]["idf_lift"] = "1000.0 1000.0 1500.0 0.0 F".split(" ")
	if mod=="bm18tif":
		new_params[1]["use_tfidf"] = "18"
		new_params[2]["length_scale"] = "0.0 -0.5 0.5 0.0 F=".split(" ")
		new_params[2]["length_norm"] = "1.0 0.5 3.0 0.0 F".split(" ")
		new_params[2]["idf_lift"] = "1000.0 500.0 1500.0 0.0 F".split(" ")
	if mod=="bm18tig":
		new_params[1]["use_tfidf"] = "18"
		new_params[2]["length_scale"] = "0.0 -0.5 0.5 0.0 F".split(" ")
		new_params[2]["length_norm"] = "1.0 0.5 5.0 0.0 F".split(" ")
		new_params[2]["idf_lift"] = "-1.0 500.0 1500.0 0.0 F=".split(" ")
	if mod=="bm20ti":
		new_params[1]["use_tfidf"] = "20"
		new_params[2]["length_scale"] = "0.5 0.0 1.0 0.0 F".split(" ")
		new_params[2]["length_norm"] = "4.0 1.0 6.0 0.0 F".split(" ")
		new_params[2]["idf_lift"] = "1500.0 1000.0 2000.0 0.0 F".split(" ")
	if mod=="bm23ti":
		new_params[1]["use_tfidf"] = "23"
		new_params[2]["length_scale"] = "0.1 0.0 0.5 0.0 F".split(" ")
		new_params[2]["length_norm"] = "1.0 0.0 3.0 0.0 F".split(" ")
		new_params[2]["idf_lift"] = "2000.0 1500.0 4000.0 0.0 F".split(" ")
	if mod=="vsm":
		new_params[1]["use_tfidf"] = "12"
		new_params[2]["length_scale"] = "0.0 0.1 1.0 0.0 F=".split(" ")
		new_params[2]["length_norm"] = "0.0 0.0 6.0 0.0 F=".split(" ")
		new_params[2]["idf_lift"] = "0.0 0.0 1000.0 0.0 F=".split(" ")
	if mod=="vsmti":
		new_params[1]["use_tfidf"] = "13"
		new_params[2]["length_scale"] = "0.0 0.1 1.0 0.0 F=".split(" ")
		new_params[2]["length_norm"] = "0.0 0.0 6.0 0.0 F=".split(" ")
		new_params[2]["idf_lift"] = "0.0 0.0 1000.0 0.0 F=".split(" ")
	if mod=="pci":
		new_params[2]["prune_count_insert"] = "-3.0 -7.0 -1.0 0.0 F".split(" ")
	if mod=="lp":
		new_params[1]["label_powerset"] = ""
	if mod=="kd":
		new_params[1]["kernel_densities"] = ""
        if mod=="mi":
		new_params[3]["min_idf"] = "1.0 0.0 3.0 49.0 F".split(" ")
        if mod=="mi2":
		new_params[3]["min_idf"] = "4.5 0.0 3.0 49.0 F=".split(" ")
	if mod=="tiX1":
		new_params[1]["use_tfidf"] = "1"
		new_params[2]["idf_lift"] = "1000.0 500.0 1500.0 49.0 F".split(" ")
	if mod=="tiX2":
		new_params[1]["use_tfidf"] = "1"
		new_params[2]["idf_lift"] = "1000.0 100.0 2000.0 49.0 F".split(" ")
	if mod=="tiX3":
		new_params[1]["use_tfidf"] = "1"
		new_params[2]["idf_lift"] = "1500.0 1300.0 1700.0 49.0 F".split(" ")
	if mod=="tiX5":
		new_params[1]["use_tfidf"] = "1"
		new_params[2]["idf_lift"] = "1500.0 1000.0 5000.0 49.0 F".split(" ")
        if mod=="mafs":
		new_params[0]["score_cmd"]= "tail #OUTFILE# | grep Results: | tr ' ' '\\n' | grep maFscore: | tr ':' '\\n' | tail -n 1"
        if mod=="mifs":
		new_params[0]["score_cmd"]= "tail #OUTFILE# | grep Results: | tr ' ' '\\n' | grep miFscore: | tr ':' '\\n' | tail -n 1"
	if mod=="fb2":
		new_params[1]["norm_posteriors"]= ""
		new_params[3]["feedback"]= "0.5 0.3 0.8 0.0 F".split(" ")
		new_params[3]["feedback_k"]= "50.0 20.0 200.0 0.0 F".split(" ")
	if mod=="fb3":
		new_params[1]["norm_posteriors"]= ""
		new_params[3]["feedback"]= "0.4 0.2 0.7 0.0 F".split(" ")
		new_params[3]["feedback_k"]= "300.0 100.0 500.0 0.0 F".split(" ")
	if mod=="fb4":
		new_params[1]["norm_posteriors"]= ""
		new_params[3]["feedback"]= "0.7 0.5 0.99 0.0 F".split(" ")
		new_params[3]["feedback_k"]= "100.0 10.0 200.0 0.0 F".split(" ")
	if mod=="fb-2":
		new_params[1]["norm_posteriors"]= ""
		new_params[3]["feedback"]= "-0.5 -1.0 0.0 0.0 F".split(" ")
		new_params[3]["feedback_k"]= "100.0 0.0 1.0 0.0 F=".split(" ")
	if mod=="vn":
		new_params[1]["vote_neighbours"]= ""
		new_params[3]["top_k"]= "20.0 3.0 100.0 0.0 F".split(" ")
        if mod=="ps0":
		new_params[3]["prior_scale"] = "0.7 0.5 1.0 0.0 F".split(" ")
        if mod=="ps1":
		new_params[3]["prior_scale"] = "1.4 1.0 3.0 0.0 F".split(" ")
        if mod=="ps2":
		new_params[3]["prior_scale"] = "0.9 0.7 1.2 0.0 F".split(" ")
        if mod=="ps3":
		new_params[3]["prior_scale"] = "2.0 1.5 3.0 0.0 F".split(" ")
        if mod=="ps5":
		new_params[3]["prior_scale"] = "0.65 0.5 0.9 0.0 F".split(" ")
        if mod=="ps6":
		new_params[3]["prior_scale"] = "0.5 0.3 0.7 0.0 F".split(" ")
        if mod=="ps7":
		new_params[3]["prior_scale"] = "0.3 0.0 1.0 0.0 F".split(" ")
        if mod=="ps8":
		new_params[3]["prior_scale"] = "0.0 0.0 0.2 0.0 F".split(" ")
	if mod=="pd2":
		new_params[3]["powerlaw_discount"]= "0.05 0.0 0.1 0.0 F".split(" ")
	if mod=="pci0":
		new_params[2]["prune_count_insert"] = "-3.1 -7.0 -1.0 0.0 F=".split(" ")
	if mod=="pci1":
		new_params[2]["prune_count_insert"] = "-3.5 -3.0 -4.5 0.0 F=".split(" ")
	if mod=="pci2":
		new_params[2]["prune_count_insert"] = "-3.0 -3.5 -2.5 0.0 F".split(" ")
	if mod=="pci3":
		new_params[2]["prune_count_insert"] = "-4.0 -4.0 -5.0 0.0 F=".split(" ")
	if mod=="pci5":
		new_params[2]["prune_count_insert"] = "-3.2 -3.0 -2.5 0.0 F=".split(" ")
	if mod=="pci6":
		new_params[2]["prune_count_insert"] = "-4.5 -3.0 -2.5 0.0 F=".split(" ")
	if mod=="pci7":
		new_params[2]["prune_count_insert"] = "-14.0 -3.0 -2.5 0.0 F=".split(" ")
	if mod=="mc0":
                new_params[3]["min_count"] = "2.0 2.0 6.0 49.0 F=".split(" ")
	if mod=="mc2":
                new_params[3]["min_count"] = "4.0 2.0 6.0 49.0 F=".split(" ")
	if mod=="pct0":
		new_params[3]["prune_count_table"] = "0.0 0.0 0.0 0.0 F=".split(" ")
	if mod=="pct2":
		new_params[3]["prune_count_table"] = "1.0 -1.0 3.0 0.0 F".split(" ")
        if mod=="jm2":
		new_params[3]["jelinek_mercer"]= "0.998 0.99 0.999 0.0 1/l".split(" ")
        if mod=="jm3":
		new_params[3]["jelinek_mercer"]= "0.998 0.95 0.999 0.0 1/l".split(" ")
        if mod=="jm4":
		new_params[3]["jelinek_mercer"]= "0.998 0.5 0.999 0.0 1/l".split(" ")
        if mod=="jm5":
		new_params[3]["jelinek_mercer"]= "0.998 0.9 0.999 0.0 1/l".split(" ")
        if mod=="jm6":
		new_params[3]["jelinek_mercer"]= "0.998 0.9 0.9999 0.0 1/l".split(" ")
        if mod=="jm7":
		new_params[3]["jelinek_mercer"]= "0.9 0.8 0.99 0.0 F".split(" ")
	if mod=="tiX0":
		new_params[1]["use_tfidf"] = "1"		
		new_params[2]["idf_lift"] = "1500.0 0.0 1000.0 49.0 F=".split(" ")	    
	if mod=="thr16":
		new_params[1]["threads"] = "16"
	if mod=="mlc0":
                new_params[3]["min_label_count"] = "2.0 -1.0 3.0 0.0 F=".split(" ")
	if mod=="lt1":
		new_params[3]["label_threshold"]= "-2.0 -5.0 -1.0 0.0 F".split(" ")
	if mod=="lt2":
		new_params[3]["label_threshold"]= "-1.5 -2.0 -1.0 0.0 F".split(" ")
	if mod=="lt5":
		new_params[3]["label_threshold"]= "-1.0 -1.5 -0.5 0.0 F".split(" ")
	if mod=="mr0":
                new_params[3]["max_retrieved"]= "20.0 -2.0 -0.1 0.0 F=".split(" ")
	if mod=="mr1":
                new_params[3]["max_retrieved"]= "10.0 -2.0 -0.1 0.0 F=".split(" ")
	if mod=="tk0":
		new_params[3]["top_k"]= "100.0 0.0 1.0 0.0 F=".split(" ")
	if mod=="tk1":
		new_params[3]["top_k"]= "20.0 0.0 1.0 0.0 F=".split(" ")
	if mod=="tk2":
		new_params[3]["top_k"]= "100.0 20.0 500.0 0.0 F".split(" ")
	if mod=="nobo":
		new_params[1]["no_backoffs"]= ""
	if mod=="ch80":
		new_params[2]["cond_hashsize"]= "80000000.0 0.0 10.0 0.0 F=".split(" ")
	if mod=="s0":
		new_params[2]["rand_seed"] = "0.0 0.0 10.0 0.0 F=".split(" ")
	if mod=="s1":
		new_params[2]["rand_seed"] = "1.0 0.0 10.0 0.0 F=".split(" ")
	if mod=="s2":
		new_params[2]["rand_seed"] = "2.0 0.0 10.0 0.0 F=".split(" ")
	if mod=="s3":
		new_params[2]["rand_seed"] = "3.0 0.0 10.0 0.0 F=".split(" ")
	if mod=="s4":
		new_params[2]["rand_seed"] = "4.0 0.0 10.0 0.0 F=".split(" ")
	if mod=="s5":
		new_params[2]["rand_seed"] = "5.0 0.0 10.0 0.0 F=".split(" ")
	if mod=="s6":
		new_params[2]["rand_seed"] = "6.0 0.0 10.0 0.0 F=".split(" ")
	if mod=="s7":
		new_params[2]["rand_seed"] = "7.0 0.0 10.0 0.0 F=".split(" ")
	if mod=="ndcg5b":
		new_params[1]["constrain_labels"]= "1"
		#new_params[1]["full_posteriors"]= ""
		new_params[1]["max_retrieved"]= "5"
		new_params[0]["score_cmd"]= "tail #OUTFILE# | grep Results: | tr ' ' '\\n' | grep NDCG@k: | tr ':' '\\n' | tail -n 1"
        if mod=="mafs2":
		new_params[0]["score_cmd"]= "tail #OUTFILE# | grep Results: | tr ' ' '\\n' | grep maFscore2: | tr ':' '\\n' | tail -n 1"
        if mod=="mafs3":
		new_params[0]["score_cmd"]= "tail #OUTFILE# | grep Results: | tr ' ' '\\n' | grep maFscore3: | tr ':' '\\n' | tail -n 1"
        if mod=="iw0":
		new_params[3]["instantiate_weight"]= "1.0 0.00001 1.0 0.0 F=".split(" ")
		new_params[1]["max_retrieved"]= "1000000"
		new_params[3]["top_k"]= "1000000.0 0.0 1.0 0.0 F=".split(" ")
        if mod=="iw1":
		new_params[3]["instantiate_weight"]= "1.2 0.00001 1.0 0.0 F=".split(" ")
		new_params[1]["max_retrieved"]= "1000000"
		new_params[3]["top_k"]= "1000000.0 0.0 1.0 0.0 F=".split(" ")
        if mod=="cs0":
		new_params[1]["load_clusters"]= "/research/antti/multi_label/hierarchy.txt"
		new_params[3]["cluster_jelinek_mercer"]= "0.9995 0.99 0.9999 0.0 1/l".split(" ")
        if mod=="uc1":
		new_params[3]["bg_unif_smooth"]= "0.95 0.0 1.0 0.0 F=".split(" ")
        if mod=="uc2":
		new_params[3]["bg_unif_smooth"]= "0.2 0.0 0.4 0.0 F".split(" ")
	if mod=="ld":
		new_params[1]["load_cutoffs"]= "label_dev_cutoffs.txt"
		new_params[3]["cutoff_weight"]= "1.0 0.0 1.0 0.0 F=".split(" ")
		new_params[3]["label_threshold"]= "-0.000001 -1.5 -0.0 0.0 F".split(" ")
		new_params[3]["prior_scale"] = "0.0 0.0 0.2 0.0 F=".split(" ")
		new_params[2]["batch_size"] = "5000.0 0.0 0.2 0.0 F=".split(" ")
		new_params[1]["max_retrieved"]= "1000000"
                new_params[3]["top_k"]= "1000000.0 0.0 1.0 0.0 F=".split(" ")
	if mod=="ld1":
		new_params[1]["load_cutoffs"]= "label_dev_cutoffs.txt"
		new_params[3]["cutoff_weight"]= "1.0 0.0 1.0 0.0 F=".split(" ")
		new_params[3]["label_threshold"]= "-0.000001 -2.5 -0.0 0.0 F".split(" ")
		new_params[3]["prior_scale"] = "0.0 0.0 0.2 0.0 F=".split(" ")
		new_params[2]["batch_size"] = "5000.0 0.0 0.2 0.0 F=".split(" ")
		new_params[1]["max_retrieved"]= "1000000"
                new_params[3]["top_k"]= "1000000.0 0.0 1.0 0.0 F=".split(" ")
	if mod=="ld2":
		new_params[1]["load_cutoffs"]= "label_dev_cutoffs.txt"
		new_params[3]["cutoff_weight"]= "1.0 0.0 1.0 0.0 F=".split(" ")
		new_params[3]["label_threshold"]= "1.0 -0.5 -0.0 0.0 F=".split(" ")
		new_params[3]["prior_scale"] = "0.0 0.0 0.2 0.0 F=".split(" ")
		new_params[2]["batch_size"] = "5000.0 0.0 0.2 0.0 F=".split(" ")
		new_params[1]["max_retrieved"]= "1000000"
                new_params[3]["top_k"]= "1000000.0 0.0 1.0 0.0 F=".split(" ")
	if mod=="ld3":
		new_params[1]["load_cutoffs"]= "label_dev_cutoffs.txt"
		new_params[3]["cutoff_weight"]= "1.0 1.0 1.5 0.0 F".split(" ")
		new_params[3]["label_threshold"]= "-0.1 -0.5 -0.0 0.0 F".split(" ")
		new_params[3]["prior_scale"] = "0.0 0.0 0.2 0.0 F=".split(" ")
		new_params[2]["batch_size"] = "5000.0 0.0 0.2 0.0 F=".split(" ")
		new_params[1]["max_retrieved"]= "1000000"
                new_params[3]["top_k"]= "1000000.0 0.0 1.0 0.0 F=".split(" ")
	if mod=="ld4":
		new_params[1]["load_cutoffs"]= "label_dev_cutoffs.txt"
		new_params[3]["cutoff_weight"]= "1.0 1.0 1.5 0.0 F".split(" ")
		new_params[3]["label_threshold"]= "-0.001 -0.01 -0.0 0.0 F".split(" ")
		new_params[3]["prior_scale"] = "0.0 0.0 0.2 0.0 F=".split(" ")
		new_params[2]["batch_size"] = "5000.0 0.0 0.2 0.0 F=".split(" ")
		new_params[1]["max_retrieved"]= "1000000"
                new_params[3]["top_k"]= "1000000.0 0.0 1.0 0.0 F=".split(" ")
	if mod=="pd3":
		new_params[3]["powerlaw_discount"]= "0.05 0.0 0.5 0.0 F".split(" ")
        if mod=="ad3":
		new_params[3]["absolute_discount"]= "0.0 0.0 0.5 0.0 F".split(" ")
	if mod=="dp3":
		new_params[3]["dirichlet_prior"]= "0.000001 0.00000000001 0.1 0.0 l/".split(" ")
	if mod=="je":
		new_params[0]["run_cmd"]= "/usr/bin/java -cp .:meka-1.6.jar -Xmx15600M SGM_Tests"
		new_params[3]["top_k"]= "10000000.0 0.0 1.0 0.0 F=".split(" ")
		new_params[3]["max_retrieved"]= "5.0 1.0 10.0 0.0 F".split(" ")
		new_params[3]["label_threshold"]= "-0.000001 -1.0 -0.0 0.0 F".split(" ")
	if mod=="qidfX2":
		new_params[1]["use_tfidf"] = "8"
		new_params[2]["idf_lift"] = "20.0 0.0 100.0 49.0 F".split(" ")
    write_params(new_params, param_file)

            


