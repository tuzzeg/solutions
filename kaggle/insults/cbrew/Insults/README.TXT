README 
------

Chris Brew (cbrew@acm.org, joshnk at Kaggle)

General approach
=================

Character n-grams plus carefully tuned SGD using elastic net. Character n-grams because
regular linguistic processing is compromised by weird spelling and grammar. SGD because
it works well with sparse features and can be tuned to give good results.

Data files
==========

Place the .csv file that you want to have results for in Data/Inputs/final.csv.

Data/Inputs/fulltrain.csv is the concatenation of the two labeled files that
Kaggle provided.

The code
========

insults.py is a single Python file running the whole process. 

What this does is to train on "fulltrain.csv" and test on "final.csv". It uses a range of values for 
SGD's alpha parameter

show.py is a utility for seeing the learning curve that I used to select a training regime.
score.py spits out some stats for the various submissions.

Submission plan
---------------

Will generate 21 (7 for each penalty type) models using the commands above, then select the five having the highest cross-validation scores.