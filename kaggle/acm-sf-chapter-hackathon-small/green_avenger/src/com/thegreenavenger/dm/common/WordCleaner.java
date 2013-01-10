/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thegreenavenger.dm.common;

/**
 *
 * @author David Thomas
 */
public class WordCleaner {
    
    public static WordMatcher wordMatcher = null;
    public static QueryMatcher queryMatcher = null;
    public static String cleanLeaveSpaces(String word)
    {
       String cleaned = word.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]+","");
       if(queryMatcher!=null)
       {
           cleaned = queryMatcher.replace(cleaned);
       }
        return cleaned;
    
    }
    public static String clean(String word)
    {
        String cleaned = word.toLowerCase().replaceAll("[^a-zA-Z0-9]+","");
       if(queryMatcher!=null)
       {
           cleaned = queryMatcher.replace(cleaned);
       }
        return cleaned;
    }
    public static String[] cleanTokens(String word)
    {
        String cleaned[] = cleanLeaveSpaces(word).split("\\s+");
        String cleanedOut[] = new String[cleaned.length];
         if(wordMatcher!=null)
       {
           int count = 0;
           for(String w:cleaned)
           {
              cleanedOut[count++] = w.replace(w, wordMatcher.replace(w));
           }
           return cleanedOut;
       }
         return cleaned;
    }
   /* 
    public static String cleanLeaveSymbols(String word)
    {
         String cleaned = word.toLowerCase().replaceAll(":"," : ").replaceAll("-", " - ").replaceAll("\'"," \' ").replaceAll("[^a-zA-Z0-9\\s]+","");
          return cleaned;
    }
    */
   
}
