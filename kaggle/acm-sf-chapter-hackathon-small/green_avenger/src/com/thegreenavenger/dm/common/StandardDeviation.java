/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.thegreenavenger.dm.common;

import java.nio.ByteBuffer;

/**
 *
 * @author dthomas
 */
public class StandardDeviation {
    public static class Values
    {
        public final double mean, stddev,min,max;
        public final int count;
        public Values(double mean,double stddev, double min, double max, int count)
        {
            this.mean = mean;
            this.stddev = stddev;
            this.count = count;
            this.min = min;
            this.max = max;
        }
        public String toString()
        {
            return "" + stddev + "," + mean + "," + count + "," + min + "," + max; 
        }
    }
  
    double mean = 0;
    double total = 0;
    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;
    int count = 0;
    
    private boolean changed = false;
    public void add(double f)
    {
            total+=f*f;
            mean+=f;
            count++;
            if(f<min) min = f;
            if(f>max) max = f;
            changed = true;
              
    }
    
    private Values cacheValue = null;
    
    public Values values()
    {
        if(changed || cacheValue==null)
        {
             double mn = mean/(1.0*count);
             double std = Math.sqrt( (1.0/count)*total-(mn*mn));       
             cacheValue =  new Values(mn,std,min,max,count); 
             changed = false;
        }
       return cacheValue;
    }
    
  
   
    
   
}
