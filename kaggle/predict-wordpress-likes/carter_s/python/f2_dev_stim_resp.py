import pandas, pandas_ext, param, features as ft
import useful_stuff
from useful_stuff import *


# list of ftures to be defined by this module
feature_list = [
	ft.Feature('post', 'is_stim')
	, ft.Feature('post', 'is_resp')
	, ft.Feature('user_post', 'like_is_stim')
	, ft.Feature('user_post', 'like_is_resp')
	, ft.Feature('user', 'is_stim')
	, ft.Feature('user', 'is_resp')
	, ft.Feature('blog', 'is_stim')
	, ft.Feature('blog', 'is_resp')
]

# setup function to load data (from store) needed to calculate each Feature 
def setup_func(feature_list, store, recreate_setup):
	
	# Load universe of posts, set stim and resp
	post_df = store.post.load_df(store.post.keys())
	post_df['is_stim'] = (post_df.is_test == False) & (post_df.week < datetime(2012, 8, 6))
	post_df['is_resp'] = (post_df.is_test == False) & (post_df.week == datetime(2012, 8, 6))

	# Load univers of likes, set stim and resp
	like_df = store.user_post.load_df(store.user_post.keys())
	like_df = like_df[like_df.is_like]
	
	like_df.add_column(name = 'post_is_stim', column = post_df.is_stim, on_level = 'post_id')
	like_df['like_is_stim'] = (like_df.like_week < datetime(2012, 8, 6)) & (like_df.post_is_stim)
	
	like_df.add_column(name = 'post_is_resp', column = post_df.is_resp, on_level = 'post_id')
	like_df['like_is_resp'] = (like_df.like_week == datetime(2012, 8, 6)) & (like_df.post_is_resp)

	# Load universe of users, set stim and resp
	user_df = store.user.load_df(store.user.keys())
	
	stim_likes = like_df.groupby(level = 'user_id')['like_is_stim'].agg(sum)
	resp_likes = like_df.groupby(level = 'user_id')['like_is_resp'].agg(sum)
	user_df['is_stim'] = (stim_likes >= 4) & (resp_likes >= 1)
	user_df.is_stim = user_df.is_stim.fillna(False)
	
	user_df['is_resp'] = user_df.is_stim

	# Save blog data
	blog_df = store.blog.load_df(store.blog.keys())
	
	stim_posts = post_df.groupby('blog_id')['is_stim'].agg(sum)
	blog_df['is_stim'] = stim_posts > 0
	blog_df.is_stim = blog_df.is_stim.fillna(False)
	
	resp_posts = post_df.groupby('blog_id')['is_resp'].agg(sum)
	blog_df['is_resp'] = resp_posts > 0
	blog_df.is_resp = blog_df.is_resp.fillna(False)
	
	return {'user': user_df, 'blog': blog_df, 'post': post_df, 'user_post': like_df}



# build function to calculate/retrieve fture from setup data	
def build_func(feature, data):

	source_df = data[feature.scope]
	return source_df[feature.name]

# define fset: fture set template
fset = ft.FeatureSet("dev_stim_resp", feature_list, setup_func, build_func)
fset.save(ft.dev_store, overwrite = True)


