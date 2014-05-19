import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.sql.Date;
import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.Instance;
import weka.filters.unsupervised.attribute.Normalize;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.nio.DoubleBuffer;

public class MetaComb2 {
    static int baseClassifierCount;
    static double[] baseTotalScores;
    static Hashtable<Integer, Double> labelPriors;
    static Double priorSmooth;
    static Hashtable<Integer, Integer> label_cutoffs;
    static Hashtable<Integer, Integer> labelDevMap;
    static Hashtable<Integer, Integer> instLabelFreqs;
    static Hashtable<Integer, Integer> instLabelCounts;
    static Hashtable<Integer, Double> maxClassifierScores;
    static int trainSize;
    static int testSize;
 
    public static void main(String[] args) throws Exception {
	int numFolds= 5;
    	int developmentRun= 0;			//1 for n-fold development on comb_dev.txt, 0 for evaluation set run on test.txt

	//Global variables
	testSize= (int)(23654.0/numFolds);
	if (developmentRun==0) {
	    testSize= 452167;
	    numFolds= 1;
	}
    	String referenceFile= "/research/antti/multi_label/MetaComb/comb_dev_reference3.txt";
	String transposedRefFile= "/research/antti/multi_label/MetaComb/comb_dev_reference2.txt";
    	String combDevDir= "/research/antti/multi_label/MetaComb/comb_dev_results3/";
	String labelsetCountsFile= "/research/antti/multi_label/MetaComb/labelset_count_table.txt";
    	String evalDir= "/research/antti/multi_label/MetaComb/eval_results2/";
     	String evalResultsFile= "/research/antti/multi_label/MetaComb/eval_results2.txt";
	//
    	String[] result_files = new File(combDevDir).list();
	Arrays.sort(result_files);
    	baseClassifierCount= result_files.length;
	HashSet<Integer> removeClassifiers= new HashSet<Integer>();	
	for(int i= 0; i< args.length; i++) {removeClassifiers.add(Integer.parseInt(args[i])); baseClassifierCount--;}
    	String[] result_filesb= new String[baseClassifierCount];
	int i2= 0;
    	for(int i=0; i<result_files.length; i++) if (!removeClassifiers.contains((Integer)i)) result_filesb[i2++]= combDevDir+result_files[i];
	for(int i=0; i<result_files.length; i++) System.out.println(i+" "+result_files[i]);
	long start_time= System.currentTimeMillis();

	readLabelsetCounts(labelsetCountsFile, transposedRefFile);
    	int[][] refInstanceSets= readRefInstancesets(referenceFile);
	int[][][] resInstanceSets= readResInstancesets(result_filesb);
	double[][] refWeights= approximateOracleWeights(resInstanceSets, refInstanceSets);
	getInstLabelCounts(resInstanceSets);
	Instances data= constructData(resInstanceSets, resInstanceSets.length, true);

	int[][][] resInstanceSets2= new int[1][][];
	int[][] refInstanceSets2= new int[1][];
	Instances data2= null;
	if (developmentRun== 0) {
	    String[] result_files2 = new File(evalDir).list();
	    Arrays.sort(result_files2);
	    String[] result_files2b= new String[baseClassifierCount];
	    i2= 0;
	    //System.out.println(removeClassifiers.size());
	    //System.out.println(result_files2.length);
	    for(int i=0; i<result_files2.length; i++) if (!removeClassifiers.contains((Integer)i)) result_files2b[i2++]= evalDir+result_files2[i];
	    resInstanceSets2= readResInstancesets(result_files2b);
	    refInstanceSets2= new int[0][];
	    //System.out.println(resInstanceSets2.length);
	    getInstLabelCounts(resInstanceSets2);
	    data2= constructData(resInstanceSets2, resInstanceSets2.length, false);
	}
	
	float sizeChunk= (float)refInstanceSets.length/numFolds;
	double score= 0.0;
	for (int k= 0; k<numFolds; k++) {
	    //Build Models
	    Classifier[] learners= new Classifier[baseClassifierCount];
	    System.out.println("Training models. Time:"+(System.currentTimeMillis()-start_time)/1000);
	    for (int i= 0; i<baseClassifierCount; i++) {
		Instances dataBuildClassifier= addLocalFeatures(pruneGlobalFeatures(data, i, 1), resInstanceSets, i); //0.5307094159798093 //0.33033
		//System.out.println("Removing evaluation instances. Time:"+(System.currentTimeMillis()-start_time)/1000);
		for (int ii= 0; ii<refInstanceSets.length; ii++) {
		    if (ii>=(int)Math.floor(sizeChunk*k) && ii<(int)Math.floor(sizeChunk*(k+1)) && developmentRun==1) 
			dataBuildClassifier.get(ii).setClassMissing();
		    else {
			Instance inst= dataBuildClassifier.get(ii);
			inst.setValue(inst.numValues()-1, refWeights[ii][i]);
		    }}
		//System.out.println("Removing evaluation instances. Time:"+(System.currentTimeMillis()-start_time)/1000);
		dataBuildClassifier.deleteWithMissingClass();
		//System.out.println("Training classifier. Time:"+(System.currentTimeMillis()-start_time)/1000);
		learners[i]= buildClassifier(dataBuildClassifier, i);
	    }

	    System.out.println("Testing models. Time:"+(System.currentTimeMillis()-start_time)/1000);
	    //Test Models
	    if (developmentRun==1) {
		double[][] instanceVoteWeights= new double[data.numInstances()][];
		Instances dataBuildClassifier= null;
		for (int ii= 0; ii<data.numInstances(); ii++) instanceVoteWeights[ii]= new double[baseClassifierCount];
		for (int i= 0; i<baseClassifierCount; i++) {
		    dataBuildClassifier= addLocalFeatures(pruneGlobalFeatures(data, i, 1), resInstanceSets, i);
		    for (int j=(int)Math.floor(sizeChunk*k);j<(int)Math.floor(sizeChunk*(k+1));j++)
			dataBuildClassifier.get(j).setClassMissing();
		    getVoteWeights(learners[i], dataBuildClassifier, instanceVoteWeights, i, k); 
		}
		score+= voteFold(learners, dataBuildClassifier, instanceVoteWeights, resInstanceSets, refInstanceSets, k, "", true);
	    } else {
		data= null; //dataLearners= null;
		double[][] instanceVoteWeights= new double[data2.numInstances()][]; 
		for (int ii= 0; ii<data2.numInstances(); ii++) instanceVoteWeights[ii]= new double[baseClassifierCount];
		for (int i= 0; i<baseClassifierCount; i++) {
		    System.out.println("Adding metafeatures: "+i+": "+(System.currentTimeMillis()-start_time)/1000);
		    Instances dataLearner2= addLocalFeatures(pruneGlobalFeatures(data2, i, 1), resInstanceSets2, i);
		    getVoteWeights(learners[i], dataLearner2, instanceVoteWeights, i, -1); 
		}
		System.out.println("Classifying test fold:"+(System.currentTimeMillis()-start_time)/1000);
		voteFold(learners, data2, instanceVoteWeights, resInstanceSets2, refInstanceSets2, -1, evalResultsFile, false);
	    }
	}
	System.out.println("Avg:" + score/(double)numFolds);
    }

    public static void getInstLabelCounts(int[][][] resInstanceSets){
	instLabelCounts= new Hashtable<Integer, Integer>();
	instLabelFreqs= new Hashtable<Integer, Integer>();
	for (int i= 0; i<resInstanceSets.length; i++) {
	    Hashtable<Integer, Integer> instCounts= new Hashtable<Integer, Integer>();
	    Hashtable<Integer, Integer> instFreqs= new Hashtable<Integer, Integer>();
	    for (int ii= 0; ii< baseClassifierCount; ii++){
		int[] instanceset= resInstanceSets[i][ii];
		for (int j = 0; j < instanceset.length; j++) {
		    Integer count= instCounts.get(instanceset[j]);
		    if (count==null) count= 1; else count+= 1; 
		    instCounts.put(instanceset[j], count);
		    //instCounts.put(instanceset[j], 1);
		    instFreqs.put(instanceset[j], 1);
		}
	    }
	    for (Map.Entry<Integer, Integer> entry: instCounts.entrySet()) {
		Integer count= instLabelCounts.get(entry.getKey());
		if (count==null) count= entry.getValue();
		else count+= entry.getValue();
		instLabelCounts.put(entry.getKey(), count);
	    }
	    for (Map.Entry<Integer, Integer> entry: instFreqs.entrySet()) {
		Integer freq= instLabelFreqs.get(entry.getKey());
		if (freq==null) freq= 1;
		else freq+= 1;
		instLabelFreqs.put(entry.getKey(), freq);
	    }
	}
    }

    public static Instances addLocalFeatures(Instances data, int[][][] resInstancesets, int id) throws Exception {
	int addedFeatures= resInstancesets[0].length-1;
	ArrayList<Attribute> attInfo = new ArrayList<Attribute>();
        for (int t= 0; t<data.numAttributes(); t++) attInfo.add(data.attribute(t));
	for (int t= 0; t<baseClassifierCount; t++) if (t!=id) attInfo.add(new Attribute("maxPrec_"+id+"_"+t));
	Instances newData= new Instances(data.relationName(), attInfo, data.numInstances());
        newData.setClassIndex(newData.numAttributes() - 1);
	int numAttributes= newData.numAttributes(), unionLength, intersectionLength, offset2= data.numAttributes()-2, offset;
	int[] instanceset1, instanceset2;
        for (int i= 0; i<data.numInstances(); i++) {
	    double[] arrayInstance = data.instance(i).toDoubleArray();
            double[] newArrayInstance = new double[numAttributes];
            for (int t= 0; t<data.numAttributes(); t++) newArrayInstance[t] = arrayInstance[t];
	    offset= offset2;
	    instanceset1= resInstancesets[i][id]; 
	    for (int t= 0; t<baseClassifierCount; t++) {
		if (t==id) continue;
		//System.out.println(offset+" "+t)
		instanceset2= resInstancesets[i][t];
		unionLength= union_length(instanceset1, instanceset2);
		intersectionLength= intersection_length(instanceset1, instanceset2);
		newArrayInstance[offset++]= (double)intersectionLength / Math.max(instanceset1.length, instanceset2.length); //0.5307094159798093 -> 0.33033 //0.5308390680911632 -> 0.33062 (78)
	    }
	    Instance inst = new DenseInstance(1.0, newArrayInstance);
	    newData.add(inst);
        }
	return newData;
    }    
    
    public static Instances pruneGlobalFeatures(Instances data, int id, int localFeatures) throws Exception {
	//Compute array of attributes to remove
	boolean[] removeAttributes = new boolean[data.numAttributes()];
	for (int t= 0; t<data.numAttributes(); t++) {
	    String[] attributeTags= data.attribute(t).name().split("_");
	    if (attributeTags.length>localFeatures) {
		boolean tagged= false;
		for (int a= 1; a< attributeTags.length; a++)
		    if (Integer.parseInt(attributeTags[a])== id) {tagged= true; break;}
		if (!tagged) {
		    removeAttributes[t] = true;
		}
	    }
	}
	
	//Create a new dataset
	// Header attribute information
	ArrayList<Attribute> attInfo = new ArrayList<Attribute>();
	for (int t= 0; t<data.numAttributes(); t++) {
	    if (removeAttributes[t] == false){
		attInfo.add(data.attribute(t));
	    }
	    //System.out.println(t+" "+removeAttributes[t]+" "+id+" "+data.attribute(t).name()+" "+attInfo.size());
	}
	Instances newData = new Instances(data.relationName(), attInfo, data.numInstances()); //Empty datasets
	newData.setClassIndex(newData.numAttributes() - 1);
	
	// Add Instances
	int numAttributes = newData.numAttributes();
	for ( int i = 0; i<data.numInstances(); i++) {
	    double[] arrayInstance = data.instance(i).toDoubleArray();
	    double[] newArrayInstance = new double[numAttributes];
	    int newCount = 0;
	    for (int t= 0; t<data.numAttributes(); t++) {
		if (removeAttributes[t] == false){
		    newArrayInstance[newCount] = arrayInstance[t];
		    newCount++;
		    }
		//System.out.println(t+" "+removeAttributes[t]+" "+id+" "+data.attribute(t).name()+" "+newCount);
	    }
	    Instance inst = new DenseInstance(1.0, newArrayInstance);
	    newData.add(inst);
	}
	return newData;
    }
    
    public static int[][] readRefInstancesets(String referenceFile) throws Exception {
    	ArrayList<int[]> refInstanceSets= new ArrayList<int[]>();
        BufferedReader bufferedReaderGT = new BufferedReader(new FileReader(referenceFile));	
        String line;
        while ((line = bufferedReaderGT.readLine()) != null) refInstanceSets.add(getInstances(line));
        bufferedReaderGT.close();
        int[][] refLabels2= new int[refInstanceSets.size()][];
        int i= 0;
        for (Object refs: refInstanceSets.toArray()) {refLabels2[i++]= (int[]) refs;}
        return refLabels2;
    }

    public static int[][][] readResInstancesets(String[] result_files) throws Exception {
    	int datasize= 0;
    	BufferedReader tmpReader= new BufferedReader(new FileReader(result_files[0]));
        while ((tmpReader.readLine()) != null) datasize++;
        tmpReader.close();
    	int [][][] resInstanceSets= new int[datasize][][];
        BufferedReader[] bufferedReader = new BufferedReader[result_files.length];
        for (int i = 0; i < baseClassifierCount; i++)
            bufferedReader[i] = new BufferedReader(new FileReader(result_files[i]));
        for (int i = 0; i < datasize; i++) {
        	resInstanceSets[i]= new int[baseClassifierCount][];
        	for (int j = 0; j < baseClassifierCount; j++)
        		resInstanceSets[i][j]= getInstances(bufferedReader[j].readLine());
        }
        for (int j = 0; j < result_files.length; j++) bufferedReader[j].close();
    	return resInstanceSets;
    }

    public static double[][] approximateOracleWeights(int[][][] resInstanceSets, int[][] refInstanceSets){
    	evaluation ev = new evaluation();
    	double[][] refWeights= new double[refInstanceSets.length][];
	maxClassifierScores= new Hashtable<Integer, Double>();
 	baseTotalScores= new double[baseClassifierCount];
   	for (int ii= 0; ii<refInstanceSets.length;ii++) {
	    refWeights[ii]= new double[baseClassifierCount];
	    double sum= 0; double maxScore= 0;
	    for (int i= 0; i < baseClassifierCount; i++){
		//System.out.println(ii+" "+i);
		ev.updateEvaluationResults(resInstanceSets[ii][i], refInstanceSets[ii]);
		sum+= refWeights[ii][i]= ev.lastCritScore;
		if (refWeights[ii][i]>= maxScore) maxScore= refWeights[ii][i];
		//System.out.println(ii+" "+i+" "+ev.lastCritScore+" "+resInstanceSets[ii][i][0]+" "+refInstanceSets[ii][0]);
	    }
	    if (sum==0) {
		for (int i= 0; i < baseClassifierCount; i++) refWeights[ii][i]= 0.0;
		continue;
	    }
	    sum= 0;
	    for (int i= 0; i < baseClassifierCount; i++) if (refWeights[ii][i]== maxScore) sum+= refWeights[ii][i];
	    	    	    
	    for (int i= 0; i < baseClassifierCount; i++) {
		if (refWeights[ii][i]== maxScore) {
		    refWeights[ii][i]/= sum; 
		}
		else refWeights[ii][i]= 0; 
	    }
	    for (int i= 0; i < baseClassifierCount; i++) baseTotalScores[i]+= refWeights[ii][i]/refInstanceSets.length;
	}
	//for (int i= 0; i < baseClassifierCount; i++) System.out.println(i+" "+maxClassifierScores.get(i));
	//for (int i= 0; i < baseClassifierCount; i++) System.out.println(i+" "+baseTotalScores[i]*baseClassifierCount);
	
	//for (int ii= 0; ii<refInstanceSets.length;ii++) {
	//if (refInstanceSets[ii].length==0) continue;
	//for (int i= 0; i < baseClassifierCount; i++) refWeights[ii][i]-= baseTotalScores[i];
	//}
    	return refWeights;
    }
    
    public static void readLabelsetCounts(String instancesetCountsFile, String transposedRefFile) throws Exception {
	String line;
	int t= 0, t2= 0;
	BufferedReader bufferedReader= new BufferedReader(new FileReader(transposedRefFile));
	labelDevMap= new Hashtable<Integer, Integer>();
	while ((line= bufferedReader.readLine()) != null) {
	    if (!line.trim().equals("")) {
		labelDevMap.put(t, t2);
		t++;
	    }	    
	    t2++;
	}
    	labelPriors= new Hashtable<Integer, Double>();
	bufferedReader= new BufferedReader(new FileReader(instancesetCountsFile));
	trainSize= 0;
	while ((line = bufferedReader.readLine()) != null) {
	    String[] line2= line.trim().split(" ", 2);
	    Double count= Double.parseDouble(line2[0]); 
	    trainSize+= count;
	    String[] sInstanceset= line2[1].split(" ");
	    for (String sLabel:sInstanceset) {
		Integer label= Integer.parseInt(sLabel);
		Double count2= labelPriors.get(label);
                if (count2==null) count2= count;
                else count2+= count;
                labelPriors.put(label, count2);
	    }
	}
        bufferedReader.close();
    }
    
    public static Instances constructData(int[][][] resInstanceSets, int dataSize, boolean comb_dev) throws Exception {
	String line;
	FastVector atts= new FastVector();
	atts.addElement(new Attribute("labelProb"));
	atts.addElement(new Attribute("labelProb2"));
	atts.addElement(new Attribute("uniqInstancesets"));
	atts.addElement(new Attribute("maxVotes"));
	for (int i= 0; i < baseClassifierCount; i++) {
	    atts.addElement(new Attribute("minInstFreq_"+i));
	    atts.addElement(new Attribute("maxInstFreq_"+i));
	    atts.addElement(new Attribute("minInstCount_"+i));
	    atts.addElement(new Attribute("instCount_"+i));
	    atts.addElement(new Attribute("emptySet_"+(i)));
	    atts.addElement(new Attribute("setCount_"+(i)));
	    atts.addElement(new Attribute("modePrec_"+(i)));
	    atts.addElement(new Attribute("modeRec_"+(i)));
	    atts.addElement(new Attribute("modeJaccard_"+(i)));
	}
        atts.addElement(new Attribute("weight"));
        Instances data= new Instances("Ensemble", atts, 0);
        data.setClassIndex(data.numAttributes() - 1);

        for (int ii= 0; ii<dataSize; ii++) {
            double[] vals= new double[data.numAttributes()];
            int offset= 0;
	    int label= ii;
	    if (comb_dev) label= labelDevMap.get(ii);
	    offset= addLabelFeatures(vals, label, offset);
	    offset= addCorrelations(vals, resInstanceSets[ii], offset);
            data.add(new DenseInstance(1.0, vals));
        }
        return data;
    }

    public static int addLabelFeatures(double[] vals, int label, int offset) {
	//System.out.println(label+" "+labelPriors.get((Integer)label));
	Double lprob= labelPriors.get((Integer)label); 
	if (lprob==null) lprob= 0.0; 
	if (lprob<10) vals[offset++]= Math.log(11-lprob); 
	else vals[offset++]= 0;
	if (lprob>50) vals[offset++]= Math.log(lprob-50); 
	else vals[offset++]= 0;
	return offset;
    }

    public static int addCorrelations(double[] vals, int[][] instancesets, int offset) throws Exception {
        Hashtable<IntBuffer, Integer> instancesetCounts= countInstancesets(instancesets);
        int[] modeInstanceSet= instancesets[getVotedClass(instancesets)];
	vals[offset++]= instancesetCounts.size(); 
        int max_vote= 0;
        for (Integer vote:instancesetCounts.values()) if (vote> max_vote) max_vote= vote;
	vals[offset++]= Math.log(1.0+max_vote);
        int modeUnionLength, modeIntersectionLength;
	for (int i= 0; i<instancesets.length; i++) {
	    int[] instanceset= instancesets[i];
	    Double min_inst_freq= (double)testSize*2, max_inst_freq= 0.0; 
	    Double min_inst_count= (double)testSize*baseClassifierCount, max_inst_count= 0.0; 
	    Double low_freq_count= 0.0;
	    for(int instance:instanceset) {
		int freq= instLabelFreqs.get(instance);
		min_inst_freq= Math.min(freq, min_inst_freq); 
		max_inst_freq= Math.max(freq, max_inst_freq);
		int count= instLabelCounts.get(instance);
		min_inst_count= Math.min(count, min_inst_count); 
		max_inst_count= Math.max(count, max_inst_count);
	    }
	    vals[offset++]= Math.log((min_inst_freq-0.5)/testSize);
	    vals[offset++]= Math.log(1+max_inst_freq/testSize); 
	    vals[offset++]= Math.log((min_inst_count-0.5)/(testSize*baseClassifierCount));
	    vals[offset++]= Math.log(1+(float)instanceset.length/testSize); 
	    if (instanceset.length==0) vals[offset++]= 1; else vals[offset++]= 0; 
	    modeUnionLength= union_length(instanceset, modeInstanceSet);
	    modeIntersectionLength= intersection_length(instanceset, modeInstanceSet);
	    vals[offset++]= instancesetCounts.get(IntBuffer.wrap(instanceset));    //Frequency of the instanceset result
	    double prec= 0;
	    if (instanceset.length!=0) prec= Math.log(1+(int)modeIntersectionLength / instanceset.length); 
	    vals[offset++]= prec;
	    double rec= 0;
	    if (modeInstanceSet.length!=0) rec= Math.log(1+(double)modeIntersectionLength / modeInstanceSet.length ); 
	    vals[offset++]= rec;
	    vals[offset++]= (double)modeIntersectionLength / modeUnionLength;      //Jaccard distance with the mode
        }
	return offset;
    }
     
    public static Hashtable<IntBuffer, Integer> countInstancesets(int[][] instancesets) {
    	Hashtable<IntBuffer, Integer> instancesetCounts= new Hashtable<IntBuffer, Integer>();
	for (int i= 0; i<instancesets.length; i++) {
	    IntBuffer instanceset_wrap= IntBuffer.wrap(Arrays.copyOf(instancesets[i], instancesets[i].length));
            Integer count= instancesetCounts.get(instanceset_wrap);
            if (count==null) count= 1;
            else count+= 1;
            instancesetCounts.put(instanceset_wrap, count);
	}
	return instancesetCounts;
    }
    
    public static int getVotedClass(int[][] instancesets) {
    	Hashtable<IntBuffer, Integer> instancesetCounts= countInstancesets(instancesets);
	int maxVoteCount= 0;
	int maxInstanceset= 0;
	for (int i= 0; i<instancesets.length; i++) {
            Integer count= instancesetCounts.get(IntBuffer.wrap(instancesets[i]));
            double tie_brake= 0;
            if (i< baseTotalScores.length) tie_brake= baseTotalScores[i];
            if (count> maxVoteCount || (count==maxVoteCount && baseTotalScores[maxInstanceset]<tie_brake)) //Resolve tie
            	{maxVoteCount= count; maxInstanceset= i;}
	}
	return maxInstanceset;
    }
    
    public static Classifier buildClassifier(Instances data, int id) throws Exception {
	weka.classifiers.functions.LinearRegression learner= new weka.classifiers.functions.LinearRegression();
	//learner.setOptions(weka.core.Utils.splitOptions("-R 500.0 -S 1"));  //                  -> 0.33569
	learner.setOptions(weka.core.Utils.splitOptions("-R 1000.0 -S 1"));  //0.5040187895737387 -> 0.33597 
	learner.buildClassifier(data);       // build classifier
	return learner;
    }
    
    public static void getVoteWeights(Classifier learner, Instances metaFeatures, double[][] instanceVoteWeights, int baseClassifier, int fold) throws Exception {
	for (int ii= 0; ii< instanceVoteWeights.length; ii++) {
            if (metaFeatures.get(ii).classIsMissing()== false && fold!=-1) continue;
	    Instance inst = new DenseInstance(1, metaFeatures.get(ii).toDoubleArray());
	    inst.setDataset(metaFeatures);
	    instanceVoteWeights[ii][baseClassifier]= Math.min(1.0, Math.max(0,learner.classifyInstance(inst)));
	}
    }

    public static void set_instantiate(double instantiate_weight) throws Exception {
        int label_count= labelPriors.size();
        double adjust_lprob= trainSize;
        label_cutoffs= new Hashtable<Integer, Integer>(labelPriors.size());
        for (Map.Entry<Integer, Double> entry : labelPriors.entrySet()) {
            int label= entry.getKey();
            label_cutoffs.put(label, (int)Math.max(1, Math.floor((entry.getValue()/adjust_lprob) *instantiate_weight)));
        }
    }

    public static double voteFold(Classifier[] learners, Instances metaFeatures, double[][]instanceVoteWeights, int[][][] resInstanceSets,
				  int[][] refInstanceSets, int fold, String resultsFile, boolean comb_dev) throws Exception {
        evaluation[] ev = new evaluation[baseClassifierCount + 1];
        for (int i= 0; i < baseClassifierCount; i++) ev[i] = new evaluation();
        ev[baseClassifierCount]= new evaluation();
        PrintWriter resultsWriter= null;
        if (resultsFile!="") resultsWriter= new PrintWriter(new FileWriter(resultsFile));
	int[][] votedInstanceset= new int[resInstanceSets.length][];
	//set_instantiate(0.9 * testSize);                                //0.32928 //0.33593
	set_instantiate(0.95 * testSize);                                 //0.32929 // 0.33597
	//set_instantiate(1.0 * testSize);                                //0.32905   // 0.33568
	//set_instantiate(1.05 * testSize); //0.31475 //0.5033785044440052 //0.32854 
	for (int ii= 0; ii< resInstanceSets.length; ii++) {
	    int label2= ii;
	    if (comb_dev) label2= labelDevMap.get(ii);
	    int[] labels;
	    //System.out.println(fold+" "+ii+" "+metaFeatures.get(ii).classIsMissing());
	    if (metaFeatures.get(ii).classIsMissing()== false && fold!=-1) continue;
	    int[][] resInstanceSets2= resInstanceSets[ii];
            int[] refInstanceSets3= new int[0];
            if (refInstanceSets.length>0) {
            	refInstanceSets3= refInstanceSets[ii];
            	for (int i= 0; i < baseClassifierCount; i++) ev[i].updateEvaluationResults(resInstanceSets2[i], refInstanceSets3); 
            }
	    Hashtable<Integer,Double> labelScores= new Hashtable<Integer,Double>();
	    double[] voteWeights= instanceVoteWeights[ii];
	    double sum= 0;
	    double max_length= 0;
	    for (int i= 0; i < baseClassifierCount; i++) max_length= Math.max(max_length, resInstanceSets2[i].length);
	    for (int i= 0; i < baseClassifierCount; i++) {
		sum+= voteWeights[i];
		//System.out.println(i+" "+voteWeights[i]+" "+sum);
		for (int j=0; j< resInstanceSets2[i].length; j++) {
		    int label= resInstanceSets2[i][j];
		    Double score= labelScores.get(label);
		    if (score==null) score= voteWeights[i]; else score+= voteWeights[i]; 
		    labelScores.put(label, score);
		}
	    }
	    //double threshold3= 0.55;  //0.5301872314898255
	    double threshold3= 0.5;     //0.5308390680911632
	    //double threshold3= 0.45;  //0.5304575712583459
	    double maxscore= 0;
	    int lc= 0;
	    TreeSet<DoubleBuffer> treesort= new TreeSet<DoubleBuffer>();
	    for (Map.Entry<Integer, Double> entry: labelScores.entrySet()) {
		double[] entry2= {entry.getValue(), entry.getKey()};
		treesort.add(DoubleBuffer.wrap(entry2));
	    }
	    Integer co= label_cutoffs.get(label2);
	    if (co== null) co= 1;
	    for (Iterator<DoubleBuffer> g= treesort.descendingIterator(); g.hasNext();) {
		double[] entry2= g.next().array();
		maxscore+= entry2[0];
		if (++lc>=co) break;
	    }
	    maxscore/= lc;

	    lc= 0;
	    treesort= new TreeSet<DoubleBuffer>();
	    for (Map.Entry<Integer, Double> entry: labelScores.entrySet()) {
		double[] entry2= {entry.getValue(), entry.getKey()};
		if (maxscore*threshold3 < entry2[0]) lc++; 
		treesort.add(DoubleBuffer.wrap(entry2));
	    }

	    lc= Math.max(co, lc); 
	    votedInstanceset[ii]= new int[lc];
	    lc= 0;
	    for (Iterator<DoubleBuffer> g= treesort.descendingIterator(); g.hasNext();) {
		if (lc>=votedInstanceset[ii].length) break;
		double[] entry2= g.next().array();
		votedInstanceset[ii][lc++]= (int)entry2[1];
	    }
	}
    	for (int ii= 0; ii< resInstanceSets.length; ii++) {
	    if (metaFeatures.get(ii).classIsMissing()== false && fold!=-1) continue;
	    int[] instanceset= votedInstanceset[ii];
	    LinkedList<Integer> label_list= new LinkedList<Integer>();
	    for (int label:instanceset) label_list.add(label);
	    if (resultsWriter!=null) {
		String result= "";
		for (int label : label_list) result+= ", "+label;
		resultsWriter.write(result.substring(Math.min(result.length(), 1)).trim()+"\n");
	    }
	    int[] refInstanceSets3= new int[0];
	    if (refInstanceSets.length>0) refInstanceSets3= refInstanceSets[ii];
	    ev[baseClassifierCount].updateEvaluationResults(instanceset, refInstanceSets3);
	    //System.out.println(ii+" "+Arrays.toString(instanceset)+"\n"+ii+" "+Arrays.toString(refInstanceSets)+" "+ev[baseClassifierCount].lastCritScore+" "+ev[baseClassifierCount].critScore);
	}
        if (refInstanceSets.length>0) {
	    for (int i = 0; i < baseClassifierCount; i++) System.out.println("Base-classifier:"+i+", "+ev[i].critScore);
	    System.out.println("Combination, " + fold+", "+ev[baseClassifierCount].critScore);
        }
        if (resultsFile!="") resultsWriter.close();
	return ev[baseClassifierCount].critScore;   
    }

    public static int union_length(int[] labels, int[] labels2) {
        if (labels == null) return labels2.length;
        if (labels2 == null) return labels.length;
        HashSet<Integer> setLabels = new HashSet<Integer>(labels.length+labels2.length);
        for (int label: labels) setLabels.add((Integer) label);
        for (int label: labels2) setLabels.add((Integer) label);
        return setLabels.size();
    }

    public static int intersection_length(int[] labels, int[] labels2) {
        if (labels==null || labels2==null) return 0;
        HashSet<Integer> setLabels= new HashSet<Integer>(labels.length);
        for (int label: labels) setLabels.add((Integer)label);
        int t= 0;
	for (int label: labels2) if (setLabels.contains(label)) t++;
        return t;
    }

    public static int[] getInstances(String line) {
	if (line == null) return new int[0];
	String[] refInstances = line.split(",");
	if (refInstances[0].equals("")) return new int[0];
        int[] refLab = new int[refInstances.length];
	for (int i = 0; i < refInstances.length; i++) {
	    refLab[i] = Integer.parseInt(refInstances[i].replace(" ", ""));
	}
        return refLab;
    }

    public static class evaluation {
    	public double critScore;
    	public double lastCritScore;
  	public double maFscore= 0.0;
	int num_classified= 0;
	
        public void updateEvaluationResults(int[] labels, int[] ref_labels) {
	    HashSet<Integer> ref_labels2 = new HashSet<Integer>(ref_labels.length);
            for (int label : ref_labels) ref_labels2.add((Integer) label);
	    int tp2= 0, fp2= 0, fn2;

	    double tp3= 0, fp3= 0;
	    double j= 1;
	    for (int label: labels) {
                if (ref_labels2.contains((Integer) label)) {
		    tp2++;
		    tp3+= 1.0*j;
		} else {
		    fp2++;
		    fp3+= 1.0*j;
		}
		j/= 2;
            }
	    num_classified++;
	    fn2= ref_labels.length - tp2;
	    double rec2 = (double) tp2 / (tp2 + fn2);
            double prec2 = (double) tp2 / (tp2 + fp2);
            double fscore = (2.0 * rec2 * prec2) / (rec2 + prec2);
            if ((rec2 == 0 && prec2 == 0) || (tp2 + fp2 == 0) || (tp2 + fn2 == 0)) fscore = 0;
	    
	    double rec3 = (double) tp3 / (tp3 + fn2);
            double prec3 = (double) tp3 / (tp3 + fp3);
            double fscore3 = (2.0 * rec3 * prec3) / (rec3 + prec3);
            if ((rec3 == 0 && prec3 == 0) || (tp3 + fp3 == 0) || (tp3 + fn2 == 0)) fscore3 = 0;
	    
	    //if (Double.isNaN(fscore3)) System.out.println(rec3+" "+prec3+" "+tp3+" "+fp3+" "+fn2+" "+fscore3+" "+labels.length+" "+ref_labels.length);
	    //System.out.println(Arrays.toString(labels));
	    //System.out.println(Arrays.toString(ref_labels));
	    //System.out.println(tp2+" "+fp2+" "+fn2+" "+fscore+" "+maFscore+" "+num_classified+"\n");
	    maFscore+= (fscore- maFscore) / num_classified;
	    critScore= maFscore;
	    //lastCritScore= fscore;  //0.5308390680911632 
	    lastCritScore= fscore3;   //0.5313078373942115
	}
    }
	
}   
