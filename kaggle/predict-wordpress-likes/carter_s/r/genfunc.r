
	scratch <- function()
	{
		for(i in 1:1000000)
			x <- i
	}
	debug(scratch)
	
	add.terms <- function(formula, terms) 
	{
		for(t in terms)
			formula[[3]] <-  as.call(c(as.name("+"), formula[[3]], as.name(t)))
		formula
	}

	## Replace NAs
	replace.na <- function(x, replace.with)
	{
		ifelse(is.na(x), replace.with, x)
	}

	## Create function that calculates ntiles (can't believe this doesnt exist?)
	ntile <- function(data, intervals, raw.labels = FALSE)
	{
		if(raw.labels)
			cut(data, quantile(data, seq(0, 1, 1/intervals), na.rm = TRUE), include.lowest = TRUE)
		else
			cut(data, quantile(data, seq(0, 1, 1/intervals), na.rm = TRUE), include.lowest = TRUE, labels = seq(1/(2*intervals), 1, 1/intervals))
			
	}

	## Create Function To Rank By Weight
	group.percentile <- function(factor.column, weights, order.column.1, ...)
	{
		
		## Get order of values first by the grouping factor, then by a rank of values
		index.order <- order(factor.column, order.column.1, ...);
		
		## By taking order of array showing ranked positions, return an array showing the rank of each element by  original order columns
		index.rank <- order(index.order);

		## Calculate the percentile of each value by weight; transpose back to orignal order by index.rank
		percentiles <- ( cumsum(weights[index.order]) / sum(weights) )[index.rank];

		## Get min and max of percentile by factor; make sure names match to factors
		max.percentiles <- tapply(percentiles, factor.column, max);
		min.percentiles <- c(0, max.percentiles[1:(length(max.percentiles) - 1)]);
		names(min.percentiles) <- names(max.percentiles);

		## Extrapolate percentiles over range
		(1-1e-10) * (percentiles - min.percentiles[factor.column]) / (max.percentiles[factor.column] - min.percentiles[factor.column])
		

	}

	## Calculate log-entropy error
	entropy.error <- function(predicted.rate, target.wins, target.trials)
	{
		- target.wins * log(predicted.rate) - (target.trials - target.wins) * log(1 - predicted.rate)
	}

	## Lift by dataset
	lift.table <- function(target, predicted, partition, weight, index, intervals = 10)
	{
		percent.rank <- group.percentile(partition, weight, predicted, index);
		percent.rank <- floor(percent.rank * intervals) / intervals;
		tapply(target, list(as.factor(percent.rank), partition), mean, na.rm = TRUE)
	}

	## Frequency table
	freq.2dim <- function(row.group, dist.group)
	{
		round(100 * ( table(row.group, dist.group) / as.vector(table(row.group)) ), 1)
	}	

## Avg precisiona at k
	apk <- function(k, actual, predicted)
	{
		score <- 0.0
		cnt <- 0.0
		for (i in 1:min(k,length(predicted)))
		{
			if (predicted[i] %in% actual && !(predicted[i] %in% predicted[0:(i-1)]))
			{
				cnt <- cnt + 1
				score <- score + cnt/i 
			}
		}
		score <- score / min(length(actual), k)
		score
	}
	
	
	## Batch table
	rf.win.predict <- function(model, data) predict(model, data, type = "prob")[, 2]
	lr.win.predict <- function(model, data) predict(model, data, type = "response")
	batch.win.predict <- function(model, data, win.predict.func, batch.size = 10000)
	{
		return.val <- rep(NA, nrow(data))
		for(i in 1:ceiling(nrow(data)/batch.size))
		{
			rowset = ((i-1)*batch.size + 1):(min(nrow(data), i*batch.size));
			return.val[rowset] <- win.predict.func(model, data[rowset,  ]);
			cat(paste("Batch predict: ", i, "of", ceiling(nrow(data)/batch.size), "completed.", Sys.time(), "\n"))
		}	
		return.val
	}


