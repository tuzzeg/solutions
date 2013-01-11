from datetime import datetime, timedelta
from threading import Lock
from inspect import getargspec as args

def unique(iterable):
	return list(set(iterable))
	
def nowstring():
	return datetime.now().strftime("%Y-%m-%d %H:%M:%S")
	
def to_week(d):
	return(d.replace(hour = 0, minute = 0, second = 0, microsecond = 0) - timedelta(days = d.weekday()))	

class Struct():
	def __init__(self, **kwargs):
		for (k, v) in kwargs.iteritems(): 
			setattr(self, k, v)