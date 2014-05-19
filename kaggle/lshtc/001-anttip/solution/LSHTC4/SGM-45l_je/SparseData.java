//package weka.classifiers.bayes.SGM;

public class SparseData{
	public int[][] terms;
	public float[][] counts;
	public int[][] labels;
	public float[][] label_weights;
	int doc_count;

	public SparseData(int doc_count, boolean use_label_weights) {
		this.doc_count= doc_count;
		terms= new int[doc_count][];
		counts= new float[doc_count][];
		labels= new int[doc_count][];
		if (use_label_weights) label_weights= new float[doc_count][];
	}
}

