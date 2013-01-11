import pandas, pandas_ext, param, features as ft
import useful_stuff
from useful_stuff import *

import jsonfiles as jf, collections as coll, random, cPickle

from math import sqrt
from nltk.util import trigrams as nltk_trigrams
from nltk.tokenize import word_tokenize as nltk_word_tokenize
from nltk.probability import FreqDist
from nltk.corpus.util import LazyCorpusLoader
from nltk.corpus.reader.api import CorpusReader
from nltk.corpus.reader.util import StreamBackedCorpusView, concat

# Setup classes
with Lock():

	def veccos(vec1, vec2):
		if (vec1 is None) or (vec2 is None):
			return 0
		else:
			denom = sqrt(sum(v*v for (i, v) in vec1)) * sqrt(sum(v*v for (i, v) in vec2))
			if denom == 0:
				return 0
			else:
				return sum(v1*v2 for (i1, v1) in vec1 for (i2, v2) in vec2 if i1 == i2) / denom

	class LangIdReader(CorpusReader):

		CorpusView = StreamBackedCorpusView

		def _get_trigrams(self, line):
			data = line.strip().split(' ')
			if len(data) == 2:
				return (data[1], int(data[0]))

		def _read_trigram_block(self, stream):
			freqs = []
			for i in range(20): 
				freqs.append(self._get_trigrams(stream.readline()))
			return filter(lambda x: x != None, freqs)

		def freqs(self, fileids=None):
			return concat([self.CorpusView(path, self._read_trigram_block) for path in self.abspaths(fileids=fileids)])

	class LangIDDict(dict):
	
		def __init__(self):	
		
			allids = dict()
			for line in open("./languagedata/table.txt"):
				linedata = [s.strip() for s in line.split("\t")]
				langid = linedata[0]
				langname = linedata[2]
				allids[langid.lower()] = langname
				
			for line in open("./languagedata/toplangid.csv"):
				self[line.strip().lower()] = allids[line.strip().lower()]
								
	class LangDetector(object):
		
		def __init__(self, languages=LangIDDict().keys()):
		
			self.language_trigrams = {}
			self.langid = LazyCorpusLoader('langid', LangIdReader, r'(?!\.).*\.txt')
			
			for lang in languages:
				self.language_trigrams[lang] = FreqDist()
				for f in self.langid.freqs(fileids=lang+"-3grams.txt"):
					self.language_trigrams[lang].inc(f[0], f[1])
				self.language_dicts = dict([
					(id, dict([(trigram, float(value)/float(fdist.N())) for trigram, value in fdist.items()]))
					for id, fdist in self.language_trigrams.items()
				])
				
		def detect(self, text):
		
			words = nltk_word_tokenize(text.lower())
			trigrams = {}
			scores = dict([(lang, 0) for lang in self.language_trigrams.keys()])

			trigcount = [(trigram, 1.0) for match in words for trigram in self.get_word_trigrams(match)]
			if len(trigcount) > 0:
				trigdf = pandas.DataFrame(trigcount, columns = ["key", "value"])
				trigrams = trigdf.groupby("key")["value"].sum().to_dict()
			else:
				trigrams = {}

			total = sum(trigrams.values())
			maxscore, maxid = 0, ""
			for trigram, count in trigrams.items():
				trishare = (float(count) / float(total))
				for lang, frequencies in filter(lambda (l, f): trigram in f, self.language_dicts.iteritems()):
					scores[lang] += frequencies[trigram] * trishare
					if scores[lang] > maxscore:
						maxid, maxscore = lang, scores[lang]
						
			return sorted(scores.items(), key=lambda x: x[1], reverse=True)

		def get_word_trigrams(self, match):
			return [''.join(trigram) for trigram in nltk_trigrams(match) if trigram != None]

# Setup functions
with Lock():


	def detect_post_groups(post_df, post_content, key_column_name, detector):
		
		ret_dict = dict()

		groups = post_df.groupby(key_column_name)
		for (n, (group_id, grouped_posts)) in enumerate(groups):
			
			if (n % 100) == 0:  print "\tDetecting posts by", key_column_name, "#", n, "at", nowstring()
			
			liked_words = [word for post_id in grouped_posts.post_id for word in post_content[post_id]]
			if len(liked_words) == 0:
				ret_dict[group_id] = []
			elif len(liked_words) <= 1000:
				ret_dict[group_id] = detector.detect(" ".join(liked_words))				
			else:
				ret_dict[group_id] = detector.detect(" ".join(random.sample(liked_words, 1000)))
		
		return ret_dict
		
		
	def get_lang_vecs(store):
	# store = ft.dev_store
			
		# Get users/likes
		resp_users = store.user.load_df(["is_resp"], reset_index = True)
		resp_users = resp_users[resp_users.pop("is_resp").fillna(False)]
		
		stim_likes = store.user_post.load_df(["like_is_stim"], reset_index = True)
		stim_likes = stim_likes[stim_likes.pop("like_is_stim").fillna(False)]
		stim_likes = stim_likes[stim_likes.user_id.isin(resp_users.user_id)]
		
		# Get stim_posts, resp_blogs
		resp_blogs = store.blog.load_df(["is_resp"], reset_index = True)
		resp_blogs = resp_blogs[resp_blogs.pop("is_resp").fillna(False)]
		
		blog_posts = store.post.load_df(["blog_id"], reset_index = True)
		blog_posts = blog_posts[blog_posts.blog_id.isin(resp_blogs.blog_id)]

		# Load post content
		post_file = jf.PostFile(["post_id", "content"])
		posts_needed = set(blog_posts.post_id) | set(stim_likes.post_id)
		post_content = dict()
		
		for (n, (k, post)) in enumerate(post_file):
			if (n % 10000) == 0:  print "\tLoading user liked posts", n, len(post_content), nowstring()
			if post["post_id"] in posts_needed:
				post_content[post["post_id"]] = post["content"]
		
		# Detect user languages
		detector = LangDetector()
		user_vecs = detect_post_groups(stim_likes, post_content, "user_id", detector)
		blog_vecs = detect_post_groups(blog_posts, post_content, "blog_id", detector)
		
		return Struct(user_vecs = user_vecs, blog_vecs = blog_vecs)
	
	
	def get_candidates(store):

		# User_blog Candidates
		user_blog = store.user_blog.load_df(
			["pagerank_by_like_share_postrank", "hist_like_ct", "all_blog_like_user_like_share"]
			, reset_index = True
		)
		
		user_blog = user_blog[
			(user_blog.user_id.isin(store.user['is_resp'].get_matching_indexes(True)))
			& (user_blog.blog_id.isin(store.blog['is_resp'].get_matching_indexes(True)))
		]


		# User_post candidates 
		user_post = store.user_post.load_df(["topic_proximity_rank"], reset_index = True)
		
		post = store.post.load_df(["is_resp", "blog_id"], reset_index = True)
		post = post[post.pop("is_resp").fillna(False)]
		user_post = user_post.merge(post, on = "post_id", how = "inner")
		
		user_post = user_post[
			(user_post.user_id.isin(store.user['is_resp'].get_matching_indexes(True)))
			& (user_post.blog_id.isin(store.blog['is_resp'].get_matching_indexes(True)))
		]
			
		return set(zip(user_post.user_id, user_post.blog_id)) | set(zip(user_blog.user_id, user_blog.blog_id))
		
# feature info
feature_list = [
	ft.Feature("user_blog", "lang_proximity")
	, ft.Feature("user", "is_english")
	, ft.Feature("blog", "is_english")
]

def setup_func(feature_list, store, recreate_setup):
	# store, recreate_setup = ft.dev_store, False
		
	# if not (recreate_setup):
	#	try:
	#		retval = cPickle.load(open(
	#			param.folders.root + "languagedata/" + store.name + "/all_setup_data.pickle", "r"))
	#		return retval
	#	except:
	#		pass
			
	retval = Struct()
			
	user_vecs, blog_vecs = None, None
	if not (recreate_setup):
		try:
			user_vecs = cPickle.load(
				open(param.folders.root + "languagedata/" + store.name + "/user_vecs.pickle", "r"))
			blog_vecs = cPickle.load(
				open(param.folders.root + "languagedata/" + store.name + "/blog_vecs.pickle", "r"))
		except:
			pass
	
	if (user_vecs is None) or (blog_vecs is None):
		vecdata = get_lang_vecs(store)
		user_vecs, blog_vecs = vecdata.user_vecs, vecdata.blog_vecs
		cPickle.dump(user_vecs, open(
			param.folders.root + "languagedata/" + store.name + "/user_vecs.pickle", "w"))
		cPickle.dump(blog_vecs, open(
			param.folders.root + "languagedata/" + store.name + "/blog_vecs.pickle", "w"))
		
		
	candidates = list(get_candidates(store))
	
	retval.scores = dict([((user_id, blog_id), 0.0) for user_id, blog_id in candidates])
	for (n, (user_id, blog_id)) in enumerate(candidates):
		if (n % 10000) == 0: print n, nowstring()
		retval.scores[(user_id, blog_id)] = veccos(user_vecs[user_id], blog_vecs[blog_id])
	
	def lang_is_eng(v):
		if len(v) == 0: return 'U'
		elif v[0][0] == "en": return 'Y'
		else: return 'N'
	retval.isblogeng = dict([(k, lang_is_eng(v)) for (k, v) in blog_vecs.iteritems()])
	retval.isusereng = dict([(k, lang_is_eng(v)) for (k, v) in user_vecs.iteritems()])
	
	# cPickle.dump(retval, open(
	#	param.folders.root + "languagedata/" + store.name + "/all_setup_data.pickle", "w"))
	
	return retval
	
def build_func(feature, data):
	
	print feature.key
	
	if feature.key == "user_blog.lang_proximity":
		s = pandas.Series(data.scores)
		s.index = pandas.MultiIndex.from_tuples(s.index, names = ["user_id", "blog_id"])
		return s
	elif feature.key == "user.is_english":
		s = pandas.Series(data.isusereng)
		s.index.names = ["user_id"]
		return s
	elif feature.key == "blog.is_english":
		print feature.key
		s = pandas.Series(data.isblogeng)
		s.index.names = ["blog_id"]
		return s
	
fset = ft.FeatureSet("language", feature_list, setup_func, build_func)
fset.save(ft.dev_store, overwrite = True)
fset.save(ft.prod_store, overwrite = True)



# # code used to get top languages used - some judgement applied		
# langdict = LangIDDict()
# df = pandas.DataFrame([(key, score, score if n == 0 else 0) 
						# for vec in user_vecs.itervalues() if len(vec) > 0 
						# for n, (key, score) in enumerate(vec[:5])], 
						# columns = ["langid", "score", "top_score"])
# df = df.groupby(["langid"]).aggregate(sum)
# df = df.sort("top_score", ascending = False)
# df['lang_name'] = map(lambda(k): langdict[k], df.index)
# df
# df.to_csv("/data/wordpress/languagedata/langsused.csv")

# # other test code
# n = 0
# for k, v in user_vecs.iteritems():
	# if (len(v) > 0) and (v[0][0] == "tl"):
		# post_id = stim_likes[stim_likes.user_id == k].post_id.values[0]
		# if len(post_content[post_id]) > 10:
			# print " ".join(post_content[post_id][:20])
			# n += 1
			# if n == 5: break
		
# # other test code
# len([v for v in blog_vecs.itervalues() if len(v) == 0])
# len([v for v in user_vecs.itervalues() if len(v) == 0])
		
# for (n, x) in enumerate(x for x in retval.scores.iteritems() if x[1] < .9):

	# u, b = x[0]
		
	# print "user", u
	# # post_id = stim_likes[stim_likes.user_id == u].post_id.values[0]
	# # if len(post_content[post_id]) > 10: print " ".join(post_content[post_id][:20])
	# for i in user_vecs[u][:5]: print i
	
	# print "blog", b
	# # post_id = blog_posts[blog_posts.blog_id == b].post_id.values[0]
	# # if len(post_content[post_id]) > 10: print " ".join(post_content[post_id][:20])
	# for i in blog_vecs[b][:5]: print i
	
	# print "match", x[1]
	# print
	
	# if n > 5: break
		
			
		