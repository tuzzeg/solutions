import pandas, pandas_ext, param, features as ft
import useful_stuff
from useful_stuff import *
import jsonfiles as jf


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

		


# list of ftures to be defined by this module
feature_list = [
	ft.Feature('post', 'author')
	, ft.Feature('blog', 'author_ct')
	, ft.Feature('user', 'as_author_post_ct')
	, ft.Feature('user', 'as_author_post_user_like_share')
	, ft.Feature('user_post', 'author_post_ct')
	, ft.Feature('user_post', 'author_like_ct')	
	, ft.Feature('user_post', 'blog_post_author_post_share')
	, ft.Feature('user_post', 'blog_like_author_like_share')
	, ft.Feature('user_post', 'author_post_user_like_share')
	, ft.Feature('user_post', 'author_like_user_like_share')
	, ft.Feature('user_post', 'user_like_author_post_share')
	, ft.Feature('user_post', 'user_is_blog_author')
	, ft.Feature('user_post', 'user_is_post_author')
]


# setup function to load data (from store) needed to calculate each Feature 
def setup_func(feature_list, store, recreate_setup):
	
	
	# Get raw data needed
	post_author_df = jf.PostFile.get_df(["author", "blog_id"])
	post_author_df = post_author_df.reset_index()
	
	# Create post/like df's for stat craeteion
	like_df = store.user_post.load_df(["is_like", "like_is_stim", "like_week"], reset_index = True)
	like_df = like_df[like_df.like_is_stim.fillna(False)]

	post_df = store.post.load_df(["blog_id", "week", "is_stim"], prefix = "post", reset_index = True)
	post_df = post_df[post_df.post_is_stim.fillna(False)]
	post_df = post_df.rename(columns = {"post_blog_id": "blog_id"})
	post_df = post_df.merge(post_author_df, on = ["post_id", "blog_id"], how = "left")

	data_df = post_df.merge(like_df, how = "left", on = "post_id")
	data_df['like_ct'] = data_df.is_like.fillna(False).apply(float)
	
	# Create stats from stim data
	author_df = pandas.DataFrame({
		'author_post_ct': data_df.groupby(['author'])['post_id'].nunique()
		, 'author_like_ct': data_df.groupby(['author'])['like_ct'].sum()
	})
	
	blog_df = pandas.DataFrame({
		'blog_post_ct': data_df.groupby(['blog_id'])['post_id'].nunique()
		, 'blog_like_ct': data_df.groupby(['blog_id'])['like_ct'].sum()
	})
	
	user_df = pandas.DataFrame({
		'user_like_ct': data_df.groupby(['user_id'])['like_ct'].sum()
	})
	
	user_author_df = pandas.DataFrame({
		'user_like_of_author_post_ct': data_df.groupby(['user_id', 'author'])['like_ct'].sum()
	})
	
	# Load candidate information, including relationship of user to post/blog
	blog_authors = post_author_df.groupby(['blog_id'])['author'].apply(lambda x: x.values)
	
	cand_df = get_candidates(store)
	cand_df = cand_df.merge(post_author_df, how = "inner", on = "post_id")
	cand_df['blog_authors'] = blog_authors[cand_df.blog_id.values].values
	
	cand_blog_groups = cand_df.groupby(['blog_id'])
	def author_relation(group_df):

		group_df['is_blog_author'] = group_df.user_id.isin(group_df.blog_authors.values[0])
		group_df['is_post_author'] = (group_df.user_id == group_df.author)
		
		return group_df
		
	cand_df = cand_blog_groups.apply(author_relation)
	cand_df.index = pandas.MultiIndex.from_arrays([cand_df.user_id, cand_df.post_id])
	
	# Add candidate stats
	cand_df = cand_df.merge(author_df.reset_index(), on = "author", how = "left")
	cand_df = cand_df.merge(blog_df.reset_index(), on = "blog_id", how = "left")
	cand_df = cand_df.merge(user_df.reset_index(), on = "user_id", how = "left")
	cand_df = cand_df.merge(user_author_df.reset_index(), on = ["user_id", "author"], how = "left")
		
	# User like rate of own posts
	user_as_author_post_ct = data_df.groupby(['author'])['post_id'].nunique().apply(float)
	user_as_author_post_ct = user_as_author_post_ct[store.user['is_resp'].get_matching_indexes(True)]
	
	user_as_author_like_ct = data_df[data_df.user_id == data_df.author].groupby("user_id")["like_ct"].sum()
	user_as_author_like_ct = user_as_author_like_ct[store.user['is_resp'].get_matching_indexes(True)]
	
	# Return values
	cand_df.index = pandas.MultiIndex.from_arrays([cand_df.user_id, cand_df.post_id])
	retval = Struct(
		author = pandas.Series(post_author_df.author, index = post_author_df.post_id)
		, author_ct = blog_authors.apply(lambda x: len(set(x)))
		, as_author_post_ct = user_as_author_post_ct
		, as_author_post_user_like_share = user_as_author_like_ct / user_as_author_post_ct
		, author_post_ct = cand_df.author_post_ct.dropna()
		, author_like_ct = cand_df.author_like_ct.dropna()
		, blog_post_author_post_share = (cand_df.author_post_ct / cand_df.blog_post_ct).dropna()
		, blog_like_author_like_share = (cand_df.author_like_ct / cand_df.blog_like_ct).dropna()
		, author_post_user_like_share = (cand_df.user_like_of_author_post_ct / cand_df.author_post_ct).dropna()
		, author_like_user_like_share = (cand_df.user_like_of_author_post_ct / cand_df.author_like_ct).dropna()
		, user_like_author_post_share = (cand_df.user_like_of_author_post_ct / cand_df.user_like_ct).dropna()
		, user_is_blog_author = cand_df.is_blog_author
		, user_is_post_author = cand_df.is_post_author
	)
	return retval

			
# build function to calculate/retrieve fture from setup data	
def build_func(feature, data):

	return getattr(data, feature.name)
	

# define fset: fture set template
fset = ft.FeatureSet("period_data", feature_list, setup_func, build_func)
fset.save(ft.dev_store, overwrite = True)
fset.save(ft.prod_store, overwrite = True)

