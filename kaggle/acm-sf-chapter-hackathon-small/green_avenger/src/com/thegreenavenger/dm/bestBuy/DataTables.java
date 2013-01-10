/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thegreenavenger.dm.bestBuy;


import com.thegreenavenger.dm.common.WordMatcher;
import com.thegreenavenger.dm.common.QueryMatcher;
import com.thegreenavenger.dm.bestBuy.entry.Entry;
import com.thegreenavenger.dm.bestBuy.entry.EntryFactory;
import com.thegreenavenger.dm.bestBuy.cftables.CFTable;
import com.thegreenavenger.dm.bestBuy.cftables.ItemTable;
import com.thegreenavenger.dm.bestBuy.cftables.ItemTableTemporal;
import com.thegreenavenger.dm.bestBuy.cftables.PopTable;
import com.thegreenavenger.dm.bestBuy.cftables.QueryTable;
import com.thegreenavenger.dm.bestBuy.cftables.QueryTableTemporal;
import com.thegreenavenger.dm.bestBuy.cftables.SimTables;
import com.thegreenavenger.dm.bestBuy.cftables.TimeTable;
import com.thegreenavenger.dm.bestBuy.cftables.WordTable;
import com.thegreenavenger.dm.bestBuy.cftables.WordTableTemporal;
import com.thegreenavenger.dm.common.WordCleaner;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author David Thomas
 */
public class DataTables {

    // maps words to entries
    private final HashMap<String, List<Entry>> wordMap = new HashMap<String, List<Entry>>();
    // maps words to skus
    private final HashMap<String, List<String>> wsMap = new HashMap<String, List<String>>();
    // makes sku string to zero-based index
    private final HashMap<String, Integer> skuMap = new HashMap<String, Integer>();
    // reverse of above
    private final HashMap<Integer, String> rSkuMap = new HashMap<Integer, String>();
    // sku idx mapped to count of sku item
    private final HashMap<Integer, Integer> skuCount = new HashMap<Integer, Integer>();
    // customer to sku map
    private final HashMap<String, List<Integer>> cMap = new HashMap<String, List<Integer>>();
    // sku to customer map
    private final HashMap<Integer, List<String>> iMap = new HashMap<Integer, List<String>>();
    // customer to item map
    public final HashMap<String, List<Entry>> ceMap = new HashMap<String, List<Entry>>();
    // word size to word list map
    private final HashMap<Integer, List<String>> wSize = new HashMap<Integer, List<String>>();
    // all entries in training set
    private final List<Entry> allEntries = new ArrayList<Entry>();
    // parser for product catalog
    private final SkuMetaDataParser metaParser;
    // min and max dates of entries
    private Date min, max;
    // log directory for files
    private File runDir;
    // keeps list of words in product catalog, matches most similar typos
    private final WordMatcher wordMatcher;
    private final QueryMatcher queryMatcher;
    // word to sku table
    WordTable wt;
    // item to item table
    ItemTable it;
    // query to sku table
    QueryTable qt;
    // popularity table time shifted
    TimeTable tt;
    // query to sku time-based table
    QueryTableTemporal qtt;
    // word to sku time-based table
    WordTableTemporal wtt;
    // item to item time-based table
    ItemTableTemporal itt;
    // overall popularity table
    PopTable pt;
    // interval used for temporal based data
    public static final Long interval = 86400000L;

    public int skuSize() {
        return skuMap.size();
    }

    public boolean containsCustomer(String customer) {
        return ceMap.containsKey(customer);
    }

    public float[] getTimeShiftedPopularityArray(Date d) {
        return pt.getArray(d);
    }

    public String getSkuMap(Integer i) {
        return rSkuMap.get(i);
    }

    public List<Entry> getCustomerEntries(String customer) {
        return ceMap.get(customer);
    }

    public boolean containsSku(String sku) {
        return skuMap.containsKey(sku);
    }

    public Integer getSkuMap(String sku) {
        return skuMap.get(sku);
    }

    public float[] getTimeShiftedItemSimArray(Integer idx, Date d) throws Exception {
        return itt.getArray(idx, d);
    }

    public float[] getItemSimArray(Integer idx) {
        return it.iSim[idx];
    }

    public float[] getTimeShiftArray(Date d) {
        return tt.tSim[tt.getIdx(d)];
    }

    public Integer getWordMap(String word) {
        return wt.qMatchMap.get(word);
    }

    public Set<java.util.Map.Entry<String, Integer>> getWordMapSet() {
        return wt.qMatchMap.entrySet();
    }

    public float[] getTimeShiftedWordArray(String word, Date d) throws Exception {
        return wtt.getArray(word, d);
    }

    public float[] getWordArray(Integer idx) {
        return wt.wSim[idx];
    }

    public Integer getQueryMap(String word) {
        return qt.qMatchMap.get(word);
    }

    public List<String> getWordsOfSize(int size) {
        return wSize.get(size);
    }

    public float[] getQueryArray(Integer idx) {
        return qt.wSim[idx];
    }

    public float[] getTimeShiftedQueryArray(String query, Date d) throws Exception {
        return qtt.getArray(query, d);
    }

    public DataTables(SkuMetaDataParser metaParser) {
        this.metaParser = metaParser;
        WordCleaner.wordMatcher = wordMatcher = new WordMatcher(metaParser);
        WordCleaner.queryMatcher = queryMatcher = new QueryMatcher(metaParser);
    }

    public void populate(File train, File runDir) throws Exception {
        Logger.getLogger(getClass().getName()).info("reading train data");
        this.runDir = runDir;
        int skuc = 0;
        for (java.util.Map.Entry<String, SkuMetaDataParser.MetaData> e : metaParser.getSkuMap().entrySet()) {
            String sku = e.getKey();
            Integer skuIdx = skuMap.get(sku);
            if (skuIdx == null) {
                skuIdx = skuc++;
                skuMap.put(sku, skuIdx);
                rSkuMap.put(skuIdx, sku);
            }
            //-------------------------------------
            Integer val = skuCount.get(skuIdx);
            if (val == null) {
                val = 0;
                skuCount.put(skuIdx, val);
            }
        }
        BufferedReader bis = new BufferedReader(new FileReader(train));
        String line = bis.readLine();

        int count = 0;
        //   Date min=null,max=null;

        while ((line = bis.readLine()) != null) {
            Entry entry = EntryFactory.createEntry(line, count++);
            allEntries.add(entry);
            //----------------------------------------
            wordMatcher.addQuery(entry.getQuery());
            queryMatcher.addQuery(entry.getQuery());
            if (min == null) {
                min = entry.getClickDate();
                max = entry.getClickDate();
            } else if (entry.getClickDate().before(min)) {
                min = entry.getClickDate();
            } else if (entry.getClickDate().after(max)) {
                max = entry.getClickDate();
            }
            //----------------------------------------

            Integer skuIdx = skuMap.get(entry.getSku());
            if (skuIdx == null) {
                skuIdx = skuc++;
                skuMap.put(entry.getSku(), skuIdx);
                rSkuMap.put(skuIdx, entry.getSku());
            }

            //-------------------------------------
            Integer val = skuCount.get(skuIdx);
            if (val == null) {
                val = 0;
                skuCount.put(skuIdx, val);
            }
            skuCount.put(skuIdx, val + 1);

            //-----------------------------------------
            String[] cleanedTokens = WordCleaner.cleanTokens(entry.getQuery());
            for (String cleaned : cleanedTokens) {

                List<Entry> elist = wordMap.get(cleaned);
                if (elist == null) {
                    elist = new ArrayList<Entry>();
                    wordMap.put(cleaned, elist);
                }
                elist.add(entry);
                List<String> skulist = wsMap.get(cleaned);
                if (skulist == null) {
                    skulist = new ArrayList<String>();
                    wsMap.put(cleaned, skulist);
                }
                if (skulist.contains(entry.getSku()) == false) {
                    skulist.add(entry.getSku());
                }
            }

            List<Integer> items = cMap.get(entry.getUser());
            if (items == null) {
                items = new ArrayList<Integer>();
                cMap.put(entry.getUser(), items);
            }
            items.add(skuIdx);
            //------------------------------------------
            List<Entry> centries = ceMap.get(entry.getUser());
            if (centries == null) {
                centries = new ArrayList<Entry>();
                ceMap.put(entry.getUser(), centries);
            }
            centries.add(entry);
            //-----------------------------------------
            List<String> customers = iMap.get(skuIdx);
            if (customers == null) {
                customers = new ArrayList<String>();
                iMap.put(skuIdx, customers);
            }
            customers.add(entry.getUser());
        }

        bis.close();
        Logger.getLogger(getClass().getName()).info("finished reading train data");
    }

    public void populateTest(File test) throws Exception {
        Logger.getLogger(getClass().getName()).info("reading test data");
        BufferedReader bis = new BufferedReader(new FileReader(test));
        String line = bis.readLine();
        int counter = 0;

        while ((line = bis.readLine()) != null) {
            com.thegreenavenger.dm.bestBuy.entry.Entry entry = line.split(",").length == 6 ? EntryFactory.createTestingEntry(line, counter++) : EntryFactory.createTestEntry(line, counter++);
            List<Entry> centries = ceMap.get(entry.getUser());
            if (centries == null) {
                centries = new ArrayList<Entry>();
                ceMap.put(entry.getUser(), centries);
            }
            centries.add(entry);
            wordMatcher.addQuery(entry.getQuery());
            queryMatcher.addQuery(entry.getQuery());
        }

        bis.close();

        Logger.getLogger(getClass().getName()).info("creating data tables");
        wt = new WordTable(skuMap, rSkuMap, skuCount, wordMap, metaParser);
        it = new ItemTable(skuMap, rSkuMap, skuCount, ceMap);
        qt = new QueryTable(skuMap, rSkuMap, skuCount, metaParser);
        tt = new TimeTable(skuMap, rSkuMap, skuCount, min, max);
        qtt = new QueryTableTemporal(skuMap, rSkuMap, skuCount, metaParser, new File(runDir,"qt.bin"), min, max);
        wtt = new WordTableTemporal(skuMap, rSkuMap, skuCount, metaParser, new File(runDir,"wt.bin"), min, max);
        itt = new ItemTableTemporal(skuMap, rSkuMap, skuCount, ceMap, min, max, new File(runDir,"i2i.bin"));
        pt = new PopTable(skuMap, rSkuMap, skuCount, min, max);
        List<CFTable> tables = new ArrayList<CFTable>();
        tables.add(it);
        tables.add(wt);
        tables.add(wtt);
        tables.add(qt);
        tables.add(itt);
        tables.add(qtt);
        tables.add(tt);
        tables.add(pt);
        SimTables st = new SimTables(tables);
        st.create(allEntries);
        st.initialize();

        for (java.util.Map.Entry<String, Integer> we : wt.qMatchMap.entrySet()) {
            List<String> words = wSize.get(we.getKey().length());
            if (words == null) {
                words = new ArrayList<String>();
                wSize.put(we.getKey().length(), words);
            }
            words.add(we.getKey());
        }
        Logger.getLogger(getClass().getName()).info(" finished reading test data");
    }
}
