/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thegreenavenger.dm.bestBuy.cftables;

import com.thegreenavenger.dm.bestBuy.entry.Entry;
import com.thegreenavenger.dm.common.StandardDeviation;
import com.thegreenavenger.dm.common.StandardDeviation.Values;
import java.util.Date;
import java.util.HashMap;


/**
 *
 * @author David Thomas
 */
public class TimeTable implements CFTable {
     
    public HashMap<String, Integer> skuMap;
    public HashMap<Integer, String> rSkuMap;
    public HashMap<Integer, Integer> skuCount;
    
    public double[] allScores;
    public float[][] tSim;
    public final int size;
    public final int dates;
    public final long interval = 86400000;
    public final Date min;
    public final Date max;
    
    public final double gaussian[] = new double[] { .006,.061,.242,.383,.242,.061,.006};

    public TimeTable(HashMap<String, Integer> skuMap,
            HashMap<Integer, String> rSkuMap,
            HashMap<Integer, Integer> skuCount,
            Date min, Date max)
    {
        this.skuMap = skuMap;
        this.rSkuMap = rSkuMap;
        this.skuCount = skuCount;
        this.size = skuMap.size();
        this.min = min;
        this.max = max;
        this.dates = (int)Math.ceil((max.getTime()-min.getTime())/(1.0*interval));
        allScores = new double[size];
        tSim = new float[dates][size];
    }

    public int getIdx(Date d)
    {
        return (int)Math.floor((d.getTime()-min.getTime())/(1.0*interval));
    }
    @Override
    public void addTrainData(Entry e) {
        
        Date clickDate = e.getClickDate();
        int tIdx = getIdx(clickDate);
        Integer idx = skuMap.get(e.getSku());
        allScores[idx]++;
        tSim[tIdx][idx]++;
    }

    @Override
    public void create() {
        // find average popularity over whole time window
        StandardDeviation std = new StandardDeviation();
        for(int i=0;i<size;i++)
        {
            std.add(allScores[i]);
        }
        Values values = std.values();
        double total = values.count*values.mean;
        for(int i=0;i<size;i++)
        {
            allScores[i]/=(1.0*total);
        }
        // run a gaussian over time information
      
        float ttSim[][] = new float[dates][size];
         for(int t=0;t<dates;t++)
        {
            
            for(int i=0;i<size;i++)
            {
                int count = 0;
                for(int tt=t-3;tt<=t+3;tt++)
                {
                    int tf = tt;
                    if(tf<0)
                    {
                        tf = 0;
                    }
                    if(tf>=dates)
                    {
                        tf=dates-1;
                    }
                    ttSim[t][i] += tSim[tf][i]*gaussian[count];
                    count++;
                }
            }
          
        }
         tSim = ttSim;
         
         // normalize
        for(int t=0;t<dates;t++)
        {
            std = new StandardDeviation();
            for(int i=0;i<size;i++)
            {
                std.add(tSim[t][i]);
            }
            values = std.values();
            total = values.count*values.mean;
             for(int i=0;i<size;i++)
            {
                tSim[t][i] = (float)(tSim[t][i]/(1.0*total)-allScores[i]);
//                System.out.println("tt " + tSim[t][i]);
            }
        }
    }
    
    
}
