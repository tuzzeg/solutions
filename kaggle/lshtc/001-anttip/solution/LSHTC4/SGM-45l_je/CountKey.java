//package weka.classifiers.bayes.SGM;
import java.io.Serializable;

public class CountKey implements Serializable{
	private static final long serialVersionUID = -3376037288335722173L;
	public int label;
	public int term;

	public CountKey(){
		label= -1;
		term= -1;
	}

	public CountKey(int label, int term){
		this.label= label;
		this.term= term;
	}

	public boolean equals(Object o) {
		if (((CountKey)o).term!=term) return false;
		if (((CountKey)o).label!=label) return false;
		return true;
	}
	public int hashCode() {
		//Cantor pairing function
		return ((label + term)*(label+term+1))/2+term;
		//return label*991 + term*997;
	}
}
