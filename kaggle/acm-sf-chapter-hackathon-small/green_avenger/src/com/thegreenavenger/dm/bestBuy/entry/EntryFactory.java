/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thegreenavenger.dm.bestBuy.entry;

import java.text.SimpleDateFormat;

/**
 *
 * @author David Thomas
 */
public class EntryFactory {
    
    public static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    public static Entry createEntry(String line, int entry) throws Exception
    {
        String tokens[] = line.split(",");
        tokens[4] = tokens[4].replaceAll("\"", "");
        tokens[5] = tokens[5].replaceAll("\"","");
        if(tokens[4].contains("\\.")==false) tokens[4]+=".000";
        if(tokens[5].contains("\\.")==false) tokens[5]+=".000";
        return new BasicEntry(tokens[0],
                              tokens[1],
                              tokens[2],
                              tokens[3].replaceAll("\"", ""),
                              formatter.parse(tokens[4]),
                              formatter.parse(tokens[5]),
                              entry
                );
    }
    
     public static Entry createTestingEntry(String line, int entry) throws Exception
    {
        String tokens[] = line.split(",");
        tokens[4] = tokens[4].replaceAll("\"", "");
        tokens[5] = tokens[5].replaceAll("\"","");
        if(tokens[4].contains("\\.")==false) tokens[4]+=".000";
        if(tokens[5].contains("\\.")==false) tokens[5]+=".000";
        return new BasicEntry(tokens[0],
                              "-1",
                              tokens[2],
                              tokens[3].replaceAll("\"", ""),
                              formatter.parse(tokens[4]),
                              formatter.parse(tokens[5]),
                              entry
                );
    }
    
     public static Entry createTestEntry(String line, int entry) throws Exception
    {
        String tokens[] = line.split(",");
        tokens[3] = tokens[3].replaceAll("\"", "");
        tokens[4] = tokens[4].replaceAll("\"","");
        if(tokens[3].contains("\\.")==false) tokens[3]+=".000";
        if(tokens[4].contains("\\.")==false) tokens[4]+=".000";
        return new BasicEntry(tokens[0],
                              new String("-1"),
                              tokens[1],
                              tokens[2].replaceAll("\"", ""),
                              formatter.parse(tokens[3]),
                              formatter.parse(tokens[4]),
                              entry
                );
    }
}
