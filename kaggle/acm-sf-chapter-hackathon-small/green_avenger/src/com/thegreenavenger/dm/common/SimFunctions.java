/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thegreenavenger.dm.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import uk.ac.shef.wit.simmetrics.similaritymetrics.CosineSimilarity;
import uk.ac.shef.wit.simmetrics.similaritymetrics.EuclideanDistance;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaccardSimilarity;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Soundex;

/**
 *
 * @author David Thomas
 */
public class SimFunctions {
        private static int minimum(int a, int b, int c) {
                return Math.min(Math.min(a, b), c);
        }
 
        public static int computeLevenshteinDistance(CharSequence str1,
                        CharSequence str2) {
                int[][] distance = new int[str1.length() + 1][str2.length() + 1];
 
                for (int i = 0; i <= str1.length(); i++)
                        distance[i][0] = i;
                for (int j = 1; j <= str2.length(); j++)
                        distance[0][j] = j;
 
                for (int i = 1; i <= str1.length(); i++)
                        for (int j = 1; j <= str2.length(); j++)
                                distance[i][j] = minimum(
                                                distance[i - 1][j] + 1,
                                                distance[i][j - 1] + 1,
                                                distance[i - 1][j - 1]
                                                                + ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0
                                                                                : 1));
 
                return distance[str1.length()][str2.length()];
        }
        
        public static double cosineDiff(CharSequence str1, CharSequence str2)
        {
            double dotProduct = 0;
            double magA = 0;
            double magB = 0;
            HashMap<Character,Integer> countA = new HashMap<Character,Integer>();
            HashMap<Character,Integer> countB = new HashMap<Character,Integer>();
            for (int i = 0; i < str1.length(); i++)
            {
                Integer c = countA.get(str1.charAt(i));
                if(c==null)
                {
                    countA.put(str1.charAt(i),1);
                }
                else
                {
                    countA.put(str1.charAt(i),c+1);
                }
            }
              for (int i = 0; i < str2.length(); i++)
            {
                Integer c = countB.get(str2.charAt(i));
                if(c==null)
                {
                    countB.put(str2.charAt(i),1);
                }
                else
                {
                    countB.put(str2.charAt(i),c+1);
                }
            }
            for(Entry<Character,Integer> e: countA.entrySet())
            {
                if(Character.isWhitespace(e.getKey())) continue;
                Integer c1 = e.getValue();
                Integer c2 = countB.get(e.getKey());
                countB.remove(e.getKey());
                if(c2==null) c2 = 0;
                dotProduct+=(c1*c2);
                magA+=(c1*c1);
                magB+=(c2*c2);
            }
            for(Entry<Character,Integer> e: countB.entrySet())
            {
                if(Character.isWhitespace(e.getKey())) continue;
                Integer c2 = e.getValue();
                magB+=(c2*c2);
            }
            if(magA==0||magB==0) return 0;
            return dotProduct/(Math.sqrt(magA)*Math.sqrt(magB));
        }
        
         public static double cosineDiff(double a[], double b[])
        {
            double dotProduct = 0;
            double magA = 0;
            double magB = 0;
            for(int i=0;i<a.length;i++)
            {
                double c1 = a[i];
                double c2 = b[i];
                dotProduct+=(c1*c2);
                magA+=(c1*c1);
                magB+=(c2*c2);
            }
        
            if(magA==0||magB==0) return 0;
            return dotProduct/(Math.sqrt(magA)*Math.sqrt(magB));
        }
         
         
          public static double cosineDiff(float a[], float b[])
        {
            double dotProduct = 0;
            double magA = 0;
            double magB = 0;
            for(int i=0;i<a.length;i++)
            {
                double c1 = a[i];
                double c2 = b[i];
                dotProduct+=(c1*c2);
                magA+=(c1*c1);
                magB+=(c2*c2);
            }
        
            if(magA==0||magB==0) return 0;
            return dotProduct/(Math.sqrt(magA)*Math.sqrt(magB));
        }
          
            public static double cosineDiff(Score a[], float b[])
        {
            double dotProduct = 0;
            double magA = 0;
            double magB = 0;
            for(int i=0;i<a.length;i++)
            {
                double c1 = a[i].get();
                double c2 = b[i];
                dotProduct+=(c1*c2);
                magA+=(c1*c1);
                magB+=(c2*c2);
            }
        
            if(magA==0||magB==0) return 0;
            return dotProduct/(Math.sqrt(magA)*Math.sqrt(magB));
        }
     
       public static double cosineDiff(double a[], List<Integer> ais, double b[], List<Integer> bis)
        {
            List<Integer> as = new ArrayList<Integer>();
            as.addAll(ais);
            List<Integer> bs = new ArrayList<Integer>();
            bs.addAll(bis);
            double dotProduct = 0;
            double magA = 0;
            double magB = 0;
            for(Integer i:as)
            {
                double c2 = 0;
                if(bs.contains(i))
                {
                     c2 = b[i];
                }
                double c1 = a[i];
                magA+=(c1*c1);
                magB+=(c2*c2);
                dotProduct+=(c1*c2);
            }
            bs.removeAll(as);
            for(Integer i: bs)
            {
                double c2 = b[i];
                magB+=(c2*c2);
            }
        
            if(magA==0||magB==0) return 0;
            return dotProduct/(Math.sqrt(magA)*Math.sqrt(magB));
        }
       
        
        public static int computeLevenshteinDistance(String str1, String str2, int add, int delete, int substitute) {
                return computeLevenshteinDistance(str1.toCharArray(), str2.toCharArray(), add, delete, substitute);
        }

        private static int computeLevenshteinDistance(char [] str1, char [] str2, int insert, int delete, int substitute) {
                int [][]distance = new int[str1.length+1][str2.length+1];

                for(int i = 0; i <= str1.length; i++)
                        distance[i][0] = i * delete;    // non-weighted algorithm doesn't take Delete weight into account
                for(int j = 0; j <= str2.length; j++)
                        distance[0][j] = j * insert;    // non-weighted algorithm doesn't take Insert weight into account
                for(int i = 1; i <= str1.length; i++)
                {
                        for(int j = 1; j <= str2.length; j++)
                        { 
                                distance[i][j]= minimum(distance[i-1][j] + delete,      // would be +1 instead of +delete
                                                distance[i][j-1] + insert,                                      // would be +1 instead of +insert
                                                distance[i-1][j-1] + ((str1[i-1] == str2[j-1]) ? 0 : substitute));      // would be 1 instead of substitute
                        }
                }
                return distance[str1.length][str2.length];
             
        }
        
        public static double multiDistance(String str1, String str2)
        {
            float value = 0;
            value+=new Levenshtein().getSimilarity(str1, str2);
            value+=new CosineSimilarity().getSimilarity(str1, str2);
            value+=new JaccardSimilarity().getSimilarity(str1,str2);
            value+=new EuclideanDistance().getSimilarity(str1, str2);
            value+=new JaroWinkler().getSimilarity(str1, str2);
            value+=new Soundex().getSimilarity(str1, str2);
            return value/=6.0f;
        }
        
        public static double lDistance(String str1, String str2)
        {
            return new Levenshtein().getSimilarity(str1,str2);
        }
    
}