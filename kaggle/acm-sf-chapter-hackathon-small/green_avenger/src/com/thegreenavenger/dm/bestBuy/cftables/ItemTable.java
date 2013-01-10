/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thegreenavenger.dm.bestBuy.cftables;

import com.thegreenavenger.dm.bestBuy.entry.Entry;
import com.thegreenavenger.dm.common.StandardDeviation;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author David Thomas
 */
public class ItemTable implements CFTable {
     
    public HashMap<String, Integer> skuMap = new HashMap<String, Integer>();
    public HashMap<Integer, String> rSkuMap = new HashMap<Integer, String>();
    public HashMap<Integer, Integer> skuCount = new HashMap<Integer, Integer>();
    public float[][] iSim;
    public final HashMap<String, List<Entry>> queries = new HashMap<String, List<Entry>>();
    public final HashMap<String, Integer> qMatchMap = new HashMap<String, Integer>();
    public final HashMap<Integer, String> rqMatchMap = new HashMap<Integer, String>();
    public final StandardDeviation qSimStats = new StandardDeviation();
    public final HashMap<Integer, List<Integer>> nZeroMap = new HashMap<Integer, List<Integer>>();
    public final HashMap<String,List<Entry>> ceMap;
    private int isize = 0;

    public ItemTable(HashMap<String, Integer> skuMap,
            HashMap<Integer, String> rSkuMap,
            HashMap<Integer, Integer> skuCount,
            HashMap<String,List<Entry>> ceMap) {
        this.skuMap = skuMap;
        this.rSkuMap = rSkuMap;
        this.skuCount = skuCount;
        this.ceMap = ceMap;
        isize = skuMap.size();
    }

    @Override
    public void addTrainData(Entry e) {
        
    }

    @Override
    public void create() {
        iSim = new float[isize][isize];
        
        for (java.util.Map.Entry<String, List<Entry>> q1 : ceMap.entrySet()) {
            
            for(Entry e: q1.getValue())
            {
                if(skuMap.containsKey(e.getSku())==false) continue;
                int sidx2 = skuMap.get(e.getSku());
                int count2 = skuCount.get(sidx2);
                
                for (Entry e2 : q1.getValue()) {
                    if(e==e2) continue;
                    if(skuMap.containsKey(e2.getSku())==false) continue;
                    int elapsed = (int)Math.abs(e.getQueryDate().getTime()-e2.getQueryDate().getTime());
                    int sidx = skuMap.get(e2.getSku());
                    int count = skuCount.get(sidx);
                    if (count != 0 && count2!=0) {
                        double score = 0.0;
                        if(elapsed<=3600000) score = 1.0;
                        else if(elapsed<=86400000) score=0.5;
                      
                       
                  //      iSim[sidx][sidx2] += (float)(score*1/(1.0*count2));
                  //      iSim[sidx2][sidx] += (float)(score*1/(1.0*count));
                          iSim[sidx][sidx2] += score;
                          iSim[sidx2][sidx] += score;
                    }

                }
            }
            
        }
        
        for(int i=0;i<iSim.length;i++)
        {
            StandardDeviation std = new StandardDeviation();
            for(int j=0;j<iSim.length;j++)
            {
                std.add(iSim[i][j]);
   //             System.out.println(String.format("%d %d %.4f",i,j,iSim[i][j]));
            }
            StandardDeviation.Values values = std.values();
            double total = values.mean*values.count;
            if(total==0) continue;
            for(int j=0;j<iSim.length;j++)
            {
                iSim[i][j]/=(1.0*total);
            }
        }


    }
    
    
}
