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
public class QueryTable implements CFTable {
     
    public HashMap<String, Integer> skuMap = new HashMap<String, Integer>();
    public HashMap<Integer, String> rSkuMap = new HashMap<Integer, String>();
    public HashMap<Integer, Integer> skuCount = new HashMap<Integer, Integer>();
    public float[][] wSim;  
    public StandardDeviation[][] tSim;
    public final HashMap<String, Integer> qMatchMap = new HashMap<String, Integer>();
    public final HashMap<Integer, String> rqMatchMap = new HashMap<Integer, String>();
    public final StandardDeviation qSimStats = new StandardDeviation();
    public final HashMap<Integer, List<Integer>> nZeroMap = new HashMap<Integer, List<Integer>>();
    public final HashMap<String,List<String>> queries = new HashMap<String,List<String>>();
    public final HashMap<String,List<Entry>> qEntries = new HashMap<String,List<Entry>>();
    public final HashMap<String,Integer> qCount = new HashMap<String,Integer>();
    public final SkuMetaDataParser metaParser;
    private int qsize = 0;

    public QueryTable(HashMap<String, Integer> skuMap,
            HashMap<Integer, String> rSkuMap,
            HashMap<Integer, Integer> skuCount,
            SkuMetaDataParser metaParser) {
        this.skuMap = skuMap;
        this.rSkuMap = rSkuMap;
        this.skuCount = skuCount;
        qsize = 0;
        int count = 0;
        this.metaParser = metaParser;
    
        for(java.util.Map.Entry<String,MetaData> me: metaParser.getSkuMap().entrySet())
        {
            {
                String q1 = WordCleaner.clean(me.getValue().getName());

                List<String> fEntries = queries.get(q1);
                if (fEntries == null) {
                    fEntries = new ArrayList<String>();

                    queries.put(q1, fEntries);
                    qMatchMap.put(q1, qsize);
                    rqMatchMap.put(qsize, q1);          
                    qsize++;
                }
                Integer c = qCount.get(q1);
                if(c==null)
                {
                    c = 0;
                    qCount.put(q1,c);
                }
                qCount.put(q1,c+1);
                fEntries.add(me.getKey());
            }
            /*
             {
                String q1 = me.getKey();

                List<String> fEntries = queries.get(q1);
                if (fEntries == null) {
                    fEntries = new ArrayList<String>();

                    queries.put(q1, fEntries);
                    qMatchMap.put(q1, qsize);
                    rqMatchMap.put(qsize, q1);          
                    qsize++;
                }
                Integer c = qCount.get(q1);
                if(c==null)
                {
                    c = 0;
                    qCount.put(q1,c);
                }
                qCount.put(q1,c+1);
                fEntries.add(me.getKey());
            }
            */
        }
     
    }

    @Override
    public void addTrainData(Entry e) {
       String q1 = WordCleaner.clean(e.getQuery());
        
        List<String> fEntries = queries.get(q1);
        if (fEntries == null) {
            fEntries = new ArrayList<String>();

            queries.put(q1, fEntries);
            qMatchMap.put(q1, qsize);
            rqMatchMap.put(qsize, q1);
            qsize++;
        }
        Integer c = qCount.get(q1);
         if(c==null)
         {
             c = 0;
             qCount.put(q1,c);
         }
         qCount.put(q1,c+1);
        fEntries.add(e.getSku());
        
        List<Entry> entries = qEntries.get(q1);
        if(entries==null)
        {
            entries = new ArrayList<Entry>();
            qEntries.put(q1,entries);
        }
        entries.add(e);
    }

    @Override
    public void create() {
        wSim = new float[qsize][skuMap.size()];
        tSim = new StandardDeviation[qsize][skuMap.size()];
        for(int i=0;i<qsize;i++)
        {
            for(int j=0;j<skuMap.size();j++)
            {
                tSim[i][j] = new StandardDeviation();
            }
        }
        for(int q=0;q<qsize;q++)
        {
            String query = rqMatchMap.get(q);
            for(String sku: queries.get(query))
            {
                Integer idx = skuMap.get(sku);
                wSim[q][idx]+=1.0/(1.0*qCount.get(query));
             
            }
           
            if(qEntries.containsKey(query)==false) continue;
            for(Entry e: qEntries.get(query))
            {
                Integer idx = skuMap.get(e.getSku());
                int elapsedTime = (int)(e.getClickDate().getTime()-e.getQueryDate().getTime());
                tSim[q][idx].add(elapsedTime);
                
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
