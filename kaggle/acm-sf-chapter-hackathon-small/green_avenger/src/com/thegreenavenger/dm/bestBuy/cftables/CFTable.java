/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thegreenavenger.dm.bestBuy.cftables;

import com.thegreenavenger.dm.bestBuy.entry.Entry;

/**
 *
 * @author David Thomas
 */
public interface CFTable {
    public void addTrainData(Entry entry);
    public void create();
}
