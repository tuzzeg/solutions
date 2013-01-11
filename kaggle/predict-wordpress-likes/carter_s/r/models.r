Sys.time()

require(biglm)
require(randomForest)


# load functions in here
source("./r/genfunc.r")
source("./r/datafunc.r")




# define dataset alteration functions
alter.dataset <- function()
{
	dataset$segment <<- with(dataset, 
		ifelse(
			(user_blog_hist_like_ct > 0) | (user_blog_all_user_like_blog_post_share > 0)
			, ifelse(blog_author_ct > 1, "dcmu", "dcsu")
			, ifelse(
				(user_blog_pagerank_by_like_share_postrank > 0 & user_blog_pagerank_by_like_share_postrank <= 1000)
				, "pr"
				, "tp"
			)
		)
	)
	
}


# load data
source("./r/build_train_data.r")
gc()
alter.dataset()
gc()

# create segmented model
{
	
	models <- list()
	
	models$dcmu = glm(
		result ~ 1 + post_weekday
			+ user_blog_all_blog_post_user_like_share + user_blog_hist_blog_post_user_like_share
			+ user_blog_all_blog_like_user_like_share + user_blog_hist_blog_like_user_like_share
			+ user_blog_all_user_like_blog_post_share + user_blog_hist_user_like_blog_post_share
			+ user_blog_weekM1_blog_post_user_like_share + user_blog_weekM2_blog_post_user_like_share
			+ user_blog_pagerank_by_like_share_blogprob
			+ user_post_topic_proximity_mean + user_post_topic_proximity_max
			+ user_blog_lang_proximity
			+ user_post_tz_proximity_to_stim_likes
			+ user_post_author_post_user_like_share + user_post_user_like_author_post_share
			+ user_post_blog_post_author_post_share + user_post_blog_like_author_like_share
			+ user_post_user_is_blog_author
			+ user_post_user_is_post_author
			+ ifelse(user_post_user_is_post_author=="True", user_as_author_post_user_like_share, 0)
		, dataset[(dataset$segment == "dcmu"), ]
		, family = binomial,  weights = dataset$lrweight[dataset$segment == "dcmu"]
	)
	models$dcmu[c("data", "weights", "model", "residuals", "fitted.values", "y")] = NULL

	models$dcsu = glm(
		result ~ 1 + post_weekday
			+ user_blog_all_blog_post_user_like_share + user_blog_hist_blog_post_user_like_share
			+ user_blog_all_blog_like_user_like_share + user_blog_hist_blog_like_user_like_share
			+ user_blog_all_user_like_blog_post_share + user_blog_hist_user_like_blog_post_share
			+ user_blog_weekM1_blog_post_user_like_share + user_blog_weekM2_blog_post_user_like_share
			+ user_blog_pagerank_by_like_share_blogprob
			+ user_post_topic_proximity_mean + user_post_topic_proximity_max
			+ user_blog_lang_proximity
			+ user_post_tz_proximity_to_stim_likes
			+ user_post_user_is_post_author
			+ ifelse(user_post_user_is_post_author=="True", user_as_author_post_user_like_share, 0)
		, dataset[(dataset$segment == "dcsu"), ]
		, family = binomial, weights = dataset$lrweight[dataset$segment == "dcsu"]
	)
	models$dcsu[c("data", "weights", "model", "residuals", "fitted.values", "y")] = NULL

	models$pr = glm(
		result ~ 1 + post_weekday
			+ user_blog_pagerank_by_like_share_blogprob
			+ user_post_topic_proximity_mean + user_post_topic_proximity_max
			+ user_blog_lang_proximity
			+ user_post_tz_proximity_to_stim_likes
			+ user_post_user_is_post_author
			+ ifelse(user_post_user_is_post_author=="True", user_as_author_post_user_like_share, 0)
		, dataset[(dataset$segment == "pr"), ]
		, family = binomial, weights = dataset$lrweight[dataset$segment == "pr"]
	)
	models$pr[c("data", "weights", "model", "residuals", "fitted.values", "y")] = NULL

	models$tp = glm(
		result ~ 1 + post_weekday
			+ user_blog_pagerank_by_like_share_blogprob
			+ user_post_topic_proximity_mean + user_post_topic_proximity_max
			+ user_blog_lang_proximity
			+ user_post_tz_proximity_to_stim_likes
			+ user_post_user_is_post_author
		, dataset[ (dataset$segment == "tp"), ]
		, family = binomial, weights = dataset$lrweight[dataset$segment == "tp"]
	)
	models$tp[c("data", "weights", "model", "residuals", "fitted.values", "y")] = NULL

	
	strata = dataset$rfstrata[, drop = TRUE]
	sampsize =rep(1, length(levels(strata)))
	models$rf = randomForest(
		as.factor(result) ~ post_weekday
			+ blog_all_post_ct + blog_hist_post_ct + blog_weekM1_post_ct 
			+ blog_all_like_ct 
			+ user_all_like_ct + user_hist_like_ct + user_weekM1_like_ct 
			+ user_blog_all_blog_post_user_like_share + user_blog_hist_blog_post_user_like_share
			+ user_blog_weekM1_blog_post_user_like_share + user_blog_weekM2_blog_post_user_like_share
			+ user_blog_all_blog_like_user_like_share + user_blog_hist_blog_like_user_like_share
			+ user_blog_weekM1_blog_like_user_like_share  
			+ user_blog_all_user_like_blog_post_share + user_blog_hist_user_like_blog_post_share 
			+ user_blog_weekM1_user_like_blog_post_share 
			+ user_blog_pagerank_by_like_share_blogprob
			+ user_post_topic_proximity_max + user_post_topic_proximity_mean 
			+ user_is_english + blog_is_english + user_blog_lang_proximity
			+ user_post_tz_proximity_to_stim_likes
			+ blog_author_ct
			+ user_post_author_post_user_like_share + user_post_user_like_author_post_share
			+ user_post_blog_post_author_post_share + user_post_blog_like_author_like_share
			+ user_post_user_is_blog_author
			+ user_post_user_is_post_author + user_as_author_post_user_like_share
		, dataset, strata = strata, sampsize = sampsize
		, do.trace = TRUE, proximity = FALSE, ntree = 1000
	)
	
	save(models, file = "./rdata/models.rdata")

}

source("./r/build_prod_data.r")
gc()
alter.dataset()
gc()

# predict models
{
		
	LR = rep(NA, nrow(dataset))
	LR[dataset$segment == "dcmu"] = batch.win.predict(models$dcmu, dataset[dataset$segment == "dcmu", ], lr.win.predict, 250000)
	LR[dataset$segment == "dcsu"] = batch.win.predict(models$dcsu, dataset[dataset$segment == "dcsu", ], lr.win.predict, 250000)
	LR[dataset$segment == "pr"] = batch.win.predict(models$pr, dataset[dataset$segment == "pr", ], lr.win.predict, 250000)
	LR[dataset$segment == "tp"] = batch.win.predict(models$tp, dataset[dataset$segment == "tp", ], lr.win.predict, 250000)

	RF = batch.win.predict(models$rf, dataset, rf.win.predict, 250000)
	
	EN = LR + RF
	dataset$EN = EN
	
	get.submission("EN", "FinalSubmission")
	
	save(LR, RF, EN, file = "./rdata/predictions.rdata")
}

Sys.time()
