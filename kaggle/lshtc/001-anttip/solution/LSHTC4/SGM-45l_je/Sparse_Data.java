public class Sparse_Data{
	public int[][] terms;
	public int[][] counts;
	public int[][] labels;
	public int label_count;
	public int doc_count;
	public int term_count;
	public int[] doc_lengths;
	
	public Sparse_Data(int label_c, int doc_c, int term_c) {
		label_count= label_c;
		doc_count= doc_c;
		term_count= term_c;
		terms= new int[doc_count][];
		counts= new int[doc_count][];
		labels= new int[doc_count][];
		doc_lengths= new int[doc_count];
	}
}

