//package weka.classifiers.bayes.SGM;
//
import java.util.*;
import java.io.*; 
import meka.core.*;
import java.util.Collections;

public class PrunedSetsPreProc extends PreProc {

	private static final long serialVersionUID = -3376837288335722175L;

	private LabelSetComparator cmp = null;

	public PrunedSetsPreProc(String filename, int p) throws Exception {
		this((HashMap<LabelSet,Integer>) MLUtils.loadObject(filename),p);
	}

	public PrunedSetsPreProc(String filename) throws Exception {
		this(filename,1);
	}

	public PrunedSetsPreProc(HashMap<LabelSet,Integer> original_map, int p) throws Exception {
		// do initial prune
		super(original_map,p);
		// do preprocessing
		doProc(original_map);
		// now do the indexing (we need a forwards and backwards mapping)
		doIndexing(outMap);
	}

	public PrunedSetsPreProc(String filename_i, String filename_o) throws Exception {
		super(filename_i, filename_o);
		// we already have done the preprocessing, do the indexing
		doIndexing(this.outMap);
		//
		cmp = new LabelSetComparator(this.outCount);
	}

	public PrunedSetsPreProc(HashMap<LabelSet,Integer> oCount, HashMap<LabelSet,SortedSet<LabelSet>> oMap) throws Exception {
		super(oCount,oMap);
		// we already have done the preprocessing, do the indexing
		doIndexing(outMap);
		//
		cmp = new LabelSetComparator(this.outCount);
	}

	public void doIndexing(HashMap<LabelSet,?> map) {
		System.out.println("Indexing meta labels ...");
		meta_labels = new LabelSet[map.size()];
		indMap = new HashMap<LabelSet,Integer>();
		int i = 0;
		for(LabelSet s : map.keySet()) {
			indMap.put(s,i);  			  // forward map
			meta_labels[i] = s;		      // backward map
			i++;
		}
		System.out.println(" done (total: "+meta_labels.length+")");
	}

	/**
	 * labels2setindex - return the index of the labelset indexed by 'labels'.
	 * @warning -- if preprocessing is done correctly, we should know that we have one.
	 */
	public int labels2setindex(int labels[]) {
		return labels2setindices(labels)[0];
	}

	/**
	 * labels2setindices - return the indices of the labelsets indexed by 'labels'.
	 * @warning -- if preprocessing is done correctly, we should know that we have some.
	 */
	public int[] labels2setindices(int labels[]) {
		LabelSet y = new LabelSet(labels);
		SortedSet<LabelSet> sets = outMap.get(y);

		return set2indices(sets);
	}

	/**
	 * labels2setindices - return the indices of the labelsets indexed by 'labels'.
	 * @warning -- if preprocessing is done correctly, we should know that we have some.
	 */
	public int[] labels2setindices(int labels[], int n) {
	    //System.out.print("| "+Arrays.toString(labels));
		LabelSet y = new LabelSet(labels);
		SortedSet<LabelSet> sets = outMap.get(y);
		if (sets == null) {
			System.out.println("\n[Warning] nothing found for "+y);
			return new int[]{};
		}

		//System.out.println("y'[] = "+Arrays.toString(labels));
		Set<LabelSet> csets = PSUtils.cover(y, sets, this.cmp);
		//System.out.println("-> "+csets);

		return set2indices(csets);

	}

	private int[] set2indices(Set<LabelSet> S) {
		if (S == null) {
			System.out.println("strange");
			return new int[]{};
		}

		int indices[] = new int[S.size()];
		int i = 0;
		for (LabelSet s : S) {
			indices[i++] = this.indMap.get(s);
		}
		return indices;
	}

	/**
	 * labels2set - return the best possible subset of labels[], e.g., [1,2,7,9] to [1,2].
	 * (one which occurs at least m_Pruning times in the training data).
	 * basically: subsets = labels2set(labels,2); return subsets[0]
	 */
	private int[] labels2set(int labels[]) {
		Arrays.sort(labels);
		LabelSet y_set = new LabelSet(labels);
		LabelSet y_subsets[] = PSUtils.getTopNSubsets(y_set, inMap, 2);
		return (y_subsets.length > 0) ? y_subsets[0].indices : new int[]{};
	}

	/**
	 * labels2sets - return the top n frequent sets that cover labels[], e.g., [1,2,7,9] to [[1,2],[2,9]].
	 * top sets = longest and most frequently occurring subsets
	 */
	private int[][] labels2sets(int labels[], int n) {

		Arrays.sort(labels); // necessary?
		LabelSet y_set = new LabelSet(labels);

		SortedSet<LabelSet> Y_subsets = outMap.get(y_set); 

		if (Y_subsets == null) { // it's not there, do it

			if (inMap.get(y_set) != null) {
				// it's already OK, map to itself!
				Y_subsets = new TreeSet<LabelSet>();
				Y_subsets.add(y_set); 
				outMap.put(y_set, Y_subsets);
			}
			else{
				// map to these subsets!
				Y_subsets = (SortedSet<LabelSet>) PSUtils.getTopNSubsetsAsSet(y_set, inMap, n);
				outMap.put(y_set, Y_subsets);
			}
		}

		return cast(Y_subsets);
	}

	/**
	 * labels2sets - return *all* frequent sets that cover labels[], e.g., [1,2,7,9] to [[1,2],[2,9],[7,9],[7],[9]].
	 * sets are ordered by goodness!
	 */
	private int[][] labels2sets(int labels[]) {

		Arrays.sort(labels); // necessary?
		LabelSet y_set = new LabelSet(labels);

		SortedSet<LabelSet> Y_subsets = outMap.get(y_set); 

		if (Y_subsets == null) { // it's not there, do it

			if (inMap.get(y_set) != null) {
				// it's already OK, map to itself!
				Y_subsets = new TreeSet<LabelSet>();
				Y_subsets.add(y_set); 
				outMap.put(y_set, Y_subsets);
			}
			else {
				// map to these subsets!
				Y_subsets = (SortedSet) PSUtils.getSortedSubsets(new LabelSet(labels), inMap);
				outMap.put(y_set, Y_subsets);
			}
		}

		return cast(Y_subsets);
	}

	/**
	 * labels2coversets : 'cover' labels[] as best as possible, e.g., [1,2,7,9] to [[1,2],[7],[9]].
	 * (no guarantee on complete covering)
	 */
	private int[][] labels2coversets(int labels[]) {

		Arrays.sort(labels); // necessary?
		LabelSet y_set = new LabelSet(labels);

		SortedSet<LabelSet> Y_subsets = outMap.get(y_set); 

		if (Y_subsets == null) {   // it's not there, do it

			LabelSet y_subsets[] = PSUtils.cover(y_set, inMap);

			Y_subsets = new TreeSet<LabelSet>(Arrays.asList(y_subsets));
			outMap.put(y_set, Y_subsets);

		}

		return cast(Y_subsets);
	}

	/**
	 * Do Proc - Do preprocessing on a HashMap 'orig'.
	 *
	 * The HashMap 'orig' represents the labelsets of original instances, with the counts of how many times they occur.
	 * Therefore we can do label preprocessing on this map, as if it were the training set (as long as we take into account the count). 
	 *
	 * The following are modified
	 * - outMap (via other functiotns we call)
	 * - outCount
	 *
	 * @param	orig	contains the <LabelSet,Count> pairs of the original (NON-PRUNED) training data.
	 */
	@Override
	public void doProc(HashMap<LabelSet,Integer> orig) {

		outCount = new HashMap<LabelSet,Integer>();

		//int size = orig.size();
		//int w = size / 100;
		//int c = 0;
		//System.out.println("Processing 100 blocks from total "+size+" ");
		for (LabelSet set_in : orig.keySet()) {
			//if ((c++ % w) == 0) System.out.println(""+(c / w)+"\t"+outMap.size());

			// [1,5,9,24],1
			//int cover[][] = labelset2coveredsets(set_in.indices);
			int cover[][] = labels2sets(set_in.indices);
			for(int i = 0; i < cover.length; i++) { 
				// [1,24],7
				LabelSet set_out = new LabelSet(cover[i]);
				if (outCount.containsKey(set_out)) {
					// already exists 
					outCount.put(set_out, outCount.get(set_out) + orig.get(set_out)); 
				}
				else  {
					outCount.put(set_out, orig.get(set_out)); 
				}
				//if (c > 1000) break;
			}
		}
	}

	public static float[] cast(Collection<Float> set) {
		Float[] array = set.toArray(new Float[0]);
		float a[] = new float[array.length];
		for(int i = 0; i < a.length; i++) {
			a[i] = (float)array[i];
		}
		return a;
	}

	public static int[] cast(Set<Integer> set) {
		Integer[] array = set.toArray(new Integer[0]);
		int a[] = new int[array.length];
		for(int i = 0; i < a.length; i++) {
			a[i] = (int)array[i];
		}
		return a;
	}

	private static int[][] cast(Set<LabelSet> set) {
		int cover[][] = new int[set.size()][];
		int i = 0;
		for (LabelSet s : set) {
			cover[i++] = s.indices;
		}
		return cover;
	}

	private static int[] threshold(HashMap<Integer,Float> P_y, double t) {
		Set<Integer> Y = new HashSet<Integer>();
		for(Integer j : P_y.keySet()) {
			float d = P_y.get(j);
			if (d >= t) {
				Y.add(j);
			}
		}
		return cast(Y);
	}


	/**
	 * Recombination - Given the posterior 'p[]' across combinations/labelsets, return the distribution across labels.
	 * @param	meta_labels[]		the list of meta label indices (referring to super classes / combinations)    e.g., [0,1]
	 * @param	meta_posterior[]	the posterior of the super classes (combinations),                            e.g., [0.3,0.7]
	 *                                                                                                            where 0,1 maps to [1,3],[2]
	 * meta_labels.length = meta_posterior.length
	 * @return	the distribution across labels,                                                                   e.g., P(1,2,3) = [0.3,0.7,0.3]
	 */
	public HashMap<Integer,Float> recombination(int meta_labels[], float meta_posterior[]) {

		HashMap<Integer,Float> y_map = new HashMap<Integer,Float>();

		//double y[] = new double[L];
		for(int i = 0; i < meta_labels.length; i++) {
			int j_m = meta_labels[i];                                        // e.g., 7
			//System.out.println(""+j_m);
			if (j_m < 0) {
			//	System.out.println("[Warning] "+j_m);
			}
			else {
				LabelSet y_m = this.meta_labels[j_m];  	                         // e.g., [1,4]
				for(int j : y_m.indices) {                                       //    j = 1,4
			//		System.out.println("_> "+j);
					float p_j = y_map.containsKey(j) ? y_map.get(j) : 0.0F;      //  p_j = p(y_j|x)
					p_j += meta_posterior[i];   
					y_map.put(j, p_j);
					//y[j] += meta_posterior[j_m];          // y[1] += p[j_m] = 0.5
				}
			}
		}
		return y_map;
	}

	/**
	 * Recombination - Given the posterior 'p[]' across combinations/labelsets, and a threshold 't', return the labels above this threshold.
	 * @param	m[]	the meta labels
	 * @param	p[]	the posterior of the super classes (combinations), e.g., P([1,3],[2]) = [0.3,0.7]
	 * @param	t	a threshold, e.g., 0.5
	 * @return	the labels, e.g., [2]
	 */
	public int[] recombination(int meta_labels[], float meta_posterior[], double t) {
		HashMap<Integer,Float> y_map = recombination(meta_labels,meta_posterior);
		return threshold(y_map, t);
		/*
		double p[] = recombination(meta_labels, meta_posterior, L);
		int y[] = new int[L];
		for(int j = 0; j < p.length; j++) {
			y[j] = (p[j] >= t) ? 1 : 0;
		}
		return y;
		*/
	}

	/**
	 * Recombination - Given the posterior 'p[]' across combinations/labelsets, return the distribution across labels.
	 * @param	p[]	the posterior of the super classes (combinations), e.g., P([1,3],[2]) = [0.3,0.7]
	 * @return	the distribution across labels, e.g., P(1,2,3) = [0.3,0.7,0.3]
	public double[] recombination(double p[], int L) {
		return recombination(A.make_sequence(p.length), p, L);
	}
	*/

	/**
	 * Recombination - Given the posterior 'p[]' across combinations/labelsets, and a threshold 't', return the labels above this threshold.
	 * @param	p[]	the posterior of the super classes (combinations), e.g., P([1,3],[2]) = [0.3,0.7]
	 * @param	t	a threshold, e.g., 0.5
	 * @return	the labels, e.g., [2]
	public int[] recombination(double p[], int L, double t) {
		LinkedHashSet<Integer> Y = new LinkedHashSet<Integer>();

		double p_y[] = recombination(p, L);
		for(int j = 0; j < p_y.length; j++) {
			if (p_y[j] >= t)
				Y.add(j);
		}

		return cast(Y);
	}
	*/

	/**
	 * Tests, etc.
	 */
	public static void main(String args[]) throws Exception {

		if (args.length >= 2) {
			// Load
			System.out.println("Loading outMap from file ...");
			HashMap<LabelSet,SortedSet<LabelSet>> aMap = (HashMap<LabelSet,SortedSet<LabelSet>>) MLUtils.loadObject(args[0]);
			System.out.println("Loading outCount from file ...");
			HashMap<LabelSet,Integer> aCount = (HashMap<LabelSet,Integer>) MLUtils.loadObject(args[1]);
			
			if (args.length == 3) {
				// for statistical comparison purposes
				System.out.println("Loading orig from file ...");
				HashMap<LabelSet,Integer> inCount = (HashMap<LabelSet,Integer>) MLUtils.loadObject(args[2]);
				// Printout
				displayStats(inCount,aMap,aCount);
			}
			else {
				// Printout
				PreProc.displayStats(aCount);
			}
			//System.out.println(toString(aMap,aCount));
		}
		else {
			// Orig
			HashMap<LabelSet,Integer> orig = (HashMap<LabelSet,Integer>) MLUtils.loadObject(args[0]);
			PreProc.displayStats(orig);
			// Pruned
			PrunedSetsPreProc proc = new PrunedSetsPreProc(new HashMap<LabelSet,Integer>(orig),1);  
			PreProc.displayStats(proc.inMap);
			// PreProcess
			proc.doProc(orig);
			// Save
			String outMapFile = "outMap_PS_topn.dat";
			String outCountFile = "outCount_PS_topn.dat";
			System.out.println("Saving ...");
			MLUtils.saveObject(proc.outMap,outMapFile);
			MLUtils.saveObject(proc.outCount,outCountFile);
			MLUtils.saveObject(proc.indMap,"indexMap_PS_topn.dat");
			MLUtils.saveObject(proc.meta_labels,"metalabelsarray_PS_topn.dat");
			System.out.println("Next, run: java PrunedSetsPreProc "+outMapFile+" "+outCountFile);
		}
		/*
		else {
			Random r = new Random(0);  
			// Orig
			System.out.println("Load data ...");
			HashMap<LabelSet,Integer> orig = (HashMap<LabelSet,Integer>)MLUtils.loadObject(args[0]);
			PreProc.displayStats(orig);
			System.out.println("Get L ...");
			int L = getL(orig);
			System.out.println("L = "+L);
			System.out.println("Generate Partition ...");
			int partition[][] = SuperLabelUtils.generatePartition(A.make_sequence(L),10,r);
			// PreProc
			int p = 1;
			for(int i = 0; i < partition.length; i++) {
				System.out.println("-----------------------------------");
				System.out.println("<partition["+i+"] of length "+partition[i].length+">");
				System.out.println("-----------------------------------");
				System.out.println("sort indices ...");
				Arrays.sort(partition[i]);
				//System.out.println("partition["+i+"] = "+Arrays.toString(partition[i]));
				System.out.println("make partition ...");
				HashMap<LabelSet,Integer> orig_i = getPartition(orig,partition[i]);
				String inMapFile = "inMap-"+i+"_PS_topn.dat";
				MLUtils.saveObject(orig_i,inMapFile);
				System.out.println("process (pruned by: "+p+" ...");
				PrunedSetsPreProc proc_i = new PrunedSetsPreProc(new HashMap<LabelSet,Integer>(orig_i),p);  
				proc_i.doProc(orig_i);
				// Saving
				System.out.println("Saving ...");
				String outMapFile = "outMap-"+i+"_PS_topn.dat";
				String outCountFile = "outCount-"+i+"_PS_topn.dat";
				MLUtils.saveObject(proc_i.outMap,outMapFile);
				MLUtils.saveObject(proc_i.outCount,outCountFile);
				System.out.println("Next, run: java PrunedSetsPreProc "+outMapFile+" "+outCountFile);
			}
		}
		*/
	}

}
