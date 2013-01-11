import pandas, pandas_ext, param, features as ft
import useful_stuff
from useful_stuff import *

import json, string, cPickle, numpy as np
from gensim import corpora, models, similarities
from HTMLParser import HTMLParser
from math import sqrt
# Corp.py
# Memory-friendly ways to deal with json files



with Lock():  # arbitrary block to hold 'set up' classes and variables - should be aux module?
# These are words that will be removed from posts, due to their frequency and poor utility in distinguishing between topics
	stop_words = [
		"a","able","about","across","after","all","almost","also","am","among","an","and","any","are","as","at","be","because",
		"been","but","by","can","cannot","could","did","do","does","either","else","ever","every","for","from","get","got","had","has","have",
		"he","her","hers","him","his","how","however","i","if","in","into","is","it","its","just","least","let","like","may","me","might",
		"most","must","my","neither","no","nor","not","of","off","often","on","only","or","other","our","own","rather","said","say","says",
		"she","should","since","so","some","than","that","the","their","them","then","there","these","they","this","to","too","us","wants",
		"was","we","were","what","when","where","which","while","who","whom","why","will","with","would","yet","you","your"
	]

	# Tools for stripping html
	class MLStripper(HTMLParser):
		def __init__(self):
			self.reset()
			self.fed = []
		def handle_data(self, d):
			self.fed.append(d)
		def get_data(self):
			return ''.join(self.fed)

	def strip_tags(html):
		s = MLStripper()
		s.feed(html)
		return s.get_data()

	# An object to read and parse files without loading them entirely into memory
	class Files():
		def __init__(self, files):
			self.files = files
			self.current_post_id = None
		def __enter__(self):
			return self
		
		def __exit__(self, exc_type, exc_value, traceback):
			self.close()
			
		def __iter__(self): # Read only one line at a time from the text files, to be memory friendly
			n = 0
			for f in self.files:
				f.seek(0) # Reset the file pointer before a new iteration
				for line in f:
					if((n)%1000 == 0): print "\t", "Files processed " + str(n), datetime.now()
					post = json.loads(line)
					self.current_post_id = post['post_id']
					content = post["content"]
					doc_words = []
					try: # parse and split the content up into a list of lower-case words
						doc_words = strip_tags(content).encode('ascii', 'ignore').translate(string.maketrans("",""), string.punctuation).lower().split()
					except: # Fails on some nasty unicode
						doc_words = []
					n+=1
					yield doc_words
		
		def __len__(self):
			n = 0
			for f in self.files:
				f.seek(0)
				for line in f:
					n += 1
			return n
		def close(self):
			for f in self.files:
				f.close()

	# A helper class, for use in gensim's LDA implementation
	class Corp():
		def __init__(self, files, dic):
			self.files = files
			self.dic = dic
		def __iter__(self):
			for (n, doc) in enumerate(self.files):
				if((n)%1000 == 0): print "\t", "Corp processed " + str(n), datetime.now()
				yield self.dic.doc2bow(doc)
		def __len__(self):
				return len(self.files)

	def veccos(vec1, vec2):
		if (vec1 is None) or (vec2 is None):
			return 0
		else:
			denom = sqrt(sum(v*v for (i, v) in vec1)) * sqrt(sum(v*v for (i, v) in vec2))
			if denom == 0:
				return 0
			else:
				return sum(v1*v2 for (i1, v1) in vec1 for (i2, v2) in vec2 if i1 == i2) / denom
		
		
			
feature_list = [
	ft.Feature("user_post", "topic_proximity_rank")
	, ft.Feature("user_post", "topic_proximity_mean")
	, ft.Feature("user_post", "topic_proximity_max")
]

def setup_func(feature_list, store, recreate_setup):
# store, recreate_setup = ft.prod_store, True

	if not recreate_setup:
		try:
			retval = pandas.HDFStore(store.path + "tmptopicdata.h5")["result"]
			print "loaded stored result"
			return retval
		except:
			print "didn't load stored result"
	
	# Create LDA model with 100 topics
	with Files([open(param.folders.source + "trainPosts.json"), open(param.folders.source + "testPosts.json")]) as myFiles:
		
		print "\tStart dict load: " + nowstring()
		
		try: 
			dictionary = corpora.dictionary.Dictionary.load(param.folders.root + "gensimdata/dictionary.saved")
		except:
			dictionary = corpora.Dictionary(doc for doc in myFiles)
			stop_ids = [dictionary.token2id[stopword] for stopword in stop_words if stopword in dictionary.token2id]
			infreq_ids = [tokenid for tokenid, docfreq in dictionary.dfs.iteritems() if docfreq < 50]
			dictionary.filter_tokens(stop_ids + infreq_ids) # remove stop words and words that appear infrequently
			dictionary.compactify() # remove gaps in id sequence after words that were removed

			dictionary.save(param.folders.root + "gensimdata/dictionary.saved")
		
		print "End dict load: ", datetime.now()
			
	# Train the LDA model calculating post affinity to topic 
	with Files([open(param.folders.source + "trainPosts.json"), open(param.folders.source + "testPosts.json")]) as myFiles:
	
		print "Start lda load: ", datetime.now()
		
		try:
			lda = models.ldamodel.LdaModel.load(param.folders.root + "gensimdata/lda.saved") 
		except:
			lda = models.ldamodel.LdaModel(corpus=Corp(myFiles, dictionary), id2word=dictionary, 
											num_topics=100, update_every=1, chunksize=10000, passes=1)
			lda.save(param.folders.root + "gensimdata/lda.saved")

		print "End lda load: ", datetime.now()

	# Calculate vector of topic affinity for each post
	with Files([open(param.folders.source + "trainPosts.json"), open(param.folders.source + "testPosts.json")]) as myFiles:
		
		print "Start post vectors load", datetime.now()
		
		try:
			post_vecs = cPickle.load(open(param.folders.root + "gensimdata/post_vecs.pickle", "r"))
		except:
			print "Didn't load"
			post_vecs = dict((myFiles.current_post_id, vec) for vec in lda[Corp(myFiles, dictionary)])
			cPickle.dump(post_vecs, open(param.folders.root + "gensimdata/post_vecs.pickle", "w"))
			
		print "End post vectors load", datetime.now()
		
	# Create 'similarity query' calculating 500 closest posts (in terms of topic affinity)
	# from the universe of response posts
	with Lock():
		
		print "Start query load", datetime.now()
		
		try:
			resp_post_array = cPickle.load(open(param.folders.root + "gensimdata/resp_post_query.keys.pickle", "r"))
			resp_post_query = similarities.Similarity.load(param.folders.root + "gensimdata/resp_post_query.sim")
		except:
			resp_post_array = store.post['is_resp'].fillna(False)
			resp_post_array = np.array(resp_post_array[resp_post_array].index)
			resp_post_query = similarities.Similarity(
				param.folders.root + "gensimdata/simdump_", 
				list(post_vecs[id] for id in resp_post_array), 
				num_features=100, num_best = 1000
			)
		
		print "End query load", datetime.now()
			
	with Lock():
		
		print "Start loading candidate", datetime.now()
		
		keys_needed = ["pagerank_by_like_share_postrank", "hist_like_ct", "all_blog_like_user_like_share"]
		for (n, k) in enumerate(k for k in keys_needed if k in store.user_blog.keys()):
			if n==0:
				index = store.user_blog[k].index 
			else:
				index = index | store.user_blog[k].index
		
		user_blog = pandas.DataFrame(list(index), columns = ["user_id", "blog_id"])
		
		user_resp = store.user['is_resp'].get_matching_indexes(True)
		blog_resp = store.blog['is_resp'].get_matching_indexes(True)
		user_blog = user_blog[user_blog.user_id.isin(user_resp) & user_blog.blog_id.isin(blog_resp)]
		
		post = store.post.load_df(["is_resp", "blog_id"], reset_index = True)
		post = post[post.pop("is_resp").fillna(False)]
		user_blog = user_blog.merge(post, on = "blog_id", how = "inner")
		
		candidate_groups = user_blog[["user_id", "post_id"]].groupby("user_id")
		
		stim_likes = store.user_post.load_df(["like_is_stim"], reset_index = True)
		stim_likes = stim_likes[stim_likes.pop('like_is_stim').fillna(False)]
	
		print "End loading candidates", datetime.now()
	
	with Lock():
	
		resp_users = pandas.Series(user_resp, name = "user_id").groupby(user_resp)
		prog = Struct(n = 0, start = datetime.now(), nmax = len(candidate_groups))		
		def evaluate_user_group(group_df):
		# group_df = resp_users.get_group(resp_users.groups.keys()[1])
			
			if prog.n == 0: prog.start = datetime.now()
			
			
			user_id = group_df.values[0]
			user_likes = stim_likes.post_id[stim_likes.user_id == user_id]
			try:
				cand_posts = dict((id, None) for id in candidate_groups.get_group(user_id).post_id)
			except:
				cand_posts = dict()
			
			user_corr = [tpl for id in user_likes for tpl in post_vecs[id]]
			user_like_vec = None
			if len(user_corr) != 0:
				user_like_vec = pandas.DataFrame(user_corr, columns = ["array_index", "score"])
				user_like_vec = user_like_vec.groupby("array_index")["score"].sum() / len(user_likes)
				user_like_vec = zip(user_like_vec.index, user_like_vec.values)
				best_posts = dict((resp_post_array[i], rho) for i, rho in resp_post_query[user_like_vec])
				cand_posts.update(best_posts)
			
				# get (stim_like, resp_post, correlation) for top N(500) correlated resp_posts per stim
				# correlations = [
					# (stim_post_id, resp_post_array[resp_post_index], rho) 
					# for stim_post_id in user_likes for resp_post_index, rho in resp_post_query[post_vecs[stim_post_id]]
				# ]
				# corr_df = pandas.DataFrame(correlations, columns = ["stim_post_id", "resp_post_id", "rho"])
				# best_posts.sort()
				# best_posts = best_posts.tail(500).index	
			
			if len(cand_posts) == 0:
				return_df = None
			else:
				stim_like_query = similarities.SparseMatrixSimilarity([post_vecs[id] for id in user_likes], num_terms = 100)
				def f(kvtuple):
					id, avgvec_rho = kvtuple
					resp_vec = post_vecs[id]
					if avgvec_rho is None: avgvec_rho = veccos(user_like_vec, resp_vec)
					correlations = stim_like_query[resp_vec]
					return (correlations.mean(), correlations.max(), avgvec_rho)
				return_df = pandas.DataFrame(map(f, cand_posts.iteritems())
											, columns = ["mean_rho", "max_rho", "avgvec_rho"], index = cand_posts)
				
				return_df = return_df.sort("mean_rho", ascending = False)
				return_df['mean_rho_rank'] = range(1, len(return_df) + 1)
			
			prog.n = prog.n + 1
			print nowstring(),  str(prog.n)+"/"+str(prog.nmax), user_id, len(return_df), (prog.start + prog.nmax * (datetime.now() - prog.start) / prog.n).strftime("%Y-%m-%d %H:%M:%S")
			return return_df
		return_value = resp_users.apply(evaluate_user_group)
		
		return_value.index.names = ['user_id', 'post_id']					
		pandas.HDFStore(store.path + "tmptopicdata.h5")["result"] = return_value
	return return_value
	
# build function to calculate/retrieve fture from setup data	
def build_func(feature, data):

	if feature.name == "topic_proximity_rank":
		return data['mean_rho_rank']
	elif feature.name == "topic_proximity_mean":
		return data['mean_rho']
	elif feature.name == "topic_proximity_max":
		return data['max_rho']
	
fset = ft.FeatureSet("all_data", feature_list, setup_func, build_func)
fset.save(ft.dev_store, overwrite = True)
fset.save(ft.prod_store, overwrite = True)
	

	