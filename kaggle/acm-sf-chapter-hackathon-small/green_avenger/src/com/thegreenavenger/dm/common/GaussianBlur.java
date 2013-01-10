/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thegreenavenger.dm.common;

/**
 *
 * @author David Thomas
 */
public class GaussianBlur {
    public static double[] buildArray(int steps, double min)
    {
        double[] array = new double[steps*2-1];
        StandardDeviation std = new StandardDeviation();
        for(int i=0;i<steps;i++)
            std.add(i*steps);
        double s2 = std.values().stddev;
        std = new StandardDeviation();
        for(int i=0;i<steps;i++)
        {
            array[i]= (1/(Math.sqrt(2*Math.PI*s2)*Math.exp((-i*i)/(2*s2))));
            std.add(array[i]);
        }
        double total = std.values().mean*std.values().count;
        for(int i=0;i<steps;i++)
        {
            array[i]/=total;
            if(array[i]<min) array[i] = min;
        }
        
        int max = steps*2-1;
        int count = 2;
        for(int i=steps;i<max;i++)
        {
            array[i] = array[steps-count++];
        }
        
        return array;
    }
    
    public static void main(String args[])
    {
        for(double f: buildArray(4,0))
        {
            System.out.println(f);
        }
    }
}
