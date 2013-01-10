/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thegreenavenger.dm.bestBuy.entry;

import java.util.Date;

/**
 *
 * @author David Thomas
 */
public class BasicEntry implements Entry{

    private final String user;
    private final String sku;
    private final String cat;
    private final String query;
    private final Date clickDate;
    private final Date queryDate;
    private final int uid;
    public BasicEntry(String user,
                      String sku,
                      String cat,
                      String query,
                      Date clickDate,
                      Date queryDate,
                      int uid)
    {
        this.user = user;
        this.sku = sku;
        this.cat = cat;
        this.query = query;
        this.clickDate = clickDate;
        this.queryDate = queryDate;
        this.uid = uid;
    }
    @Override
    public String getUser() { return user; }
    @Override
    public String getSku() { return sku; }
    @Override
    public String getCat() { return cat; }
    @Override
    public String getQuery() { return query; }
    @Override
    public Date getClickDate() { return clickDate; }
    @Override
    public Date getQueryDate() { return queryDate; }
    @Override
    public int getUID() { return uid; }
    
}
