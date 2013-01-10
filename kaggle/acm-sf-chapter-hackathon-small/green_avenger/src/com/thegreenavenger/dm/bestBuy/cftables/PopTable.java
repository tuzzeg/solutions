/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thegreenavenger.dm.bestBuy.cftables;

import com.thegreenavenger.dm.bestBuy.entry.Entry;
import com.thegreenavenger.dm.common.GaussianBlur;
import com.thegreenavenger.dm.common.StandardDeviation;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author David Thomas
 */
public class PopTable implements CFTable {
     
    public HashMap<String, Integer> skuMap = new HashMap<String, Integer>();
    public HashMap<Integer, String> rSkuMap = new HashMap<Integer, String>();
    public HashMap<Integer, Integer> skuCount = new HashMap<Integer, Integer>();
    public float[][] iSim;
   
    private int isize = 0;
    private int tsize = 0;
    public final long interval = 86400000;
    private final Date min,max;
    public PopTable(HashMap<String, Integer> skuMap,
            HashMap<Integer, String> rSkuMap,
            HashMap<Integer, Integer> skuCount,
            Date min, Date max) {
        this.skuMap = skuMap;
        this.rSkuMap = rSkuMap;
        this.skuCount = skuCount;
        isize = skuMap.size();
        this.min = min;
        this.max = max;
        tsize = (int)Math.ceil((max.getTime()-min.getTime())/(1.0*interval));   
        iSim = new float[tsize][isize];
    }
    
      public int getIdx(Date d)
    {
        return (int)Math.floor((d.getTime()-min.getTime())/(1.0*interval));
    }

    @Override
    public void addTrainData(Entry e) {
        Integer tIdx = getIdx(e.getQueryDate());
        Integer sIdx = skuMap.get(e.getSku());
        if(sIdx==null) return;
        if(tIdx<0) tIdx = 0;
        if(tIdx>tsize-1) tIdx = tsize-1;
        iSim[tIdx][sIdx]++;
    }

    @Override
    public void create() {
      for(int t=0;t<tsize;t++)
      {
          StandardDeviation std = new StandardDeviation();
          for(int i=0;i<isize;i++)
          {
              std.add(iSim[t][i]);
          }
          double total = std.values().mean*isize;
          for(int i=0;i<isize;i++)
          {
              iSim[t][i]/=(1.0*total);
          }
      }
      gaussian = GaussianBlur.buildArray(4,0);
    }
    
    public double gaussian[] = new double[] { .006,.061,.242,.383,.242,.061,.006};
    
    public float[] getArray(Date d)
    {
        int tIdx = getIdx(d);
        if(tIdx<0) tIdx=0;
        if(tIdx>tsize-1) tIdx = tsize-1;
      //  if(true) return iSim[tIdx];
        float output[] = new float[skuMap.size()];
        for(int i=0;i<output.length;i++)
        {
            int count = 0;
            for(int tt=tIdx-3;tt<=tIdx+3;tt++)
            {
                int tf = tt;
                if(tf<0)
                {
                    tf = 0;
                }
                if(tf>=tsize)
                {
                    tf=tsize-1;
                }
                output[i] += iSim[tf][i]*gaussian[count];
                count++;
            }
        }
        return output;
    }
    
    
}
