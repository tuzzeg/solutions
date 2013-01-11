max_post_to_compare = 200

getCappedBinomialDeviance <- function(actualY, classProbabilities) {
  classProbabilities[classProbabilities < 0.01] = 0.01
  classProbabilities[classProbabilities > 0.99] = 0.99
  
  cappedBinomialDeviance = 0
  cappedBinomialDeviance = cappedBinomialDeviance + sum(log10(classProbabilities[actualY == 1]))
  cappedBinomialDeviance = cappedBinomialDeviance + sum(log10(1 - classProbabilities[actualY == 0]))
  
  cappedBinomialDeviance = -1 * cappedBinomialDeviance / length(actualY)
  
  return(cappedBinomialDeviance)
}

printConfusionMatrix <- function(actualY, classProbabilities, probThreshold=0.5) {
  predictedY = 1 * (classProbabilities > probThreshold)
  
  c11 = sum(1 * ((actualY == 1) & (predictedY == 1)))
  c12 = sum(1 * ((actualY == 1) & (predictedY == 0)))
  c21 = sum(1 * ((actualY == 0) & (predictedY == 1)))
  c22 = sum(1 * ((actualY == 0) & (predictedY == 0)))
  
  labelsCorrect   = c11 + c22
  labelsIncorrect = c21 + c12
  
  precision = 1.0 * c11 / (c11 + c21);
  recall    = 1.0 * c11 / (c11 + c12);
  F1_Score = 2 * precision * recall / (precision + recall);
  
  print( paste('Precision = ', round(100 * precision), '%', sep='') )
  print( paste('Recall    = ', round(100 * recall), '%', sep='') )
  print( paste('F1 Score  = ', F1_Score, sep='') )
  print( paste('Accuracy  = ', (100 * labelsCorrect/(labelsCorrect + labelsIncorrect)), '%', sep='') )
}

preprocess <- function(trData, cvData, replaceByMedian) {
  print(paste('preprocess(): replaceByMedian = ', replaceByMedian))
  
  trData$blog_post_day = trData$blog_post_day / 7
  cvData$blog_post_day = cvData$blog_post_day / 7
  
  r = handleMissingValues(trData, cvData, 'max_tag_like_fraction', replaceByMedian)
  r = handleMissingValues(r$trData, r$cvData, 'avg_tag_like_fraction', replaceByMedian)
  r = handleMissingValues(r$trData, r$cvData, 'max_category_like_fraction', replaceByMedian)
  r = handleMissingValues(r$trData, r$cvData, 'avg_category_like_fraction', replaceByMedian)
  
  return(r)
}

handleMissingValues <- function(trData, cvData, colName, replaceByMedian) {
  defaultValue = 0
  if (replaceByMedian) {
    #array of bool [element is true if value is missing; else false]
    indexesWithMissingValues = (trData[[colName]] == -1)
    
    defaultValue = median(trData[[colName]][!indexesWithMissingValues])
  }
  
  trData = handleMissingValues.do(trData, colName, defaultValue)
  cvData = handleMissingValues.do(cvData, colName, defaultValue)
  
  return( list('trData'=trData, 'cvData'=cvData) )
}

handleMissingValues.do <- function(data, colName, defaultValue) {
  #array of bool [element is true if value is missing; else false]
  indexesWithMissingValues = (data[[colName]] == -1)  
  
  data[[colName]][indexesWithMissingValues] = defaultValue
  data[[sprintf('missing__%s', colName)]] = factor(1 * indexesWithMissingValues)
  
  return(data)
}

readTrainingFile <- function() {
  trainingFile = sprintf('~/gom/generated/training__more_features1__max-posts-%d.csv', max_post_to_compare)
  writeLines( sprintf('trainingFile = %s', trainingFile) )
  trData = read.csv(trainingFile)  
  return(trData)
}

readTestFile <- function() {
  testFile = sprintf('~/gom/generated/test__more_features1__max-posts-%d.csv', max_post_to_compare)
  writeLines( sprintf('testFile = %s', testFile) )
  testData = read.csv(testFile)  
  return(testData)
}

writePredictionsToFile <-function(tag, testData, classProbabilities) {
  csvOutput = cbind(testData$uid, testData$post_id, classProbabilities)
  colnames(csvOutput) = c('uid', 'post_id', 'like_probability')
  predictionOutputFile = sprintf('~/gom/predictions/pred-%s___test__more_features1__max-posts-%d.csv', tag, max_post_to_compare)
  writeLines( sprintf('predictionOutputFile = %s', predictionOutputFile) )
  write.table(csvOutput, predictionOutputFile, row.names=F, col.names=T, sep=',')  
}
