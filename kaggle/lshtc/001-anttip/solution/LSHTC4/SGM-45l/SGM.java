//package weka.classifiers.bayes.SGM;
import java.util.HashSet; 
import java.util.Hashtable; 
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.Math;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Set;

public class SGM implements Serializable{
    private static final long serialVersionUID = -3376037288335722173L;
    public int debug;
    private BufferedReader input_file;
    public SparseData data;
    public int num_classified;
    //public int num_labels_classified;
    Hashtable<Integer, IntBuffer> inverted_index;
    Hashtable<Integer, FloatBuffer> inverted_index2;
    //Hashtable<Integer, ArrayList<Integer>> inverted_index;
    //Hashtable<Integer, ArrayList<Float>> inverted_index2;
    int prior_max_label;
    public double prune_count_insert;
    double label_threshold;
    double cutoff_weight;
    //double poisson_scale;
    int cond_hashsize;
    public SGM_Params model;

    Random rand;
    int rand_seed;

    public boolean no_backoffs;
    public boolean vote_neighbours;
    int constrain_labels; // 0= no constrains, %2== 1 constrain evaluation, >1 constrain search
    HashSet<Integer> constrained_labels;
    TFIDF tfidf;
    int num_classes;
    public boolean norm_posteriors;
    public boolean full_posteriors;
    Hashtable<IntBuffer, Integer> labels2powerset;
    Hashtable<Integer, IntBuffer> powerset2labels;
    Hashtable<Integer, Integer> component_counts;
    Hashtable<Integer, Double> bo_mean_lweights;
    public int top_k;
    public int max_retrieved;
    public double feedback;
    public int feedback_k;
    public double instantiate_weight;
    public double instantiate_threshold;
    public double combination;
    public double cond_norm;
    public double prior_scale;
    int tp, fp, fn, tp0, fp0, fn0;
    Hashtable<Integer, Integer> tps, fps, fns;
    double rec, prec, fscore, rec0, prec0, fscore0, map, mapK, precK, ndcg, ndcgK, meanjaccard;//, mapErrors, wAcc;
    public final double ILOG2= 1.0/Math.log(2);

    Hashtable<Integer, Integer> label_cutoffs;
    Hashtable<Integer, TreeSet<DoubleBuffer>> label_instances;

    public SGM() {
    }

    public String hello() {
	return("Hello!");
    }
    
    public void init_model(int cond_hashsize) throws Exception {
	//System.out.println("SGM Initializing model");
	debug= 0;
	this.cond_hashsize= cond_hashsize;
	model= new SGM_Params(cond_hashsize);
	constrained_labels= null;
	prior_max_label= -1;
	labels2powerset= null;
	powerset2labels= null;
	norm_posteriors= false;
	full_posteriors= false;
	no_backoffs= false;
	vote_neighbours= false;
	cond_norm= 1;
	rand= new Random(1000);
	cutoff_weight= 0;
    }

    public void init_model(int cond_hashsize, TFIDF tfidf, int rand_seed, double cond_norm) throws Exception {
	init_model(cond_hashsize);
	this.tfidf= tfidf;
	this.cond_norm= cond_norm;
	rand= new Random(rand_seed);
    }
    
    public void train_model(int batch_size, double prune_count_insert) throws Exception {
	this.prune_count_insert= prune_count_insert;
	if (debug>0) System.out.println("Updating model " + data.doc_count + " "+ model.train_count);
	if (data.label_weights==null) for (int w= 0; w < data.doc_count; w++) add_instance(data.terms[w], data.counts[w], data.labels[w], null);
	else for (int w= 0; w < data.doc_count; w++) add_instance(data.terms[w], data.counts[w], data.labels[w], data.label_weights[w]);
    }
    
    public int[] get_label_powerset(int[] labels) {
	int[] labels2= Arrays.copyOf(labels, labels.length);
	if (labels2.length>1) Arrays.sort(labels2);
	IntBuffer wrap_labels= IntBuffer.wrap(labels2);
	Integer powerset= labels2powerset.get(wrap_labels);
	if (powerset==null) {
	    powerset= labels2powerset.size();
	    labels2powerset.put(wrap_labels, powerset);
	}
	labels= new int[1];
	labels[0]= powerset;
	return labels;
    }

    public void add_instance(int[] terms, float[] counts, int[] labels, float[] label_weights) {
	//if (counts.length<4) return;
	tfidf.length_normalize(terms, counts);
	if (labels2powerset!=null) labels= get_label_powerset(labels);
	//if (labels.length>1) Arrays.sort(labels);
	if (model.prior_lprobs!=null) {
	    for (int label:labels) {
		Integer label2= label;
		Float lsp= model.prior_lprobs.get(label2);
		lsp= (lsp==null) ? (float) 0.0 : flogaddone(lsp);
		model.prior_lprobs.put(label2, lsp);
	    }
	}
	if (model.node_links!=null) {
	    //int bo_label= -(labels[0]+1);
	    //model.node_links.put(model.train_count, bo_label);
	    //if (bo_label<model.min_encoded_label) model.min_encoded_label= bo_label;
	    int[] labels2= Arrays.copyOf(labels, labels.length);
	    int t= 0;
	    for (int label:labels2) {
		int bo_label= -(label+1);
		if (bo_label<model.min_encoded_label) model.min_encoded_label= bo_label;
		labels2[t++]= bo_label;
	    }
	    model.node_links.put(model.train_count, IntBuffer.wrap(labels2));
	    labels= new int[1];
	    labels[0]= model.train_count;
	}
	model.train_count++;
	int t= 0, j;
	
	for (t= 0;t<terms.length;) counts[t]= (float)Math.log(counts[t++]);	
	if (label_weights!=null) for (t= 0;t<labels.length;) label_weights[t]= (float)Math.log(label_weights[t++]);
	//float norm= -1000000;
	//if (tfidf.use_tfidf==8) 
	//for (float count:counts) norm= (float)logsum(norm, count);
	for (t= 0; t<terms.length; t++) {
	    int term= terms[t];
	    Integer term2= new Integer(term);
	    double prune= prune_count_insert;
	    //if (tfidf.use_tfidf==1 || tfidf.use_tfidf==3 || tfidf.use_tfidf==5) prune-= Math.log(tfidf.get_idf(term2));
	    if (tfidf.use_tfidf==1 || tfidf.use_tfidf==3 || tfidf.use_tfidf==5|| tfidf.use_tfidf==8 || tfidf.use_tfidf==15|| tfidf.use_tfidf==16 || tfidf.use_tfidf==17|| tfidf.use_tfidf==18|| tfidf.use_tfidf==19|| tfidf.use_tfidf==23|| tfidf.use_tfidf==24) prune-= Math.log(tfidf.get_idf(term2));
	    //if (tfidf.use_tfidf==8) 
	    //prune+= norm; //approximate
	    double lprob= counts[t];
	    j= 0;
	    for (int label:labels) {
		if (label_weights!=null) lprob+= label_weights[j++];
		else if (labels.length>1) lprob-= Math.log(labels.length);
		add_lprob(label, term, lprob, prune);
	    }
	}
	//if (model.train_count % batch_size == 0) prune_counts(prune_count_insert, cond_hashsize);
    }
    
    public void add_lprob(int label, int term, double lprob2, double prune) {
	CountKey p_index= new CountKey(label, term);
	Float lprob= model.cond_lprobs.get(p_index);
	if (lprob==null) {
	    label= -2147483648;
	    if (model.cond_lprobs.size()==cond_hashsize) return;
	    lprob= (float) lprob2;
	}
	else lprob= (float) logsum(lprob, lprob2);
	//System.out.println(label+" "+term+" "+lprob2+" "+prune); 
	if (lprob < prune) {if (label!=-2147483648) model.cond_lprobs.remove(p_index);}
	else model.cond_lprobs.put(p_index, lprob);
    }
    
    public double dlog(double number) {
	if (number==0.0) return -1000000;
	return Math.log(number);
    }

    public void make_bo_models(double clusters, double cluster_smooth, int cluster_iters, double cluster_min_idf, double cluster_split_count, BufferedReader clustersf) throws Exception {
	CountKey p_index= new CountKey();
	if (model.node_links!=null) //Sample the backoff node from the labelset
            for (Map.Entry<Integer, IntBuffer> entry: model.node_links.entrySet()) {
                int[] labels= entry.getValue().array();
                int max_label= labels[0], max_i= 0;
                max_i= (entry.getKey()+rand_seed) % labels.length;
                max_label= labels[max_i];
                labels[max_i]= labels[0];
                labels[0]= max_label;
	    }
	Hashtable<Integer, Double> norms= get_cond_norms(model.cond_lprobs);
	//Hashtable<CountKey, Float> bo_lprobs= new Hashtable<CountKey, Float>();
	//System.out.println(model.cond_lprobs.size()+" "+cond_hashsize+" "+model.min_encoded_label+ " " +!no_backoffs);
	HashSet<CountKey> keys1= new HashSet<CountKey>(model.cond_lprobs.keySet());
	Iterator<CountKey> entries1= keys1.iterator();
	while (entries1.hasNext()) {
	    p_index= entries1.next();
	    //for (Map.Entry<CountKey, Float> entry : model.cond_lprobs.entrySet()) {
	    //p_index= entry.getKey();
	    int label= p_index.label;
	    //float lprob2= (float)(entry.getValue()- norms.get((Integer)label));
	    float lprob2= (float)(model.cond_lprobs.get(p_index)- norms.get((Integer)label));
	    Integer term2= p_index.term;
	    Float cond_bg= model.cond_bgs.get(term2);
	    cond_bg= (cond_bg==null) ? lprob2 : (float)logsum(cond_bg, lprob2);
	    model.cond_bgs.put(term2, cond_bg);
	    if (model.min_encoded_label!=0 && !no_backoffs) {
		int[] labels= model.node_links.get(label).array();
		for (int label2: labels) {
                    //Integer label2= model.node_links.get((Integer)p_index.label);
                    //CountKey p_index2= new CountKey(label2, p_index.term);
		    add_lprob(label2, p_index.term, lprob2, -1000000.0);
                    //Float lprob= bo_lprobs.get(p_index2);
                    //if (lprob==null) lprob= lprob2;
                    //else lprob= (float)logsum(lprob, lprob2);
                    //bo_lprobs.put(p_index2, lprob);
                }
		//Integer label2= model.node_links.get((Integer)p_index.label);
		//CountKey p_index2= new CountKey(label2, p_index.term);
		//Float lprob= bo_lprobs.get(p_index2);
		//if (lprob==null) lprob= lprob2;
		//else lprob= (float)logsum(lprob, lprob2);
		//bo_lprobs.put(p_index2, lprob);
	    }
	}
	System.out.println(model.cond_lprobs.size()+" "+cond_hashsize);

	//System.out.println(bo_lprobs.size()+" "+model.cond_lprobs.size()+" "+cond_hashsize);
	//if (bo_lprobs.size()+model.cond_lprobs.size()>cond_hashsize) prune_counts(-1000000.0, cond_hashsize-bo_lprobs.size());
	/*if (model.min_encoded_label!=0) {
	    for (Map.Entry<CountKey, Float> entry : bo_lprobs.entrySet()) {
                p_index= entry.getKey();
                add_lprob(p_index.label, p_index.term, entry.getValue(), -1000000.0);
            }
	    //HashSet<CountKey> keys= new HashSet<CountKey>(bo_lprobs.keySet());
	    // Iterator<CountKey> entries= keys.iterator();
	    // while (entries.hasNext()) {
	    //p_index= entries.next();
	    //p_index= entry.getKey();
	    //add_lprob(p_index.label, p_index.term, bo_lprobs.get(p_index), -1000000.0);
	    //bo_lprobs.remove(p_index);
	    //}
	    }*/

	if (clusters>0 || clustersf!=null) {
	    if (model.min_encoded_label==0) {
		//model.node_links= new Hashtable<Integer, Integer>();
		if (model.node_links== null) model.node_links= new Hashtable<Integer, IntBuffer>();
		HashSet<CountKey> keys= new HashSet<CountKey>(model.cond_lprobs.keySet());
		Iterator<CountKey> entries= keys.iterator();
		int min_recode= 0;
		while (entries.hasNext()) {
		    CountKey p_index2= entries.next();
		    int recode= -(p_index2.label+1);
		    if (recode<min_recode) min_recode= recode;
		    CountKey new_key= new CountKey(recode, p_index2.term);
		    model.cond_lprobs.put(new_key, model.cond_lprobs.get(p_index2));
		    model.cond_lprobs.remove(p_index2);
		}
		//model.min_encoded_label= min_recode;
		model.min_encoded_label= min_recode;
	    }
	    if (model.prior_lprobs!=null) {
		Iterator<Entry<Integer, Float>> entries= model.prior_lprobs.entrySet().iterator();
		while (entries.hasNext()) {
		    int label= -(entries.next().getKey()+1);
		    if (label<model.min_encoded_label) model.min_encoded_label= label;
		}
	    }
	    Hashtable<CountKey, Float> bo_links= null;
	    if (clustersf==null) {
		//Hashtable<Integer, Integer> bo_links= cluster_nodes(model.cond_lprobs, (float)clusters, (float)cluster_smooth, cluster_iters, model.min_encoded_label, 0, (float)cluster_min_idf);
		bo_links= cluster_nodes(model.cond_lprobs, (float)clusters, (float)cluster_smooth, cluster_iters, model.min_encoded_label, 0, (float)cluster_min_idf, (float)cluster_split_count);
	    } else {
		bo_links= new Hashtable<CountKey, Float>();
		String l;
		String[] s;
		while ((l = clustersf.readLine())!= null) {
		    s= l.trim().split(" ");
		    //System.out.println(l);
		    //CountKey link= new CountKey(-(Integer.decode(s[0])+1), Integer.decode(s[1]));
		    CountKey link= new CountKey(-(Integer.decode(s[1])+1), Integer.decode(s[0]));
		    //CountKey link= new CountKey(-(Integer.decode(s[1])+1),  -(Integer.decode(s[0])+1));
		    //if (model.prior_lprobs.containsKey(-(link.label+1))) System.out.println(link.label+" "+link.term);
		    //System.out.println(link.label+" "+model.prior_lprobs.containsKey(link.label));
		    //if (model.prior_lprobs.containsKey(-(link.label+1))) bo_links.put(link, (float)0.0); //Use only the first layer
		    if (link.label>=model.min_encoded_label) bo_links.put(link, (float)0.0); //Use only the first layer
		}
	    }
	    Hashtable<Integer, Integer> encode= new Hashtable<Integer, Integer>();
	    HashSet<CountKey> keys= new HashSet<CountKey>(bo_links.keySet());
	    Iterator<CountKey> entries2= keys.iterator();
	    while (entries2.hasNext()) {
		CountKey link= entries2.next();
		Integer node= encode.get(link.term);
		if (node==null) {
		    node= model.min_encoded_label -1 -encode.size();
		    encode.put(link.term, node);
		}
		int[] node2= {node}; IntBuffer wrap_node2= IntBuffer.wrap(node2);
		//model.node_links.put(link.label, node); //Fix!
		model.node_links.put(link.label, wrap_node2); 
		//System.out.println(":"+link.label+" "+node+" "+model.min_encoded_label+" "+encode.size());
	    }
	    CountKey link= new CountKey();
	    keys= new HashSet<CountKey>(model.cond_lprobs.keySet());
	    entries2= keys.iterator();
	    while (entries2.hasNext()) {
		CountKey p_index2= entries2.next();
		//if (p_index2.label>=0) continue;
		float lprob= model.cond_lprobs.get(p_index2);
		IntBuffer label2= model.node_links.get(p_index2.label);
		//System.out.println(p_index2.label+" "+label2.get(0));
		//if (label2== null) continue;
		//link.label= p_index2.label;
		//link.term= label2.get(0);
		//System.out.println(p_index2.label+" "+label2.get(0));
		//lprob+= bo_links.get(link); //fix?
		add_lprob(label2.get(0), p_index2.term, lprob, -1000000.0);
		//System.out.println(label2.get(0)+" "+p_index2.term+" "+ lprob);
		//Integer label2= model.node_links.get(p_index2.label); //Fix!
		//if (label2== null) continue; // Fix!
		//link.label= p_index2.label;
		//link.term= label2;
		//lprob+= bo_links.get(link);
		//System.out.println(bo_links.get(p_index2.label)+" "+ p_index2.label+" "+p_index2.term+" "+lprob);
		//add_lprob(label2, p_index2.term, lprob, -1000000.0); //Fix!
	    }
	    //System.exit(-1);
	}
	if (model.cond_lprobs.size()==cond_hashsize && model.min_encoded_label!=0 && !no_backoffs){ //Remove holes
	    keys1= new HashSet<CountKey>(model.cond_lprobs.keySet());
	    entries1= keys1.iterator();
	    CountKey p_index2= new CountKey();
	    IntBuffer labels;
	    while (entries1.hasNext()) {
		p_index= entries1.next();
		if (p_index.label< model.min_encoded_label) continue;
		labels= model.node_links.get(p_index.label);
		if (labels==null) continue;
		p_index2.term= p_index.term;
		p_index2.label= labels.get(0);
		if (!model.cond_lprobs.containsKey(p_index2)) model.cond_lprobs.remove(p_index);
	    }
	    System.out.println("Removed holes from backoff graph: model.cond_lprobs.size():"+model.cond_lprobs.size());
	}

	//if (tfidf!=null) {
	Iterator<Entry<Integer, Float>> entries= tfidf.idfs.entrySet().iterator();
	while (entries.hasNext()) {
	    Entry<Integer,  Float> entry= (Entry<Integer,  Float>) entries.next();
	    if (!model.cond_bgs.containsKey(entry.getKey())) entries.remove();
	}
    }
    
    //public Hashtable<Integer, Integer> cluster_nodes(Hashtable<CountKey, Float> lprobs, float clusters, float unif_smooth, int cluster_iters, int top, int low, float min_idf) {
    public Hashtable<CountKey, Float> cluster_nodes(Hashtable<CountKey, Float> lprobs, float clusters, float unif_smooth, int cluster_iters, int top, int low, float min_idf, float split_count) throws Exception{
	//unif_smooth= (float)Math.max(1E-10, unif_smooth);
	unif_smooth= (float)(1.0- (Math.pow(unif_smooth, 10)/model.cond_bgs.size()));

	float a1= (float)Math.log(1.0- unif_smooth); 
	float u1= (float)Math.log(unif_smooth/model.cond_bgs.size());
	Hashtable<Integer, Double> norms= get_cond_norms(model.cond_lprobs);
	Hashtable<Integer, Float> q_entropies= new Hashtable<Integer, Float>();
	Hashtable<Integer, ArrayList<Integer>> node_index= new Hashtable<Integer, ArrayList<Integer>>();
	for (Map.Entry<CountKey, Float> entry : lprobs.entrySet()) {
	    CountKey p_index= entry.getKey();
            if (p_index.label<top || p_index.label>= low) continue;
            Integer term= p_index.term;
	    if (tfidf.get_idf(term)<=min_idf) continue;
	    Integer node= p_index.label;
	    //Float lprob=(float)(entry.getValue() + Math.log(tfidf.get_idf(term)));
	    ArrayList<Integer> terms= node_index.get(node);
	    if (terms == null) {
		terms= new ArrayList<Integer>(4);
		terms.add(term);
		node_index.put(node, terms);
	    } else terms.add(term);
	    float lprob= (float)(entry.getValue() - norms.get(node));
	    Float q_entropy= q_entropies.get(node);
	    //if (q_entropy== null) q_entropy= (float)-Math.exp(lprob)*lprob;
	    //else q_entropy-= (float)Math.exp(lprob)*lprob;
	    //if (q_entropy== null) q_entropy= (float)(-Math.exp(lprob)*(logsum(lprob+Math.log(tfidf.get_idf(p_index.term)) - norms.get(node)+a1, u1)));
	    //else q_entropy-= (float)(-Math.exp(lprob)*(logsum(lprob+Math.log(tfidf.get_idf(p_index.term)) - norms.get(node)+a1, u1)));

	    //float qent= (float)(-Math.exp(lprob)*lprob* tfidf.get_idf(p_index.term));
	    float qent= (float)(-Math.exp(lprob)*lprob);
	    if (tfidf.use_tfidf==1 || tfidf.use_tfidf==3 || tfidf.use_tfidf==8) qent*= tfidf.get_idf(p_index.term);
	    if (q_entropy== null) q_entropy= qent;
	    else q_entropy+= qent;
	    q_entropies.put(node, q_entropy);
	}
	
	//System.exit(-1);
	ArrayList<Integer> pool= new ArrayList<Integer>(norms.keySet());
	int node_count= (int)(clusters* Math.log(norms.size()));
	
	//System.out.println(unif_smooth +" " +a1 +" "+u1+ " "+model.cond_bgs.size());
	Hashtable<Integer, ArrayList<Integer>> bo_index= new Hashtable<Integer, ArrayList<Integer>>();
	Hashtable<Integer, ArrayList<Float>> bo_index2= new Hashtable<Integer, ArrayList<Float>>();
	HashSet<Integer> nodes= sample_nodes(node_count, pool, false);
	HashSet<Integer> nodes2= new HashSet<Integer>();
	add_cluster_nodes(nodes, bo_index, bo_index2, lprobs, norms, a1, u1, min_idf);
	Hashtable<Integer, Hashtable<Integer, Float>> distances2= new Hashtable<Integer, Hashtable<Integer, Float>>();
	for (Integer node: nodes) {Hashtable<Integer, Float> distances3= new Hashtable<Integer, Float>(); distances2.put(node, distances3);}
	//Hashtable<Integer, Integer> bo_links= new Hashtable<Integer, Integer>(node_count);
	Hashtable<CountKey, Float> bo_links= new Hashtable<CountKey, Float>(node_count);
	Hashtable<Integer, Double> norms2= null;
	double sum_norms2= -100000;
	int max_prior= -100000000;
	float mean_dist_div=(float)(-Math.log(nodes.size())- Math.log(node_index.size()));
	double mean_dist= -100000, old_mean_dist= -1000000;
	CountKey p_index= new CountKey();	
	
	boolean use_medoids= true;
	boolean prune_nodes= true;
	// Iterate
	for (int i= 0; i< cluster_iters; i++) {
	    ArrayList<Integer> terms;
	    prune_nodes= false;
	    //if ((i+1)%5==0) prune_nodes= true;
	    if ((i+1)%2==0) prune_nodes= true;
	    Hashtable<Integer, Integer> cluster_ids= new Hashtable<Integer, Integer>(node_count);
	    Hashtable<Integer, Integer> medoids= new Hashtable<Integer, Integer>(node_count);
	    Hashtable<Integer, Float> medoid_similarities= new Hashtable<Integer, Float>(node_count);
	    //Hashtable<Integer, Float> medoid_counts= new Hashtable<Integer, Float>(node_count);
	    Hashtable<Integer, Float> distances;
	    Hashtable<CountKey, Float> bo_lprobs= new Hashtable<CountKey, Float>();
	    float anneal= (float)1.0-(float)(cluster_iters-i-1)/cluster_iters/2;
	    
	    mean_dist= -100000;
	    for (Map.Entry<Integer, ArrayList<Integer>> entry : node_index.entrySet()) {
		int node= p_index.label= entry.getKey();
		Integer bo_node;
		float sum_s1= 0, s2;
		distances= new Hashtable<Integer, Float>();
		terms= entry.getValue();
		for (Iterator<Integer> f= terms.listIterator(); f.hasNext();) {
		    Integer term2= p_index.term= f.next();
		    //float s1= (float)(Math.exp(lprobs.get(p_index)- norms.get(node))* tfidf.get_idf(term2));
		    float s1= (float)(Math.exp(lprobs.get(p_index)- norms.get(node)));
		    if (tfidf.use_tfidf==1 || tfidf.use_tfidf==3 || tfidf.use_tfidf==8) s1*= tfidf.get_idf(p_index.term);
		    sum_s1+= s1;
		    if (bo_index2.get(term2)==null) continue;
		    Iterator<Float> e= bo_index2.get(term2).listIterator();
		    for (Iterator<Integer> g= bo_index.get(term2).listIterator(); g.hasNext();) {
			bo_node= g.next();
			s2= s1 * e.next();
			Float distance= distances.get(bo_node);
			if (distance==null) distance= s2;
			else distance+= s2;
			distances.put(bo_node, distance);
		    }
		}
		float q_entropy= q_entropies.get(node);
		//Integer max_node= -100000000;
		Integer max_node= max_prior;
		float max_distance= -10000000;
		//float max_distance= q_entropy + sum_s1 * u1;
		//float q_entropy=(float) Math.log(terms.size());
		//System.out.println(q_entropy+" "+q_entropies.get(node));
		//for (Map.Entry<Integer,Float> entry2 : distances.entrySet()) float post_dist= (float)(anneal* q_entropy + entry2.getValue()+ sum_s1 * u1);
		//System.out.println(node+ " a "+distances.size());
		for (Map.Entry<Integer,Float> entry2 : distances.entrySet()) {
		    entry2.setValue((float) q_entropy + entry2.getValue() + sum_s1 * u1);
		    if (use_medoids) distances2.get(entry2.getKey()).put((int)node, entry2.getValue());
		}
		if (use_medoids) for (Integer node2: nodes) if (nodes2.contains(node2)) {
			Float distance2= distances2.get(node2).get(node); if (distance2!=null) distances.put(node2, distance2);}
		float norm_distance= -10000000;
		//if (norms2!=null) System.out.println(distances.size()+" "+norms2.size()+" "+nodes.size());
		for (Map.Entry<Integer,Float> entry2 : distances.entrySet()) {
		    //double post_dist= anneal* entry2.getValue();
		    double post_dist= entry2.getValue();

		    //if (norms2!=null) {
			//System.out.println(norms2.size()+" "+nodes.size()+" "+entry2.getKey()+" "+Math.exp(norms2.get(entry2.getKey())-sum_norms2)+" "+post_dist);
			//post_dist+= 0.00001*(norms2.get(entry2.getKey())-sum_norms2);
		    //}
		    norm_distance= (float) logsum(norm_distance, post_dist);
		    if (post_dist>max_distance) {
			max_node= entry2.getKey(); 
			max_distance= (float)post_dist;
		    }
		    mean_dist= (float)logsum(mean_dist, post_dist+mean_dist_div);
		}
		//if (distances.size()==0) norm_distance= max_distance;
		//norm_distance= (float)(q_entropy + sum_s1 * u1 + Math.log(node_count-distances.size()));
		if (node_count-distances.size()>0) {
		    mean_dist= (float) logsum(mean_dist, Math.log(node_count-distances.size())+ sum_s1 * u1 +mean_dist_div);
		    norm_distance= (float) logsum(norm_distance, Math.log(node_count-distances.size())+ sum_s1 * u1);
		}
		if (max_node==-100000000) continue;
		//System.out.println(max_node)
		float max_post= (float)(max_distance -norm_distance - norms.get(node));
		//float max_post= -norms.get(node);
		cluster_ids.put(node, max_node);
		for (Iterator<Integer> f= terms.listIterator(); f.hasNext();) { 
		    p_index.term= f.next();
		    CountKey p_index2= new CountKey(max_node, p_index.term);
		    Float lprob= bo_lprobs.get(p_index2);
		    if (lprob==null) lprob= (float) (lprobs.get(p_index) + max_post);
		    else lprob= (float) logsum(lprob, lprobs.get(p_index) + max_post);
		    bo_lprobs.put(p_index2, lprob);
		}
		
		if (i== cluster_iters-1) {
		    CountKey link= new CountKey(node, max_node);
		    bo_links.put(link, max_post);
		}
	    }
	    if (i== cluster_iters-1) continue;
	    norms2= get_cond_norms(bo_lprobs);

	    String tmp= "";
	    for (Map.Entry<Integer, Double> entry : norms2.entrySet()) {
		tmp+= " "+entry.getKey()+ " "+ Math.exp(entry.getValue());
	    }
	    System.out.println("Iter: "+i+"/"+(cluster_iters-1)+" "+ norms2.size()+" # "+tmp +" prune:"+prune_nodes);
	    //System.out.println(nodes.size()+" "+nodes2.size()+" "+norms2.size()+" "+medoids.size()+" "+cluster_ids.size());
	    if (prune_nodes) {
	    //if (1==0) {
		//sum_norms2= -100000;
		//Iterator<Entry<Integer, Double>> entries= norms2.entrySet().iterator();
		Iterator<Integer> entries= nodes.iterator();
		while (entries.hasNext()) {
		    Integer entry= entries.next();
		    Double norm2= norms2.get(entry);
		    if (norm2==null || Math.exp(norm2)<=5.0) {norms2.remove(entry); entries.remove();}
		    //else sum_norms2= logsum(sum_norms2, norm2);
		}	  
	    }
	    bo_index= new Hashtable<Integer, ArrayList<Integer>>();
	    bo_index2= new Hashtable<Integer, ArrayList<Float>>();
	    if (!use_medoids) {add_cluster_nodes(nodes, bo_index, bo_index2, bo_lprobs, norms2, a1, u1, min_idf); continue;}

	    CountKey p_index3= new CountKey();
	    for (Map.Entry<Integer, ArrayList<Integer>> entry : node_index.entrySet()) {
		int node= p_index.label= entry.getKey();
		Integer bo_node= cluster_ids.get(node);
		if (bo_node==null || !norms2.containsKey(bo_node)) continue;
		//if (node==bo_node) continue;
		//if (prune_nodes) if (node==bo_node && Math.exp(norms2.get(bo_node))>split_count) continue;
		p_index3.label= bo_node;
		float sum_s1= 0, s2;
		float q_entropy= q_entropies.get(node);
		terms= entry.getValue();
		float similarity= 0;
		for (Iterator<Integer> f= terms.listIterator(); f.hasNext();) {
		    Integer term2= p_index.term= p_index3.term= f.next();
		    //float s1= (float)(Math.exp(lprobs.get(p_index)- norms.get(node))* tfidf.get_idf(term2));
		    float s1= (float)(Math.exp(lprobs.get(p_index)- norms.get(node)));
		    if (tfidf.use_tfidf==1 || tfidf.use_tfidf==3 || tfidf.use_tfidf==8) s1*= tfidf.get_idf(p_index.term);
		    sum_s1+= s1;
		    similarity+= s1 * bo_lprobs.get(p_index3);
		}
		similarity= (float) q_entropy + similarity + sum_s1 * u1;
		Float medoid_similarity= medoid_similarities.get(bo_node);
		if (medoid_similarity==null || similarity> medoid_similarity) {
		    medoid_similarities.put(bo_node, similarity);
		    medoids.put(bo_node, node);
		    //System.out.println(bo_node+" "+node+" "+medoid_similarity+" "+similarity);
		}
	    }
	    //System.out.println(nodes.size()+" "+nodes2.size()+" "+norms2.size()+" "+medoids.size()+" "+cluster_ids.size());
	    System.out.println("mean_dist: "+mean_dist);
	    HashSet<Integer> nodes3= new HashSet<Integer>();
	    //for (Map.Entry<Integer, Float> entry : medoid_counts.entrySet()) tmp+= " "+entry.getKey()+ " "+ Math.exp(entry.getValue());
	    //System.out.println("Iter: "+i+"/"+(cluster_iters-1)+" "+ medoid_counts.size()+" # "+tmp);
	    nodes2.addAll(nodes);
	    double max_norm= -100000000;
	    for (Integer node2: norms2.keySet()) {
		//for (Integer node2: medoids.keySet()) {
		Integer node3= medoids.get(node2);
		//if (!prune_nodes) {
		if (!node3.equals(node2)) {
		    //System.out.println(node2+" "+node3+" "+nodes.size()+" "+nodes.contains(node2));
		    if (!prune_nodes || !(Math.exp(norms2.get(node2))>split_count)) {
			nodes.remove(node2);
			nodes2.remove(node2);
			distances2.remove(node2);
		    }
		    nodes3.add(node3);
		    //norms2.put(node3, norms2.get(node2));
		    //norms2.remove(node2);
		}
		//}
		double norm2= norms2.get(node2);
		if (norm2>=max_norm) {
		    max_norm= norm2; 
		    //if (node3.equals(node2)) 
		    max_prior= node2;
		    //else max_prior= node3;
		}
	    }
	    if (!nodes.contains(max_prior)) nodes3.add(max_prior);
	    //System.out.println(nodes.size()+" "+nodes2.size()+" "+norms2.size()+" "+medoids.size()+" "+cluster_ids.size());
	    if (nodes3.size()!=0) {
		System.out.println("Replacing medoids: " +nodes3.size());
		for (Integer node2: medoids.keySet()) {
		    Integer node3= medoids.get(node2);
		    if (!node2.equals(node3)){
			//System.out.println("z "+node2+ " "+node3+" "+norms2.get(node2)+" "+norms2.containsKey(node3)+" "+norms2.size());
			norms2.put(node3, norms2.get(node2));
			//norms2.remove(node2);
		    }
		}
		add_cluster_nodes(nodes3, bo_index, bo_index2, lprobs, norms, a1, u1, min_idf);
		nodes.addAll(nodes3);
		for (Integer node: nodes3) {Hashtable<Integer, Float> distances3= new Hashtable<Integer, Float>(); distances2.put(node, distances3);}
	    }
	    System.out.println("Nodes: "+nodes.size()+" "+nodes2.size()+" "+norms2.size()+" "+medoids.size()+" "+cluster_ids.size());

	    //System.out.println(old_mean_dist-mean_dist+" "+EQ(old_mean_dist,mean_dist));
	    //if (Math.abs(old_mean_dist -mean_dist) < 0.0001 && i< cluster_iters-2) i= cluster_iters-2;
	    if (old_mean_dist==mean_dist && i< cluster_iters-2) i= cluster_iters-2;
	    old_mean_dist= mean_dist;
	    mean_dist_div=(float)(-Math.log(nodes.size())- Math.log(node_index.size()));
	}
	//System.exit(-1);
	return bo_links;
    }

    public HashSet<Integer> sample_nodes(int node_count, ArrayList<Integer> pool, boolean replacement) {
	System.out.println("Sampling nodes:"+node_count+"/"+pool.size());
	if (node_count==0) return new HashSet<Integer>();
        CountKey p_index= new CountKey();
        HashSet<Integer> nodes= new HashSet<Integer>(node_count);
        for (int i= 0; i<node_count; i++) {
	    if (pool.size()==0) break;
	    int node= rand.nextInt(pool.size());
            nodes.add(pool.get(node));
	    if (replacement==false) pool.remove(node);
	}
	return nodes;
    }
    
    public void add_cluster_nodes(HashSet<Integer> nodes, Hashtable<Integer, ArrayList<Integer>> bo_index, Hashtable<Integer, ArrayList<Float>> bo_index2, Hashtable<CountKey, Float> lprobs, Hashtable<Integer, Double> norms, float a1, float u1, float min_idf) {
	//System.out.println("Adding seeds: "+nodes.size());
	if (nodes.size()==0) return;
	CountKey p_index= new CountKey();
	for (Map.Entry<CountKey, Float> entry : lprobs.entrySet()) {
            p_index= entry.getKey();
            Integer node= p_index.label;
            if (!nodes.contains(node) || tfidf.get_idf(p_index.term)<=min_idf) continue;
	    Float lprob= (float)logsum((entry.getValue() - norms.get(node)+a1), u1)- u1;
	    //Float lprob= (float)logsum((entry.getValue()+Math.log(tfidf.get_idf(p_index.term)) - norms.get(node)+a1), u1)- u1;
	    //System.out.println(logsum((entry.getValue() - norms.get(node)+a1), u1)+" "+u1);
	    inv_index_add(node, p_index.term, lprob, bo_index, bo_index2);
	}
    }

    public void inv_index_add(int node, int term, float lprob, Hashtable<Integer, ArrayList<Integer>> index, Hashtable<Integer, ArrayList<Float>> index2) {
	//Integer node= node2, term= term2;
	ArrayList<Integer> nodes= index.get(term);
	if (nodes == null) {
	    nodes= new ArrayList<Integer>(4);
	    nodes.add(node);
	    index.put(term, nodes);
	} else nodes.add(node);
	ArrayList<Float> lprobs= index2.get(term);
	if (lprobs == null) {
	    lprobs= new ArrayList<Float>(4);
	    lprobs.add(lprob);
	    index2.put(term, lprobs);
	} else lprobs.add(lprob);
	//System.out.println("add: "+ node+" "+ term);
    }
    
    public void prune_labels(int min_label_count) {
	if (min_label_count<2 || model.prior_lprobs==null) return;
	double min_lcount= Math.log(min_label_count);
	Integer label;
	System.out.println("model.prior_lprobs.size():"+ model.prior_lprobs.size());//+" model.node_links.size():"+model.node_links.size());
	Iterator<Entry<Integer, Float>> entries= model.prior_lprobs.entrySet().iterator();
	while (entries.hasNext()) {
	    Entry<Integer, Float> entry= entries.next();
	    label= entry.getKey();
	    if (entry.getValue()<min_lcount) entries.remove();
	    
	}
	Set<Integer> label_keys= model.prior_lprobs.keySet();
	if (model.node_links!=null) {
	    Iterator<Entry<Integer, IntBuffer>> entries2= model.node_links.entrySet().iterator();
	    while (entries2.hasNext()) {
		Entry<Integer, IntBuffer> entry2= entries2.next();
		int[] labels2= entry2.getValue().array();
		int i= 0;
		for (int label2: labels2) if (model.prior_lprobs.containsKey(-(label2+1))) labels2[i++]= label2;
		if (i==labels2.length) continue;
		if (i==0) {entries2.remove(); continue;}
		labels2= Arrays.copyOf(labels2, i);
		entry2.setValue(IntBuffer.wrap(labels2));
	    }
	    label_keys= model.node_links.keySet();
	}
	System.out.println("model.prior_lprobs.size():"+ model.prior_lprobs.size());//+" model.node_links.size():"+model.node_links.size());
	Iterator<Entry<CountKey, Float>> entries2= model.cond_lprobs.entrySet().iterator();
	if (debug>0) System.out.println("Pruning conditional hash table:"+ model.cond_lprobs.size());
	while (entries2.hasNext()) {
	    label= entries2.next().getKey().label;
	    if (!label_keys.contains(label)) entries2.remove();
	}
	if (debug>0) System.out.println("Hash table pruned:"+ model.cond_lprobs.size());
    }
    
    public void prune_counts(double prune_count_table, int cond_hashsize) {
	//this.cond_hashsize= cond_hashsize;
	if (debug>0) System.out.println("Pruning conditional hash table:"+ model.cond_lprobs.size()+ " " + prune_count_table);
	Iterator<Entry<CountKey, Float>> entries= model.cond_lprobs.entrySet().iterator();
	while (entries.hasNext()) {
	    Entry<CountKey,  Float> entry= (Entry<CountKey,  Float>) entries.next();
	    float lprob= (Float) entry.getValue();
	    
	    //if (tfidf.use_tfidf!=0 && tfidf.use_tfidf!=2) lprob+= Math.log(tfidf.get_idf(((CountKey)entry.getKey()).term));
	    if (tfidf.use_tfidf==1 || tfidf.use_tfidf==3 || tfidf.use_tfidf==8|| tfidf.use_tfidf==15 || tfidf.use_tfidf==16|| tfidf.use_tfidf==17|| tfidf.use_tfidf==18|| tfidf.use_tfidf==19|| tfidf.use_tfidf==23|| tfidf.use_tfidf==24) lprob+= Math.log(tfidf.get_idf(((CountKey)entry.getKey()).term));
	    //System.out.println(lprob+" "+tfidf.get_idf(((CountKey)entry.getKey()).term)+ " "+prune_count_table);
	    if (lprob <= prune_count_table) entries.remove();
	}
	if (debug>0) System.out.println("Hash table pruned:"+ model.cond_lprobs.size());
	if (model.cond_lprobs.size() > cond_hashsize) prune_counts_size(model.cond_lprobs, cond_hashsize);
    }
    
    public void prune_counts2(double prune_count_table) {
	if (debug>0) System.out.println("Pruning conditional hash table:"+ model.cond_lprobs.size()+ " " + prune_count_table);
	Iterator<Entry<CountKey, Float>> entries= model.cond_lprobs.entrySet().iterator();
	while (entries.hasNext()) {
	    Entry<CountKey,  Float> entry= (Entry<CountKey,  Float>) entries.next();
	    float lprob= (Float) entry.getValue();
		if (lprob <= prune_count_table) entries.remove();
	}
	if (debug>0) System.out.println("Hash table pruned:"+ model.cond_lprobs.size());
    }

    public void prune_counts_size(Hashtable<CountKey, Float> counts, int hashsize) {
	if (counts.size() > hashsize) {
	    int bins= (int) Math.log(counts.size());
	    int i= 0, j= 0;
	    double tmp[]= new double[1+counts.size()/bins];
	    for (Map.Entry<CountKey, Float> entry : counts.entrySet()) {
		if (i++%bins==0) {
		    float lprob= (Float)entry.getValue();
		    if (tfidf.use_tfidf==1 || tfidf.use_tfidf==3 || tfidf.use_tfidf==5) lprob+= Math.log(tfidf.get_idf(((CountKey)entry.getKey()).term));
		    //if (tfidf.use_tfidf!=0 && tfidf.use_tfidf!=2) lprob+= Math.log(tfidf.get_idf(((CountKey)entry.getKey()).term));
		    tmp[j++]= lprob;
		}
	    }
	    Arrays.sort(tmp);
	    double prune = tmp[(counts.size() - hashsize)/bins];
	    Iterator<Entry<CountKey, Float>> entries= counts.entrySet().iterator();
	    while (entries.hasNext()) {
		Entry<CountKey, Float> entry= (Entry<CountKey, Float>)entries.next();
		float lprob= (Float)entry.getValue();
		//if (tfidf.use_tfidf!=0 && tfidf.use_tfidf!=2) lprob+= Math.log(tfidf.get_idf(((CountKey)entry.getKey()).term));
		if (tfidf.use_tfidf==1 || tfidf.use_tfidf==3 || tfidf.use_tfidf==5) lprob+= Math.log(tfidf.get_idf(((CountKey)entry.getKey()).term));
		if (lprob <= prune) entries.remove();
	    }
	}
	if (debug>0) System.out.println("Hash table pruned:"+ model.cond_lprobs.size());
    }
    
    public void use_icfs() throws Exception {
        int t= 0;
        float norm;
	double[] tmp_vals;
        if (model.cond_bgs.size()>0) {
            tmp_vals= new double[model.cond_bgs.size()];
            for (Float lprob: model.cond_bgs.values()) tmp_vals[t++]= (double) lprob;
            norm= (float) logsum_doubles(tmp_vals);
	    for (Map.Entry<Integer, Float> entry : model.cond_bgs.entrySet()) entry.setValue((Float)(entry.getValue()- norm));
        }
	for (Iterator<Integer> d = tfidf.idfs.keySet().iterator(); d.hasNext();) {
	    Integer term= d.next();
	    Float idf= (float) -model.cond_bgs.get(term);
	    tfidf.idfs.put(term, idf);
	    //System.out.println(term+" "+ idf);
	}
    }

    public void apply_idfs() throws Exception {
	//if (tfidf.use_tfidf==0) return;
	for (Iterator<Integer> d = model.cond_bgs.keySet().iterator(); d.hasNext();) {
	    Integer term= d.next();
	    Float idf= (float)tfidf.get_idf(term);
	    if (!tfidf.idfs.containsKey(term) || idf<=0.0) d.remove(); 
	    else {
		if (tfidf.use_tfidf!=1 && tfidf.use_tfidf!=3 && tfidf.use_tfidf!=13 && tfidf.use_tfidf!=16) idf= (float) 1;
		Float lprob= model.cond_bgs.get(term) +(float) Math.log(idf);
		model.cond_bgs.put(term, lprob);
		//System.out.println(term+" "+lprob);
	    }
	}
	for (Iterator<CountKey> e= model.cond_lprobs.keySet().iterator(); e.hasNext();) {
	    CountKey p_index= e.next();
	    Integer term= p_index.term;
	    Float idf= (float)tfidf.get_idf(term);
	    if (!tfidf.idfs.containsKey(term) || idf<=0.0) {
		//System.out.println(idf+" "+term+" "+tfidf.idfs.containsKey(term));
		e.remove();
	    }
	    else {
		if (tfidf.use_tfidf!=1 && tfidf.use_tfidf!=3 && tfidf.use_tfidf!=13 && tfidf.use_tfidf!=16) idf= (float) 1;
		Float lprob= (float)(model.cond_lprobs.get(p_index))+(float)Math.log(idf);
		//Float lprob= (float)(model.cond_lprobs.get(p_index))+(float)Math.log(tfidf.get_idf(term));
		model.cond_lprobs.put(p_index, lprob);
	    }
	}
    }
    
    public void scale_conditionals(double cond_scale) throws Exception {
	if (debug>0) System.out.println("Scaling conditionals");
	for (Map.Entry<CountKey, Float> entry : model.cond_lprobs.entrySet()) entry.setValue((Float)(entry.getValue()*(float)cond_scale));
    }

    public Hashtable<Integer, Double> get_cond_norms(Hashtable<CountKey, Float> lprobs) throws Exception {
	Hashtable<Integer, Double> norms= new Hashtable<Integer, Double>();
	for (Map.Entry<CountKey, Float> entry : lprobs.entrySet()) {
            Integer label= ((CountKey)entry.getKey()).label;
            Double lsum= norms.get(label);
            if (lsum == null) lsum= -100000.0;
            //lsum= logsum(lsum, entry.getValue());
	    //if (tfidf.use_tfidf==23) lsum= logsum(lsum, 0);
	    //if (tfidf.use_tfidf==23) lsum= logsum(lsum, entry.getValue()*0.5);
            //else 
	    lsum= logsum(lsum, entry.getValue()* Math.abs(cond_norm));
	    norms.put(label, lsum);
        }
	//if (tfidf.use_tfidf==23) for (Map.Entry<Integer, Double> entry : norms.entrySet()) entry.setValue(entry.getValue()*0.5);
	return norms;
    }

    public void normalize_conditionals(Hashtable<Integer, Double> norms) throws Exception {
	//Hashtable<Integer, Double> norms= get_cond_norms(model.cond_lprobs);
	if (Math.abs(cond_norm)!=1) for (Map.Entry<Integer, Double> entry : norms.entrySet()) entry.setValue(entry.getValue() / Math.abs(cond_norm));
	for (Map.Entry<CountKey, Float> entry : model.cond_lprobs.entrySet())
	    entry.setValue((Float)(entry.getValue()- (float)(double)norms.get(((CountKey)entry.getKey()).label)));
	double[] tmp_vals;
	int t= 0;
	float norm;
	if (model.cond_bgs.size()>0) {
	    tmp_vals= new double[model.cond_bgs.size()];
	    //for (Float lprob: model.cond_bgs.values()) tmp_vals[t++]= (double) lprob;
	    //norm= (float) logsum_doubles(tmp_vals);
	    for (Float lprob: model.cond_bgs.values()) tmp_vals[t++]= (double) lprob * Math.abs(cond_norm);
	    norm= (float)(logsum_doubles(tmp_vals) / Math.abs(cond_norm));
	    for (Map.Entry<Integer, Float> entry : model.cond_bgs.entrySet()) entry.setValue((Float)(entry.getValue()- norm));
	}
    }
    
    public void normalize_priors() throws Exception {
	//normalize_conditionals();
	double[] tmp_vals;
	int t= 0;
	float norm;
	if (model.prior_lprobs!=null) {
	    t= 0; tmp_vals= new double[model.prior_lprobs.size()];
	    for (Float lprob: model.prior_lprobs.values()) tmp_vals[t++]= (double) lprob;
	    norm= (float) logsum_doubles(tmp_vals);
	    for (Map.Entry<Integer, Float> entry : model.prior_lprobs.entrySet()) entry.setValue((Float)(entry.getValue()- norm));
	}
    }
    
    public void smooth_conditionals(double bg_unif_smooth, double jelinek_mercer, double dirichlet_prior, double absolute_discount, double powerlaw_discount,double backoff_discount, double cond_scale, double kernel_jelinek_mercer, double kernel_powerlaw_discount, double kernel_dirichlet_prior, double cluster_jelinek_mercer, double poisson_scale) throws Exception {
        System.out.println("model.cond_bgs.size(): "+model.cond_bgs.size());
	jelinek_mercer= Math.max(jelinek_mercer, 1.0E-60);
        kernel_jelinek_mercer= Math.max(kernel_jelinek_mercer, 1.0E-60);
	double jm= Math.log(jelinek_mercer);
	double a5= Math.log(1.0-bg_unif_smooth); // + Math.log(jelinek_mercer);
        double bg_uniform= Math.log(bg_unif_smooth) - Math.log(model.cond_bgs.size());// + Math.log(jelinek_mercer);
	double ad= Math.log(absolute_discount);
	double pd= Math.log(powerlaw_discount);
	double dp= -1000000;
	//if (dirichlet_prior!=0) dp= logsum(dirichlet_prior, 0);
	if (dirichlet_prior!=0) dp= Math.log(dirichlet_prior*model.cond_bgs.size());
	model.bo_lweights= new Hashtable<Integer, Float>();

	if (tfidf.use_tfidf==12 || tfidf.use_tfidf==13) {
	    cond_norm= 2.0;
	    Hashtable<Integer, Double> norms= get_cond_norms(model.cond_lprobs);
	    normalize_conditionals(norms);
	    for (Map.Entry<CountKey, Float> entry : model.cond_lprobs.entrySet()) entry.setValue((float)Math.exp(entry.getValue()));
	    for (Map.Entry<Integer, Float> entry : model.cond_bgs.entrySet()) entry.setValue((float)0);
	    //for (Map.Entry<Integer, Double> entry : norms.entrySet()) model.bo_lweights.put(entry.getKey(), (float)0);
	    return;
	}

        //if (tfidf.use_tfidf>=9) {
	if (tfidf.use_tfidf==9 || tfidf.use_tfidf==10 || tfidf.use_tfidf==11 || tfidf.use_tfidf==15 || tfidf.use_tfidf==16|| tfidf.use_tfidf==20|| tfidf.use_tfidf==21) {
	    //bm25
	    for (Map.Entry<Integer, Float> entry : tfidf.idfs.entrySet()) {
		if (tfidf.use_tfidf==9)entry.setValue((float)Math.log((tfidf.train_count+1)/(entry.getValue()+0.5))); // Fang:04, Lv:11
		//System.out.println(entry.getKey()+" " +entry.getValue()+" "+(tfidf.train_count-entry.getValue()));
		if (tfidf.use_tfidf==10) entry.setValue((float)Math.log(Math.max(1, (tfidf.train_count-entry.getValue()+0.5)/(entry.getValue()+0.5)))); // Manning:08, wikipedia
		//if (tfidf.use_tfidf==10) entry.setValue((float)((tfidf.train_count-entry.getValue()+0.5)/(entry.getValue()+0.5))); // Manning:08, wikipedia
		if (tfidf.use_tfidf==11)entry.setValue((float)Math.log((tfidf.train_count)/(entry.getValue()))); // Atire
		//if (tfidf.use_tfidf==11) entry.setValue((float)Math.log(Math.max(1, (tfidf.train_count-entry.getValue()+0.5)/(entry.getValue()+0.5))));
		if (tfidf.use_tfidf==20) entry.setValue((float)Math.log(Math.max(1, (tfidf.train_count-entry.getValue()+0.5)/(entry.getValue()+0.5))));
		if (tfidf.use_tfidf==21) entry.setValue((float)Math.log(Math.max(1, tfidf.train_count/(entry.getValue()))));
	    }
	    tfidf.normalized= 1;
	    Hashtable<Integer, Double> norms= get_cond_norms(model.cond_lprobs);
	    double avgdl= -1000000;
	    for (Map.Entry<Integer, Float> entry : model.cond_bgs.entrySet()) entry.setValue((float)0);
	    for (Map.Entry<Integer, Double> entry : norms.entrySet()) {
		//System.out.println(entry.getKey()+" "+ entry.getValue());
		//model.bo_lweights.put(entry.getKey(), (float)0);
		avgdl= logsum(avgdl, entry.getValue());
	    }
	    avgdl= Math.exp(avgdl-Math.log(norms.size()));
	    double k1= tfidf.length_norm;
	    double b= tfidf.length_scale;
	    //k3= tfidf.idf_lift;
	    Iterator<Entry<CountKey, Float>> entries= model.cond_lprobs.entrySet().iterator();
	    while (entries.hasNext()) {
		Entry<CountKey,  Float> entry= (Entry<CountKey,  Float>) entries.next();
		//for (Map.Entry<CountKey, Float> entry : model.cond_lprobs.entrySet()){
		double fqd= Math.exp(entry.getValue());
		double dl= Math.exp(norms.get(entry.getKey().label));
		double idf= tfidf.idfs.get(entry.getKey().term);
		if (tfidf.use_tfidf==20|| tfidf.use_tfidf==21) idf= 1;
		//double c= fqd/(1-b+b*(dl/avgdl));
		//if (tfidf.use_tfidf==11) entry.setValue((float)(tfidf.idfs.get(entry.getKey().term) * ((k1+1)*(c+0.5)) / (k1+c+0.5)));
		//if (tfidf.use_tfidf==11) entry.setValue((float)(tfidf.idfs.get(entry.getKey().term) * ((k1+1)*(c+1.0)) / (k1+c+1.0)));
		//else entry.setValue((float)(tfidf.idfs.get(entry.getKey().term) * ((k1+1)*c) / (k1+c)));
		
		entry.setValue((float)(idf * (fqd*(k1+1)) / (fqd+ k1*(1-b+b*dl/avgdl))));
		//System.out.println(entry.getValue());
		if (entry.getValue()==0) entries.remove();
	    }
	    //if (model.node_links!=null) for (int label: norms.keySet()) model.bo_lweights.put(label, (float)-1000000);
	    if (tfidf.use_tfidf!=20|| tfidf.use_tfidf==21) return;
	}

	Hashtable<Integer, Double> norms= get_cond_norms(model.cond_lprobs);

	/*double poisson_mean= -100000;
	double docs= -100000;
	for (Map.Entry<Integer, Double> entry: norms.entrySet()) {
	    Integer label= entry.getKey();
	    double norm= entry.getValue();
	    float prior_lprob= model.prior_lprobs.get(label);
	    //System.out.println("label:"+ label+" "+norm+" "+ prior_lprob+" "+ Math.exp(norm-prior_lprob));
	    Float mean_length= (float)Math.exp(norm-prior_lprob);
	    model.length_lprobs.put(label, mean_length);
	    poisson_mean= logsum(poisson_mean, norm);
	    docs= logsum(docs, prior_lprob);
	}
	//System.out.println(Math.exp(poisson_mean)+" "+docs);
	this.poisson_scale= poisson_scale * Math.exp(poisson_mean - docs);*/

	if (powerlaw_discount== -1) {
	    float ones= 0, twos= 0;
	    for (Float lprob: model.cond_lprobs.values()) if (lprob<=2.00000001) {
		    if (lprob<=1.00000001) ones+= 1;
		    twos+= 1;
		}
	    twos-= ones;
	    powerlaw_discount= ones/(ones+twos*2); //Huang:10, Ney:04
	    pd= Math.log(powerlaw_discount);
	    System.out.println("powerlaw_discount: "+powerlaw_discount);
	}
	double norm;
	Hashtable<Integer, Double> norms2= norms;


	if (absolute_discount!=0 || powerlaw_discount!=0 || cluster_jelinek_mercer!=0 || kernel_powerlaw_discount!=0) {
	    Iterator<Entry<CountKey, Float>> entries= model.cond_lprobs.entrySet().iterator();
	    while (entries.hasNext()) {
		Entry<CountKey,  Float> entry= (Entry<CountKey,  Float>) entries.next();
		float lprob= entry.getValue();
		
		Integer node= ((CountKey)entry.getKey()).label;
		double pd2= pd, powerlaw_discount2= powerlaw_discount;
		if (model.node_links!=null) {
		    if (node>=0) {
			pd2= Math.log(kernel_powerlaw_discount);
			powerlaw_discount2= kernel_powerlaw_discount;
		    }
		    else if (node<model.min_encoded_label) {absolute_discount= 0; powerlaw_discount= 0;} 
		}
		if (absolute_discount!=0) lprob= (float) logsubtract(lprob, ad);
		if (powerlaw_discount2!=0) lprob= (float) logsubtract(lprob, pd2+lprob*powerlaw_discount2);
		entry.setValue(lprob);
	    }
	    norms2= get_cond_norms(model.cond_lprobs);
	} 

	model.bo_lweights= new Hashtable<Integer, Float>();
	for (Map.Entry<Integer, Double> entry: norms.entrySet()) {
	    norm= entry.getValue();
	    Double norm2= norms2.get(entry.getKey());
	    if (norm2==null) norm2= norm;
	    Integer node= entry.getKey();
	    double jm2= jm;
	    double dp2= dp;
	    if (model.node_links!=null) {
		if (node>=0) {
		    jm2= Math.log(kernel_jelinek_mercer);
		    dp2= -1000000;
		    if (kernel_dirichlet_prior!=0) dp2= Math.log(kernel_dirichlet_prior*model.cond_bgs.size());
		}
		else if (node<model.min_encoded_label) {jm2= Math.log(cluster_jelinek_mercer); dp2= -1000000;}
	    }
	    Float bo_weight= (float)(logsubtract(0, logsubtract(norm2, norm2+jm2) - logsum(norm, dp2)));
	    model.bo_lweights.put(node, bo_weight);
	    //System.out.println(node+" "+bo_weight+" "+norm2+" "+norm+" "+jm2+" "+dp2);
	}
	norms= norms2;
	if (backoff_discount!= 0){
            double[] tmp_vals= new double[model.cond_bgs.size()];
            int t= 0;
	    for (Map.Entry<Integer, Float> entry : model.cond_bgs.entrySet()) entry.setValue(entry.getValue());
            for (Float lprob: model.cond_bgs.values()) tmp_vals[t++]= (double) lprob;
            norm= (float)(logsum_doubles(tmp_vals));

            Iterator<Entry<CountKey, Float>> entries= model.cond_lprobs.entrySet().iterator();
            while (entries.hasNext()) {
                Entry<CountKey,  Float> entry= (Entry<CountKey,  Float>) entries.next();
                float lprob= entry.getValue();
                if (backoff_discount!=0) lprob= (float) logsubtract(lprob, norms.get(((CountKey)entry.getKey()).label)+ Math.log(backoff_discount)+model.cond_bgs.get(((CountKey)entry.getKey()).term) -norm);
                if (lprob>-1000000) entry.setValue(lprob);
                else entries.remove();
            }
            norms= get_cond_norms(model.cond_lprobs);
	}
	
	if (cond_scale!=1.0) scale_conditionals(cond_scale);
	normalize_conditionals(norms);
	for (Map.Entry<Integer, Float> entry : model.cond_bgs.entrySet()) entry.setValue((Float)(float)(logsum(a5 + entry.getValue(), bg_uniform)));
	if (model.node_links==null) smooth_cond_nodes(-1000000, 1000000);
 	else {
	    if (cluster_jelinek_mercer!=0) smooth_cond_nodes(-100000000, model.min_encoded_label);
	    smooth_cond_nodes(model.min_encoded_label, 0);
	    smooth_cond_nodes(0, 100000000);
	}
	
	if (cond_norm<0) {
	    for (Map.Entry<Integer, Float> entry : model.cond_bgs.entrySet()) entry.setValue((float)Math.exp(entry.getValue()));
	    for (Map.Entry<CountKey, Float> entry : model.cond_lprobs.entrySet()) entry.setValue((float)Math.exp(entry.getValue()));
	}

	if (model.node_links==null) precompute_cond_nodes(-1000000, 1000000);
 	else {
	    precompute_cond_nodes(0, 100000000);
	    precompute_cond_nodes(model.min_encoded_label, 0);
	    if (cluster_jelinek_mercer!=0) precompute_cond_nodes(-100000000, model.min_encoded_label);
	}
    }
    
    public void precompute_cond_nodes(int top, int bottom) {
	CountKey p_index= new CountKey(), p_index2= new CountKey();
	float bo_lprob;
	Integer node, bo_node= null;
	IntBuffer node_list= null;
	for (Map.Entry<CountKey, Float> entry : model.cond_lprobs.entrySet()) {
	    p_index= entry.getKey();
	    if (p_index.label<top || p_index.label>=bottom) continue;
	    p_index2.term= p_index.term;
	    node= p_index.label;
	    bo_lprob= model.bo_lweights.get(node);
	    bo_node= null;
	    if (model.node_links!=null && !no_backoffs) node_list= model.node_links.get(node);
	    if (node_list!=null) bo_node= node_list.get(0);
	    //if (model.node_links!=null) bo_node= model.node_links.get(node);
	    if (bo_node!=null) { 
		p_index2.label= bo_node;
		bo_lprob+= model.cond_lprobs.get(p_index2);
	    }
	    else bo_lprob+= model.cond_bgs.get(p_index.term);
	    entry.setValue((float)entry.getValue()-bo_lprob);
	}
    }

    public void smooth_cond_nodes(int top, int bottom) {
	CountKey p_index= new CountKey(), p_index2= new CountKey();
	float bo_lprob, node_lweight;
	System.out.println(top+" "+bottom);
	Integer node, bo_node= null;
	IntBuffer node_list= null;
	for (Map.Entry<CountKey, Float> entry : model.cond_lprobs.entrySet()) {
	    p_index= entry.getKey();
	    if (p_index.label<top || p_index.label>=bottom) continue;
	    p_index2.term= p_index.term;
	    node= p_index.label;
	    bo_lprob= model.bo_lweights.get(node);
	    node_lweight= (float)logsubtract(0, bo_lprob);
	    bo_node= null;
	    if (model.node_links!=null && !no_backoffs) node_list= model.node_links.get(node);
	    if (node_list!=null) bo_node= node_list.get(0);
	    //if (model.node_links!=null) bo_node= model.node_links.get(node);
	    //System.out.println(node+" "+bo_node+" "+p_index2.term);
	    if (bo_node!=null) { 
		p_index2.label= bo_node;
		bo_lprob+= model.cond_lprobs.get(p_index2);
	    }
	    else bo_lprob+= model.cond_bgs.get(p_index.term);
	    entry.setValue((float)logsum(bo_lprob, node_lweight+entry.getValue()));
	}
    }
    
    //public void smooth_prior(double prior_scale) throws Exception{
    //normalize_priors();
	//if (model.prior_lprobs!=null) {
	//for (Map.Entry<Integer, Float> entry : model.prior_lprobs.entrySet()) entry.setValue((Float)(float) (entry.getValue() * prior_scale));
	//if (cond_norm<0) for (Map.Entry<Integer, Float> entry : model.prior_lprobs.entrySet()) entry.setValue((float)Math.exp(entry.getValue()));
	//}
    // }
    
    public final int[] intbuf_add(int number, int[] buf) {
	int buf_size2= buf.length + 1;
	int[] buf2= new int[buf_size2];
	int h= 0;
	for (int j: buf) 
	    if (j < number) buf2[h++]= j; 
	    else break;
	buf2[h++]= number;
	for (; h < buf_size2;) buf2[h]= buf[(h++)-1];
	return buf2;
    }
    
    public final int[] intbuf_add2(int number, int[] buf, int[] buf2) {
	int buf_size2= buf2.length;
	int h= 0;
	for (int j: buf) 
	    if (j < number) buf2[h++]= j; 
	    else break;
	buf2[h++]= number;
	for (; h < buf_size2;) buf2[h]= buf[(h++)-1];
	return buf2;
    }
    
	public final int[] intbuf_remove(int number, int[] buf) {
		int buf_size2= buf.length - 1;
		int[] buf2= new int[buf_size2];
		int h= 0;
		for (int j:buf) if (j != number) buf2[h++]= j; else break;
		for (; h < buf_size2;) buf2[h]= buf[++h];
		return buf2;
	}

    public double log2(double val1) {return Math.log(val1)* ILOG2;}

	public double logsum(double val1, double val2) {
	    if (val1+20.0 < val2) return val2;
	    if (val2+20.0 < val1) return val1;
	    if (val1 > val2) return Math.log(Math.exp(val2 - val1) + 1.0) + val1;
	    return Math.log(Math.exp(val1 - val2) + 1.0) + val2;
	}

	public double logsubtract(double val1, double val2) {
		// Note: negative values floored to 0
		if (val2+20.0 < val1) return val1;
		if (val1 > val2) return Math.log(-Math.exp(val2 - val1) + 1.0) + val1;
		return (-1000000.0);
	}

	public float flogsum(float val1, float val2) {
		if (val1 > val2) return (float) Math.log(Math.exp(val2 - val1) + 1) + val1;
		else return (float) Math.log(Math.exp(val1 - val2) + 1) + val2;
	}

	public float flogaddone(float val1) {
		return (float) Math.log(Math.exp(val1) + 1.0);
	}

	public final double sum_doubles(double[] vals) {
		TreeSet<DoubleBuffer> treesort = new TreeSet<DoubleBuffer>();
		for (double val: vals) {
			double[] entry = {Math.abs(val), val};
			treesort.add(DoubleBuffer.wrap(entry));
		}             
		double sum= 0.0;
		for (Iterator<DoubleBuffer> e = treesort.descendingIterator(); e.hasNext();) sum+= e.next().get(1);	
		return sum;
		//while (treesort.size()>1){
		//   //Iterator<DoubleBuffer> e = treesort.descendingIterator();
		//    Iterator<DoubleBuffer> e = treesort.iterator();
		//    double val= e.next().get(1);
		//    e.remove();
		//    val+= e.next().get(1);
		//    e.remove();
		//    double[] entry = {Math.abs(val), val};
		//    treesort.add(DoubleBuffer.wrap(entry));
		//}
		//return treesort.first().get(1);
	}

	public final double logsum_doubles(double[] vals) {
		TreeSet<DoubleBuffer> treesort = new TreeSet<DoubleBuffer>();
		for (double val: vals) {
			double[] entry = {Math.abs(val), val};
			treesort.add(DoubleBuffer.wrap(entry));
		}
		//double sum= 0.0;
		double sum= -1000000.0;
		for (Iterator<DoubleBuffer> e = treesort.descendingIterator(); e.hasNext();) sum= logsum(sum, e.next().get(1));
		return sum;
	}

	public final double logsum_ndoubles(double[] vals) {
		//Note: Sorts original
		Arrays.sort(vals); //reduce double sum error
		double sum= -1000000.0;
		for (double val: vals) sum= logsum(val, sum);
		return sum;
	}

	public final double sum_ndoubles(double[] vals, int count) {
		Arrays.sort(vals, 0, count);
		double sum= 0.0;
		for (int i= 0; i<count;) sum+= vals[i++];
		return sum;
	}

	public final double sum_ndoubles(double[] vals) {
		//Note: Sorts original
		Arrays.sort(vals); //reduce double sum error
		double sum= 0.0;
		//double correct= 0.0;
		//double t, c;
		//for (double val: vals){
		//    val-= correct;
		//    t= sum + val;
		//    float tmp= (float)(t-sum); 
		//    correct= tmp- val;
		//    sum= t;
		//}

		for (double val: vals) sum+= val;
		return sum;

		//double mean= 0.0;
		//int i= 0;
		//for (double val: vals) mean+= (val - mean) / ++i;
		//return mean*i;
		//int fork= 1;
		//int length= vals.length;
		//while (fork< length) {
		//    for (int j= 0; j< length; j+= fork+fork) {
		//	if (j+fork< vals.length) vals[j]+= vals[j+fork];
		//	else {vals[j-fork-fork]+= vals[j]; length=j;}
		//	//System.out.println(j+" "+(j+fork)+" "+vals.length);
		//    }
		//   fork+=fork;
		//}
		//return vals[0];
	}


	/*
	  private final boolean EQ(double d1, double d2){
	  if (Math.abs(d1-d2)< 0.000000001)
	  return true;
	  return false;
	  }
	  
	private final boolean GE(double d1, double d2){
		if (EQ(d1, d2)) return true;
		return GT(d1, d2);
	}

	private final boolean GT(double d1, double d2){
		if (d1> d2 + 0.000000001) return true;
		return false;
	}*/
    
    //public <T> void addToHash(T a, Hashtable<T> c) {
    //}

    public final SparseVector inference(int[] terms, float[] counts, float doc_length, double top_k, int max_retrieved, int w) {
	Integer term2;
        float count;
	int term_count= 0;
        Hashtable<Integer, Double> lprobs= new Hashtable<Integer, Double>();
	float sum_counts= 0;
        double[] bg_lprobs= new double[terms.length];
	Iterator<Integer> e;
        Iterator<Float> g;
	for (int t= 0; t<terms.length; t++) {
            term2= terms[t];
            count= counts[t];
	    
            int[] nodes= inverted_index.get(term2).array();
            float[] cond_lprobs= inverted_index2.get(term2).array();
	    for (int i= 0; i< nodes.length; i++) {
		Integer node= nodes[i];
		double lprob= cond_lprobs[i] * count;
		
		//g= inverted_index2.get(term2).listIterator();
		//for (e= inverted_index.get(term2).listIterator(); e.hasNext();) {
		//   Integer node= e.next();
		//   double lprob= (float) g.next() * count;
		
		if (constrain_labels>1 && node>=model.min_encoded_label) 
		    if (model.node_links!=null) {if (!constrained_labels.contains(-(node+1))) continue;}	
		    else if (!constrained_labels.contains(node)) continue;
	    
		Double lprob2= lprobs.get(node);
		if (lprob2==null) lprob2= lprob;
		else lprob2+= lprob;
		lprobs.put(node, lprob2);
		//System.out.println(node+ " "+lprob+" "+ (lprob+ count * (model.cond_bgs.get(term2) + model.bo_lweights.get(node))) +" "+model.cond_bgs.get(term2)+ " "+ model.bo_lweights.get(node));
            }
	    sum_counts+= count;
	    bg_lprobs[term_count++]= model.cond_bgs.get(term2) * count;
        }
	//System.out.println(term_count);
        double sum_bg_lprob= sum_ndoubles(bg_lprobs, term_count);
        int n= 0;
        int[] node_sort= new int[lprobs.size()];
        for (Map.Entry<Integer,Double> entry: lprobs.entrySet()) node_sort[n++]= (int)entry.getKey();
        if (model.node_links!=null) Arrays.sort(node_sort);
        TreeSet<DoubleBuffer> retrieve_sort= new TreeSet<DoubleBuffer>();
	
	Hashtable<Integer, Double> bo_lprobs= new Hashtable<Integer, Double>();
        double lprob, max_lprob= -1000000.0, topset_lprob= -1000000.0;
        Hashtable<Integer, Double> label_posteriors= new Hashtable<Integer, Double>();
	IntBuffer node_list;
        for (int i= 0; i<node_sort.length; i++) {
	    Integer node= node_sort[i];
            if (node< model.min_encoded_label) continue;
            lprob= lprobs.get(node);
	    //System.out.println(node+" "+lprob);
	    
	    Float bo_lweight= model.bo_lweights.get(node);
	    //if (bo_lweight==null) bo_lweight= 0;
	    if (model.node_links!=null) {
		Integer bo_node= null;
		//bo_node= model.node_links.get(node);
		node_list= model.node_links.get(node);
		if (node_list!=null) bo_node= node_list.get(0);
		if (bo_node!=null) {
		    Double lprob2= lprobs.get(bo_node);
		    /*while (lprob2==null) {
			bo_lweight+= model.bo_lweights.get(bo_node);
			bo_node= null;
			node_list= model.node_links.get(bo_node);
			if (node_list==null) break;
			bo_node= node_list.get(0);
			lprob2= lprobs.get(bo_node);
			}*/
		    if (lprob2==null) lprob2= -1000000.0;
		    if (no_backoffs) lprob2= sum_bg_lprob;
		    //System.out.println(lprob+" "+lprob2+" "+sum_counts+" "+bo_lweight);
		    if (bo_lweight!=null) lprob+= lprob2 + sum_counts* bo_lweight;
		}
		else if (bo_lweight!=null) lprob+= sum_bg_lprob + sum_counts* bo_lweight;
		lprobs.put(node, lprob);
		if (node<0 && component_counts.get(node)!=null) {label_posteriors.put(-(node+1), -1000000.0); continue;}
		if (model.prior_lprobs!=null) 
		    if (node>=0) lprob+= model.prior_lprobs.get(-(bo_node+1))*prior_scale;
		    else lprob+= model.prior_lprobs.get(-(node+1))*prior_scale;
            } else {
		if (bo_lweight!=null) lprob+= sum_bg_lprob + sum_counts* bo_lweight;
		//lprob+= sum_bg_lprob + sum_counts* model.bo_lweights.get(node);
		if (model.prior_lprobs!=null) lprob+= model.prior_lprobs.get(node)*prior_scale;
	    }

	    //float length_lprob= model.length_lprobs.get(node);
	    //lprob+= (doc_length* Math.log(length_lprob) - length_lprob) * (poisson_scale/ model.cond_bgs.size()); //Poisson PMF
	    //System.out.println(lprob+" "+doc_length+" "+length_lprob+" "+(((doc_length* Math.log(length_lprob) - length_lprob))* (poisson_scale/model.cond_bgs.size()))+" "+poisson_scale+" "+doc_length);

            if (lprob>= topset_lprob) {
		//if (node<0) node= -(node+1);
		if (node<0 && !no_backoffs) node= -(node+1);
                double[] entry2 = {lprob, node};
                retrieve_sort.add(DoubleBuffer.wrap(entry2));
		//if (lprob == topset_lprob) {System.out.println(lprob+" "+max_lprob+" "+node+" "+retrieve_sort.first().array()[1]);}
		if (lprob >= max_lprob) {
		    //System.out.println(lprob+" "+max_lprob+" "+node+" "+retrieve_sort.first().array()[1]); 
		    max_lprob= lprob;
		}
		if (retrieve_sort.size()> top_k) retrieve_sort.pollFirst();
		//if (retrieve_sort.size()== top_k && top_k>1) topset_lprob= Math.max(max_lprob+label_threshold, retrieve_sort.first().array()[0]);
		//if (retrieve_sort.size()== top_k) topset_lprob= Math.max(max_lprob+label_threshold, retrieve_sort.first().array()[0]);
		if (retrieve_sort.size()== top_k) topset_lprob= retrieve_sort.first().array()[0];
            }
        }
	//System.out.println(topset_lprob+" "+max_lprob);

	//System.out.println(w+" "+retrieve_sort.size()+" "+max_lprob+" "+lprobs.size()+" "+terms.length);
        if (constrain_labels<2 && retrieve_sort.size()==0 && label_posteriors.size()==0 && !full_posteriors) {
            SparseVector results= new SparseVector(2);
	    //System.out.println(prior_max_label);
            results.indices[0]= prior_max_label;
            results.values[0]= (float)sum_bg_lprob;
            if (model.prior_lprobs!=null) results.values[0]+= model.prior_lprobs.get(prior_max_label)*prior_scale;
            results.indices[1]= -1;
            return results;
        }
			
	//System.out.println(node_sort.length+" "+label_posteriors.size()+ " "+retrieve_sort.size());
        Iterator<DoubleBuffer> f= retrieve_sort.descendingIterator();
        int labelsize= retrieve_sort.size();
        //System.out.println(labelsize+" "+top_k);
	if (model.node_links!=null) {
	    Integer count2;
            Hashtable<Integer, Integer> component_counts2= new Hashtable<Integer, Integer>();
            for (n= 0; n< labelsize; n++) {
		double[] entry2= f.next().array();
		int node= (int)entry2[1];
		node_list= model.node_links.get(node);
		if (model.prior_lprobs!=null) entry2[0]-= model.prior_lprobs.get(-(node_list.get(0)+1))*prior_scale;
		for (int bo_node:node_list.array()) {
		    bo_node= -(bo_node+1);
		    count2= component_counts2.get(bo_node);
		    Double lprob4= entry2[0] * Math.abs(combination);
		    if (count2==null) count2= 1;
		    else {
			count2++;
			lprob4= logsum(label_posteriors.get(bo_node), lprob4);
		    }
		    component_counts2.put(bo_node, count2);
		    label_posteriors.put(bo_node, lprob4);
		}
            }
            retrieve_sort= new TreeSet<DoubleBuffer>();
	    int missing_count;
            for (Map.Entry<Integer, Double> entry : label_posteriors.entrySet()) {
                Integer label= entry.getKey();
		Integer label_node= -(label+1);
                double[] entry2 = {entry.getValue(), (int)label};
		Integer component_count= component_counts.get(label_node);
		Integer component_count2= component_counts2.get(label);
		if (component_count2==null) missing_count= component_count;
		else missing_count= component_count - component_count2;
		//System.out.println(label+" "+label_node+" "+missing_count+" "+component_count+" "+component_count2);
		if (missing_count!=0 && combination!=0) {
		    Double lprob4= lprobs.get((Integer)label_node);
		    if (no_backoffs) lprob4= sum_bg_lprob;
		    //System.out.println(label+" "+bo_mean_lweights.size());
		    if (lprob4==null) lprob4= -1000000.0;
		    else lprob4+= sum_counts* bo_mean_lweights.get(label_node);
		    entry2[0]= logsum(entry2[0], Math.log(missing_count)+ Math.abs(combination)*lprob4);
		}
		//System.out.println(entry2[0]+" "+label+" "+ bo_mean_lweights.get(label_node)+" "+component_counts.get(-(label+1)));
		if (combination>0) entry2[0]-= Math.log(component_count) / Math.abs(combination);
		//if (combination>0) entry2[0]= (entry2[0]- Math.log(component_bolweight))/ combination;
		if (model.prior_lprobs!=null) entry2[0]+= model.prior_lprobs.get(label)*prior_scale;
		retrieve_sort.add(DoubleBuffer.wrap(entry2));
	    }
	    f= retrieve_sort.descendingIterator();
	}

	HashSet<Integer> fill_nodes= new HashSet<Integer>();
	if (full_posteriors && retrieve_sort.size()<max_retrieved) { //tiny approx, if bo_lweights differ
	    if (constrain_labels<2) fill_nodes= new HashSet<Integer>(model.prior_lprobs.keySet());
	    else fill_nodes= new HashSet<Integer>(constrained_labels);
	    for (; f.hasNext();) fill_nodes.remove((int)f.next().array()[1]);
	    for (Integer label: fill_nodes){
		double[] entry2 = {sum_bg_lprob, (int)label};
		Float bo_lweight=  model.bo_lweights.get(label);
		if (bo_lweight!=null) {
		    if (model.node_links!=null) {
			entry2[0]+= sum_counts* bo_lweight * bo_mean_lweights.get(label);
			//Integer bo_node= model.node_links.get(-((int)entry2[1])+1);
			//if (bo_node!=null){Double lprob4= lprobs.get(bo_node);
		    } else entry2[0]+= sum_counts* bo_lweight;
		}
		if (model.prior_lprobs!=null) entry2[0]+= model.prior_lprobs.get(label)*prior_scale;
		retrieve_sort.add(DoubleBuffer.wrap(entry2));
	    }
	    f= retrieve_sort.descendingIterator();
	}

        if ((label_threshold>-1000000 | cutoff_weight>0) && retrieve_sort.size()>0 && w!=-1) {
	    //max_lprob= f.next().array()[0];
	    Integer cutoff= null;
	    if (cutoff_weight>0) cutoff= label_cutoffs.get(w);
	    if (cutoff== null) cutoff= 1;
	    
	    max_lprob= -1000000;
	    int t= 0;
	    f= retrieve_sort.descendingIterator(); 
	    while (f.hasNext()) {max_lprob= logsum(max_lprob, f.next().array()[0]); if (++t>= cutoff) break;}
	    max_lprob-= Math.log(t);
	    
	    f= retrieve_sort.iterator(); 
	    t= retrieve_sort.size();
	    while (f.hasNext()) if (max_lprob+label_threshold > f.next().array()[0] & t-- > cutoff) f.remove(); 
	    	else break;
	    
            f= retrieve_sort.descendingIterator();
        }
	    
        int labelsize2= Math.min(retrieve_sort.size(), max_retrieved);
        SparseVector result= new SparseVector(labelsize2+1);
        for (n= 0; n< labelsize2;) {
            double[] entry2= f.next().array();
            result.indices[n]= (int)entry2[1];
            result.values[n++]= (float)entry2[0];
        }
        float bg_score= (float) sum_bg_lprob;
        int missing= num_classes- labelsize2;
        if (constrain_labels>1) missing= Math.min(missing, constrained_labels.size()-labelsize2);
	//System.out.println(retrieve_sort.size()+" "+max_retrieved+" "+constrained_labels.size()+" "+missing);
        result.indices[labelsize2]= -1;
        result.values[labelsize2]= bg_score;

        if (norm_posteriors) {
	    /*float q_entropy= 0;
	      double q_norm= 0;
	    for (float count2:counts) q_norm+= count2;
	    q_norm= Math.log(q_norm);
	    for (float count2:counts) {
	    double lprob2= Math.log(count2) - q_norm;
	    //double lprob2= logsum((count2 - q_norm +a1), u1);
		q_entropy-= (float)Math.exp(lprob2)* lprob2;
	    }
	    for (n= 0; n< labelsize2+1;) result.values[n++]+= q_entropy;
	    float sum= -1000000;
	    for (n= 0; n< labelsize2;) sum= (float)logsum(sum, result.values[n++]);
	    if (missing!= 0) sum= (float) logsum(sum, Math.log(missing)+ bg_score+ q_entropy);
            for (n= 0; n< labelsize2+1;) result.values[n++]-= sum;*/
	    
	    float sum= -1000000;
	    for (n= 0; n< labelsize2;) sum= (float)logsum(sum, result.values[n++]);
	    //System.out.println(sum);
	    if (missing!= 0) sum= (float) logsum(sum, Math.log(missing)+ bg_score); //Note: not exact. bg_score depends on missing. Fix if needed
	    for (n= 0; n< labelsize2+1;) result.values[n++]-= sum;
	    //System.out.println(sum+" "+missing+" "+bg_score+" "+Math.log(missing)+" "+ bg_score+" "+num_classes);
	}
        return result;
    }

    public void prepare_inference(double top_k, int max_retrieved, double label_threshold, double combination, boolean full_posteriors, boolean norm_posteriors, int constrain_labels, double feedback, int feedback_k, double prior_scale) {
	this.label_threshold= (float) label_threshold;
	this.max_retrieved= max_retrieved;
	this.full_posteriors= full_posteriors;
	this.norm_posteriors= norm_posteriors;
	this.combination= combination;
	this.constrain_labels= constrain_labels;
	this.feedback= feedback;
	this.feedback_k= feedback_k;
	this.prior_scale= prior_scale;
	/*if (model.prior_lprobs==null) {
	    if (full_posteriors) model.prior_lprobs= new Hashtable<Integer, Float>();
	    for (Enumeration<CountKey> e = model.cond_lprobs.keys(); e.hasMoreElements();) {
		CountKey key= e.nextElement();
		if (model.prior_lprobs!
		if (key.label<0) continue;
		prior_max_label= key.label;
		//if (model.node_links!=null) prior_max_label= -(model.node_links.get(prior_max_label)+1);
		//if (full_posteriors) {
		//if (model.node_links==null || key.label<model.min_encoded_label) {Integer label= key.label; model.prior_lprobs.put(label, (float)0);}}
		//else break;
	    }
	    }*/
	if (model.node_links!=null) {
	    component_counts= new Hashtable<Integer, Integer>();
	    bo_mean_lweights= new Hashtable<Integer, Double>();
	    //for (Map.Entry<Integer, Integer> entry : model.node_links.entrySet()) {
	    for (Map.Entry<Integer, IntBuffer> entry : model.node_links.entrySet()) {
		Integer node= entry.getKey();
		IntBuffer node_list= entry.getValue();
		for (int bo_node:node_list.array()){
		    //Integer bo_node= entry.getValue();
		    //if (bo_node!=null) {
		    Double bo_mean_lweight= bo_mean_lweights.get(bo_node);
		    Float bo_mean_lweight2= model.bo_lweights.get(node);
		    //if (bo_mean_lweight2==null) bo_mean_lweight2= (float)0; 
		    if (bo_mean_lweight2==null) bo_mean_lweight2= (float)-1000000; 
		    if (bo_mean_lweight==null) bo_mean_lweight= (double)(float) bo_mean_lweight2;
		    else bo_mean_lweight= logsum(bo_mean_lweight, (double)(float) bo_mean_lweight2);
		    bo_mean_lweights.put(bo_node, bo_mean_lweight);
		    Integer count= component_counts.get(bo_node);
		    if (count==null) count= 1; else count++;
		    //if (count==null) {count= 1; sum_boweight= bo_weight;}
		    //else {count++; sum_boweight= logsum(sum_boweight, bo_weight);}
		    //component_bolweights.put(node, sum_boweight);
		    component_counts.put(bo_node, count);
		    //System.out.println("a "+node+" "+bo_node);
		}
		//Integer count= component_counts.get(bo_node);
		//if (count==null) count= 1; else count++;
                //if (count==null) {count= 1; sum_boweight= bo_weight;}
		//else {count++; sum_boweight= logsum(sum_boweight, bo_weight);}
		//component_bolweights.put(node, sum_boweight);
                //component_counts.put(bo_node, count);
	    }
	    for (Map.Entry<Integer, Double> entry : bo_mean_lweights.entrySet())
		entry.setValue(entry.getValue() - Math.log(component_counts.get(entry.getKey())));
	}
	inverted_index= new Hashtable<Integer, IntBuffer>();
	inverted_index2= new Hashtable<Integer, FloatBuffer>();
	//inverted_index= new Hashtable<Integer, ArrayList<Integer>>();
	//inverted_index2= new Hashtable<Integer, ArrayList<Float>>();

	HashSet<Integer> tmp_labelset= new HashSet<Integer>();
	Hashtable<Integer, Integer> term_counts= new Hashtable<Integer, Integer>();
	for (Map.Entry<CountKey, Float> entry : model.cond_lprobs.entrySet()) {
	    CountKey p_index= entry.getKey();
	    tmp_labelset.add(p_index.label);
	    Integer term= p_index.term;

	    /*ArrayList<Integer> labels= inverted_index.get(term);
	    if (labels == null) {
		labels= new ArrayList<Integer>(4);
		labels.add(p_index.label);
		inverted_index.put(term, labels);
	    } else labels.add(p_index.label);
	    ArrayList<Float> lprobs= inverted_index2.get(term);
	    if (lprobs == null) {
		lprobs= new ArrayList<Float>(4);
		lprobs.add(entry.getValue());
		inverted_index2.put(term, lprobs);
	    } else lprobs.add(entry.getValue());
	    */
	    
	    //if (lprob==0) continue;
	    Integer count= term_counts.get(term);
	    if (count==null) count= 1;
	    else count+= 1;
	    term_counts.put(term, count);
	}
	for (Map.Entry<Integer, Integer> entry : term_counts.entrySet()) {
	    Integer term= entry.getKey();
	    Integer count= entry.getValue();
	    int[] nodes= new int[count];
	    IntBuffer wrap_nodes= IntBuffer.wrap(nodes);
	    float[] lprobs= new float[count];
	    FloatBuffer wrap_lprobs= FloatBuffer.wrap(lprobs);
	    inverted_index.put(term, wrap_nodes);
	    inverted_index2.put(term, wrap_lprobs);
	    entry.setValue(0);
	}

	Iterator<Entry<CountKey, Float>> entries= model.cond_lprobs.entrySet().iterator();
        while (entries.hasNext()) {
            Entry<CountKey, Float> entry= entries.next();
	    //for (Map.Entry<CountKey, Float> entry : model.cond_lprobs.entrySet()) {
	    float lprob= entry.getValue();
	    CountKey p_index= entry.getKey();
	    Integer term= p_index.term;
	    Integer node= p_index.label;
	    //if (lprob==0) continue;
	    //System.out.println(node +" "+term+" "+lprob);
	    Integer count= term_counts.get(term);
	    ((IntBuffer)inverted_index.get(term)).put(count, node);
	    ((FloatBuffer)inverted_index2.get(term)).put(count, lprob);
	    term_counts.put(term, count+1);
	    entries.remove();
	}
	
	model.cond_lprobs= null;
	
	num_classes= tmp_labelset.size();
	this.top_k= (int)Math.max(1, top_k);
	//System.out.println(this.top_k);
	if (labels2powerset!=null) {
	    powerset2labels= new Hashtable<Integer, IntBuffer>(labels2powerset.size());
	    for (Enumeration<IntBuffer> d= labels2powerset.keys(); d.hasMoreElements();) {
		IntBuffer labels= d.nextElement();
		Integer powerset= labels2powerset.get(labels);
		//System.out.println(powerset+" "+labels.array());
		powerset2labels.put(powerset, labels);
		labels2powerset.remove(labels);
	    }
	}
	if (model.prior_lprobs!=null) {
	    float max_lprob= -1000000;
	    for (Map.Entry<Integer, Float> entry : model.prior_lprobs.entrySet()) if (entry.getValue()> max_lprob) {
		    max_lprob= entry.getValue();
		    prior_max_label= entry.getKey();
		}
	    num_classes= model.prior_lprobs.size();
	}
    }
    
    public SparseVector infer_posterior(int[] terms, float[] counts, int w) throws Exception {
	if (tfidf.use_tfidf!=16 && tfidf.use_tfidf!=17 && tfidf.use_tfidf!=22) tfidf.length_normalize(terms, counts);
	int j= 0;
	float doc_length= 0;
	for (int t= 0; t<terms.length; t++) {
	    Integer term2= terms[t];
	    float count= counts[t];
	    if (!inverted_index.containsKey(term2)) continue;
	    if (tfidf.use_tfidf==1 || tfidf.use_tfidf==3 || tfidf.use_tfidf==8|| tfidf.use_tfidf==13|| tfidf.use_tfidf==15|| tfidf.use_tfidf==17|| tfidf.use_tfidf==18|| tfidf.use_tfidf==19|| tfidf.use_tfidf==20|| tfidf.use_tfidf==21|| tfidf.use_tfidf==22|| tfidf.use_tfidf==23|| tfidf.use_tfidf==24) count*= tfidf.get_idf(term2);
	    if (tfidf.use_tfidf==9 || tfidf.use_tfidf==10) count= ((tfidf.idf_lift+1)*count) / (tfidf.idf_lift+count);

	    terms[j]= term2;
	    doc_length+= counts[j++]= count;
	}
	if (j!=terms.length) {
	    counts= Arrays.copyOf(counts, j);
	    terms= Arrays.copyOf(terms, j);
	}
	if (tfidf.use_tfidf==24) for (int t= 0; t<terms.length; t++) counts[t]/= doc_length;
	//
	int[] label_priors= null; float[] prior_scores= null;
	SparseVector inference_results= null;
	/*if (priorsf!=null) {
	    l= priorsf.readLine();
	    s= l.split(" ");
	    label_priors= new int[s.length+1];
	    prior_scores= new float[label_priors.length];
	    int i= 0, l2= 0;
	    boolean weights= false;
	    HashSet<Integer> tmp_labels= new HashSet<Integer>();
	    for (String label: s) {
		String[] splits= label.split(":");
		if (splits.length==2) {
		    weights= true;
		    prior_scores[i]= (float)dlog((float)Float.parseFloat(splits[1]));
		}
		l2= Integer.decode(splits[0]);
		if (!tmp_labels.contains((Integer)l2)){
		    tmp_labels.add(l2);
		    label_priors[i++]= l2;
		}
	    }
	    if (i!= s.length) label_priors= Arrays.copyOf(label_priors, i+1);
	    label_priors[label_priors.length-1]= -1; 
	    inference_results= new SparseVector(label_priors);
	    if (!weights) for (i= 0; i< label_priors.length;) prior_scores[i]= (float) (Math.log(1.0/ ++i) * 1.0);
	    inference_results.values= prior_scores;
	    } else {*/
	if (feedback!=0) inference_results= inference(terms, counts, doc_length, feedback_k, feedback_k, -1);
	else inference_results= inference(terms, counts, doc_length, top_k, max_retrieved, w);
	//}
	if (feedback!=0) {
	    int[] feedback_labels= Arrays.copyOf(inference_results.indices, inference_results.indices.length);
	    float[] feedback_counts= new float[terms.length];
	    Hashtable<Integer, Float> feedback_t= new Hashtable<Integer, Float>();
	    
	    double sum_fb_lweights= -1000000, sum_bo_lweights= -1000000;
	    for (int t= 0; t<feedback_labels.length-1; t++) {
		//for (int label: feedback_labels) {
		Integer label= feedback_labels[t];
		Float fb_lweight= (float)inference_results.values[t];
		if (feedback<0) fb_lweight= (float)Math.log((1.0/(t+1)));
		feedback_t.put(label, fb_lweight);
		sum_fb_lweights= logsum(sum_fb_lweights, fb_lweight);
		Float bo_lweight= model.bo_lweights.get(label);
		if (bo_lweight!= null) sum_bo_lweights= logsum(sum_bo_lweights, bo_lweight+fb_lweight);
	    }
	    Iterator<Integer> e;
	    Iterator<Float> g;
	    for (int t= 0; t<terms.length; t++) {
		Integer term2= terms[t];
		//float count= (float) (model.cond_bgs.get(term2)+ sum_bo_lweights);
		float count= (float) model.cond_bgs.get(term2);
		if (count==0) count= -1000000; //hack fix for bm25/vsm
		int[] nodes= inverted_index.get(term2).array();
		float[] cond_lprobs= inverted_index2.get(term2).array();
		for (int i= 0; i< nodes.length; i++) {
		    Integer node= nodes[i];
		    double lprob= cond_lprobs[i];
		    
		    //g= inverted_index2.get(term2).listIterator();
		    //for (e= inverted_index.get(term2).listIterator(); e.hasNext();) {
		    //Integer node= e.next();
		    //	double lprob= (float) g.next();
		    Float bo_lweight= model.bo_lweights.get(node);
		    if (!feedback_t.containsKey(node)) continue;
		    float posterior= (float)(feedback_t.get(node)- sum_fb_lweights);
		    if (bo_lweight== null) bo_lweight= (float)0; //hack fix for bm25/vsm
		    else count= (float)logsubtract(count, model.cond_bgs.get(term2)+ model.bo_lweights.get(node) + posterior);
		    //count= (float)logsum(count, lprob+model.cond_bgs.get(term2)+ model.bo_lweights.get(node) + posterior);
		    count= (float)logsum(count, lprob+model.cond_bgs.get(term2)+ bo_lweight + posterior);
		}
		//System.out.println(t+" "+count+" "+(float)Math.exp(count));
		feedback_counts[t]= (float)Math.exp(count);
	    }
	    
	    double sum= 0, sum2= 0;
	    for (float count:counts) sum+= count;
	    for (float count:feedback_counts) sum2+= count;
	    if (sum2==0) sum2= 0; else sum2= sum*(Math.abs(feedback)/sum2);
	    //if (Math.abs(feedback)!=1) sum=(1.0-Math.abs(feedback));
	    sum*=(1.0-Math.abs(feedback));
	    
	    //System.out.println(sum+" "+sum2+" "+feedback_labels.length);
	    for (int t= 0; t< counts.length; t++) feedback_counts[t]= (float)(feedback_counts[t]*sum2 + counts[t] * sum);
	    inference_results= inference(terms, feedback_counts, doc_length, top_k, max_retrieved, w);
	}	    
	if (powerset2labels!=null) {
	    if (inference_results.indices.length==2) {
		int[] labels2= powerset2labels.get((Integer)inference_results.indices[0]).array();
		inference_results= new SparseVector(Arrays.copyOf(labels2, labels2.length+1));
		inference_results.indices[labels2.length]= -1;
	    } else {
		Hashtable<Integer, Double> labelscores= new Hashtable<Integer, Double>();
		for (int i= 0; i<inference_results.indices.length-1; i++) {
		    int[] labels2= powerset2labels.get((Integer)inference_results.indices[i]).array();
		    double score= inference_results.values[i];
		    for (int label: labels2) {
			Double score2= labelscores.get(label); 
			if (score2!=null) score2= logsum(score2, score);
			else score2= score;
			labelscores.put(label, score2);
			//System.out.println(label);
		    }
		}
		TreeSet<DoubleBuffer> treesort= new TreeSet<DoubleBuffer>();
		for (Map.Entry<Integer, Double> entry : labelscores.entrySet()) {
		    double[] entry2= {entry.getValue(), entry.getKey()};
		    treesort.add(DoubleBuffer.wrap(entry2));
		}
		//System.out.println(inference_results.indices.length+" "+treesort.size());
		inference_results= new SparseVector(treesort.size()+1);
		inference_results.indices[treesort.size()]= -1;
		int t= 0;
		for (Iterator<DoubleBuffer> g= treesort.descendingIterator(); g.hasNext();) {
                    double[] entry2= g.next().array();
		    inference_results.indices[t]= (int)entry2[1];
		    inference_results.values[t++]= (float)entry2[0];
		}
	    }
	}
	return inference_results;
    }

    class ThreadInfers extends Thread {
	private ConcurrentLinkedQueue<ThreadInfers> activeThreads;
	private Hashtable<Integer, SparseVector> results_table;
	private int w;
	public ThreadInfers(ConcurrentLinkedQueue<ThreadInfers> activeThreads, int w, Hashtable<Integer, SparseVector> results_table) {
	    this.activeThreads= activeThreads;
	    this.results_table= results_table;
	    //synchronized(lock) {
	    this.w= w;
	    // }
	}
	public void start() {
	    activeThreads.add(this);
	    super.start();
	}
	public void run() {
	    try {
		int[] labels= data.labels[w]; 
 		float[] label_weights= null;
		if (data.label_weights!=null) label_weights= data.label_weights[w];
		if (constrain_labels>0) {
		    constrained_labels= new HashSet<Integer>(labels.length);
		    int label_count= 0;
		    HashSet<Integer> tmp_labels= new HashSet<Integer>(); //hack fix
		    for (int i= 0; i < labels.length; i++) {
			if (!tmp_labels.contains((Integer)labels[i])){
			    tmp_labels.add(labels[i]);
			    constrained_labels.add((Integer)labels[i]);
			    if (label_weights[i]>0) label_count++;
			} else label_weights[i]= 0;
		    }
		    if (constrain_labels%2== 1) {
			int[] labels2= new int[label_count];
			float[] label_weights2= new float[label_count];
			label_count= 0;
			for (int i= 0; i < labels.length; i++) if (label_weights[i]>0) {
				labels2[label_count]= labels[i]; label_weights2[label_count++]= label_weights[i];}
			labels= labels2;
			label_weights= label_weights2;
		    }
		}
		data.labels[w]= labels; if (label_weights!=null)data.label_weights[w]= label_weights;
		if (labels.length!=0) {
		    int[] terms= data.terms[w];
		    float[] counts= data.counts[w];
		    SparseVector results= infer_posterior(terms, counts, w);
		    //instantiate_labels(results, w);
		    results_table.put(w,results);
		}
	    }
	    catch(Exception e) {e.printStackTrace();}
	    activeThreads.remove(this);
	}
    }
    
    public void load_cutoffs(String cutoff_file, double cutoff_weight) throws Exception {
	if (cutoff_weight==0) return;
	System.out.println("Loading cutoffs, weight:"+" "+cutoff_weight);
	this.cutoff_weight= cutoff_weight;
	label_cutoffs= new Hashtable<Integer, Integer>();
	BufferedReader input= new BufferedReader(new FileReader(cutoff_file));
	String l;
	String[] s;
	double adjust_lprob= Math.log(model.train_count);
	while ((l = input.readLine())!= null) {
	    s= l.trim().split(" ");
	    Integer label= new Integer(s[0]);
            Float lprob = new Float(s[1]);
	    label_cutoffs.put(label, (int)Math.max(1, Math.floor(Math.exp(lprob+adjust_lprob)*cutoff_weight)));
	    //System.out.println(label+" "+Math.exp(lprob+adjust_lprob)+" "+model.train_count+" "+label_cutoffs.get(label));
	}
	input.close();
    }
    
    public void set_instantiate(double instantiate_weight, double instantiate_threshold) throws Exception {
	this.instantiate_weight= instantiate_weight;
	if (instantiate_weight==0) return;
	this.instantiate_threshold= instantiate_threshold;
	int doc_count= data.doc_count; //results_table.size();
	//int doc_count= 452167;
	int label_count= model.prior_lprobs.size();
	double adjust_lprob= 1000000;
	for (Map.Entry<Integer, Float> entry : model.prior_lprobs.entrySet()) if (entry.getValue()< adjust_lprob) adjust_lprob= entry.getValue();
	adjust_lprob= adjust_lprob+Math.log(model.train_count)-Math.log(doc_count);
	label_cutoffs= new Hashtable<Integer, Integer>(model.prior_lprobs.size());
	label_instances= new Hashtable<Integer, TreeSet<DoubleBuffer>>();
	Hashtable<Integer, Float> label_priors= model.prior_lprobs;
	if (powerset2labels!=null) {
	    label_priors= new Hashtable<Integer, Float>();
	    for (Map.Entry<Integer, Float> entry : model.prior_lprobs.entrySet()) {
		Integer label= entry.getKey();
		int[] labelset= powerset2labels.get(label).array();
		float lprob= entry.getValue(); 
		for (int i= 0; i<labelset.length; i++) {
		    Integer label2= labelset[i];
		    Float lprob2= label_priors.get(label2);
		    if (lprob2!=null) lprob2= (float)logsum(lprob2, lprob);
		    else lprob2= lprob;
		    label_priors.put(label2, lprob2);
		}
	    }
	}
	for (Map.Entry<Integer, Float> entry : label_priors.entrySet()) {
	    int label= entry.getKey();
	    //label_cutoffs.put(label, (int)Math.max(1, Math.floor(Math.exp(entry.getValue()-adjust_lprob)*instantiate_weight)));
	    label_cutoffs.put(label, (int)Math.max(1, Math.floor(Math.exp(entry.getValue()-adjust_lprob)*Math.abs(instantiate_weight))));
	    //System.out.println(label+" "+label_cutoffs.get(label));
	    TreeSet<DoubleBuffer> treesort= new TreeSet<DoubleBuffer>();
	    label_instances.put(label, treesort);
	}
    }

    public void instantiate_labels(SparseVector inference_results, int instance) throws Exception {
	if (instantiate_weight==0) return; //0.667132901941265
	int i= 0;
	for (int label: inference_results.indices) {
	    //System.out.println(label+" "+inference_results.indices.length);
	    if (label==-1) continue;
	    double score= Math.log(1.0/++i);
	    if (instantiate_weight<0) score= inference_results.values[i-1];
	    TreeSet<DoubleBuffer> treesort= label_instances.get(label);
	    if (treesort.size()< label_cutoffs.get(label) || score> ((DoubleBuffer)treesort.first()).get(0) || score>((DoubleBuffer)treesort.last()).get(0)-instantiate_threshold){
		double[] entry2= {score, instance};
		treesort.add(DoubleBuffer.wrap(entry2));
	    }
	    while (treesort.size()> label_cutoffs.get(label) && ((DoubleBuffer)treesort.first()).get(0) <=((DoubleBuffer)treesort.last()).get(0)-instantiate_threshold) treesort.pollFirst();
	}
	inference_results.indices= null;
	inference_results.values= null;
    }
 
    public void output_instantiations(Hashtable<Integer, SparseVector> results_table) throws Exception {
	if (instantiate_weight==0) return;
	Hashtable<Integer, ArrayList<Integer>> instance_labels= new Hashtable<Integer, ArrayList<Integer>>();
	for (Map.Entry<Integer, SparseVector> entry : results_table.entrySet()) {
	    ArrayList<Integer> labels= new ArrayList<Integer>();
	    instance_labels.put(entry.getKey(), labels);
	}
	for (Map.Entry<Integer, TreeSet<DoubleBuffer>> entry : label_instances.entrySet()) {
	    int label= entry.getKey();
	    TreeSet<DoubleBuffer> treesort= entry.getValue();
	    for (Iterator<DoubleBuffer> g= treesort.descendingIterator(); g.hasNext();) {
		double[] entry2= g.next().array();
		//System.out.println(label+" "+(int)entry2[1]);
		instance_labels.get((int)entry2[1]).add(label);
	    }
	}
	for (Map.Entry<Integer, SparseVector> entry : results_table.entrySet()) {
            SparseVector inference_results= entry.getValue();
            ArrayList<Integer> labels= instance_labels.get(entry.getKey());
            inference_results.indices= new int[labels.size()+1];
            inference_results.indices[labels.size()]= -1;
            int i= 0;
            for (int label: labels) inference_results.indices[i++]= label;
            inference_results.values= null;
	    //entry.setValue(inference_results);
        }
    }
    
    public void infer_posteriors_parallel(PrintWriter resultsf, int threads) throws Exception {
	ConcurrentLinkedQueue<ThreadInfers> activeThreads = new ConcurrentLinkedQueue<ThreadInfers>();
	int w= 0;
	int doc_count2= data.doc_count, doc_count3= 0;
	Hashtable<Integer, SparseVector> results_table= null;
	while (doc_count3!=data.doc_count) {
	    if (instantiate_weight!=0) doc_count2= Math.min(80, data.doc_count-doc_count3);
	    results_table= new Hashtable<Integer, SparseVector> (doc_count2*2); 
	    doc_count3+= doc_count2;
	    while(w<doc_count3 || activeThreads.size() !=0) {
		while(w<doc_count3 && activeThreads.size() <= threads) {new ThreadInfers(activeThreads, w++, results_table).start();}
		try {Thread.sleep(25); 
		} catch(Exception e) {}
	    }
	    if (instantiate_weight!=0) {
		if (w%5000==0) System.out.println("Scored:"+ w);
		for(int i= doc_count3-doc_count2; i<doc_count3; i++) {
		    SparseVector inference_results= results_table.get(i);
		    instantiate_labels(inference_results, i);
		}
		
	    }
	}
	if (instantiate_weight!=0) {
	    results_table= new Hashtable<Integer, SparseVector> (doc_count2*2);
	    for(int i= 0; i<doc_count3; i++) results_table.put(i, new SparseVector());
	    output_instantiations(results_table);
	}
	SparseVector inference_results= null;
	for (w= 0; w < data.doc_count; w++) {
	    //inference_results= f.next();
	    inference_results= results_table.get(w);
	    if (inference_results== null || data.labels[w]==null) continue;
	    float[] label_weights= null; if (data.label_weights!=null) label_weights= data.label_weights[w];
	    if (resultsf == null) {
                update_evaluation_results(inference_results.indices, inference_results.values, data.labels[w], label_weights, 1);
            } else {
                String results = "";
		for (int n = 0; n < inference_results.indices.length-1; n++)
                    //results += inference_results.indices[n]+":"+inference_results.values[n] + " ";
                    results += inference_results.indices[n]+" ";
                resultsf.println(results.trim());
            }
	}
    }
    
    public void infer_posteriors(PrintWriter resultsf) throws Exception {
	int[] labels;
	float[] label_weights= null;
	//String l;
	//String[] s;
	for (int w= 0; w < data.doc_count; w++) {
	    labels= data.labels[w];
	    if (data.label_weights!=null) label_weights= data.label_weights[w];
	    if (constrain_labels>0) {
		constrained_labels= new HashSet<Integer>(labels.length);
		int label_count= 0;
		HashSet<Integer> tmp_labels= new HashSet<Integer>(); //hack fix
		for (int i= 0; i < labels.length; i++) {
		    if (!tmp_labels.contains((Integer)labels[i])){
			tmp_labels.add(labels[i]);
			constrained_labels.add((Integer)labels[i]);
			if (label_weights[i]>0) label_count++;
		    } else label_weights[i]= 0;
		}
		if (constrain_labels%2== 1) {
		    int[] labels2= new int[label_count];
		    float[] label_weights2= new float[label_count];
		    label_count= 0;
		    for (int i= 0; i < labels.length; i++) if (label_weights[i]>0) {
			    labels2[label_count]= labels[i]; label_weights2[label_count++]= label_weights[i];}
		    labels= labels2;
		    label_weights= label_weights2;
		}
	    }
	    //
	    if (labels.length==0) continue;
	    
	    int[] terms= data.terms[w];
	    float[] counts= data.counts[w];
	    SparseVector inference_results= infer_posterior(terms, counts, w);
	    /*if (priorsf!=null) {
	      Hashtable<Integer, Double> scores= new Hashtable<Integer, Double>();
	      int i= 0;
	      scores.put(label_priors[i], (double) prior_scores[i]);
	      for (int n = 0; n < inference_results.indices.length-1; n++) {
	      Integer label= inference_results.indices[n];
	      Double score= scores.get(label);
	      if (score==null) score= (double)inference_results.values[n];
	      else score+= inference_results.values[n];
	      scores.put(label, score);
	      }
	      TreeSet<DoubleBuffer> treesort= new TreeSet<DoubleBuffer>();
	      for (Map.Entry<Integer, Double> entry : scores.entrySet()) {
	      double[] entry2= {entry.getValue(), (double)entry.getKey()};
	      //System.out.println(entry2[0]+" "+entry2[1]);
	      treesort.add(DoubleBuffer.wrap(entry2));
	      }
	      SparseVector result= new SparseVector(treesort.size()+1);
	      Iterator<DoubleBuffer> f= treesort.descendingIterator();
	      for (int n= 0; n< treesort.size();) {
	      double[] entry2= f.next().array();
	      result.indices[n]= (int)entry2[1];
	      result.values[n++]= (float)entry2[0];
	      //System.out.println(result.indices[n-1]+" "+result.values[n-1]+" "+entry2[1]+" "+entry2[0]);
	      }
	      result.indices[treesort.size()]= -1;
	      //inference_results.indices= label_priors;		
	      inference_results= result;
	      //System.out.println(result.indices.length+" "+ label_priors.length+" "+labels.length);
	      }*/
	    if (resultsf == null) {
		update_evaluation_results(inference_results.indices, inference_results.values, labels, label_weights, 1);
	    } else {
		String results = "";
		//System.out.println(inference_results.indices.length);
		for (int n = 0; n < inference_results.indices.length-1; n++)
		    //results += inference_results.indices[n]+":"+inference_results.values[n] + " ";
		    results += inference_results.indices[n]+" ";
		resultsf.println(results.trim());
	    }
	}
    }
    
    public void use_powerset() {labels2powerset= new Hashtable<IntBuffer, Integer>();}
    
    public void kernel_densities() {model.node_links= new Hashtable<Integer, IntBuffer>();}

    public void prepare_evaluation() {
	//prior_max_label= model.cond_lprobs.keys().nextElement().label;
	//if (powerset2labels!=null) prior_max_label= powerset2labels.get(prior_max_label).get(0);
	num_classified= tp= fp= fn= tp0= fp= fn0= 0;// num_labels_classified= 0;
	tps= new Hashtable<Integer, Integer>(); fps= new Hashtable<Integer, Integer>(); fns= new Hashtable<Integer, Integer>();
	rec= prec= fscore= rec0= prec0= fscore0= map= mapK= precK= ndcg= ndcgK= 0;// mapErrors= wAcc= 0;
    }
    
    private void update_evaluation_results(int[] labels, float[] label_weights, int[] ref_labels, float[] ref_label_weights, int print_results) {
	String ref= num_classified + " Ref:"+Arrays.toString(ref_labels), res= num_classified + " Res:"+Arrays.toString(labels);
	//HashSet<Integer> ref_labels2= new HashSet<Integer>(ref_labels.length);
	Hashtable<Integer, Float> ref_labels2= new Hashtable<Integer, Float>(ref_labels.length);
	///for (int label:ref_labels) ref_labels2.add((Integer)label); 
	if (ref_label_weights== null) for (int i= 0; i < ref_labels.length; i++) ref_labels2.put(ref_labels[i], (float)1);
	else for (int i= 0; i < ref_labels.length; i++) ref_labels2.put(ref_labels[i], ref_label_weights[i]); 
	int tp2= 0, fp2= 0;
	double ap2= 0.0, apK2= 0.0, dcg2= 0.0, idcg2= 0.0;
	int labels_length= labels.length-1;
	//int labels_length2= Math.max(labels_length, ref_labels.length);
	num_classified++;
	//System.out.println(labels_length+" "+ref_labels.length+" "+constrained_labels.size());
	//int map_div= Math.min(max_retrieved, ref_labels.length);
	//int map_div= 0;
	//System.out.println("ref_labels.length:"+ref_labels.length);
	float[] i_w= new float[ref_labels.length];
	int m= 0;
	if (ref_label_weights== null) for (; m<ref_labels.length; m++) i_w[m++]= (float)1; 
	else for (float weight:ref_label_weights) i_w[m++]= (float) Math.pow(2, weight)-1;
	Arrays.sort(i_w);
	for(int i=0 ;i<i_w.length/2 ;i++) {float tmp= i_w[i]; i_w[i]= i_w[i_w.length-(i+1)]; i_w[i_w.length-(i+1)]= tmp;}
	m= 0; Float ref_weight;
	for (int label:ref_labels) {Integer fn3= fns.get(label); if (fn3==null) fns.put(label, 1); else fns.put(label, fn3+1);}
	for (int label:labels) {
	    if (label==-1) continue;
	    ref_weight= ref_labels2.get(label);
	    if (ref_weight!=null) {
	    //if (ref_labels2.contains((Integer)label)) {
		tp2++;
		dcg2+= ref_weight/ log2(m+2);
		//System.out.println(m+" dcg "+dcg2+" "+ref_weight+" "+log2(m+1));
		ap2+= ((double)tp2/(tp2 + fp2));
		//System.out.println(m+" "+ap);
		Integer tp3= tps.get(label);
		if (tp3==null) tps.put(label, 1); else tps.put(label, tp3+1);
		fns.put(label, fns.get(label)-1);
	    } else {
		fp2++;
		Integer fp3= fps.get(label);
		if (fp3==null) fps.put(label, 1); else fps.put(label, fp3+1);
	    }
	    if (m< ref_labels.length) idcg2+= i_w[m]/ log2(m+2);
	    m++;
	    //num_labels_classified++;
	}
	
	double idcgK2= idcg2; 
	for (int i= m; i<ref_labels.length; i++) idcg2+= i_w[i]/ log2(i+2);
	//System.out.println(ap+" "+ref_labels.length);
	tp+= tp2; fp+= fp2; fn+= ref_labels.length- tp2;
	if (tp2==0) ap2= 0; else {apK2= ap2/tp2; ap2/= ref_labels.length;}
 	double jaccard= (double)tp2/(labels_length+ref_labels.length-tp2);
	rec= (double) tp / (tp + fn); 
	prec= (double) tp / (tp + fp); 
	fscore= (2.0 * rec * prec) / (rec + prec);
	double prec2= (double) tp2 / (tp2 + fp2);
	meanjaccard+= (jaccard-meanjaccard)/num_classified;
	map+= (ap2-map)/num_classified;
	mapK+= (apK2-mapK)/num_classified;
	precK+= (prec2-precK)/num_classified;
	ndcg+= ((dcg2/idcg2)-ndcg)/num_classified;
	ndcgK+= ((dcg2/idcgK2)-ndcgK)/num_classified;
	if ((rec == 0 && prec == 0) || (tp + fp==0) || (tp + fn==0)) fscore= 0;
	if (debug>=0) System.out.println(res);
	System.out.println(ref + "      TP:" + tp + " FN:" + fn + " FP:" + fp + " miFscore:" + fscore + " MAP:" + map+" NDCG: " +ndcg);//+ " MAP@k:" + mapK+" Prec@k:"+precK+
	//System.out.println(rec+" "+prec);
	//System.out.println(ref + "      TP:" + tp + " FN:" + fn + " FP:" + fp + " meanJaccard:"+meanjaccard+" miFscore:" + fscore + " MAP:" + map + " MAP@k:" + mapK+" Prec@k:"+precK+" prec:"+prec);
    }
    
    public void print_evaluation_summary() {
	//String res= "";
	//System.out.println("Results: meanJaccard:"+meanjaccard+" miFscore:" +fscore+ " MAP:" +map+ " MAP@k:" + mapK+" prec@K:"+precK);//+ " mapErrors:" + mapErrors+" wAcc:"+wAcc);
	//for (Map.Entry<Integer, Integer> entry : fps.entrySet()) {
	//   int label= entry.getKey(); if (!fns.containsKey((Integer)label)) fns.put(label, 0);
	//}

	double rec2, prec2, fscore2, maFscore= 0, maFscore2= 0, maFscore3= 0, prec4, fscore4;
	Integer label, fn2, tp2, fp2, fp4;
	Hashtable<Integer, Integer> fps4= new Hashtable<Integer, Integer>();

	for (Map.Entry<Integer, Integer> entry : fns.entrySet()) {
	    label= entry.getKey();
	    fn2= entry.getValue();
	    tp2= tps.get(label); if (tp2==null) tp2= 0;
	    fp2= fps.get(label); if (fp2==null) fp2= 0;
	    rec2= (double) tp2 / (tp2 + fn2);
	    prec2= (double) tp2 / (tp2 + fp2);
	    fscore2= (2.0 * rec2 * prec2) / (rec2 + prec2);
            if ((rec2 == 0 && prec2 == 0) || (tp2 + fp2==0) || (tp2 + fn2==0)) fscore2= 0;
	    maFscore3+= fscore2;
	    //System.out.println(label_cutoffs.get(label)+" "+label);
	}
	maFscore3/= fns.size();
	
	//	System.out.println(fps.size()+" "+fns.size());
	if (fps.size()!=fns.size()){ //missing labels, use surrogate measure
	    int i= 0;
	    int tp_size= 0;
	    for (int count: tps.values()) tp_size+= count;
	    //for (int count: fns.values()) tp_size+= count;
	    int[] label_list= new int[tp_size];
	    for (Map.Entry<Integer, Integer> entry : tps.entrySet()) {
		int label2= entry.getKey(); 
		int tp= entry.getValue();
		for (int j= 0; j<tp; j++) label_list[i++]= label2;
	    }
	    //for (Map.Entry<Integer, Integer> entry : fns.entrySet()) {
	    //int label2= entry.getKey(); 
	    //int fn= entry.getValue();
	    //for (int j= 0; j<fn; j++) label_list[i++]= label2;
	    //}
	    Hashtable<Integer, Integer> fps2= new Hashtable<Integer, Integer>();
	    fps2.putAll(fps);
	    fps4.putAll(fps);
	    
	    Iterator<Integer> entries= fps2.keySet().iterator();
	    int j= 0;
	    while (entries.hasNext()) {
		label= entries.next();
		fn2= fns.get(label);
		if (fn2==null) {
		    fp2= fps2.get(label);
		    while (fp2-->0) {
			label= label_list[j];
			j= (j+1)%i;
			//label= label_list[((10*fp2-- +label)*100)%i];
			Integer fp3= fps.get(label);
			if (fp3==null) fp3= 0;
			fps.put(label, fp3+1);
			if (fp2>0){
			    Integer fp5= fps4.get(label);
			    if (fp5==null) fp5= 0;
			    fps4.put(label, fp5+1);
			}
		    }
		}
	    }
	}
	for (Map.Entry<Integer, Integer> entry : fns.entrySet()) {
	    label= entry.getKey();
	    fn2= entry.getValue();
	    tp2= tps.get(label); if (tp2==null) tp2= 0;
	    fp2= fps.get(label); if (fp2==null) fp2= 0;
	    rec2= (double) tp2 / (tp2 + fn2);
	    prec2= (double) tp2 / (tp2 + fp2);
	    fscore2= (2.0 * rec2 * prec2) / (rec2 + prec2);
	    if ((rec2 == 0 && prec2 == 0) || (tp2 + fp2==0) || (tp2 + fn2==0)) fscore2= 0;
	    maFscore+= fscore2;

	    fp4= fps4.get(label); if (fp4==null) fp4= 0;
	    //fp4= fp2* 0.6884091; // 223481.0/324634.0
	    prec4= (double) tp2 / (tp2 + fp4);
	    fscore4= (2.0 * rec2 * prec4) / (rec2 + prec4);
            if ((rec2 == 0 && prec4 == 0) || (tp2 + fp4==0) || (tp2 + fn2==0)) fscore4= 0;
	    maFscore2+= fscore4;
	    //if (fn2==0) System.out.println(tp2+" "+fp2+" "+fn2+" "+prec2+" "+rec2+" "+fscore2+" "+maFscore);
	}
	/*if (model.prior_lprobs!=null) { //
	    for (Map.Entry<Integer, Float> entry : model.prior_lprobs.entrySet()) {
		label= entry.getKey();
		if (powerset2labels!=null) {
		    for (int label2: powerset2labels.get(label).array()) if (!fns.containsKey((Integer)label2)) fns.put(label2, 0);
		}
		else if (!fns.containsKey((Integer)label)) fns.put(label, 0);
	    }
	    }*/
	maFscore/= fns.size();
	maFscore2/= fns.size();
	//System.out.println(fns.size()+" "+tps.size()+" "+fps.size()+" "+(powerset2labels!=null));
	System.out.println("Results: miFscore:" +fscore+" maFscore:" +maFscore+" maFscore2:" +maFscore2+" maFscore3:" +maFscore3+" meanJaccard:"+meanjaccard+" MAP:" +map+ " MAP@k:" + mapK+" Prec@k:"+precK+" NDCG:" +ndcg+" NDCG@k:" +ndcgK);//+ " mapErrors:" + mapErrors+" wAcc:"+wAcc);
	//System.out.println("Fscore: " +fscore+ "  " +res);
    }
    
    public void open_stream(String data_file, int docs, boolean use_label_weights) throws Exception {
	if (debug>0) System.out.println("SGM opening data stream: " + data_file);
	input_file= new BufferedReader(new FileReader(data_file));
	data= new SparseData(docs, use_label_weights);
	//if (data==null || docs!=data.doc_count) data= new SparseData(-1, docs, -1);
    }

	public void close_stream() throws Exception {
		input_file.close();
	}

	public int get_features(int docs) throws Exception {
		int w= 0;
		w= read_libsvm_stream(docs);
		return w;
	}

	public int read_libsvm_stream(int docs) throws Exception {
		String l;
		String[] splits, s;
		int[] labels, terms;
		float[] counts, label_weights= null;
		int w= 0;
		for (; w < docs; w++) {
		    if ((l = input_file.readLine()) == null) break;
		    int term_c= 0, i= 0;//, length= 0;
		    for (char c: l.toCharArray()) if (c==':') term_c++;
		    splits= l.split(" ");
		    //System.out.println(splits.length+" "+term_c);
		    int label_c= splits.length - term_c;
		    data.labels[w]= labels= new int[label_c];
		    data.terms[w]= terms= new int[term_c];
		    data.counts[w]= counts= new float[term_c];
		    if (data.label_weights!=null) data.label_weights[w]= label_weights= new float[label_c];
		    for (; i < label_c; i++) {
			s= splits[i].split(",")[0].split(";");
			labels[i]= Integer.decode(s[0]);
			if (data.label_weights!=null)
			    if (s.length>1) label_weights[i]= new Float(s[1]);
			    else label_weights[i]= 1;
			//if (s.length>1 && data.label_weights!=null) label_weights[i]= new Float(s[1]);
		    }
		    for (; i < splits.length;) {
			//System.out.println(splits[i]);
			s= splits[i].split(":");
			Integer term= Integer.decode(s[0]);
			terms[i - label_c]= term;
			//counts[i++ - label_c]= (float)Integer.decode(s[1]);
			counts[i++ - label_c]= (float)Float.parseFloat(s[1]);
		    }
		}
		if (w != docs) data.doc_count = w;
		return w;
	}

	public void save_model(String model_name) throws Exception {
		PrintWriter model_file = new PrintWriter(new FileWriter(model_name));
		model_file.println("train_count: " + model.train_count);
		if (model.prior_lprobs!=null) model_file.println("prior_lprobs: " + model.prior_lprobs.size());
		else model_file.println("prior_lprobs: 0");
		if (labels2powerset!=null) model_file.println("labels2powerset: " + labels2powerset.size());
		else model_file.println("labels2powerset: 0");
		model_file.println("tf_idf.normalized: "+ tfidf.normalized);
		model_file.println("idfs: " + tfidf.idfs.size());
		model_file.println("cond_bgs: " + model.cond_bgs.size());
		model_file.println("lprobs: " + model.cond_lprobs.size());
		if (model.node_links!=null) model_file.println("node_links: " + model.node_links.size());
		else model_file.println("node_links: 0");
		model_file.println("model.min_encoded_label: "+ model.min_encoded_label);
		if (model.prior_lprobs!=null)
		    for (Map.Entry<Integer, Float> entry : model.prior_lprobs.entrySet()) model_file.println(entry.getKey()+" "+entry.getValue());
		if (labels2powerset!=null) 
		    for (Enumeration<IntBuffer> d = labels2powerset.keys(); d.hasMoreElements();) {
			IntBuffer in = d.nextElement();
			int[] labelset= in.array();
			String tmp= "";
			for (int i= 0; i < labelset.length; i++) tmp+= labelset[i] + " ";
			model_file.println(tmp + labels2powerset.get(in));
		    }
		for (Enumeration<Integer> d = tfidf.idfs.keys(); d.hasMoreElements();) {
		    Integer in = d.nextElement();
		    model_file.println(in + " " + tfidf.idfs.get(in));
		}
		for (Enumeration<Integer> d = model.cond_bgs.keys(); d.hasMoreElements();) {
		    Integer in = d.nextElement();
		    model_file.println(in + " " + model.cond_bgs.get(in));
		}
		for (Enumeration<CountKey> d = model.cond_lprobs.keys(); d.hasMoreElements();) {
		    CountKey in = d.nextElement();
		    model_file.println(in.label + " " + in.term + " " + model.cond_lprobs.get(in));
		}
		if (model.node_links!=null) for (Enumeration<Integer> d = model.node_links.keys(); d.hasMoreElements();) {
                    Integer in = d.nextElement();
                    model_file.println(in + " " + model.node_links.get(in));
                }
		model_file.close();
	}

	public void load_model(String model_name) throws Exception {
		model= new SGM_Params(10000000);
		BufferedReader input= new BufferedReader(new FileReader(model_name));
		model.train_count= tfidf.train_count= new Integer(input.readLine().split(" ")[1]);
		int prior_lprobs= new Integer(input.readLine().split(" ")[1]);
		int labels2powersets= new Integer(input.readLine().split(" ")[1]);
		tfidf.normalized= new Integer(input.readLine().split(" ")[1]);
		int idfs= new Integer(input.readLine().split(" ")[1]);
		int cond_bgs= new Integer(input.readLine().split(" ")[1]);
		int lprobs= new Integer(input.readLine().split(" ")[1]);
		int node_links= new Integer(input.readLine().split(" ")[1]);
		//if (node_links!=0) model.node_links= new Hashtable<Integer, Integer>(node_links);
		if (node_links!=0) model.node_links= new Hashtable<Integer, IntBuffer>(node_links);
		model.min_encoded_label= new Integer(input.readLine().split(" ")[1]);
		if (prior_lprobs==0) model.prior_lprobs= null;
		while (prior_lprobs > 0) {
			String[] s= input.readLine().split(" ");
			Integer label= new Integer(s[0]);
			Float lprob = new Float(s[1]);
			model.prior_lprobs.put(label, lprob);
			prior_lprobs-= 1;
		}
		while (labels2powersets > 0) {
			String[] s= input.readLine().split(" ");
			int[] labelset = new int[s.length - 1];
			IntBuffer wrap_labelset = IntBuffer.wrap(labelset);
			for (int i = 0; i < labelset.length; i++) labelset[i] = new Integer(s[i]);
			Integer label= new Integer(s[s.length - 1]);
			labels2powerset.put(wrap_labelset, label);
			labels2powersets-= 1;
		}
		while (idfs > 0) {
			String[] s= input.readLine().split(" ");
			Integer label= new Integer(s[0]);
			Float idf= new Float(s[1]);
			tfidf.idfs.put(label, idf);
			idfs-= 1;
		}
		while (cond_bgs > 0) {
			String[] s= input.readLine().split(" ");
			Integer label= new Integer(s[0]);
			Float smooth= new Float(s[1]);
			model.cond_bgs.put(label, smooth);
			cond_bgs-= 1;
		}
		while (lprobs > 0) {
			String[] s= input.readLine().split(" ");
			Float lprob= new Float(s[2]);
			CountKey p_index= new CountKey(new Integer(s[0]), new Integer(s[1]));
			model.cond_lprobs.put(p_index, lprob);
			lprobs-= 1;
		}
		/*while (node_links > 0) {
			String[] s= input.readLine().split(" ");
			Integer node= new Integer(s[0]);
			Integer bo_node= new Integer(s[1]);
			model.node_links.put(node, bo_node);
			node_links-= 1;
			}*/ //Fix!
		input.close();
	}

}
