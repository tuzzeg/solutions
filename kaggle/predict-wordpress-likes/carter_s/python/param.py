import useful_stuff
from useful_stuff import *

# Create folder struct with file locations
folders = Struct(root = "./")
folders.source = folders.root + "sourcedata/"
folders.saved = folders.root + "saveddata/"
folders.auxdata = folders.root + "auxdata/"
folders.tmpdata = folders.root + "tmp/"