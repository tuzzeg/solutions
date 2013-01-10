David Thomas (kaggle username: Green Avenger)
Developed for the Data Mining Hackathon on (20 mb) Best Buy mobile web site - ACM SF Bay Area Chapter
https://www.kaggle.com/c/acm-sf-chapter-hackathon-small/
This code generates an answer with a Private score of 0.78921 and Public Score of 0.78800


This code was developed as a Netbeans 7.2 project using jdk 1.7 on Windows 7.
The code uses simmetrics_jar_v_6_2_d07_02_07 for word comparison functions.  This jar is
covered under a GPL license. It was downloaded from http://sourceforge.net/projects/simmetrics/


First, copy test.csv, train.csv, and small_product_data.xml into the BestBuy directory.

The code is delivered with the dist/DataMining.jar already compiled, to recompile open in 
netbeans and compile it. The compilation creates a non-executable jar.  

To run on Windows, double-click the createAnswer.bat file (you will need java in your path or modify the bat)
At the start of running, the program will create some binary table files in tableDir. 
i2i.bin 61,907,048 bytes
qt.bin 660,291,880 bytes
wt.bin 355,103,624 bytes

This will only be done the first time you run.

Code Notes
====================================================
The main class is com.thegreenavenger.dm.bestBuy.BestBuyTest
The paths for train.csv, test.csv, answers.csv, and small_product_data.xml are hardcoded here

BestBuyTest first calls member function preProcess() which creates all the data tables needed to run.
The member function runTest() creates the answer file

The DataTables class handles all the data tables and has maps for accessing their indices

The ScoringAlgorithm class generates a score per entry from the test file using the member function getScores()
getScores() combines several different scorings using different weights

