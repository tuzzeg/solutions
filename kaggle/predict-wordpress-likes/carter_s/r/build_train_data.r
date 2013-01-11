	rm(dataset)
	gc()

	tryCatch(load(file = "./rdata/traindata.rdata"), error = function(e) NULL)
	
	if(!exists("dataset"))
	{
		print("Reloading data")
		dataset = read.table("./tmp/dev/RDataSet.csv", header = TRUE, sep = ",")
		prepare.dataset()
		save(dataset, file = "./rdata/traindata.rdata")
	}

