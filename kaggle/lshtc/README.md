# Large Scale Hierarchical Text Classification

[Kaggle Competetion page](http://www.kaggle.com/c/lshtc)

## Comments
[Winning Solution Description](http://www.kaggle.com/c/lshtc/forums/t/7980/winning-solution-description)

### anttip (1st)

Our winning submission to the 2014 Kaggle competition for Large Scale Hierarchical Text Classification (LSHTC) consists mostly of an ensemble of sparse generative models extending Multinomial Naive Bayes. The base-classifiers consist of hierarchically smoothed models combining document, label, and hierarchy level Multinomials, with feature pre-processing using variants of TF-IDF and BM25. Additional diversification is introduced by different types of folds and random search optimization for different measures. The ensemble algorithm optimizes macroFscore by predicting the documents for each label, instead of the usual prediction of labels per document. Scores for documents are predicted by weighted voting of base-classifier outputs with a variant of Feature-Weighted Linear Stacking. The number of documents per label is chosen using label priors and thresholding of vote scores.

The full description .pdf file is attached, and the code can be downloaded from: [https://kaggle2.blob.core.windows.net/competitions/kaggle/3634/media/LSHTC4_winner_solution.zip](https://kaggle2.blob.core.windows.net/competitions/kaggle/3634/media/LSHTC4_winner_solution.zip)

The above code package includes precomputed result files for the base-classifiers used by our ensemble. These take close to 300MB. A package omitting the base-classifier output files is also available: [https://kaggle2.blob.core.windows.net/competitions/kaggle/3634/media/LSHTC4_winner_solution_omit_resultsfiles.zip](https://kaggle2.blob.core.windows.net/competitions/kaggle/3634/media/LSHTC4_winner_solution_omit_resultsfiles.zip)
