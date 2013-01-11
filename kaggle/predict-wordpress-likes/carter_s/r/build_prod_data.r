	rm(dataset)
	gc()

	tryCatch(load(file = "./rdata/proddata.rdata"), error = function(e) NULL)
	
	if(!exists("dataset"))
	{
		print("Reloading data")
		dataset = read.table("./tmp/prod/RDataSet.csv", header = TRUE, sep = ",")
		prepare.dataset()
		save(dataset, file = "./rdata/proddata.rdata")
	}
