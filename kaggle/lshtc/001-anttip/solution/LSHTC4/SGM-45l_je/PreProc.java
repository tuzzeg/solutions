//package weka.classifiers.bayes.SGM;
//
import java.util.*;
import java.io.*; 
import meka.core.LabelSet;
import meka.core.MLUtils;
import meka.core.PSUtils;
import meka.core.SuperLabelUtils;
import meka.core.A;
import java.util.Collections;

public class PreProc {

	private static final long serialVersionUID = -3376837288335722173L;

	HashMap<LabelSet,Integer> inMap = null;                  // e.g., [1],3   [2,3],6  [1,2],4   [1,2,3],1
	LabelSet meta_labels[] = null;                           // e.g., <1>     <2>      <3>
	HashMap<LabelSet,SortedSet<LabelSet>> outMap = null;     // e.g., [1]->[1], ...., [1,2,3]->{[1],[2,3]}
	HashMap<LabelSet,Integer> indMap = null;                 // e.g., [3]->1  [2,3]->2 [1,2]->3                      //[1,2,3]->1,2   <-- indices ordered by score
	HashMap<LabelSet,Integer> outCount = null;               // e.g., [1],4   [2,3],7  [1,2],4                  <-- statistical interest only
	private int L = -1;

	int m_Pruning = 5;

	public PreProc(String filename) throws Exception {
		this(filename,0);
	}

	public PreProc(String filename, int p) throws Exception {
		this((HashMap<LabelSet,Integer>) MLUtils.loadObject(filename),p);
	}

	public PreProc(HashMap<LabelSet,Integer> map, int p) throws Exception {
		this.inMap = map;
		this.m_Pruning = p;
		System.out.println("Pruning (p="+m_Pruning+") ...");
		MLUtils.pruneCountHashMap(inMap,m_Pruning); // <-------- PRUNING
		System.out.println("... to "+inMap.size()+" distinct entries");
		outMap = new HashMap<LabelSet,SortedSet<LabelSet>>();
	}

	public PreProc(String filename_c, String filename_o) throws Exception {
		System.out.println("Loading outMap from file ...");
		this.outMap = (HashMap<LabelSet,SortedSet<LabelSet>>) MLUtils.loadObject(filename_o);
		System.out.println("Loading outCount from file ...");
		this.outCount = (HashMap<LabelSet,Integer>) MLUtils.loadObject(filename_c);
	}

	public PreProc(HashMap<LabelSet,Integer> cMap, HashMap<LabelSet,SortedSet<LabelSet>> oMap) throws Exception {
		this.outCount = cMap;
		this.outMap = oMap;
	}

	/**
	 * Make Partition - reduce 'inMap' to only consider indices in 'part'; return the result.
	public HashMap<LabelSet,Integer> makePartition(int part[]) {
		return inMap;
	}
	*/

	/**
	 * Rate Partition - rate the partition 'part' wrt this type of preprocessing.
	public int ratePartition(int part[][]) {
		return 0;
	}
	*/

	/**
	 * Display Stats (about the conversion from 'icount' to 'omap')
	 */
	public static void displayStats(HashMap<LabelSet,Integer> icount, HashMap<LabelSet,SortedSet<LabelSet>> omap, HashMap<LabelSet,Integer> ocount) {

		System.out.println("Original #classes: "+icount.size());
		System.out.println("Now      #classes: "+ocount.size());
		System.out.println("Original #instncs: "+PSUtils.sumCounts(icount));
		System.out.println("Now      #instncs: "+PSUtils.sumCounts(ocount));

		// Print the mapping
		for(LabelSet set : omap.keySet()) {
		//for(Set s : omap.values()) {
			System.out.println(""+set+" : "+icount.get(set));
			for(LabelSet s : omap.get(set)) {
				System.out.println("\t"+s+" : "+ocount.get(s));
			}
		}
	}

	/**
	 * Display Stats (about 'map').
	 */
	public static void displayStats(HashMap<LabelSet,Integer> map) {

		System.out.println("Entries: "+map.size());

		int lim = 15;
		int c[] = new int[lim];
		int l[] = new int[lim];
		int c_max = 0;
		String l_max = "";

		for (LabelSet key : map.keySet()) {
			int c_ = map.get(key);

			// occurs c_ times
			if (c_ < lim) {
				c[c_]++;
					l[c_] += key.indices.length;
			}

			// max occ.
			if (c_ >  c_max) {
				l_max = key.toString();
				c_max = c_;
			}
		}

		for(int i = 0; i < lim; i++) {
			System.out.println(""+i+"\t"+c[i]+"\t"+(double)(l[i]/(double)c[i]));
		}
		System.out.println("c_max = "+c_max);
		System.out.println("l_max = "+l_max);
		// how many = 1
		//          = 2
		// the max occurence
		// mean 
	}

	/**
	 * Do PreProcessing - Load in the labelsets and their counts, do preprocessing into 'outMap' (mappings) and 'outCount' (counts).
	 */
	public void doProc(HashMap<LabelSet,Integer> map) {
	}

	public String toString() {
		return toString(this.outMap,this.inMap);
	}

	/**
	 * ToString - Output to plain-text format String.
	 */
	public static String toString(HashMap<LabelSet,SortedSet<LabelSet>> omap, HashMap<LabelSet,Integer> cmap) {
		StringBuffer sb = new StringBuffer();  
		for (LabelSet key : omap.keySet()) {
			sb.append(key.toString());
			sb.append(" : ");
			for (LabelSet set : omap.get(key)) {
				sb.append(set);
				sb.append(cmap.get(set));
				sb.append(" ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	/*
	public int score(LabelSet y) {
		return inMap.get(y);
	}

	public int score(int labels[]) {
		return inMap.get(new LabelSet(labels));
	}
	*/

	/**
	 * Get L - get the number of labels, based on those stored in 'map'.
	 */
	public static int getL(HashMap<LabelSet,Integer> map) {
		int top = 0;
		for (LabelSet set : map.keySet()) {
			for (int l : set.indices) {
				if (l > top)
					top = l;
			}
		}
		return top;
	}

	/*
	public static int setL(String filename) {
		HashMap<LabelSet,Integer> orig = (HashMap<LabelSet,Integer>) MLUtils.loadObject(filename);
		this.L = getL(orig);
	}
	*/

	/**
	 * Get Partition - partition 'map', to have only the labels indexed in 'partition'.
	 */
	public static HashMap<LabelSet,Integer> getPartition(HashMap<LabelSet,Integer> map, int partition[]) {
		HashMap<LabelSet,Integer> part = new HashMap<LabelSet,Integer>(); 
		for (LabelSet set : map.keySet()) {
			ArrayList<Integer> set_new = new ArrayList<Integer>(); 
			for (int l : set.indices) {
				if (Arrays.binarySearch(partition, l) > 0) { 
					set_new.add(l);
				}
			}
			part.put(new LabelSet(set_new),map.get(set));
		}
		return part;
	}

	public static void main(String args[]) throws Exception {


		/*
		 * Phase 1 -- original data in a map, labelsets with counts.
		 */
		HashMap<LabelSet,Integer> orig = new HashMap<LabelSet,Integer>();
		orig.put(new LabelSet(new int[]{2,3}),4);
		orig.put(new LabelSet(new int[]{1}),3);
		orig.put(new LabelSet(new int[]{1,2}),2);
		orig.put(new LabelSet(new int[]{1,2,3}),1);
		//System.out.println(""+orig);

		/*
		 * Phase 2 -- we can do everything off the bat
		 */
		PrunedSetsPreProc proc = new PrunedSetsPreProc(new HashMap<LabelSet,Integer>(orig),1);  
		proc.doProc(orig);
		displayStats(orig,proc.outMap,proc.outCount);

		MLUtils.saveObject(proc.outMap,"test_omap.dat");
		MLUtils.saveObject(proc.outCount,"test_ocount.dat");

		System.out.println("==========================");

		/*
		 * Phase 3 -- or we can load it up
		 */
		PrunedSetsPreProc proc2 = new PrunedSetsPreProc("test_ocount.dat","test_omap.dat");
		System.out.println(""+proc2.outMap);
		//displayStats(orig,proc2.outMap,proc2.outCount);

		for(LabelSet y : orig.keySet()) {
			System.out.println(""+y+ " -> ");
			for(LabelSet s : proc2.outMap.get(y)) {
				System.out.println("\t"+s+", index "+proc2.indMap.get(s));
			}
			System.out.println("i.e., "+Arrays.toString(proc2.labels2setindices(y.indices, 0)));

		}
		System.exit(1);

		/*
		HashMap<LabelSet,Integer> orig = (HashMap<LabelSet,Integer>) MLUtils.loadObject(args[0]);
		HashMap<LabelSet,Set<LabelSet>> aMap = (HashMap<LabelSet,Set<LabelSet>>) MLUtils.loadObject(args[1]);
		HashMap<LabelSet,Integer> aCount = (HashMap<LabelSet,Integer>) MLUtils.loadObject(args[2]);

		displayStats(orig,aMap,aCount);
		*/
	}

}
