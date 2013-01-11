rf = read.csv('~/gom/predictions/pred-rf___test__more_features1__max-posts-200.csv')
gbm_1200_trees        = read.csv('~/gom/predictions/pred-gbm-tree-1200___test__more_features1__max-posts-200.csv')
lr_sub_009            = read.csv('~/gom/predictions/pred-lr___test__more_features1__max-posts-100.csv')

pred_new = rf
pred_new$like_probability = (0.4 * rf$like_probability) + (0.4 * gbm_1200_trees$like_probability) + (0.2 * lr_sub_009$like_probability)
	
predNewFileName = '~/gom/predictions/rf_AND_gbm_1200_trees_AND_lr_sub_009.csv'
writeLines( sprintf('predNewFileName = %s', predNewFileName) )
write.table(pred_new, predNewFileName, row.names=F, col.names=T, sep=',')
