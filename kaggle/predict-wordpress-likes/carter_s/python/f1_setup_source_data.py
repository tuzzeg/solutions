import pandas, pandas_ext, param, features as ft
import jsonfiles as jf
import useful_stuff
from useful_stuff import *

# list of ftures to be defined by this module
feature_list = [
	ft.Feature('post', 'date', fileclass = jf.PostFile)
	, ft.Feature('post', 'blog_id', fileclass = jf.PostFile)
	, ft.Feature('post', 'is_test', fileclass = jf.PostFile)
	, ft.Feature('post', 'week', fileclass = None)
	, ft.Feature('post', 'weekday', fileclass = None)
	, ft.Feature('user_post', 'is_like', fileclass = jf.LikeFile)
	, ft.Feature('user_post', 'like_date', fileclass = jf.LikeFile)
	, ft.Feature('user_post', 'like_week', fileclass = None)
	, ft.Feature('user', 'is_test', fileclass = jf.UserFile)
	, ft.Feature('user', 'hist_like_ct', fileclass = jf.UserHistFile)
	, ft.Feature('user_blog', 'hist_like_ct', fileclass = jf.UserBlogHistFile)
	, ft.Feature('blog', 'hist_like_ct', fileclass = jf.BlogHistFile)
	, ft.Feature('blog', 'hist_post_ct', fileclass = jf.BlogHistFile)
]

# setup function to load data (from store) needed to calculate each Feature 
def setup_func(feature_list, store, recreate_setup):
	
	# will store feature key and data frame with data
	retval = dict()
	features_left = dict((f.key, f) for f in feature_list)
		
	# if reloading is allowed, it is ok to get to get series from prod store if they exist
	# (means setup for dev store can copy prod store rather than reload from file)
	if not recreate_setup:
		scopes = set(f.scope for f in features_left.itervalues())
		for s in scopes:
			scope_features = [f for f in features_left.itervalues() 
								if (s == f.scope) and (f.name in ft.prod_store[s])]
			if len(scope_features):
				df = ft.prod_store[s].load_df([f.name for f in scope_features])
				retval.update((f.key, df) for f in scope_features)
				for f in scope_features: features_left.pop(f.key)
		
	# after loading from prod_store (if possible), get rest from source files
	file_classes = set(f.fileclass for f in features_left.itervalues() if f.fileclass is not None)
	for fc in file_classes:
		file_features = [f for f in features_left.itervalues() if f.fileclass is fc]
		if len(file_features):
			df = fc.get_df([f.name for f in file_features])
			retval.update((f.key, df) for f in file_features)
			for f in file_features: features_left.pop(f.key)
		
	# add week fields if needed
	if ("post.week" in features_left) or ("post.weekday" in features_left):
	
		try:
			postdf = retval["post.date"]
		except:
			postdf = store.post.load_df(['date'])
			
		postdf['week'] = postdf.date.apply(to_week)
		retval["post.week"] = postdf
		
		postdf['weekday'] = postdf.date.apply(lambda x: x.weekday())
		retval["post.weekday"] = postdf
		
	if "user_post.like_week" in features_left:
	
		try:
			likedf = retval["user_post.like_date"]
		except:
			likedf = store.user_post.load_df(['like_date']).dropna()
			
		likedf['like_week'] = likedf.like_date.apply(to_week)
		retval["user_post.like_week"] = likedf
		
	return retval

# build function to calculate/retrieve fture from setup data	
def build_func(feature, data):

	source_df = data[feature.key]
	return source_df[feature.name]

# define fset: fture set template
fset = ft.FeatureSet("source_data", feature_list, setup_func, build_func)
fset.save(ft.prod_store, overwrite = True)
fset.save(ft.dev_store, overwrite = True)

