/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thegreenavenger.dm.bestBuy;

import com.thegreenavenger.dm.bestBuy.entry.Entry;
import com.thegreenavenger.dm.common.BasicScore;
import com.thegreenavenger.dm.common.Score;
import com.thegreenavenger.dm.common.SimFunctions;
import com.thegreenavenger.dm.common.StandardDeviation;
import com.thegreenavenger.dm.common.StandardDeviation.Values;
import com.thegreenavenger.dm.common.TestCase;
import com.thegreenavenger.dm.common.WordCleaner;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author David Thomas
 */
public class ScoringAlgorithm {

    private final DataTables dt;

    public ScoringAlgorithm(DataTables dt,
            File logDir) throws Exception {

        this.dt = dt;
    }
    
    // get popularity for a date
    public Score[] getPopScores(TestCase testCase) {
      
        float pSim[] = dt.getTimeShiftedPopularityArray(((Entry)testCase).getQueryDate());
        Score[] output = new BasicScore[dt.skuSize()];
        for(int i=0;i<output.length;i++)
        {
            output[i] = new BasicScore(pSim[i],i);
        }
       return output;
    }

  
    // find similar items depending on customer history
    public Score[] getSimItems(String customer) {
        Score[] score = new BasicScore[dt.skuSize()];
        for (int i = 0; i < dt.skuSize(); i++) {
            score[i] = new BasicScore(0, i);
        }
        if (dt.containsCustomer(customer) == false) {
            return score;
        }
        
        for(Entry e: dt.getCustomerEntries(customer))
        {
           
            int se = dt.getCustomerEntries(customer).size();
            Integer j = dt.getSkuMap(e.getSku());
            if(j==null) continue;
            float iSim[] = dt.getItemSimArray(j);
            
            for(int i=0;i<dt.skuSize();i++)
            {
                ((BasicScore) score[i]).add(iSim[i] / (1.0 * se));
            }
        }
        
       
        StandardDeviation std = new StandardDeviation();
        for (int i = 0; i < score.length; i++) {
            std.add(score[i].get());
        }
        Values values = std.values();
        double total = values.count * values.mean;
        if (total != 0) {
            for (int i = 0; i < score.length; i++) {
                ((BasicScore) score[i]).divide(total);
            }
        }

        return score;
    }
    
    // get score based on queries within the last day
    public Score[] getSimItemsTest(String customer, Entry entry) throws Exception {
        Score[] score = new BasicScore[dt.skuSize()];
        for (int i = 0; i < dt.skuSize(); i++) {
            score[i] = new BasicScore(0, i);
        }
        if (dt.containsCustomer(customer) == false) {
            return score;
        }
        
        for(Entry e: dt.getCustomerEntries(customer))
        {
            if(e.getUID()==entry.getUID()) continue;
            int se = dt.getCustomerEntries(customer).size();
            Integer j = dt.getSkuMap(e.getSku());
            if(j==null)
            {
                if(Math.abs(e.getClickDate().getTime()-entry.getClickDate().getTime())<dt.interval)
                {
                    Score qwi[] = this.getCalcScoresQueryIterative(entry);
                    for(int i=0;i<dt.skuSize();i++)
                    {
                        ((BasicScore)score[i]).add(qwi[i].get()/(1.0f*se));
                    }
                   
                }
                 continue;
            }
             
        }      
       
        StandardDeviation std = new StandardDeviation();
        for (int i = 0; i < score.length; i++) {
            std.add(score[i].get());
        }
        Values values = std.values();
        double total = values.count * values.mean;
        if (total != 0) {
            for (int i = 0; i < score.length; i++) {
                ((BasicScore) score[i]).divide(total);
            }
        }

        return score;
    }
    

    // overall popularity time offset
    public Score[] getTimeOffset(Entry entry) {
        Date clickDate = entry.getClickDate();
        // float tScores[] = tSim[ttable.getIdx(clickDate)];
        float tScores[] = dt.getTimeShiftArray(clickDate);
        Score[] scores = new BasicScore[dt.skuSize()];
        for (int i = 0; i < dt.skuSize(); i++) {
            scores[i] = new BasicScore(tScores[i], i);
        }
        return scores;
    }
    
    
    public Score[] getScores(TestCase testCase) {
        try {
            List<Score[]> scores = new ArrayList<Score[]>();
          //  Score[] qs = getCalcScoresQueryTemporal(testCase);
            Score[] qt = getCalcScoresQueryIterative(testCase);
            Score[] to = getTimeOffset((Entry) testCase);
            Score[] kw = getCalcScoresKWTemporal(testCase);
            Score[] cs = getSimItems(((Entry) testCase).getUser());
            Score[] cst = getSimItemsTest(((Entry)testCase).getUser(),(Entry)testCase);
            Score[] ps = getPopScores(testCase);
            scores.add(qt); // query comparison
            scores.add(kw); // keyword comparison
     
            {   // customer history comparison mixed with temporal offset
                List<Double> subWeights = new ArrayList<Double>();
                List<Score[]> subScores = new ArrayList<Score[]>();
                subScores.add(cs);
                subScores.add(to);
                subWeights.add(0.9);
                subWeights.add(0.1);
                scores.add(merge(subScores, subWeights,dt.skuSize()));
            }
            scores.add(ps);
            scores.add(cst);
            // get weighting and merge scores
            List<Double> weights = calculateWeights(scores);
            Score score[] = merge(scores, weights, dt.skuSize());
            // remove any duplicate items for a customer
            if(dt.containsCustomer(((Entry)testCase).getUser()))
            {
                for (Entry e : dt.getCustomerEntries(((Entry) testCase).getUser())) {
                    if (e == testCase) {
                        continue;
                    }
                    if (dt.containsSku(e.getSku()) == false) {
                        continue;
                    }
                    int idx = dt.getSkuMap(e.getSku());
                    ((BasicScore) score[idx]).set(0);
                }
            }
            String words[] = WordCleaner.cleanTokens(((Entry)testCase).getQuery());
            // if a word is a sku, give it a high score
            for(String w:words )
            {
                if(dt.containsSku(w))
                {
       //             System.out.println("Found " + w);
                    ((BasicScore)score[dt.getSkuMap(w)]).set(10.0f);
                }
            }
           
            return score;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    List<Double> calculateWeights(List<Score[]> scores) {
        List<Double> weights = new ArrayList<Double>();
        for (Score[] s : scores) {
            weights.add(0.0);
        }
        // find highest score per entry, use for weighting
        for (int i = 0; i < dt.skuSize(); i++) {
            for (int j = 0; j < scores.size(); j++) {
                double score = scores.get(j)[i].get();
                if (score > weights.get(j)) {
                    weights.set(j, score);
                }
            }
        }
        
        // blend customer history higher if query scores are sufficiently low
        if (0.5 * (weights.get(0) + weights.get(1)) < .3 && weights.get(2) > 0.0001) {
            weights.set(0, .4);
            weights.set(1, .4);
            weights.set(2, 0.2);
             
        } else {
            // weight according to higher 1st score
             if(weights.get(0)>weights.get(1))
            {
                weights.set(0, .7);
                weights.set(1, .1);
            }
            else
            {
                weights.set(0, .1);
                weights.set(1, .7);
            }
          
            weights.set(2, 0.1);
            
        }
        weights.set(3,0.001);
        weights.set(4,0.4);
        return weights;

    }
    
  
    public Score[] getCalcScoresKWTemporal(TestCase testCase) throws Exception {
        if (testCase instanceof Entry == false) {
            return null;
        }
        Entry entry = (Entry) testCase;
        String words[] = WordCleaner.cleanTokens(entry.getQuery());
        Score[] scores = new Score[dt.skuSize()];
        for (int i = 0; i < dt.skuSize(); i++) {
            scores[i] = new BasicScore(0, i);
        
        }

        for (String w : words) {
            Integer idx = dt.getWordMap(w);
            String close = w;
            if (idx == null) // no word matches, find closest
            {
                double best = 0;
                Integer bi = null;
                for (java.util.Map.Entry<String, Integer> we : dt.getWordMapSet()) {
                    double s = SimFunctions.multiDistance(w, we.getKey());
                    if (bi == null || s > best) {
                        best = s;
                        bi = we.getValue();
                        close = we.getKey();
                    }
                }
                idx = bi;
            }
            
           // else
            {
                float wtSim[] = dt.getTimeShiftedWordArray(close, entry.getClickDate());
                float wSim[] = dt.getWordArray(idx);
                // combine overall sim score with a one-week temporal window
                // to mitigate associations depending on time
                for (int i = 0; i < dt.skuSize(); i++) {
                    ((BasicScore) scores[i]).add(wSim[i] + wtSim[i]);
                }
            }
        }
        
        // normalize the output so that it can be combined with other scores
        StandardDeviation std = new StandardDeviation();
        for (int i = 0; i < scores.length; i++) {
            std.add(scores[i].get());
        }
        Values values = std.values();
        double total = values.count * values.mean;
        if (total != 0) {
            for (int i = 0; i < scores.length; i++) {
                ((BasicScore) scores[i]).divide(total);
            //    ((BasicScore) scores[i]).set(scores[i].get()*mask[i].get());
            }
        }

        return scores;
    }
    
    
    
     public Score[] getCalcScoresQueryIterative(TestCase testCase) throws Exception {
        if (testCase instanceof Entry == false) {
            return null;
        }
        Entry entry = (Entry) testCase;
        String query = WordCleaner.clean(entry.getQuery());
        Score[] scores = new Score[dt.skuSize()];
        for (int i = 0; i < dt.skuSize(); i++) {
            scores[i] = new BasicScore(0, i);
        }

        Integer idx = dt.getQueryMap(query);

        if (idx == null) // new query, find closest match
        {
            List<String> splitWords = new ArrayList<String>();
            splitWords.add(query);
            
            int count = query.length();
           
            List<String> words = new ArrayList<String>();
         //   while(count>0)
            {
                for(int j=0;j<splitWords.size();j++)
                {
                 String q = splitWords.get(j);
                 int widx = q.length();
                  String word = null;
                 while(widx>0)
                 {
                     List<String> cwords = dt.getWordsOfSize(widx);
                     if(cwords==null) 
                     {
                         widx--;
                         continue;
                     }
                     for(String s: cwords)
                     {
                         if(q.contains(s))
                         {
                             word = s;
                             widx = 0;
                             break;
                         }
                         
                     }
                     widx--;
                 }
               
                 if(word==null) count-=q.length();
                 else
                 {
                     count-=word.length();
                     if( (word.length()==1 && Character.isDigit(word.charAt(0))) ||
                          (word.length()>2 ))
                         words.add(word);
           
                     int si = q.indexOf(word);
                     int ei = si+word.length();
                     if(si>0)
                     {
                         splitWords.add(q.substring(0,si));
                     }
                     if(ei<q.length())
                     {
                         splitWords.add(q.substring(ei,q.length()));
                     }
                 }
                }
            }
       //     System.out.print("Matching " + query + "->");
            for (String w : words) {
       //         System.out.print(w + ",");
            idx = dt.getWordMap(w);
            String close = w;
           
           // else
            {
                float wtSim[] = dt.getTimeShiftedWordArray(close, entry.getClickDate());
                float wSim[] = dt.getWordArray(idx);
                // combine overall sim score with a one-week temporal window
                // to mitigate associations depending on time
                for (int i = 0; i < dt.skuSize(); i++) {
                    ((BasicScore) scores[i]).add(wSim[i] + wtSim[i]);
                }
            }
     //       System.out.println();
        }
         
        }
        else
        {
             float qtSim[] = dt.getTimeShiftedQueryArray(query, entry.getClickDate());
             float qqSim[] = dt.getQueryArray(idx);
          // combine overall score with one-week time window
        for (int i = 0; i < dt.skuSize(); i++) {
            ((BasicScore) scores[i]).add(qtSim[i] +qqSim[i]);
        }
        }

        StandardDeviation std = new StandardDeviation();
        for (int i = 0; i < scores.length; i++) {
            std.add(scores[i].get());
        }
        Values values = std.values();
        double total = values.count * values.mean;
        if (total != 0) {
            for (int i = 0; i < scores.length; i++) {
                ((BasicScore) scores[i]).divide(total);
            }
        }
        return scores;
    }
     
     
    
    public static Score[] merge(List<Score[]> scores, List<Double> weights, int size)
    {
       Score[] results = new Score[size];
       for(int i=0;i<results.length;i++) { 
           float s = 0;
           for(int j=0;j<scores.size();j++)
           {
             s+=(scores.get(j)[i].get()*weights.get(j));
           }
           results[i] = new BasicScore(s,i); 
       }
       return results;
    }
     
   
}
