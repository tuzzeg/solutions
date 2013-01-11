import pandas, pandas_ext, param
import abc, json, string, math

from datetime import datetime, timedelta
from HTMLParser import HTMLParser

# function for sampling a file
def sample_source_file(filename, line_to_read = 0):
	f = open(param.folders.source + filename)
	for (n, l) in enumerate(f):
		if n == line_to_read:
			return json.loads(l)
	
	# Tools for stripping html

class JSONFile:
	""" Abstract class to iterate through json text file and get features of each json object.

		Iterator of class returns a tuple of object key and "needed features" specified at instantiation
		for each json object returned by internal iterator.

		Key functions to override:

		--	_iterate_json_objects:  
			**	Iterates parsed json objects in file.   
			**	Default is to iterate each line in self.file_list after json parsing.
		--	_get_item_feature:
			**	Extracts a feature from item by name.   
			**	Default is to return named entry from item dictionary
			**	Intent is to allow conversion or 'meta' characteristics to be pulled
		--	_get_key_tuple:
			**	Returns tuple of features listed in key_names

	"""

	__metaclass__ = abc.ABCMeta
	
	def __init__(self, features_needed):
		self.features_needed = features_needed
		self.file_names = []
		self.key_names = []

	# iterator method that feeds json object from file list; override as needed
	def _iterate_json_objects(self):
		for filename in self.file_names:
			fileobj= open(filename)
			for rawline in fileobj:
				yield json.loads(rawline)
			fileobj.close()
			
	# method - should be overriden to implement conversions - to get features
	def _get_item_feature(self, item, feature_name):
		return item[feature_name]
		
	# abstract method that gets key - no default
	def _get_key_tuple(self, item):
		return tuple(self._get_item_feature(item, kn) for kn in self.key_names)

	# iterator - all functionality should be in _next_item and _get_feature
	def __iter__(self):
		# for each file line, get json parsed item and key...
		for item in self._iterate_json_objects():
			yield (self._get_key_tuple(item), dict((fn, self._get_item_feature(item, fn)) for fn in self.features_needed))
			
	# class method to store features
	@classmethod
	def get_df(cls, features_to_get, resave = False):
		
		# load data and save features
		jsonfile = cls(features_to_get)
		datadict = dict(jsonfile)
		
		dataindex = pandas.MultiIndex.from_tuples(datadict.keys())
		dataindex.names = jsonfile.key_names
		
		series = dict(
			(fn, pandas.Series([v[fn] for v in datadict.itervalues()], index = dataindex, name = fn))
			for fn in features_to_get
		)			
		return(pandas.DataFrame(series))
	
class PostFile(JSONFile):

	def __init__(self, features_needed):
		self.features_needed = features_needed
		self.file_names = []
		self.key_names = ['post_id']
		self.is_test = None

	def _iterate_json_objects(self):
		
		trainfile = open(param.folders.source + "trainPosts.json")
		self.is_test = False
		for rawline in trainfile:
			yield json.loads(rawline)
		trainfile.close()
		
		testfile = open(param.folders.source + "testPosts.json")
		self.is_test = True
		for rawline in testfile:
			yield json.loads(rawline)
		testfile.close()
		
		self.is_test = None
	
	def _content_to_list(self, content):
	
		try:
			parser = HTMLParser()
			parser.fed = []
			parser.handle_data = lambda d: parser.fed.append(d)
			
			parser.feed(content)
			return (''.join(parser.fed).encode('ascii', 'ignore')
					.translate(string.maketrans("",""), string.punctuation).lower().split())
		
		except:
			return list()
		
	def _get_item_feature(self, item, feature_name):
		if feature_name == "post_id":
			return str(item['post_id'])
		elif feature_name == "blog_id":
			return str(item['blog'])
		elif feature_name == "date":
			return datetime.strptime(item['date_gmt'], "%Y-%m-%d %H:%M:%S")
		elif feature_name == "date_local":
			try:
				return datetime.strptime(item['date'], "%Y-%m-%d %H:%M:%S")
			except:
				# Think about fixing this to do correct dates; may not need to if Kaggle fixes
				return datetime.strptime(item['date_gmt'], "%Y-%m-%d %H:%M:%S")
		elif feature_name == "is_test":
			return self.is_test
		elif feature_name == "content":
			return self._content_to_list(item['content'])
		elif feature_name == "time_zone":
			try:
				b = datetime.strptime(item['date_gmt'], "%Y-%m-%d %H:%M:%S")
				l = datetime.strptime(item['date'], "%Y-%m-%d %H:%M:%S")
				return float((b - l).total_seconds() / 3600)
			except:
				return 0.0
		else:
			return item[feature_name]		

class LikeFile(JSONFile):
	
	def __init__(self, features_needed):
		self.features_needed = features_needed
		self.file_names = []
		self.key_names = ['user_id', 'post_id']
		self.current_post = None
		
	def _iterate_json_objects(self):
		likefile = open(param.folders.source + "trainPosts.json")
		for rawline in likefile:
			self.current_post = json.loads(rawline)
			for like in self.current_post['likes']:
				yield like
				
	def _get_item_feature(self, item, feature_name):
		if feature_name == "user_id":
			return str(item['uid'])
		elif feature_name == "post_id":
			return str(self.current_post['post_id'])
		elif feature_name == "is_like":
			return True
		elif feature_name == "like_date":
			return datetime.strptime(item['dt'], "%Y-%m-%d %H:%M:%S")
		else:
			return None
			
class UserFile(JSONFile):
	
	def __init__(self, features_needed):
		self.features_needed = features_needed
		self.file_names = [param.folders.source + "trainUsers.json"]
		self.key_names = ['user_id']
						
	def _get_item_feature(self, item, feature_name):
		if feature_name == "user_id":
			return str(item['uid'])
		elif feature_name == "is_test":
			return item['inTestSet']
		else:
			return None
			
class UserHistFile(JSONFile):
	
	def __init__(self, features_needed):
		self.features_needed = features_needed
		self.file_names = [param.folders.source + "kaggle-stats-user.json"]
		self.key_names = ['user_id']
						
	def _get_item_feature(self, item, feature_name):
		if feature_name == "user_id":
			return str(item['user_id'])
		elif feature_name == "hist_like_ct":
			return int(item['num_likes'])
		else:
			return None
			
class UserBlogHistFile(JSONFile):
	
	def __init__(self, features_needed):
		self.features_needed = features_needed
		self.file_names = []
		self.key_names = ['user_id', 'blog_id']
		self.current_userhist = None
		
	def _iterate_json_objects(self):
		fileobj = open(param.folders.source + "kaggle-stats-user.json")
		for rawline in fileobj:
			self.current_userhist = json.loads(rawline)
			for userbloghist in self.current_userhist['like_blog_dist']:
				yield userbloghist
				
	def _get_item_feature(self, item, feature_name):
		if feature_name == "user_id":
			return str(self.current_userhist['user_id'])
		if feature_name == "blog_id":
			return str(item['blog_id'])
		if feature_name == "hist_like_ct":
			return int(item['likes'])
		else:
			return item[feature_name]

class BlogHistFile(JSONFile):
	
	def __init__(self, features_needed):
		self.features_needed = features_needed
		self.file_names = [param.folders.source + "kaggle-stats-blog.json"]
		self.key_names = ['blog_id']

	def _get_item_feature(self, item, feature_name):
		if feature_name == "blog_id":
			return str(item['blog_id'])
		if feature_name == "hist_like_ct":
			return int(item['num_likes'])
		if feature_name == "hist_post_ct":
			return int(item['num_posts'])
		else:
			return item[feature_name]

			


