source('util.r')

require(glmnet)

replaceByMedian = T

predictForTestData <- function() {
  alpha = 0.5
  
  #read train data
  trData = readTrainingFile()
  
  #read test data
  testData = readTestFile()
  
  #preproces: (1) week / 7 AND (2) missing values for [max_tag_like_fraction, avg_tag_like_fraction, max_category_like_fraction, avg_category_like_fraction]
  r = preprocess(trData, testData, replaceByMedian)
  trData   = r$trData
  testData = r$cvData
  
  #train  
  trData.glmnet = trData[, !(colnames(trData) %in% c('did_user_like_the_blog_post'))]
  lm.fit = glmnet(x=data.matrix(trData.glmnet), y=factor(trData[['did_user_like_the_blog_post']]), family='binomial', standardize=F, alpha=alpha)
  lambdaIndex = length(lm.fit$lambda)
  writeLines( sprintf('converged upto lambdaIndex = %d', lambdaIndex) )
  writeLines( sprintf('alpha = %f', alpha) )
  writeLines('Training Prediction Summary:')
  predictionSummary(lm.fit, trData, lambdaIndex)
  
  #predict
  classProbabilities = getClassProbabilities(lm.fit, testData, lambdaIndex)
  
  #save predictions to file
  writePredictionsToFile('lr', testData, classProbabilities)
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
  
  trData.glmnet = trData[, !(colnames(trData) %in% c('did_user_like_the_blog_post'))]
  
  #reset the seed to a "random" value for training 
  set.seed(   unclass( Sys.time() )   )		
  
  alpha = 0.5
  
  lm.fit = glmnet(x=data.matrix(trData.glmnet), y=factor(trData[['did_user_like_the_blog_post']]), family='binomial', standardize=F, alpha=alpha)
  
  modelFileName = sprintf('generatedModels/lr2_alpha-%f.rda', alpha)
  save(lm.fit, file=modelFileName)
  print( paste('saved model: modelFileName = ', modelFileName) )
  
  lambdaIndex = length(lm.fit$lambda)
  writeLines( sprintf('converged upto lambdaIndex = %d', lambdaIndex) )
  
  writeLines( sprintf('alpha = %f', alpha) )
  writeLines('Training Prediction Summary:')
  predictionSummary(lm.fit, trData, lambdaIndex)
  writeLines('CV Prediction Summary:')
  predictionSummary(lm.fit, cvData, lambdaIndex)
}

predictionSummary <- function(lm.fit, data, argLambdaIndex) {
  #for (lambdaIndex in 1:argLambdaIndex) {    
  for (lambdaIndex in c(argLambdaIndex)) {
    #writeLines(sprintf('...........lambdaIndex = %d', lambdaIndex))
    classProbabilities = getClassProbabilities(lm.fit, data, lambdaIndex)
    cappedBinomialDeviance = getCappedBinomialDeviance(data[['did_user_like_the_blog_post']], classProbabilities)
    writeLines( sprintf('cappedBinomialDeviance = %f', cappedBinomialDeviance) )
    printConfusionMatrix(data[['did_user_like_the_blog_post']], classProbabilities)
  }
}

getClassProbabilities <- function(lm.fit, data, lambdaIndex) {
  data = data[, !(colnames(data) %in% c('did_user_like_the_blog_post', 'uid', 'post_id'))]
  probabilities = predict(lm.fit, newx=data.matrix(data), type='response', s=lm.fit$lambda[lambdaIndex])
  return(probabilities)
}
