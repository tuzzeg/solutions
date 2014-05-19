//package weka.classifiers.bayes.SGM;
import java.io.Serializable;
import java.util.Hashtable;
import java.nio.IntBuffer;


public class SGM_Params implements Serializable{
    private static final long serialVersionUID = -3376037288335722173L;
    int train_count;
    int min_encoded_label;
    public Hashtable<CountKey, Float> cond_lprobs;
    public Hashtable<Integer, Float> cond_bgs;
    public Hashtable<Integer, Float> prior_lprobs;
    public Hashtable<Integer, Float> bo_lweights;
    //public Hashtable<Integer, Integer> node_links;
    public Hashtable<Integer, IntBuffer> node_links;
    public Hashtable<Integer, Float> length_lprobs;
    
    public SGM_Params(int cond_hashsize) {
	train_count= 0;
	cond_lprobs= new Hashtable<CountKey, Float>(cond_hashsize);
	cond_bgs= new Hashtable<Integer, Float>();
	prior_lprobs= new Hashtable<Integer, Float>();
	length_lprobs= new Hashtable<Integer, Float>();
	bo_lweights= null;
	node_links= null;
	min_encoded_label= 0;
    }
}
