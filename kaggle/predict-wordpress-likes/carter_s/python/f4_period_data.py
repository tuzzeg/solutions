import pandas, pandas_ext, param, features as ft
import useful_stuff
from useful_stuff import *


# list of ftures to be defined by this module
feature_list = []
for (name, period) in [('hist', -1), ('all', 0), ('weekM1', 1), ('weekM2', 2), ('weekM3', 3)]:
	
	if period != -1:
		feature_list.append(ft.Feature('blog', name + '_post_ct'
			, period_name = name, period_index = period, calc_type = 'blog_post_ct'))
		feature_list.append(ft.Feature('blog', name + '_like_ct'
			, period_name = name, period_index = period, calc_type = 'blog_like_ct'))
		feature_list.append(ft.Feature('user', name + '_like_ct'
			, period_name = name, period_index = period, calc_type = 'user_like_ct'))
			
	feature_list.append(ft.Feature('user_blog', name + '_blog_post_user_like_share'
		, period_name = name, period_index = period, calc_type = 'blog_post_user_like_share'))
	feature_list.append(ft.Feature('user_blog', name + '_blog_like_user_like_share'
		, period_name = name, period_index = period, calc_type = 'blog_like_user_like_share'))
	feature_list.append(ft.Feature('user_blog', name + '_user_like_blog_post_share'
		, period_name = name, period_index = period, calc_type = "user_like_blog_post_share"))


# setup function to load data (from store) needed to calculate each Feature 
def setup_func(feature_list, store, recreate_setup):

	retval = dict()
	needed_periods = list(set(f.period_index for f in feature_list))
	
	# Get historical stats first if needed
	if -1 in needed_periods:
	
		blog_df = store.blog.load_df(["hist_post_ct", "hist_like_ct"])
		user_df = store.user.load_df(["hist_like_ct"])
		user_blog_df = store.user_blog.load_df(["hist_like_ct"])

		retval[-1] = Struct(
			blog_post_ct = blog_df.hist_post_ct
			, blog_like_ct = blog_df.hist_like_ct
			, user_like_ct = user_df.hist_like_ct
			, blog_post_is_user_like_ct = user_blog_df.hist_like_ct
			, user_like_is_blog_post_ct = user_blog_df.hist_like_ct
		)
		needed_periods.remove(-1)
		
	# Get raw data needed
	if len(needed_periods) > 0:
	
		like_df = store.user_post.load_df(["is_like", "like_is_stim", "like_week"], reset_index = True)
		like_df = like_df[like_df.like_is_stim]

		post_df = store.post.load_df(["blog_id", "week", "is_stim"], prefix = "post", reset_index = True)
		post_df = post_df[post_df.post_is_stim]
		post_df = post_df.rename(columns = {"post_blog_id": "blog_id"})

		now_df = post_df.merge(like_df, how = "left", on = "post_id")
		now_df['like_ct'] = now_df.is_like.fillna(False).apply(float)
	
		# Get weekly, all stats if needed
		stim_weeks = list(set(now_df.post_week[now_df.post_is_stim.fillna(False)]))
		for p in needed_periods:
		
			post_df = now_df.copy()
			like_df = now_df[now_df.is_like.fillna(False)]
			
			if p > 0:  # (0 is all weeks, 1, 2, 3 are now - 1 week, -2 week, ...
				post_df = post_df[post_df.post_week == stim_weeks[-p]]
				like_df = like_df[like_df.like_week == stim_weeks[-p]]
			
			retval[p] = Struct(
				blog_post_ct = post_df.groupby(['blog_id'])['post_id'].nunique()
				, blog_like_ct = like_df.groupby(['blog_id'])['like_ct'].sum()
				, user_like_ct = like_df.groupby(['user_id'])['like_ct'].sum()
				, blog_post_is_user_like_ct = post_df.groupby(['user_id', 'blog_id'])['like_ct'].sum()
				, user_like_is_blog_post_ct = like_df.groupby(['user_id', 'blog_id'])['like_ct'].sum()
			)
			
			
	return retval

			
# build function to calculate/retrieve fture from setup data	
def build_func(feature, data):

	per_data = data[feature.period_index]
	
	if feature.calc_type == 'blog_post_ct':
		return per_data.blog_post_ct
	elif feature.calc_type == 'blog_like_ct':
		return per_data.blog_like_ct
	elif feature.calc_type == 'user_like_ct':
		return per_data.user_like_ct
	elif feature.calc_type == 'blog_post_user_like_share':
		return (per_data.blog_post_is_user_like_ct 
				/ per_data.blog_post_ct.reindex(per_data.blog_post_is_user_like_ct.index, level = "blog_id"))
	elif feature.calc_type == 'blog_like_user_like_share':
		return (per_data.user_like_is_blog_post_ct 
				/ per_data.blog_like_ct.reindex(per_data.user_like_is_blog_post_ct.index, level = "blog_id"))
	elif feature.calc_type == 'user_like_blog_post_share':
		return (per_data.user_like_is_blog_post_ct 
				/ per_data.user_like_ct.reindex(per_data.user_like_is_blog_post_ct.index, level = "user_id"))
	else: 
		return None
	

# define fset: fture set template
fset = ft.FeatureSet("period_data", feature_list, setup_func, build_func)
fset.save(ft.dev_store, overwrite = True)
fset.save(ft.prod_store, overwrite = True)


