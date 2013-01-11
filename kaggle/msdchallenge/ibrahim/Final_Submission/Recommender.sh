#!/bin/bash 
python Recommender.py;
echo "generating final file";
echo "==============================================================================================="
cat output1.txt output2.txt output3.txt output4.txt > submission_file.csv;
echo "Checking if file is valid......."
echo "==============================================================================================="
python ~/Desktop/validator.py submission_file.csv;
echo "Creating zip and adding to Desktop";
echo "==============================================================================================="
zip submission.zip submission_file.csv;
cp submission.zip ~/Desktop/ ;
echo "Good to go............"
echo "==============================================================================================="
