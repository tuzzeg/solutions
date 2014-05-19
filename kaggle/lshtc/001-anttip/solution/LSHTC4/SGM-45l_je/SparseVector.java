//package weka.classifiers.bayes.SGM;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Enumeration;

public class SparseVector implements Serializable{
	private static final long serialVersionUID = -3376037288335722173L;
	public int[] indices;
	public float[] values;

	public SparseVector(){
		indices= null;
		values= null;
	}

	public SparseVector(int length){
		indices= new int[length];
		values= new float[length];
	}

	public SparseVector(int[] indices2, float[] values2){
		indices= Arrays.copyOf(indices2, indices2.length);
		values= Arrays.copyOf(values2, indices2.length);
		//indices= indices2;
		//values= values2;
	}

	public SparseVector(int[] indices2){
		indices= Arrays.copyOf(indices2, indices2.length);
	}

	public static SparseVector sum_vectors(SparseVector vector1, SparseVector vector2) {
		int length1= vector1.indices.length, length2= vector2.indices.length, t;
		Hashtable<Integer, Float> tmp_table= new Hashtable<Integer, Float>(length1+length2);
		for (t= 0; t<length1; t++) {
			Integer index= new Integer(vector1.indices[t]);
			if (index==-1) break;
			Float value= tmp_table.get(index);
			value= (value==null) ? (float) vector1.values[t] : value+vector1.values[t];
			if (value==0.0) continue;
			tmp_table.put(index, value);
		}
		for (t= 0; t<length2; t++) {
			Integer index= new Integer(vector2.indices[t]);
			if (index==-1) break;
			Float value= tmp_table.get(index);
			value= (value==null) ? (float) vector2.values[t] : value+vector2.values[t];
			if (value==0.0) continue;
			tmp_table.put(index, value);
		}
		SparseVector new_vector= new SparseVector(tmp_table.size());
		t= 0;
		for (Enumeration<Integer> e= tmp_table.keys(); e.hasMoreElements();) {
			Integer g= e.nextElement();
			new_vector.indices[t]= g;
			new_vector.values[t++]= tmp_table.get(g);
		}
		return new_vector;
	}

	public static SparseVector append_vector(SparseVector vector1, SparseVector vector2) {
		int length1= vector1.indices.length, length2= vector2.indices.length, t;
		Hashtable<Integer, Float> tmp_table= new Hashtable<Integer, Float>(length1+length2);
		for (t= 0; t<length1; t++) {
			Integer index= new Integer(vector1.indices[t]);
			if (index==-1) break;
			Float value= tmp_table.get(index);
			value= (value==null) ? (float) vector1.values[t] : value+vector1.values[t];
			tmp_table.put(index, value);
		}
		for (t= 0; t<length2; t++) {
			Integer index= new Integer(vector2.indices[t]);
			if (index==-1) break;
			Float value= tmp_table.get(index);
			if (value!=null) continue;
			value= vector2.values[t];
			tmp_table.put(index, value);
		}
		SparseVector new_vector= new SparseVector(tmp_table.size());
		t= 0;
		for (Enumeration<Integer> e= tmp_table.keys(); e.hasMoreElements();) {
			Integer g= e.nextElement();
			new_vector.indices[t]= g;
			new_vector.values[t++]= tmp_table.get(g);
		}
		if (length2>0) System.out.println("ADD: "+new_vector.indices.length+" "+length1+" "+length2+" "+vector2.indices[0]);
		return new_vector;
	}
}
