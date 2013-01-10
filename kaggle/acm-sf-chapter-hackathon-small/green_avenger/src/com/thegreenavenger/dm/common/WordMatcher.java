/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thegreenavenger.dm.common;

import com.thegreenavenger.dm.bestBuy.SkuMetaDataParser;
import com.thegreenavenger.dm.bestBuy.SkuMetaDataParser.MetaData;
import com.thegreenavenger.dm.common.SimFunctions;
import com.thegreenavenger.dm.common.WordCleaner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author David Thomas
 */
public class WordMatcher {
    private final HashMap<String,List<String>> dictionary = new HashMap<String,List<String>>();
    private final HashMap<String,String> closeMap = new HashMap<String,String>();
    private final SkuMetaDataParser metaParser;
    public WordMatcher(SkuMetaDataParser metaParser)
    {
        this.metaParser = metaParser;
        // any word in the catalog is put in the dictionary
        for(java.util.Map.Entry<String,MetaData> e: metaParser.getSkuMap().entrySet())
        {
            String words[] = WordCleaner.cleanLeaveSpaces(e.getValue().getName()).split("\\s+");
            for(String word:words)
            {
                List<String> skus = dictionary.get(word);
                if(skus==null)
                {
                    skus = new ArrayList<String>();
                    dictionary.put(word, skus);
                }
                skus.add(e.getKey());
            }
        }
    }
   
    
    public void addQuery(String query)
    {
        String words[] = WordCleaner.cleanLeaveSpaces(query).split("\\s+");
        for(String word: words)
        {
            if(dictionary.containsKey(word)) continue;
            if(closeMap.containsKey(word)) continue;
             int numbers[] = new int[10];
             for(int i=0;i<word.length();i++) { if(Character.isDigit(word.charAt(i))) { numbers[Integer.parseInt(""+word.charAt(i))]++; }}
  
            double bestScore = -1;
            String cword = null;
            for(String dword: dictionary.keySet())
            {
                int numbers2[] = new int[10];
                for(int i=0;i<dword.length();i++) { if(Character.isDigit(dword.charAt(i))) { numbers2[Integer.parseInt(""+dword.charAt(i))]++; }}
                boolean numMisMatch = false;
                for(int i=0;i<10;i++)
                {
                    if(numbers[i]!=numbers2[i]) numMisMatch = true;
                }
                if(numMisMatch) continue;
                double score = SimFunctions.lDistance(word, dword);
                if(score>bestScore)
                {
                    cword = dword;
                    bestScore = score;
                }
            }
            if(bestScore>=0.80) 
            {
                closeMap.put(word,cword);
 //               System.out.println("WordMatcher Matching " + word + " to " + cword + " " + bestScore);
            }
            
        }     
    }
    
    public String replace(String word)
    {
        if(closeMap.containsKey(word)) return closeMap.get(word);
        return word;
    }
    
}
