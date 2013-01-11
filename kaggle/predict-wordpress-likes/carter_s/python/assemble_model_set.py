import pandas, pandas_ext, param, features as ft
import useful_stuff
from useful_stuff import *



def get_all_possible(store):
	
	print "Start all possible:", nowstring()

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

	# Reduce
	df = df[
			(df.user_blog_hist_like_ct > 0)
			| (df.user_blog_all_user_like_blog_post_share > 0)
			| ((df.user_blog_pagerank_by_like_share_postrank > 0) & (df.user_blog_pagerank_by_like_share_postrank < 1000))
			| ((df.user_post_topic_proximity_rank > 0) & (df.user_post_topic_proximity_rank < 1000))
	]
	print "\tJust potentials:", len(df)
		
	# Add result
	def f(df):
		like_df = store.user_post.load_df(["like_is_resp"], reset_index = True)
		df = df.merge(like_df, how = "left", on = ["user_id", "post_id"])
		df['result'] = df.pop("like_is_resp").fillna(False).apply(float)
		return df
	df = f(df)

	# return dat
	print "End all possible:", nowstring()
	return(df)

	
def select_candidates(df, store, pagecutoff, proxcutoff):

	cand = (
		(df.user_blog_pagerank_by_like_share_postrank <= pagecutoff)
		| (df.user_blog_hist_like_ct > 0)
		| (df.user_blog_all_user_like_blog_post_share > 0)
		| (df.user_post_topic_proximity_rank <= proxcutoff)
	)
	
	rows = df[cand].user_id.value_counts()
	like_df = store.user_post.load_df(["like_is_resp"], reset_index = True)

	print "Total rows;", cand.sum()
	print "Rows per user:", cand.sum() / df.user_id.nunique()
	print "Users under 100 rows:", len(rows[rows < 100])
	print "Percent used:", cand.sum()/float(len(df))
	print "Percent available likes used:", df.result[cand].sum()/df.result.sum()
	print "Percent all likes used:", df.result[cand].sum()/like_df.like_is_resp.sum()
	
	df = df[cand][["user_id", "blog_id", "post_id", "result"]]
	return df


def add_data(df, store):

	print "Start add data", nowstring()
	
	# blog data
	def f(df):
		blog = store.blog.load_df(
			[	
				"all_like_ct", "all_post_ct", "hist_like_ct" ,"hist_post_ct", "weekM1_like_ct", "weekM1_post_ct"
				, "weekM2_like_ct", "weekM2_post_ct", "weekM3_like_ct", "weekM3_post_ct", "is_english", "author_ct"
			]
			, prefix = "blog", reset_index = True
		)
		return df.merge(blog, on = "blog_id", how = "left")
	df = f(df)
	print "\tAdded blog", nowstring()
	
	# user data
	def f(df):
		user = store.user.load_df(
			[
				"all_like_ct", "hist_like_ct", "weekM1_like_ct", "weekM2_like_ct", "weekM3_like_ct"
				, "is_english", "mean_stim_like_time_zone", 'as_author_post_ct', 'as_author_post_user_like_share'
			]
			, prefix = "user", reset_index = True
		)
		return df.merge(user, on = "user_id", how = "left")
	df = f(df)
	print "\tAdded user", nowstring()
	
	# post data
	def f(df):
		post = store.post.load_df(["weekday", "time_zone"], prefix = "post", reset_index = True)
		return df.merge(post, on = "post_id", how = "left")
	df = f(df)
	print "\tAdded post", nowstring()
	
	# userblog data
	def f(df):
		columns_needed = [
			"all_blog_like_user_like_share", 
			"all_blog_post_user_like_share", 
			"all_user_like_blog_post_share", 
			"hist_blog_like_user_like_share", 
			"hist_blog_post_user_like_share", 
			"hist_like_ct", 
			"hist_user_like_blog_post_share", 
			"pagerank_by_like_share_blogprob", 
			"pagerank_by_like_share_postrank", 
			"weekM1_blog_like_user_like_share", 
			"weekM1_blog_post_user_like_share", 
			"weekM1_user_like_blog_post_share", 
			"weekM2_blog_like_user_like_share", 
			"weekM2_blog_post_user_like_share", 
			"weekM2_user_like_blog_post_share", 
			"weekM3_blog_like_user_like_share", 
			"weekM3_blog_post_user_like_share", 
			"weekM3_user_like_blog_post_share",
			"lang_proximity"
		]
		for c in columns_needed:
			user_blog = store.user_blog.load_df([c], reset_index = True, prefix = "user_blog")
			df = df.merge(user_blog, on = ["user_id", "blog_id"], how = "left")
			print "Added from user_blog column", c, "for total rows", len(df)
		return df
	df = f(df)
	print "\tAdded userblog", nowstring()

	# userpost data		
	def f(df):
		columns_needed = [
			"topic_proximity_max", 
			"topic_proximity_mean", 
			"topic_proximity_rank", 
			"tz_proximity_to_stim_likes",
			"blog_post_author_post_share",
			"blog_like_author_like_share",
			"author_post_user_like_share",
			"author_like_user_like_share",
			"user_like_author_post_share",
			"user_is_blog_author",
			"user_is_post_author"
		]
		for c in columns_needed:
			userpost = store.user_post.load_df([c], reset_index = True, prefix = "user_post")
			df = df.merge(userpost, on = ["user_id", "post_id"], how = "left")
			print "Added from user_post column", c, "for total rows", len(df)
			print
		return df
	df = f(df)
	print "\tAdded userpost", nowstring()

	print "End add data", nowstring()
	return df
	
def save_dataset(store, pagerank_cut, topicprox_cut):
#store, pagerank_cut, topicprox_cut = ft.dev_store, 400, 100

	# Assemble potential candidates
	df = get_all_possible(store)
	
	
	# Get candidate cutoffs
	df = select_candidates(df, store, pagerank_cut, topicprox_cut)
	df = add_data(df, store)

	# Fill missing values - anything different for langauge
	for k in df.columns:
		if df[k].dtype in ['float64', 'int64']: 
			df[k] = df[k].fillna(0)
	
	# Save result
	df.to_csv(store.tmpdata + "RDataSet.csv")

print "Started Assembly:", nowstring()
save_dataset(ft.dev_store, 400, 100)
save_dataset(ft.prod_store, 400, 100)
print "Ended Assembly:", nowstring()
