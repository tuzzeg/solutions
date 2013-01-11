'''
Created on Oct 10, 2012

@author: kingsfield
'''
import csv
import string

exclude = set(string.punctuation)
stopwords = set(["a", "an", "and", "are", "as", "at", "be", "but", "by",
      "for", "if", "in", "into", "is", "it",
      "no", "not", "of", "on", "or", "such",
      "that", "the", "their", "then", "there", "these",
      "they", "this", "to", "was", "will", "with"])

def readfile(f):
    infile = open(f)
    reader = csv.reader(infile, delimiter=",")
    reader.next() # burn the header
    return reader

def writefile(f):
    outfile = open(f, 'w')
    writer = csv.writer(outfile, delimiter=",")
    writer.writerow(["sku"])
    return writer

def is_legal(word):
    if word.find(' ') > 0 or len(word) <= 0 or word in stopwords:
        return False
    else:
        return True

def get_words(raw):
    raw.strip()
    raw = raw.lower()
    res = ''
    for ch in raw:
        res += ch if ch not in exclude else ' '
    res = res.split(' ')
    words = [w for w in res if is_legal(w)]
    return words
