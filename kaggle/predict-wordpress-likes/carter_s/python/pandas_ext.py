import pandas
import param
import subprocess
import gc
from datetime import datetime


# Monkey patching into pandas Series and DataFrame
Series = pandas.Series
DataFrame = pandas.DataFrame

# Apply name
def f(self, name):
	self.name = name
	return self
Series.apply_name = f

# Fitler Indexes
def f(self, value):
	return self[self == value].index
Series.get_matching_indexes = f


# Add col prefix
def f(self, prefix):
	if prefix is not None:
		return self.rename(columns = lambda(s): prefix + "_" + s)
	else:
		return self
DataFrame.add_col_prefix = f


# Index to columns
def f(self, prefix = None):
	n = self.add_col_prefix(prefix)
	return self.add_col_prefix(prefix).reset_index()
DataFrame.index_to_columns = f


# Add df with left join on level of index
def f(self, newdf, prefix = None, on_level = None):
	newdf = newdf.reindex(self.index, level = on_level).add_col_prefix(prefix)
	return self.join(newdf)
DataFrame.add_df = f
	
	
# Add column with left join on level of index
def f(self, column, name = None, on_level = None):
	if name is None: name = column.name
	column = column.reindex(self.index, level = on_level)
	self[name] = column
DataFrame.add_column = f


# Get index as a column
def f(self, index_name):
	try:
		v = self.index.get_level_values(index_name)
	except:
		v = self.index.values
	return pandas.Series(v, index = self.index, name = index_name)
Series.get_index_column = f
DataFrame.get_index_column = f


# Apply as segment - needs work
def f(self, chunk_func, num_chunks, verbose = False):

	nrow = len(self)
	
	chunk_size = int((nrow-1) / num_chunks) + 1
	slice_indexes = range(0, 1 + int((nrow-1) / chunk_size))
	slice_start = [(i * chunk_size) for i in slice_indexes]
	slice_end = [min(nrow, (i+1) * chunk_size) for i in slice_indexes]
	
	for (n, (start, end)) in enumerate(zip(slice_start, slice_end)):
	
		chunk = self[start:end]
		chunkdata = chunk_func(chunk)
		
		if n == 0:
			cls = chunkdata.__class__
			index_names = chunkdata.index.names
			if None in index_names:
				index_names = ["index" + str(n) for (n, i) in enumerate(index_names) if i is None]
		
		chunkdata.index.names = index_names
		chunkdata = chunkdata.reset_index()
		chunkdata.save(param.folders.tmpdata + "tmp" + str(n) + ".df")	
		if verbose: 
			print "\tSegment apply processed segment " + str(n + 1), (start, end), datetime.now()
	
	for n in slice_indexes:
		
		chunk = pandas.DataFrame.load(param.folders.tmpdata + "tmp" + str(n) + ".df")
		subprocess.call(["rm", param.folders.tmpdata + "tmp" + str(n) + ".df"])	
		if n == 0:
			retval = chunk
		else:
			retval = retval.append(chunk, ignore_index = True)
	
	retval.index = pandas.MultiIndex.from_arrays(list(retval.pop(n) for n in index_names))
	if cls is pandas.Series: retval = retval[retval.columns[0]]
	return retval
	
Series.chunk_apply = f
DataFrame.chunk_apply = f


	# for (n, (slice_start, slice_end)) in enumerate(zip(slice_start, slice_end)):
	
		# chunk = self[slice_start:slice_end]
		# chunkdata = chunk_func(chunk)
		
		# if n == 0:
		
			# cls = chunkdata.__class__
			# index_names = chunkdata.index.names
			# if sum([i is not None for i in index_names]) == 0:
				# index_names = ["index" + str(n) for (n, i) in enumerate(index_names)]
				# chunkdata.index.names = index_names
				
			# chunkdata = chunkdata.reset_index()
			# all_names = chunkdata.columns
			# all_types = chunkdata.dtypes
			
			# chunkdata.to_csv(param.folders.tmpdata + "tmp.csv", mode = "w", header = True)
			
		# else:
			# chunkdata = chunkdata.reset_index()
			# chunkdata.to_csv(param.folders.tmpdata + "tmp.csv", mode = "a", header = False)
			
		# if verbose: 
			# print "\tSegment apply processed segment " + str(n + 1), (slice_start, slice_end), datetime.now()
		
	# retval = pandas.DataFrame.from_csv(param.folders.tmpdata + "tmp.csv")
	# retval.columns = all_names
	# retval = retval.apply(lambda x: x.astype(all_types[x.name]))
	# retval.index = pandas.MultiIndex.from_arrays(list(retval.pop(n) for n in index_names))
	
	# 

