//package weka.classifiers.bayes.SGM;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Hashtable;

public class SGM_Tests {
    public static void main(String[] args) throws Exception{
	long start_time= System.currentTimeMillis();
	// Defaults:
	int rand_seed= 0;
	int debug= 1;
	int threads= 0;
	double cond_norm= 1;
	double combination= 1;
	boolean label_powerset= false;
	boolean use_label_weights= false;
	boolean no_priors= false;
	int use_tfidf= 1;
	int constrain_labels= 0;
	boolean kernel_densities= false;
	boolean no_backoffs= false;
	boolean vote_neighbours= false;
	boolean no_normalization= false;
	boolean full_posteriors= false;
	boolean norm_posteriors= false;
	double feedback= 0.0;
	int feedback_k= 50;
	double instantiate_weight= 0.0;
	double instantiate_threshold= 0.0;

	int batch_size= 5000;
	int cond_hashsize= 10000000;
	double prune_count_table= -1000000.0;
	double prune_count_insert= -10.0;		
	double min_count= 0.0;
	double min_idf= 0.0;
	int min_label_count= 0;
	double length_norm= 0.0;
	double length_scale= 0.5;
	double idf_lift= 0.0;
	double label_threshold= -1000000.0;
	double top_k= 1000000.0;
	int max_retrieved= 10000000;
	//double cond_unif_weight= 0.3;
	//double cond_bg_weight= 0.0;
	double bg_unif_smooth= 1.0;
	double jelinek_mercer= 0.5;
	double dirichlet_prior= 0.0;
	double absolute_discount= 0.0;
	double powerlaw_discount= 0.0;
	double backoff_discount= 0.0;
	double kernel_jelinek_mercer= 0.0;
	double kernel_powerlaw_discount= 0.0;
	double kernel_dirichlet_prior= 0.0;
	double clusters= 0.0;
	int cluster_iters= 0;
	double cluster_jelinek_mercer= 0.0;
	double cluster_min_idf= 0.0;
	double cluster_split_count= 100000.0;
	double cond_scale= 1.0;
	double prior_scale= 0.5;
	double poisson_scale= 0.0;
	
	String workdir= System.getProperty("user.dir");
	String train_file= "";
	String test_file= "";
	String save_model= "";
	String load_model= "";
	String results_file= "";
	String priors_file= "";
	double priors_weight= 0.5;
	String load_clusters= "";
	String load_cutoffs= "";
	double cutoff_weight= 0.0;
	//PrintWriter logf= new PrintWriter(new FileWriter(log_file));
	
	Date date= new Date(start_time);
	SimpleDateFormat ft= new SimpleDateFormat ("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
	System.out.println("time:"+ft.format(date));
	String argtmp=" "; for (int i= 0;i < args.length; i++) argtmp+= args[i]+" ";
	if (args.length>0) {
	    Hashtable<String, String> arguments= new Hashtable<String, String>();
	    String[] args2= argtmp.split(" -");
	    for (int i= 0;i < args2.length; i++) {
		String[] tmp2= args2[i].split(" "); 
		//if (tmp2.length!= 2) continue;
		if (tmp2.length!= 2) {String[] tmp3= {tmp2[0], ""}; tmp2= tmp3;}
		arguments.put(tmp2[0], tmp2[1].replace('\\', ' '));
	    }
	    if (arguments.containsKey("workdir")) workdir= arguments.get("workdir");
	    if (arguments.containsKey("train_file")) train_file= arguments.get("train_file");
	    if (arguments.containsKey("test_file")) test_file= arguments.get("test_file");
	    if (arguments.containsKey("load_model")) load_model= arguments.get("load_model");
	    if (arguments.containsKey("save_model")) save_model= arguments.get("save_model");
	    if (arguments.containsKey("no_normalization")) no_normalization= true;
	    if (arguments.containsKey("results_file")) results_file= arguments.get("results_file");
	    if (arguments.containsKey("threads")) threads= (Integer)(int)Double.parseDouble(arguments.get("threads"));
	    if (arguments.containsKey("priors_file")) priors_file= arguments.get("priors_file");
	    if (arguments.containsKey("priors_weight")) priors_weight= Double.parseDouble(arguments.get("priors_weight"));
	    if (arguments.containsKey("load_clusters")) load_clusters= arguments.get("load_clusters");
	    if (arguments.containsKey("load_cutoffs")) load_cutoffs= arguments.get("load_cutoffs");
	    if (arguments.containsKey("cutoff_weight")) cutoff_weight= Double.parseDouble(arguments.get("cutoff_weight"));
	    if (arguments.containsKey("batch_size")) batch_size= (Integer)(int)Double.parseDouble(arguments.get("batch_size"));
	    if (arguments.containsKey("cond_hashsize")) cond_hashsize= (Integer)(int)Double.parseDouble(arguments.get("cond_hashsize"));
	    if (arguments.containsKey("prune_count_table")) prune_count_table= Double.parseDouble(arguments.get("prune_count_table"));
	    if (arguments.containsKey("prune_count_insert")) prune_count_insert= Double.parseDouble(arguments.get("prune_count_insert"));
	    if (arguments.containsKey("min_count")) min_count= Double.parseDouble(arguments.get("min_count"));
	    if (arguments.containsKey("min_idf")) min_idf= Double.parseDouble(arguments.get("min_idf"));
	    if (arguments.containsKey("min_label_count")) min_label_count= (Integer)(int)Double.parseDouble(arguments.get("min_label_count"));
	    if (arguments.containsKey("length_norm")) length_norm= Double.parseDouble(arguments.get("length_norm"));
	    if (arguments.containsKey("length_scale")) length_scale= Double.parseDouble(arguments.get("length_scale"));
	    if (arguments.containsKey("idf_lift")) idf_lift= Double.parseDouble(arguments.get("idf_lift"));
	    //if (arguments.containsKey("cond_unif_weight")) cond_unif_weight= Double.parseDouble(arguments.get("cond_unif_weight"));
	    //if (arguments.containsKey("cond_bg_weight")) cond_bg_weight= Double.parseDouble(arguments.get("cond_bg_weight"));
	    if (arguments.containsKey("bg_unif_smooth")) bg_unif_smooth= Double.parseDouble(arguments.get("bg_unif_smooth"));
	    if (arguments.containsKey("jelinek_mercer")) jelinek_mercer= Double.parseDouble(arguments.get("jelinek_mercer"));
	    if (arguments.containsKey("dirichlet_prior")) dirichlet_prior= Double.parseDouble(arguments.get("dirichlet_prior"));
	    if (arguments.containsKey("absolute_discount")) absolute_discount= Double.parseDouble(arguments.get("absolute_discount"));
	    if (arguments.containsKey("powerlaw_discount")) powerlaw_discount= Double.parseDouble(arguments.get("powerlaw_discount"));
	    if (arguments.containsKey("backoff_discount")) backoff_discount= Double.parseDouble(arguments.get("backoff_discount"));
	    if (arguments.containsKey("kernel_jelinek_mercer")) kernel_jelinek_mercer= Double.parseDouble(arguments.get("kernel_jelinek_mercer"));
	    if (arguments.containsKey("kernel_powerlaw_discount")) kernel_powerlaw_discount= Double.parseDouble(arguments.get("kernel_powerlaw_discount"));
	    if (arguments.containsKey("kernel_dirichlet_prior")) kernel_dirichlet_prior= Double.parseDouble(arguments.get("kernel_dirichlet_prior"));
  	    if (arguments.containsKey("clusters")) clusters= Double.parseDouble(arguments.get("clusters"));
	    if (arguments.containsKey("cluster_min_idf")) cluster_min_idf= Double.parseDouble(arguments.get("cluster_min_idf"));
	    if (arguments.containsKey("cluster_split_count")) cluster_split_count= Double.parseDouble(arguments.get("cluster_split_count"));
	    if (arguments.containsKey("cluster_iters")) cluster_iters= (Integer)(int)Double.parseDouble(arguments.get("cluster_iters"));
	    if (arguments.containsKey("cluster_jelinek_mercer")) cluster_jelinek_mercer= Double.parseDouble(arguments.get("cluster_jelinek_mercer"));
	    if (arguments.containsKey("cond_scale")) cond_scale= Double.parseDouble(arguments.get("cond_scale"));
	    if (arguments.containsKey("prior_scale")) prior_scale= Double.parseDouble(arguments.get("prior_scale"));
	    if (arguments.containsKey("poisson_scale")) poisson_scale= Double.parseDouble(arguments.get("poisson_scale"));
	    if (arguments.containsKey("label_threshold")) label_threshold= Double.parseDouble(arguments.get("label_threshold"));
	    if (arguments.containsKey("top_k")) top_k= Double.parseDouble(arguments.get("top_k"));
	    if (arguments.containsKey("max_retrieved")) max_retrieved= (Integer)(int)Double.parseDouble(arguments.get("max_retrieved"));
	    if (arguments.containsKey("label_powerset")) label_powerset= true;
	    if (arguments.containsKey("use_label_weights")) use_label_weights= true;
	    if (arguments.containsKey("no_priors")) no_priors= true;
	    if (arguments.containsKey("use_tfidf")) use_tfidf= (Integer)(int)Double.parseDouble(arguments.get("use_tfidf"));
	    if (arguments.containsKey("constrain_labels")) constrain_labels= (Integer)(int)Double.parseDouble(arguments.get("constrain_labels"));
	    if (arguments.containsKey("cond_norm")) cond_norm= Double.parseDouble(arguments.get("cond_norm"));
	    if (arguments.containsKey("combination")) combination= Double.parseDouble(arguments.get("combination"));
	    if (arguments.containsKey("kernel_densities")) kernel_densities= true;
	    if (arguments.containsKey("no_backoffs")) no_backoffs= true;
	    if (arguments.containsKey("vote_neighbours")) vote_neighbours= true;
	    if (arguments.containsKey("full_posteriors")) full_posteriors= true;
	    if (arguments.containsKey("norm_posteriors")) norm_posteriors= true;
	    if (arguments.containsKey("feedback")) feedback= Double.parseDouble(arguments.get("feedback"));
	    if (arguments.containsKey("feedback_k")) feedback_k= (Integer)(int)Double.parseDouble(arguments.get("feedback_k"));
	    if (arguments.containsKey("instantiate_weight")) instantiate_weight= Double.parseDouble(arguments.get("instantiate_weight"));
	    if (arguments.containsKey("instantiate_threshold")) instantiate_threshold= Double.parseDouble(arguments.get("instantiate_threshold"));

	    if (arguments.containsKey("rand_seed")) rand_seed= (Integer)(int)Double.parseDouble(arguments.get("rand_seed"));
	    if (arguments.containsKey("debug")) debug= (Integer)(int)Double.parseDouble(arguments.get("debug"));
	}
	workdir= (workdir+"/").replace("//", "/");
	if (!train_file.startsWith("/")) train_file= (workdir + "/"+ train_file).replace("//", "/");
	if (!test_file.startsWith("/")) test_file= (workdir + "/"+ test_file).replace("//", "/");
	if (!results_file.startsWith("/")) results_file= (workdir + "/"+ results_file).replace("//", "/");
	if (!priors_file.startsWith("/")) priors_file= (workdir + "/"+ priors_file).replace("//", "/");
	if (!load_clusters.startsWith("/")) load_clusters= (workdir + "/"+ load_clusters).replace("//", "/");
	if (!load_cutoffs.startsWith("/")) load_cutoffs= (workdir + "/"+ load_cutoffs).replace("//", "/");
	//if (!model_file.startsWith("/")) model_file= (workdir + "/"+ model_file).replace("//", "/");
	if (!load_model.startsWith("/")) load_model= (workdir + "/"+ load_model).replace("//", "/");
	if (!save_model.startsWith("/")) save_model= (workdir + "/"+ save_model).replace("//", "/");
	
	PrintWriter resultsf= null;
	if (!results_file.equals(workdir)) resultsf= new PrintWriter(new FileWriter(results_file));
	BufferedReader priorsf= null;
	if (!priors_file.equals(workdir)) priorsf= new BufferedReader(new FileReader(priors_file));
	BufferedReader clustersf= null;
	if (!load_clusters.equals(workdir)) clustersf= new BufferedReader(new FileReader(load_clusters));

	//logf.println("args:"+argtmp);
	//logf.close();
	//if (true) return;
	
	//TFIDF tfidf= new TFIDF(length_scale, idf_lift, length_norm, use_tfidf);
	TFIDF tfidf= new TFIDF(length_norm, length_scale, idf_lift, Math.abs(use_tfidf));
	SGM sgm= new SGM();
	sgm.init_model(cond_hashsize, tfidf, rand_seed, cond_norm);
	sgm.debug= debug;
	if (no_priors) {prior_scale= 0; if (!full_posteriors) sgm.model.prior_lprobs= null;}
	if (constrain_labels>0) use_label_weights= true;
	if (label_powerset) sgm.use_powerset();
	if (kernel_densities) sgm.kernel_densities();
	if (no_backoffs) sgm.no_backoffs= true;
	if (vote_neighbours) sgm.vote_neighbours= true;
	if (!load_model.endsWith("/")) sgm.load_model(load_model);
	else if (!train_file.endsWith("/")) {
	    int incr= 40000;
	    int max_read= incr * 1000000000;
	    int total= 0;
	    int read;
	    sgm.open_stream(train_file, incr, use_label_weights);
	    while ((read= sgm.get_features(incr))!=0) {
		total+= read;
		System.out.println("Reading data. Time:"+(System.currentTimeMillis()-start_time)+" Read:"+total);
		sgm.train_model(batch_size, prune_count_insert);
		if (total>= max_read || sgm.model.cond_lprobs.size()==0) break;
	    }
	    sgm.close_stream();
	    System.out.println("Counts gathered. Time:"+(System.currentTimeMillis()-start_time));
	}
	if (sgm.model.cond_lprobs.size()==0) return;
	if (!no_normalization){
	    System.out.println("Normalizing model. sgm.model.cond_lprobs.size:"+  sgm.model.cond_lprobs.size());
	    sgm.prune_labels(min_label_count);
	    tfidf.normalize(min_count, min_idf);
	    //if (prune_count_table>-1000000.0) sgm.prune_counts(prune_count_table, cond_hashsize);
	    sgm.apply_idfs();
	    System.out.println("sgm.model.cond_lprobs.size:"+  sgm.model.cond_lprobs.size());
	    
	    //if (kernel_densities) sgm.normalize_conditionals();
	    sgm.make_bo_models(clusters, bg_unif_smooth, cluster_iters, cluster_min_idf, cluster_split_count, clustersf);
	    System.out.println("Added bo_models. sgm.model.cond_lprobs.size:"+  sgm.model.cond_lprobs.size());
	    if (use_tfidf<0) sgm.use_icfs();
	    //if (cond_scale!=1.0) sgm.scale_conditionals(cond_scale);
	    //sgm.normalize_model();
	    sgm.smooth_conditionals(bg_unif_smooth, jelinek_mercer, dirichlet_prior, absolute_discount, powerlaw_discount, backoff_discount, cond_scale, kernel_jelinek_mercer, kernel_powerlaw_discount, kernel_dirichlet_prior, cluster_jelinek_mercer, poisson_scale);
	    //sgm.smooth_prior(prior_scale);
	    sgm.normalize_priors();
	    if (prune_count_table>-1000000.0) sgm.prune_counts(prune_count_table, cond_hashsize);
	    System.out.println("sgm.model.cond_lprobs.size():"+sgm.model.cond_lprobs.size());
	    if (sgm.model.prior_lprobs!=null) System.out.println("sgm.model.prior_lprobs.size():" +sgm.model.prior_lprobs.size());
	    //sgm.prune_counts2(kernel_jelinek_mercer2);
	    System.out.println("Model normalized. Time:"+(System.currentTimeMillis()-start_time));
	}
	if (!save_model.endsWith("/")) sgm.save_model(save_model);
	sgm.prepare_inference(top_k, max_retrieved, label_threshold, combination, full_posteriors, norm_posteriors, constrain_labels, feedback, feedback_k, prior_scale);
	sgm.load_cutoffs(load_cutoffs, cutoff_weight); 
	if (!test_file.endsWith("/")) {
	    sgm.prepare_evaluation();
	    System.out.println("Evaluating: "+test_file+". Time:"+(System.currentTimeMillis()-start_time));
	    int incr= batch_size;
	    int total= 0;
	    int read;
	    sgm.open_stream(test_file, incr, use_label_weights);
	    while ((read= sgm.get_features(incr))!=0) {
		total+= read;
		System.out.println("Reading data. Time:"+(System.currentTimeMillis()-start_time));
		//sgm.infer_posteriors(resultsf);
		sgm.set_instantiate(instantiate_weight, instantiate_threshold);
		if (threads==0)sgm.infer_posteriors(resultsf);
		else sgm.infer_posteriors_parallel(resultsf, threads);
		if (read!=incr) break;
	    }
	    sgm.close_stream();
	    if (resultsf!=null) resultsf.close();
	    //logf.close();
	    System.out.println("Done. Time:"+(System.currentTimeMillis()-start_time)+" "+total);
	    sgm.print_evaluation_summary();
	}
	if (clustersf!=null) clustersf.close();
    }
}

