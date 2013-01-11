\source('util.r')

library(randomForest)  

replaceByMedian = T

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
  rffit = do_train(trData)
  writeLines('Training Prediction Summary:')
  predictionSummary(rffit, trData)
  
  #predict
  classProbabilities = getClassProbabilities(rffit, testData)
  
  #save predictions to file
  writePredictionsToFile('rf', testData, classProbabilities)
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
  
  rffit = do_train(trData)
  
  writeLines('Training Prediction Summary:')
  predictionSummary(rffit, trData)
  writeLines('CV Prediction Summary:')
  predictionSummary(rffit, cvData)
}

do_train <- function(trData) {
  ntree_value = 500
  writeLines( sprintf('ntree_value = %d', ntree_value) )
  
  nodesize_value = 100
  writeLines( sprintf('nodesize_value = %d', nodesize_value) )
  
  sampsize_value = round(nrow(trData) / 2)
  writeLines( sprintf('sampsize_value = %d', sampsize_value) )
  
  #reset the seed to a "random" value for training 
  set.seed(   unclass( Sys.time() )   )    
  
  rffit = randomForest(factor(did_user_like_the_blog_post) ~ .,
                       data=trData,
                       ntree=ntree_value,
                       nodesize=nodesize_value,
                       sampsize=sampsize_value,
                       do.trace=TRUE, importance=TRUE, keep.forest=TRUE)
  
  modelFileName = sprintf('generatedModels/rf2_trees-%d.rda', ntree_value)
  save(rffit, file=modelFileName)
  print( paste('saved model: modelFileName = ', modelFileName) )  
  
  return(rffit)
}

predictionSummary <- function(rffit, data) {
  classProbabilities = getClassProbabilities(rffit, data)
  cappedBinomialDeviance = getCappedBinomialDeviance(data[['did_user_like_the_blog_post']], classProbabilities)
  writeLines( sprintf('cappedBinomialDeviance = %f', cappedBinomialDeviance) )
  printConfusionMatrix(data[['did_user_like_the_blog_post']], classProbabilities)
}

getClassProbabilities <- function(rffit, data) {
  matrixOfClassProbabilities = predict(rffit, data, type='prob', progress='text')
  return(matrixOfClassProbabilities[,2])
}
