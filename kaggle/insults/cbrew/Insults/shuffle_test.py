"""
Prepare a modified version of test.csv to use as a stand-in for final.csv, which we do not have yet.

Entries are shuffled and only first 1000 entries are kept
"""

import pandas
import numpy as np
from insults import DataFile

table = pandas.read_table(DataFile('Inputs','test.csv'),sep=',')
print table.tail()
index = np.array(table.index)
np.random.shuffle(index)
table = table.reindex(index)
print table.tail()
table = table.head(1000)
table.to_csv(DataFile('Inputs','final.csv'),index=False)