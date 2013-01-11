# corp.py
# Memory-friendly ways to deal with json files

from HTMLParser import HTMLParser
from gensim import corpora,utils
import string
import json

# These are words that will be removed from posts, due to their frequency and poor utility in distinguishing between topics
stop_words = ["a","able","about","across","after","all","almost","also","am","among","an","and","any","are","as","at","be","because","been","but","by","can","cannot","could","did","do","does","either","else","ever","every","for","from","get","got","had","has","have","he","her","hers","him","his","how","however","i","if","in","into","is","it","its","just","least","let","like","may","me","might","most","must","my","neither","no","nor","not","of","off","often","on","only","or","other","our","own","rather","said","say","says","she","should","since","so","some","than","that","the","their","them","then","there","these","they","this","to","too","us","wants","was","we","were","what","when","where","which","while","who","whom","why","will","with","would","yet","you","your"]

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
    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        self.close()

    def __iter__(self): # Read only one line at a time from the text files, to be memory friendly
        for f in self.files:
            f.seek(0) # Reset the file pointer before a new iteration
            for line in f:
                post = json.loads(line)
                try: # parse and split the content up into a list of lower-case words
                    content = strip_tags(post["title"])
                    doc_words = utils.simple_preprocess(content)
                except: # Fails on some nasty unicode
                    doc_words = []
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
        for doc in self.files:
            yield self.dic.doc2bow(doc)
    def __len__(self):
        return len(self.files)
