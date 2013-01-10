/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thegreenavenger.dm.bestBuy.entry;

import com.thegreenavenger.dm.common.TestCase;
import java.util.Date;

/**
 *
 * @author David Thomas
 */
public interface Entry extends TestCase {
    public String getUser();
    public String getSku();
    public String getCat();
    public String getQuery();
    public Date getClickDate();
    public Date getQueryDate();
    public int getUID();
}
