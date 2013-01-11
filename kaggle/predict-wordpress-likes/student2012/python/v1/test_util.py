import csv
from v1.config_and_pickle import test_loc

def load_test_users():
    test_file = open(test_loc, 'r')
    test_file.readline()#skip first line; it is a header
    return [row[0] for row in csv.reader(test_file)]

