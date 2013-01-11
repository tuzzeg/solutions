	.actual_pids <- with(read.table("./tmp/dev/TargetLikes.csv", header = TRUE, sep=","), {
		tapply(post_id, user_id, c)
	})
	
	reset.segments <- function()
	{
		user_segment = tapply(unique(dataset$user_id), unique(dataset$user_id), function(x) runif(1))
		dataset$train_segment <<- user_segment[as.character(dataset$user_id)]
	}

	prepare.dataset <- function()
	{
		dataset$dummy <<- 1
		dataset$post_weekday <<- as.factor(dataset$post_weekday)
				
		dataset$lrweight <<- with(dataset, {
			userrows = table(user_id);
			userposrows = tapply(result, user_id, function(r) sum(r==1));
			usernegrows = userrows - userposrows;
			ifelse(result == 1
				, ifelse(userposrows == 0, 0, .5 / userposrows)[as.character(user_id)]
				, ifelse(userposrows == 0, 0, .5 / usernegrows)[as.character(user_id)]
			)
		})
				
		dataset$rfstrata <<- interaction(dataset$result == 1, dataset$user_id)[, drop = TRUE]
		reset.segments()
	}
	
	

	get.user.scores <- function(predicted)
	{
		gpred <- split(predicted, dataset$user_id)
		gpost <- split(dataset$post_id, dataset$user_id)
		predicted_pids <- mapply(function(pred, post) post[order(pred, decreasing = TRUE)], gpred, gpost)
		sapply(names(predicted_pids), function(un) apk(100, .actual_pids[[un]], predicted_pids[[un]]))
	}

	scores.by.dataset <- function(predicted, train_cutoff, predict_cutoff)
	{
		user_scores = get.user.scores(predicted)
		
		train_cutoff = max(1e-12, train_cutoff)
		predict_cutoff = max(train_cutoff * (1 + 1e-12), predict_cutoff)
		end_cutoff = max(predict_cutoff * (1 + 1e-12), 1)
		cut_vector = c(0, train_cutoff, predict_cutoff, end_cutoff)
		
		user_train_segments = tapply(dataset$train_segment, dataset$user_id, function(segs) segs[1])
		user_train_segments = cut(user_train_segments, cut_vector, labels = c("train", "val", "not used"))
		
		tapply(user_scores, user_train_segments, mean)
	}

	
	model.init <- function(name, type, model.formula)
	{
		models[[name]] <<- list(name = name, type = type, formula = model.formula, fits = list())
	}
	
	model.fit <- function(model.name, fit.name, fit.cutoff, ...)
	{
		cat(paste("Started fitting ", fit.name, "for", model.name, "at", Sys.time(), "\n"))
		model = models[[model.name]]
		
		rows = (1:nrow(dataset))[dataset$train_segment < fit.cutoff]
		if(model$type == "lr")
		{
			fit = bigglm(model$formula, dataset[rows, ], family = binomial(), weights = ~lrweight, ...)
		}
		else if (model$type == "rf")
		{
			strata = dataset$rfstrata[rows, drop = TRUE]
			sampsize =rep(1, length(levels(strata)))
			set.seed(898983)
			fit = randomForest(model$formula, dataset[rows, ], strata = strata, sampsize = sampsize
								, do.trace = TRUE, proximity = FALSE, ...)
		}

		models[[model.name]]$fits[[fit.name]] <<- list(fit = fit, cutoff = fit.cutoff, evals = list())
		
		cat(paste("Finished fitting ", model.name, "at", Sys.time(), "\n"))
	}
	
	model.eval <- function(model.name, fit.name, eval.name, eval.cutoff, save.in.df = FALSE)
	{
		cat(paste("Started evaluating ", eval.name, "for", fit.name, "of", model.name, "at", Sys.time(), "\n"))
		model = models[[model.name]]
		fit = model$fits[[fit.name]]
		
		predict_indexes = (1:nrow(dataset))[dataset$train_segment < eval.cutoff]
		predicted = rep(NA, nrow(dataset))
		if(model$type == "lr")
			predicted[predict_indexes] = batch.win.predict(fit$fit, dataset[predict_indexes, ], lr.win.predict, 250000)
		else if(model$type == "rf")
			predicted[predict_indexes] = batch.win.predict(fit$fit, dataset[predict_indexes, ], rf.win.predict, 250000)
		;
		
		if(save.in.df) dataset[[model.name]] <<- predicted
		
		scores = scores.by.dataset(predicted, fit$cutoff, eval.cutoff)
		models[[model.name]]$fits[[eval.name]]$evals[[eval.name]] <<- list(cutoff = eval.cutoff, scores = scores)
		scores[[paste(model.name, "/", fit.name, "/", eval.name)]] <<- scores

		cat(paste("Finished evaluating ", eval.name, "for", fit.name, "of", model.name, "at", Sys.time(), "\n"))
		predicted
	}
	
	model.predict <- function(model.name, fit.name, save.in.df = TRUE)
	{
		cat(paste("Started predicting with", fit.name, "of", model.name, "at", Sys.time(), "\n"))
		model = models[[model.name]]
		fit = model$fits[[fit.name]]
		
		if(model$type == "lr")
			predicted = batch.win.predict(fit$fit, dataset, lr.win.predict, 250000)
		else if(model$type == "rf")
			predicted = batch.win.predict(fit$fit, dataset, rf.win.predict, 250000)
		;
		if(save.in.df) dataset[[model.name]] <<- predicted
		
		cat(paste("Ended predicting with", fit.name, "of", model.name, "at", Sys.time(), "\n"))
		predicted
	}
	
	get.submission <- function(prediction.column, file.name)
	{
		predicted = dataset[[prediction.column]]
		
		# get predicted posts for users
		gpred <- split(predicted, dataset$user_id)
		gpost <- split(dataset$post_id, dataset$user_id)
		predicted_pids <- mapply(function(pred, post) post[order(pred, decreasing = TRUE)], gpred, gpost)
		
		# get most predicted 
		most_predicted <- sort(tapply(predicted, dataset$post_id, sum), decreasing = TRUE)[1:100]
		
		# get test users
		test_users <- read.table("./sourcedata/testUsers.json", header = FALSE, col.names = "user_id")
		
		# write file
		outfile = file(paste("./submissions/", file.name, ".csv", sep = ""))
		open(outfile, "w")
		for(user_id in as.character(test_users$user_id))
		{
			user_predicted = paste(c(predicted_pids[[user_id]], most_predicted)[1:100], collapse = " ")
			writeLines( paste(user_id, user_predicted, sep=", "), outfile)
		}
		close(outfile)

	}
	
	model.init.fit.eval <- function(model.name, type, formula, fit.name, fit.cutoff, eval.name, eval.cutoff
									, save.in.df = FALSE, ...)
	{
		model.init(model.name, type, formula)
		model.fit(model.name, fit.name, fit.cutoff, ...)
		predicted = model.eval(model.name, fit.name, eval.name, eval.cutoff, save.in.df)
		print(sapply(scores, I))
		predicted
	}
	
	model.fit.eval <- function(model.name, fit.name, fit.cutoff, eval.name, eval.cutoff, save.in.df = FALSE, ...)
	{
		model.fit(model.name, fit.name, fit.cutoff, ...)
		predicted = model.eval(model.name, fit.name, eval.name, eval.cutoff, save.in.df)
		print(sapply(scores, I))
		predicted
	}
