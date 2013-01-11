source('util.r')

library(gbm)

replaceByMedian = T
ntree_value = 1200

predictForTestData <- function() {
  #read train data
  trData = readTrainingFile()
  
  #read test data
  testData = readTestFile()
  
  #preproces: (1) week / 7 AND (2) missing values for [max_tag_like_fraction, avg_tag_like_fraction, max_category_like_fraction, avg_category_like_fraction]
  r = preprocess(trData, testData, replaceByMedian)
  trData   = r$trData
  testData = r$cvData
  
  #train  
  gbmfit = do_train(trData)
  writeLines('Training Prediction Summary:')
  predictionSummary(gbmfit, trData)
  
  #predict
  classProbabilities = getClassProbabilities(gbmfit, testData)
  
  #save predictions to file
  writePredictionsToFile(sprintf('gbm-tree-%d', ntree_value), testData, classProbabilities)
}

train <- function() {
  cvFold = 5
  
  data = readTrainingFile()
  
  numberOfCvFolds = 5
  set.seed(673)  
  #shuffle
  data = data[sample.int(nrow(data)),]
  data_row_count            = dim(data)[1]
  rand_vector               = runif(data_row_count)
  rangeOfEachCvFold = 1 / numberOfCvFolds
  lowerLimit = (cvFold - 1) * rangeOfEachCvFold
  upperLimit = lowerLimit + rangeOfEachCvFold
  cvIndexes = ((rand_vector >= lowerLimit) & (rand_vector < upperLimit))
  trData    = data[!cvIndexes,]
  cvData    = data[cvIndexes,]
  print( paste('cvFold = ', cvFold, ', numberOfCvFolds = ', numberOfCvFolds, ', rangeOfEachCvFold = ', rangeOfEachCvFold, ', lowerLimit = ', lowerLimit, ', upperLimit = ', upperLimit) )
  print( paste('nrow(trData) = ', nrow(trData), ', nrow(cvData) = ', nrow(cvData)) )
  
  #preproces: (1) week / 7 AND (2) missing values for [max_tag_like_fraction, avg_tag_like_fraction, max_category_like_fraction, avg_category_like_fraction]
  r = preprocess(trData, cvData, replaceByMedian)
  trData = r$trData
  cvData = r$cvData
  
  gbmfit = do_train(trData)
  
  writeLines('Training Prediction Summary:')
  predictionSummary(gbmfit, trData)
  writeLines('CV Prediction Summary:')
  predictionSummary(gbmfit, cvData)
}

do_train <- function(trData) {
  writeLines( sprintf('do_train(): ntree_value = %d', ntree_value) )
  
  #reset the seed to a "random" value for training 
  set.seed(   unclass( Sys.time() )   )    
  
  gbmfit <- gbm(did_user_like_the_blog_post ~ ., 
                data=trData,
                distribution="bernoulli", 
                n.trees=ntree_value,              # number of trees 
                shrinkage=0.01,           # shrinkage or learning rate, 0.001 to 0.1 usually work 
                interaction.depth=4,       # 1: additive model, 2: two-way interactions, etc 
                bag.fraction = 0.5,        # subsampling fraction, 0.5 is probably best 
                train.fraction = 1,      # fraction of data for training, first train.fraction*N used for training 
                #cv.folds=5,                # do 5-fold cross-validation 
                n.minobsinnode = 10,      # minimum number of observations in the trees terminal nodes.
                verbose=T  # print out progress
  )
  
  modelFileName = sprintf('generatedModels/gbm2_trees-%d.rda', ntree_value)
  save(gbmfit, file=modelFileName)
  print( paste('saved model: modelFileName = ', modelFileName) )  
  
  return(gbmfit)
}

predictionSummary <- function(gbmfit, data) {
  classProbabilities = getClassProbabilities(gbmfit, data)
  cappedBinomialDeviance = getCappedBinomialDeviance(data[['did_user_like_the_blog_post']], classProbabilities)
  writeLines( sprintf('cappedBinomialDeviance = %f', cappedBinomialDeviance) )
  printConfusionMatrix(data[['did_user_like_the_blog_post']], classProbabilities)
}

getClassProbabilities <- function(gbmfit, data) {
  writeLines( sprintf('getClassProbabilities(): ntree_value = %d', ntree_value) )
  
  classProbabilities = predict(gbmfit, newdata=data, type="response", n.trees=ntree_value)
  return(classProbabilities)
}
