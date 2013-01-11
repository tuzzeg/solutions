import pandas, pandas_ext, param
import subprocess
import useful_stuff
from collections import OrderedDict 
from useful_stuff import *


	
class FeatureStoreGroup(pandas.HDFStore):
	def load_df(self, keys = None, reset_index = False, prefix = None):
		if keys is None: keys = self.keys()
		series = dict((k, self[k]) for k in keys)
		df = pandas.DataFrame(series)
		df.index.names = series.values()[0].index.names
		df = df.add_col_prefix(prefix)
		if reset_index: df = df.reset_index()
		return df
	def save_df(self, df, overwrite = False):
		series_to_save = dict((k, df[k]) for k in df if overwrite or (k not in self.keys()))
		self.save_series(**series_to_save)
		self.refresh()
	def save_series(self, *args, **kwargs):
		for s in args:
			self[s.name] = s
		for k in kwargs:
			self[k] = kwargs[k]
		self.refresh()
	def clear(self):
		keylist = self.keys()
		for kn in keylist: 
			self.remove(kn)
		self.refresh(True)
	def copy(self, targetstore):
		for kn in self.keys(): 
			targetstore[kn] = self[kn]
		targetstore.flush()
	def refresh(self, repack = False):
		self.flush()
		self.close()
		if repack:
			subprocess.call(["h5repack", "-i", self.path , "-o", self.path + ".tmp"])
			subprocess.call(["mv", self.path + ".tmp", self.path])	
		self.open()
	
class FeatureStore(dict):
	def __init__(self, name, store_name_list = [], rootfolder = param.folders.saved):
		self.name = name
		self.path = rootfolder + name + "/"
		self.tmpdata = param.folders.tmpdata + name + "/"
		self.auxdata = param.folders.auxdata + name + "/"
		self.add_stores(store_name_list)
	def add_stores(self, store_name_list):
		for store_name in store_name_list:
			self[store_name] = FeatureStoreGroup(self.path + store_name + ".h5")
			self.__setattr__(store_name, self[store_name])
	def clear_all(self):
		for store in self.itervalues(): store.clear()
	def refesh_all(self, repack = False):
		for store in self.itervalues(): store.refresh(repack)

		
prod_store = FeatureStore(
	name = "prod", 
	store_name_list = ["post", "user", "blog", "user_post", "user_blog"]
)	
dev_store = FeatureStore(
	name = "dev", 
	store_name_list = ["post", "user", "blog", "user_post", "user_blog"]
)

def clear_stores():
	prod_store.clear_all()
	dev_store.clear_all()

	
class Feature:

	def __init__(self, scope, name, **kwargs):
		self.name = name
		self.scope = scope
		self.key = scope + "." + name
		self.parent_set = None
		for (k, v) in kwargs.iteritems(): setattr(self, k, v)
		
	def save(self, data_store, overwrite = False, recreate_setup = False):
		return self.parent_set.save(
			data_store = data_store, feature_keys = [self.key], overwrite = overwrite, recreate_setup = recreate_setup
		)
		
class FeatureSet:

	def __init__(self, name, features, setup_func, build_func):
		self.name = name
		self.feature_dict = OrderedDict((f.key, f) for f in features)
		for f in features: 
			f.parent_set = self
		self.setup = setup_func
		self.build_series = build_func
			
	def save(self, data_store, feature_keys = None, overwrite = False, recreate_setup = False):
	
		# save features in passed list (or all if no passed list)
		# save features if they don't exist in store or are being overwritten
		if feature_keys is None: feature_keys = self.feature_dict.keys()
		save_features = [self.feature_dict[k] for k in feature_keys]
		if not overwrite: save_features = [f for f in save_features if (f.name not in data_store[f.scope])]	
		
		# execute setup and build functions 
		if len(save_features) > 0:
			print "\n" + nowstring() + " - Start saving features from " + self.name
			print "\t" + nowstring() + " - Start loading setup data"
			setup_data = self.setup(save_features, data_store, recreate_setup)
			print "\t" + nowstring() + " - End loading setup data"
			for f in save_features:
				s = self.build_series(f, setup_data).apply_name(f.name).dropna()
				data_store[f.scope].save_series(s)
				print "\t" + nowstring() + " - Saved feature " + f.key + " to " + data_store[f.scope].path
			print nowstring() + " - End saving features from " + self.name
			
			