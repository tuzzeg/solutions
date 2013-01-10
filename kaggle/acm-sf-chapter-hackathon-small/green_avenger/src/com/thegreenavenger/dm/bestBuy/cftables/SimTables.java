/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thegreenavenger.dm.bestBuy.cftables;

import com.thegreenavenger.dm.bestBuy.entry.Entry;
import com.thegreenavenger.dm.bestBuy.cftables.CFTable;
import java.util.List;

/**
 *
 * @author David Thomas
 */
public class SimTables {
   
     private final List<CFTable> cfTables;
     public SimTables(List<CFTable> cfTables)
     {
         this.cfTables = cfTables;
     }
     public void create(List<Entry> entries)
     {
         for(Entry entry: entries)
         {
            for(CFTable cfTable: cfTables)
            {
                cfTable.addTrainData(entry);
            }
         }
     }
     
     public void initialize()
     {
        for(CFTable cfTable: cfTables)
        {
            cfTable.create();
        }
     }
     
    
}
