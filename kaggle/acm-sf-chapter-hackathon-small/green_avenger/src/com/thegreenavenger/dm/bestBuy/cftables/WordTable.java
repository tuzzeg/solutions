/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thegreenavenger.dm.bestBuy.cftables;

import com.thegreenavenger.dm.bestBuy.entry.Entry;
import com.thegreenavenger.dm.common.StandardDeviation;
import com.thegreenavenger.dm.common.StandardDeviation.Values;
import com.thegreenavenger.dm.common.WordCleaner;
import com.thegreenavenger.dm.bestBuy.SkuMetaDataParser;
import com.thegreenavenger.dm.bestBuy.SkuMetaDataParser.MetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author David Thomas
 */
public class WordTable implements CFTable {
     
    public HashMap<String, Integer> skuMap = new HashMap<String, Integer>();
    public HashMap<Integer, String> rSkuMap = new HashMap<Integer, String>();
    public HashMap<Integer, Integer> skuCount = new HashMap<Integer, Integer>();
    public float[][] wSim;  
    public final HashMap<String, Integer> qMatchMap = new HashMap<String, Integer>();
    public final HashMap<Integer, String> rqMatchMap = new HashMap<Integer, String>();
    public final StandardDeviation qSimStats = new StandardDeviation();
    public final HashMap<Integer, List<Integer>> nZeroMap = new HashMap<Integer, List<Integer>>();
    public final HashMap<String,List<Entry>> wordMap ;
    public final HashMap<String,List<String>> wsMap = new HashMap<String,List<String>>();
    public final HashMap<String,Integer> wordCount = new HashMap<String,Integer>();
    public final SkuMetaDataParser metaParser;
    private int qsize = 0;

    public WordTable(HashMap<String, Integer> skuMap,
            HashMap<Integer, String> rSkuMap,
            HashMap<Integer, Integer> skuCount,
            HashMap<String,List<Entry>> wordMap,
            SkuMetaDataParser metaParser) {
        this.skuMap = skuMap;
        this.rSkuMap = rSkuMap;
        this.skuCount = skuCount;
        this.wordMap = wordMap;
      
     
        this.metaParser = metaParser;
        for(java.util.Map.Entry<String,MetaData> me: metaParser.getSkuMap().entrySet())
        {
            String tokens[] = WordCleaner.cleanTokens(me.getValue().getName());
            List<String> words = wsMap.get(me.getKey());
            if(words==null)
            {
                words = new ArrayList<String>();
                wsMap.put(me.getKey(),words);
                 
            }
             for(String t: tokens)
            {
              //  if(words.contains(t)==false) 
                {
                    words.add(t);
                    Integer wc = wordCount.get(t);
                    if(wc==null)
                    {
                        wc = 0;
                        wordCount.put(t,wc);
                         qMatchMap.put(t, qsize);
                        rqMatchMap.put(qsize,t);
                        ++qsize;
                    }
                    wordCount.put(t,wc+1);
                   
                }
            }
            
        }
        
    }

    @Override
    public void addTrainData(Entry e) {
       String tokens[] = WordCleaner.cleanTokens(e.getQuery());
       List<String> words = wsMap.get(e.getSku());
       if(words==null)
       {
           words = new ArrayList<String>();
           wsMap.put(e.getSku(),words);
       }
         for(String t: tokens)
            {
             //   if(words.contains(t)==false) 
                {
                    words.add(t);
                    Integer wc = wordCount.get(t);
                    if(wc==null)
                    {
                        wc = 0;
                        wordCount.put(t,wc);
                        qMatchMap.put(t, qsize);
                        rqMatchMap.put(qsize,t);
                        ++qsize;
                    }
                    wordCount.put(t,wc+1);
                  
                }
            }
         
    }

    @Override
    public void create() {
        wSim = new float[qsize][skuMap.size()];
        
        
        for(int s=0;s<skuMap.size();s++)
        {
            List<String> words = wsMap.get(rSkuMap.get(s));
            if(words==null) continue;
            for(String word:words) 
            {
                int w1 = qMatchMap.get(word);
            //    wSim[w1][s]+=1/(1.0*skuCount.get(s));
                 wSim[w1][s]+=1.0/(1.0*wordCount.get(word));
            }
        }
        for(int w=0;w<qsize;w++)
        {
            StandardDeviation std = new StandardDeviation();
            for(int s=0;s<skuMap.size();s++)
            {
                std.add(wSim[w][s]);
            }
            Values values = std.values();
            double total = values.count*values.mean;
            if(total==0) continue;
             for(int s=0;s<skuMap.size();s++)
            {
                wSim[w][s]/=(1.0*total);
            }
        }
    }
    
   
    

    
}
