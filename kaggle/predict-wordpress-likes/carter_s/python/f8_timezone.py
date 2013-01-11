import pandas, pandas_ext, param, features as ft
import useful_stuff
from useful_stuff import *

import jsonfiles as jf
import math

# underyling functions
with Lock():

	def angular_mean(vectors):
		return math.atan2(
			sum(v[0] for v in vectors)/float(len(vectors))
			, sum(v[1] for v in vectors)/float(len(vectors))
		)
		
	def time_zn_dist(tz1, tz2):
		return min(abs(tz1 + 24 - tz2), abs(tz1 - tz2), abs(tz1 - 24 - tz2))

# subs
with Lock():

	def get_candidates(store):
	
		print "Start candidates:", nowstring()

		# get all user post candidates with user_blog records qualifying
		def f():
		
			df = store.user_blog.load_df(
				["hist_like_ct", "all_user_like_blog_post_share", "pagerank_by_like_share_postrank"]
				, prefix = "user_blog", reset_index = True
			)
			
			df = df[
				(df.user_id.isin(store.user['is_resp'].get_matching_indexes(True)))
				& (df.blog_id.isin(store.blog['is_resp'].get_matching_indexes(True)))
			]
			
			df = df[
				(df.user_blog_hist_like_ct > 0)
				| (df.user_blog_all_user_like_blog_post_share > 0)
				| (df.user_blog_pagerank_by_like_share_postrank > 0)
			]
			
			post_df = store.post.load_df(["blog_id", "is_resp"], prefix = "post", reset_index = True)
			post_df = post_df.rename(columns = {'post_blog_id': 'blog_id'})
			post_df = post_df[post_df.pop("post_is_resp").fillna(False)]
			
			return df.merge(post_df, how = "inner", on = ["blog_id"])
		df = f()
		print "\tHist and page rank candidates", len(df)
		
		# add all user post candidates with topic proximity qualifying
		def f(df):
		
			user_post_df = store.user_post.load_df(["topic_proximity_rank"]
													, prefix = "user_post", reset_index = True)						
			user_post_df = user_post_df[user_post_df.user_post_topic_proximity_rank > 0]
			
			post_df = store.post.load_df(["blog_id", "is_resp"], prefix = "post", reset_index = True)
			post_df = post_df.rename(columns = {'post_blog_id': 'blog_id'})
			post_df = post_df[post_df.pop("post_is_resp").fillna(False)]
			
			user_post_df = user_post_df.merge(post_df, how = "inner", on = ["post_id"])
			
			return df.merge(user_post_df, how = "outer", on = ["user_id", "post_id", "blog_id"])
		
		df = f(df)
		print "\tAdd topic candidate:", len(df)

		df = df[
				(df.user_blog_hist_like_ct > 0)
				| (df.user_blog_all_user_like_blog_post_share > 0)
				| ((df.user_blog_pagerank_by_like_share_postrank > 0) & (df.user_blog_pagerank_by_like_share_postrank < 1000))
				| ((df.user_post_topic_proximity_rank > 0) & (df.user_post_topic_proximity_rank < 1000))
		]
		
		# return data
		print "End candidates", nowstring()
		return df[["user_id", "post_id"]]

# feature list
feature_list = [
	ft.Feature("post", "time_zone")
	, ft.Feature("user", "mean_stim_like_time_zone")
	, ft.Feature("user_post", "tz_proximity_to_stim_likes")
]

def setup_func(feature_list, store, recreate_setup):
# store, recreate_setup = ft.dev_store, True

	# get all tz; convert to vectors (location of tz looking at world from N pole, with GMT at left 
	post_tz = jf.PostFile.get_df(["time_zone"]).time_zone
	post_vec = post_tz.apply(lambda x: (math.sin(x * (math.pi / 12.0)), math.cos(x * (math.pi / 12.0))))
				
	# calculate average time zone of user likes using TZ vector
	df = store.user_post.load_df(["like_is_stim"], reset_index = True)
	df = df[df.like_is_stim.fillna(False)]
	df['vec_from_gmt'] = post_vec[df.post_id].values

	user_tz = df.groupby("user_id")["vec_from_gmt"].aggregate(angular_mean)
	user_tz = (12.0 / math.pi) * user_tz
	
	# calculate distance between for all candidates
	candidates = get_candidates(store)
	cand_groups = candidates.groupby("user_id")
	
	def process_group(groupdf):
		user_id = groupdf.user_id.values[0]
		distance = map(lambda x: time_zn_dist(user_tz[user_id], post_tz[x]), groupdf.post_id)
		return pandas.DataFrame({'user_id': groupdf.user_id, 'post_id': groupdf.post_id, 'distance': distance})
	distance = cand_groups.apply(process_group)
	
	distance = pandas.Series(distance.distance, index = pandas.MultiIndex.from_arrays([distance.user_id, distance.post_id]))
	
	return Struct(post_tz = post_tz.apply_name("time_zone"), user_tz = user_tz.apply_name("mean_stim_like_time_zone"), 
					distance_tz = distance)
					
def build_func(feature, data):
	
	if feature.key == "post.time_zone":
		return data.post_tz
	elif feature.key == "user.mean_stim_like_time_zone":
		return data.user_tz
	elif feature.key == "user_post.tz_proximity_to_stim_likes":
		return data.distance_tz
	
fset = ft.FeatureSet("time_zone", feature_list, setup_func, build_func)
fset.save(ft.dev_store, overwrite = True)
fset.save(ft.prod_store, overwrite = True)

	
