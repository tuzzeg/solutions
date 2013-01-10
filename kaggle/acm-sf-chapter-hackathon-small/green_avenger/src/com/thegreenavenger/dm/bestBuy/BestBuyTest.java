/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thegreenavenger.dm.bestBuy;

import com.thegreenavenger.dm.bestBuy.entry.EntryFactory;
import com.thegreenavenger.dm.common.BasicScore;
import com.thegreenavenger.dm.common.Score;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 *
 * @author David Thomas
 */
public class BestBuyTest {

    private final File train, test, xmlFile, runDir;
    private DataTables dt;
    private ScoringAlgorithm sa;

    public BestBuyTest(File train, File test, File runDir, File xmlFile) {
        this.train = train;
        this.test = test;
        this.xmlFile = xmlFile;
        this.runDir = runDir;
    }

    // preprocess various data tables needed
    public void preProcess() throws Exception {
        SkuMetaDataParser metaParser = new SkuMetaDataParser(xmlFile);
        dt = new DataTables(metaParser);
        dt.populate(train, runDir);
        dt.populateTest(test);


    }

    public void runTest(File answers) throws Exception {
        FileWriter fw = new FileWriter(answers);
        fw.write("sku\n");
        sa = new ScoringAlgorithm(dt, runDir);
        BufferedReader bis = new BufferedReader(new FileReader(test));
        int counter = 0;
        String line = bis.readLine();
        while ((line = bis.readLine()) != null) {
            if (counter % 100 == 0) {
                Logger.getLogger(getClass().getName()).info("Counter " + counter);
            }
            com.thegreenavenger.dm.bestBuy.entry.Entry entry = line.split(",").length == 6 ? EntryFactory.createEntry(line, counter++) : EntryFactory.createTestEntry(line, counter++);
            Score[] results = sa.getScores(entry);
            Arrays.sort(results);
            String answerLine = createLine(results);
            fw.write(answerLine + "\n");
        }
        fw.close();
        bis.close();
    }

    public int findSku(int idx, Score[] scores) {
        Arrays.sort(scores);
        int count = 5;
        for (int i = scores.length - 1; i > scores.length - 6; i--) {
            if (((BasicScore) scores[i]).getID() == idx) {
                return count;
            }
            count--;
        }
        return 0;
    }

    public String createLine(Score[] scores) {
        String line = new String("");
        for (int i = scores.length - 1; i > scores.length - 6; i--) {
            BasicScore bs = (BasicScore) scores[i];
            {
                String sku = dt.getSkuMap(bs.getID());
                line += (sku + " ");
            }
        }
        return line;
    }

    public static void main(String args[]) throws Exception {
        File runDir = new File("tableDir");
        runDir.mkdir();
        File file = new File("train.csv");
        File file2 = new File("test.csv");
        File answers = new File("answers.csv");
        File xmlFile = new File("small_product_data.xml");

        BestBuyTest bbt = new BestBuyTest(file, file2, runDir, xmlFile);
        bbt.preProcess();
        bbt.runTest(answers);
    }
}
