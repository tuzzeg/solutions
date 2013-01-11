import pandas, pandas_ext, param, features as ft
import useful_stuff
from useful_stuff import *


# list of ftures to be defined by this module
feature_list = [
	ft.Feature("user_blog", "pagerank_by_like_share_postrank")
	, ft.Feature("user_blog", "pagerank_by_like_share_blogprob")
]
	
# setup function to load data (from store) needed to calculate each Feature 
def setup_func(feature_list, store, recreate_setup):
	
	# Get blog share of user likes and user share of blog likes over stim period "all"
	# Use these as transition probabilities for user moving to other blogs, users
	user_blog_df = store.user_blog.load_df(
		["all_blog_like_user_like_share", "all_user_like_blog_post_share"], reset_index = True)
	user_blog_df = user_blog_df.rename(columns = {
		"all_blog_like_user_like_share": "bu_prob", 
		"all_user_like_blog_post_share": "ub_prob"
	})
	
	# get list of stim users - they're only ones we ultimately care about
	tmp = store.user.load_df(['is_stim'], reset_index = True)
	stim_users = tmp[tmp.is_stim.fillna(False)].user_id
	
	# function to get all current user blog indexes for which data is needed	
	def get_user_blog_index(store):
	
		keys_needed = ["hist_like_ct", "all_blog_like_user_like_share"]
		for (n, k) in enumerate(k for k in keys_needed if k in store.user_blog.keys()):
			if n==0:
				index = store.user_blog[k].index 
			else:
				index = index | store.user_blog[k].index
		
		keys_needed = ["topic_proximity_rank"]
		post_index = None
		for (n, k) in enumerate(k for k in keys_needed if k in store.user_post.keys()):
			if n == 0:
				post_index = store.user_post[k].index 
			else:
				post_index = post_index | store.user_post[k].index
				
		if(post_index is not None):
			blog_ids = store.post["blog_id"]
			post_index = pandas.MultiIndex.from_tuples(set((user_id, blog_ids[post_id]) for user_id, post_id in post_index))
			post_index.names = index.names
			index = index | post_index
		
		return index
	
	blog_is_needed = pandas.Series(True, index = get_user_blog_index(store), name = "is_needed").sort_index()
	
	# get count of resp posts
	blog_resp_posts = store.post.load_df(["blog_id", "is_resp"])
	blog_resp_posts = blog_resp_posts.groupby("blog_id")["is_resp"].agg(lambda x: x.apply(float).sum())
	
	# function to calculate blog rank for group of users
	def get_user_blog_prob(user_chunk):
	
		df = user_blog_df[user_blog_df.user_id.isin(user_chunk)]
		
		df = df.merge(user_blog_df, on = "blog_id", suffixes = ["_1", "_2"])
		df['prob'] = df.ub_prob_1 * df.bu_prob_2
		df = df.groupby(['user_id_1', 'user_id_2'], as_index = False).agg({'prob': sum})
		
		df = df.merge(user_blog_df, left_on = "user_id_2", right_on = "user_id", suffixes = ["", "_3"])
		df['prob'] = df.prob * df.ub_prob
		df = df.groupby([df.user_id_1, df.blog_id]).agg({'prob': sum})
		
		df.index.names = ['user_id', 'blog_id']
		
		return df
		
	# function to filter to needed blogs and post counts and add prob column
	def filter_and_add_data(prob_df, resp_posts_needed):
		
		groups = prob_df.groupby(level = "user_id")
		
		def f(group_df):
		
			user_id = group_df.index[0][0]
			
			group_df = group_df.sort("prob", ascending = False)
			
			try:
				is_needed = blog_is_needed.ix[user_id]
			except:
				is_needed = pandas.Series(False, index = group_df.get_index_column('blog_id'))
			group_df.add_column(name = "is_needed", column = is_needed, on_level = "blog_id")
			
			group_df.add_column(name = "blog_resp_posts", column = blog_resp_posts, on_level = "blog_id")
			group_df["post_rank"] = 1 + group_df.blog_resp_posts.cumsum() - group_df.blog_resp_posts
			
			group_df = group_df[(group_df.pop("blog_resp_posts") > 0) 
								& ((group_df.post_rank <= resp_posts_needed) 
									| (group_df.pop("is_needed").fillna(False)))]
		
			return group_df
			
		r = groups.apply(f)
		r.index = r.index.droplevel(0)
		return r
		
	# function to calculate blog rank from group of users
	def process_user_chunk(user_chunk):
		prob_df = get_user_blog_prob(user_chunk)
		final_data = filter_and_add_data(prob_df, 1000)
		return final_data
		
	result = stim_users.chunk_apply(process_user_chunk, 50, True)
	return result
		
# build function to calculate/retrieve fture from setup data	
def build_func(feature, data):

	if feature.name == "pagerank_by_like_share_postrank":
		return data['post_rank']
	elif feature.name == "pagerank_by_like_share_blogprob":
		return data['prob']
	
	

# define fset: fture set template
fset = ft.FeatureSet("period_data", feature_list, setup_func, build_func)
fset.save(ft.prod_store, overwrite = True)
fset.save(ft.dev_store, overwrite = True)
