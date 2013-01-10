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
import com.thegreenavenger.dm.common.GaussianBlur;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author David Thomas
 */
public class WordTableTemporal implements CFTable {
     
    public HashMap<String, Integer> skuMap = new HashMap<String, Integer>();
    public HashMap<Integer, String> rSkuMap = new HashMap<Integer, String>();
    public HashMap<Integer, Integer> skuCount = new HashMap<Integer, Integer>();
  
   
    public final HashMap<String, Integer> qMatchMap = new HashMap<String, Integer>();
    public final HashMap<Integer, String> rqMatchMap = new HashMap<Integer, String>();
    public final StandardDeviation qSimStats = new StandardDeviation();
    public final HashMap<Integer, List<Integer>> nZeroMap = new HashMap<Integer, List<Integer>>();
    public final HashMap<String,List<String>> queries = new HashMap<String,List<String>>();
    public final HashMap<String,List<Entry>> qEntries = new HashMap<String,List<Entry>>();
    public final HashMap<String,Integer> qCount = new HashMap<String,Integer>();
    public final SkuMetaDataParser metaParser;
    private int qsize = 0;
    private int tsize = 0;
    public final long interval = 86400000;
    public final File cacheFile;
    private final Date min, max;
    private RandomAccessFile raf = null;
    public WordTableTemporal(HashMap<String, Integer> skuMap,
            HashMap<Integer, String> rSkuMap,
            HashMap<Integer, Integer> skuCount,
            SkuMetaDataParser metaParser,File cacheFile,
            Date min, Date max) {
        this.skuMap = skuMap;
        this.rSkuMap = rSkuMap;
        this.skuCount = skuCount;
        qsize = 0;
        int count = 0;
        this.metaParser = metaParser;
        this.cacheFile = cacheFile;
        this.min = min;
        this.max = max;
        tsize = (int)Math.ceil((max.getTime()-min.getTime())/(1.0*interval));
    
        for(java.util.Map.Entry<String,MetaData> me: metaParser.getSkuMap().entrySet())
        {
            {
                String tokens[] = WordCleaner.cleanTokens(me.getValue().getName());
                for(String q1: tokens)
                {
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
            }
      
        }
     
    }

    @Override
    public void addTrainData(Entry e) {
       String tokens[] = WordCleaner.cleanTokens(e.getQuery());
        
       for(String q1: tokens)
       {
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
    }
      public int getIdx(Date d)
    {
        return (int)Math.ceil((d.getTime()-min.getTime())/(1.0*interval));
    }
    @Override
    public void create() {
        // for each query, store an array of time vs sku
  //      System.out.println(qsize + " " + skuMap.size() + " " + tsize);
        long pos = 0;
        if(cacheFile.exists()==false)
        {
            FileOutputStream out = null;
            try { out = new FileOutputStream(cacheFile); }
            catch(Exception e) { System.err.println("Could not open " + cacheFile.getName()); System.exit(1); }
            for(int q=0;q<qsize;q++)
            {
                String query = rqMatchMap.get(q);
                float tSim[][] = new float[tsize][skuMap.size()];
                if(qEntries.containsKey(query))
                {
                    for(Entry e: qEntries.get(query))
                    {
                        Integer idx = skuMap.get(e.getSku());
                        Integer tIdx = getIdx(e.getClickDate());
                        if(tIdx>=tsize) tIdx = tsize-1;
                        tSim[tIdx][idx]+=1.0/(1.0*qCount.get(query));

                    }
                    for(int t=0;t<tsize;t++)
                    {
                        StandardDeviation std = new StandardDeviation();
                        for(int s=0;s<skuMap.size();s++)
                        {
                            std.add(tSim[t][s]);
                        }
                        Values values = std.values();
                        double total = values.count*values.mean;
                        if(total==0) continue;
                         for(int s=0;s<skuMap.size();s++)
                        {
                            tSim[t][s]/=(1.0*total);
                        }
                    }
                }
                try { storeFC(tSim,out,pos+=(tsize*skuMap.size()*4)); }
                catch(Exception e)
                {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
            try { out.close(); 
            raf = new RandomAccessFile(cacheFile,"r");
            }
            catch(Exception e)
            {}
            
        }
         try { 
            raf = new RandomAccessFile(cacheFile,"r");
            }
            catch(Exception e){}   
           gaussian = GaussianBlur.buildArray(4,0);
    }
    
    public double gaussian[] = new double[] { .006,.061,.242,.383,.242,.061,.006};
    public float[] getArray(String query, Date d) throws Exception
    {
        int blockSize = tsize*skuMap.size()*4;
        Integer q = qMatchMap.get(query);
        Integer tidx = getIdx(d);
        if(q==null) return null;
        if(tidx>=tsize) tidx = tsize-1;
        if(tidx<0) tidx=0;
        raf.seek(q*blockSize);
        byte buffer[] = new byte[blockSize];
        raf.read(buffer);
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        float tSim[][] = new float[tsize][skuMap.size()];
        for(int t=0;t<tsize;t++)
        {
             for(int s=0;s<skuMap.size();s++)
            {
                tSim[t][s] = bb.getFloat();
            }
        }
        float output[] = new float[skuMap.size()];
        for(int i=0;i<output.length;i++)
        {
            int count = 0;
            for(int tt=tidx-3;tt<=tidx+3;tt++)
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
                output[i] += tSim[tf][i]*gaussian[count];
                count++;
            }
        }
        return output;
    }
    
  private static void storeFC(float[][] floats,FileOutputStream out,long pos) throws Exception {
 
   
    byte[] array = new byte[4 * floats.length*floats[0].length];
    ByteBuffer buf = ByteBuffer.wrap(array);
    for (int t=0;t<floats.length;t++) {
        for(int s=0;s<floats[t].length;s++)
        {
            buf.putFloat(floats[t][s]); 
        }
     
    }
    out.write(array);
   // file.close();
   
}


    
   
    

    
}
