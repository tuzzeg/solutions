/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thegreenavenger.dm.bestBuy.cftables;

import com.thegreenavenger.dm.bestBuy.entry.Entry;
import com.thegreenavenger.dm.common.GaussianBlur;
import com.thegreenavenger.dm.common.StandardDeviation;
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
public class ItemTableTemporal implements CFTable {
     
    public HashMap<String, Integer> skuMap = new HashMap<String, Integer>();
    public HashMap<Integer, String> rSkuMap = new HashMap<Integer, String>();
    public HashMap<Integer, Integer> skuCount = new HashMap<Integer, Integer>();
   
    public final HashMap<Integer, List<Integer>> nZeroMap = new HashMap<Integer, List<Integer>>();
    public final HashMap<String,List<Entry>> ceMap;
    public final HashMap<String,List<String>> scMap = new HashMap<String,List<String>>();
    private int isize = 0;
    private int tsize = 0;
    public final long interval = 86400000;
    public final File cacheFile;
    private final Date min, max;
    private RandomAccessFile raf = null;

    public ItemTableTemporal(HashMap<String, Integer> skuMap,
            HashMap<Integer, String> rSkuMap,
            HashMap<Integer, Integer> skuCount,
            HashMap<String,List<Entry>> ceMap,
            Date min, Date max, File cacheFile) {
        this.skuMap = skuMap;
        this.rSkuMap = rSkuMap;
        this.skuCount = skuCount;
        this.ceMap = ceMap;
        this.min = min;
        this.max = max;
        this.cacheFile = cacheFile;
        tsize = (int)Math.ceil((max.getTime()-min.getTime())/(1.0*interval));
    
        isize = skuMap.size();
    }
    
       public int getIdx(Date d)
    {
        return (int)Math.floor((d.getTime()-min.getTime())/(1.0*interval));
    }

    @Override
    public void addTrainData(Entry e) {
        List<String> customers = scMap.get(e.getSku());
        if(customers==null)
        {
            customers = new ArrayList<String>();
            scMap.put(e.getSku(),customers);
        }
        if(customers.contains(e.getUser())==false)
        {
            customers.add(e.getUser());
        }
    }

    @Override
    public void create() {
       
        if (cacheFile.exists() == false) {
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(cacheFile);
            } catch (Exception e) {
                System.err.println("Could not open " + cacheFile.getName());
                System.exit(1);
            }
            
            for(int i=0;i<skuMap.size();i++)
            {
                float tSim[][] = new float[tsize][skuMap.size()];
                // find all customers for a particular sku
                List<String> customers = scMap.get(rSkuMap.get(i));
                if(customers==null) continue;
                // for each item of a customer, see if purchased close to this item
                for(String c: customers)
                {
                    List<Entry> entries = ceMap.get(c);
                    if(entries==null) continue;
                    // first find if the customer bought this item
                    Entry found = null;
                    for(Entry entry: entries)
                    {
                        if(entry.getSku().equalsIgnoreCase(rSkuMap.get(i)))
                        {
                            found = entry;
                            break;
                        }
                    }
                    if(found==null) continue;
                    for(Entry entry: entries)
                    {
                        if(skuMap.get(entry.getSku())==null) continue;
                        int elapsed = (int)Math.abs(found.getQueryDate().getTime()-entry.getQueryDate().getTime());
                        int sidx = skuMap.get(entry.getSku());
                        int count = skuCount.get(sidx);
                        int tidx = getIdx(found.getQueryDate());
                        if(tidx<0) tidx=0;
                        if(tidx>=tsize) tidx = tsize-1;
                        if(entry!=found && elapsed <= interval)
                        {
                            tSim[tidx][sidx]+=1/(1.0f*skuCount.get(sidx));
                        }
                    }
                        
                }
                
                for (int t = 0; t < tSim.length; t++) {
                    StandardDeviation std = new StandardDeviation();
                    for (int j = 0; j < skuMap.size(); j++) {
                        std.add(tSim[t][j]);
                    }
                    StandardDeviation.Values values = std.values();
                    double total = values.mean * values.count;
                    if (total == 0) {
                        continue;
                    }
                    for (int j = 0; j < skuMap.size(); j++) {
                        tSim[t][j] /= (1.0 * total);
                    }
                }
                 try { storeFC(tSim,out); }
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
        
    private static void storeFC(float[][] floats,FileOutputStream out) throws Exception {
 
   
    byte[] array = new byte[4 * floats.length*floats[0].length];
    ByteBuffer buf = ByteBuffer.wrap(array);
    for (int t=0;t<floats.length;t++) {
        for(int s=0;s<floats[t].length;s++)
        {
            buf.putFloat(floats[t][s]); 
        }
     
    }
    out.write(array);
    
}
    public float[] getArray(Integer sku, Date d) throws Exception
    {
        int blockSize = tsize*skuMap.size()*4;
     
        Integer tidx = getIdx(d);
 
        if(tidx>=tsize) tidx = tsize-1;
        if(tidx<0) tidx=0;
        raf.seek(sku*blockSize);
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
}
