# Large Scale Hierarchical Text Classification

[Kaggle Competetion page](http://www.kaggle.com/c/lshtc)

## Comments
[Winning Solution Description](http://www.kaggle.com/c/lshtc/forums/t/7980/winning-solution-description)

### anttip (1st)

Our winning submission to the 2014 Kaggle competition for Large Scale Hierarchical Text Classification (LSHTC) consists mostly of an ensemble of sparse generative models extending Multinomial Naive Bayes. The base-classifiers consist of hierarchically smoothed models combining document, label, and hierarchy level Multinomials, with feature pre-processing using variants of TF-IDF and BM25. Additional diversification is introduced by different types of folds and random search optimization for different measures. The ensemble algorithm optimizes macroFscore by predicting the documents for each label, instead of the usual prediction of labels per document. Scores for documents are predicted by weighted voting of base-classifier outputs with a variant of Feature-Weighted Linear Stacking. The number of documents per label is chosen using label priors and thresholding of vote scores.

The full description .pdf file is attached, and the code can be downloaded from: [https://kaggle2.blob.core.windows.net/competitions/kaggle/3634/media/LSHTC4_winner_solution.zip](https://kaggle2.blob.core.windows.net/competitions/kaggle/3634/media/LSHTC4_winner_solution.zip)

The above code package includes precomputed result files for the base-classifiers used by our ensemble. These take close to 300MB. A package omitting the base-classifier output files is also available: [https://kaggle2.blob.core.windows.net/competitions/kaggle/3634/media/LSHTC4_winner_solution_omit_resultsfiles.zip](https://kaggle2.blob.core.windows.net/competitions/kaggle/3634/media/LSHTC4_winner_solution_omit_resultsfiles.zip)

### Alexander D'yakonov (2nd)

tf-idf + kNN
I didn't use the hierarchy...

### nagadomi (3rd)

My approach is based on Nearest Centroid Classifier.

I didn't use the hierarchy too...

[github:nagami/kaggle-lshtc](https://github.com/nagadomi/kaggle-lshtc)

### Dmitriy Anisimov (7th)

I implemented centroid approach described in [DH Lee - Multi-Stage Rocchio Classification for Large-scale Multi-labeled Text data](http://lshtc.iit.demokritos.gr/system/files/lshc3_lee.pdf). I thought about using hierarchy and even tried one idea, but it didn't give good results and I didn't experiment with it further.

### Julian de Wit (13th)

- I wrote my own custom kNN implementation to avoid memory/scaling problems.
- Removed features from trainset that were not in testset
- 1NN on train instances with as many features as possible. I had big problems finding a good multi category selector when doing kNN so that's why I chose 1NN.
- BM25 turned out to be the best similarity function for me.
- Later I did also 1NN on the powerset categories. The features in one (powerset) category are the averaged features of all train documents  in that (powerset) category. This classifier gave me a better score. 
- Then I "ensembled" the two classifiers by just taking a union of all predicted categories. That gave me another 0.007.

In the end the solution was pretty simple. But the whole process was one big learning experience for me since I started from scratch. I'm very interested how Alexander got such good results with kNN. I thought that the higher ranked players were all mixing a number of different models/algorithms. Also tf-idf did worse for me than bm25. However.. that could have been a bug in my code.. :S
