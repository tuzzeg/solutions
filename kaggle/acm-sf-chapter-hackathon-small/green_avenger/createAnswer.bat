
@echo %time%
set PATH="C:\Program Files (x86)\Java\jre7\bin":%PATH%
java -classpath ext/simmetrics_jar_v1_6_2_d07_02_07.jar;dist/DataMining.jar com.thegreenavenger.dm.bestBuy.BestBuyTest
@echo %time%
pause