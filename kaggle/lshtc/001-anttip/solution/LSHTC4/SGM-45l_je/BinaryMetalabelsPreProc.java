//package weka.classifiers.bayes.SGM;
//
import java.util.*; 
import java.io.*; 
import meka.core.LabelSet;
import meka.core.MLUtils;
import meka.core.A;
import meka.core.PSUtils;
import java.util.Collections;

public class BinaryMetalabelsPreProc extends PreProc {

	private static final long serialVersionUID = -3376837288335722174L;

	public BinaryMetalabelsPreProc(String filename) throws Exception {
		this(filename,0);
	}

	public BinaryMetalabelsPreProc(String filename, int p) throws Exception {
		super(filename,p);
		System.out.println("Making Meta Labels ...");
		meta_labels = make_meta_labels(inMap,p);
		System.out.println("... made "+meta_labels.length+" meta labels:");
		System.out.println(""+Arrays.toString(meta_labels));
	}

	public BinaryMetalabelsPreProc(HashMap<LabelSet,Integer> map, int p) throws Exception {
		super(map,p);
		System.out.println("Making Meta Labels ...");
		meta_labels = make_meta_labels(inMap,p);
		System.out.println("... made "+meta_labels.length+" meta labels:");
		System.out.println(""+Arrays.toString(meta_labels));
	}



	/** 
	 * labels2powerset : returns the indice(s) of the labelsets corresponding to the labels of an example.
	 * e.g., [A,B,C] to [AC,B]
	 * e.g., [1,2,3] to [2,0]
	 * */
	public int[] labels2labelsets(int labels[]) {
		Arrays.sort(labels);             // sort them
		LabelSet y_set = new LabelSet(labels);

		HashSet<Integer> y_meta = new HashSet<Integer>();
		for(int j = 0; j < meta_labels.length; j++) {
			if (meta_labels[j].subsetof(y_set) > 0) {
				y_meta.add(j);
			}
		}
		Integer[] array = y_meta.toArray(new Integer[0]);
		return A.toPrimitive(array);
	}

	/** 
	 * powerset2labels : returns the labels corresponding to an example, given it's labelsets.
	 * e.g., [AC,B] to [A,B,C]
	 * */
	public int[] labelsets2labels(int labelsets[]) {
		HashSet<Integer> Y = new HashSet<Integer>();
		for (int j : labelsets) {
			for(int k = 0; k < meta_labels[j].indices.length; k++) {
				Y.add(meta_labels[j].indices[k]);
			}
		}
		int labels[] = A.toPrimitive(Y.toArray(new Integer[0]));
		Arrays.sort(labels);
		return labels;
	}

	int stats[] = new int[6];


		/*
		inMap = new HashMap<LabelSet,Integer>();

		for (int i = 0; i < data.doc_count; i++) {
			Arrays.sort(data.labels[i]); // careful!
			LabelSet y = new LabelSet(data.labels[i]);
			Integer c = inMap.get(y);
			inMap.put(y,c == null ? 1 : c+1);
			System.out.println(""+y);
		}

		System.out.println("J1>"+inMap.size());
		System.out.println(""+inMap);
		//$.set(inMap);
		// prune
		MLUtils.pruneCountHashMap(inMap,1);
		System.out.println("J2>"+inMap.size());
		*/


	/**
	 * Make Meta Labels - add any pair as a meta label where the combination is greater than some p.
	 */
	public LabelSet[] make_meta_labels(HashMap<LabelSet,Integer> imap, int p) {

		SortedSet<LabelSet> meta_set = new TreeSet<LabelSet>();

		for(LabelSet s : imap.keySet()) {
			int c = imap.get(s);
			SortedSet<LabelSet> meta_set_s = new TreeSet<LabelSet>();
			for(int j = 0; j < s.indices.length; j++) {
				for(int k = j+1; k < s.indices.length; k++) {
					LabelSet meta_label = new LabelSet(new int[]{s.indices[j], s.indices[k]});
					// @TODO, take into account the count 'c'
					if (PSUtils.countSubsets(meta_label,imap.keySet()) > p) {
						// add meta label
						meta_set.add(meta_label);
						// index meta label:    [3,4,8] -> [3,4], [4,8]
						meta_set_s.add(meta_label);
					}
				}
			}
			outMap.put(s,meta_set_s);
		}
		 
		return meta_set.toArray(new LabelSet[0]);
	}

	@Override
	public void doProc(HashMap<LabelSet,Integer> orig) {

		outCount = new HashMap<LabelSet,Integer>();

		for (LabelSet set_in : orig.keySet()) {
		}
	}

	public static void main(String args[]) throws Exception {
		// Pruned
		BinaryMetalabelsPreProc proc = new BinaryMetalabelsPreProc(args[0],1);  
		System.out.println(""+Arrays.toString(proc.meta_labels));
		MLUtils.saveObject(proc.meta_labels,"outMap_ML_p5.dat");
	}
}
