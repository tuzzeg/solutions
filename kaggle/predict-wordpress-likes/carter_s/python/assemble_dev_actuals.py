import pandas, pandas_ext, param, features as ft
import useful_stuff
from useful_stuff import *

print "Start saving list of all train resp likes", nowstring()

store = ft.dev_store
df = store.user_post.load_df(["like_is_resp"], reset_index = True)
df = df[df.pop("like_is_resp").fillna(False)]
df.to_csv(store.tmpdata + "TargetLikes.csv")
	
print "End saving list of all train resp likes", nowstring()
	