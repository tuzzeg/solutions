from collections import defaultdict
import json
import logging
from datetime import datetime
from v1.config_and_pickle import testPosts_loc, pickle

lines_to_read = -1

################################################################################################################################
logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)
logger = logging.getLogger('test_data_structure_populator')
################################################################################################################################

def populate_data_structures():
    logger.info('started populating data structures...')

    #key = blog
    #value = list of (post_id, author, tags, categories, date_struct) tuples
    test_blog_post_tuples_map = defaultdict(list)

    with open(testPosts_loc, 'r') as f:
        for line_number, line_text in enumerate(f):
            if lines_to_read != -1 and line_number >= lines_to_read:
                break

            blog_json = json.loads(line_text)

            blog        = blog_json['blog']
            post_id     = blog_json['post_id']
            author      = blog_json['author']
            tags        = blog_json['tags']
            categories  = blog_json['categories']
            date_struct = datetime.strptime(blog_json['date_gmt'], '%Y-%m-%d %H:%M:%S')

            test_blog_post_tuples_map[blog] += [(post_id, author, tags, categories, date_struct)]

    logger.info('finished populating data structures')

    pickle(test_blog_post_tuples_map, 'test_blog_post_tuples_map')

populate_data_structures()
