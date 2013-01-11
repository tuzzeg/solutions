from collections import defaultdict
import json
import time
from v1.config_and_pickle import trainPosts_loc, testPosts_loc

lines_to_read = -1

def print_stats():
    date_count_map = defaultdict(int)
#    with open(trainPosts_loc, 'r') as f:
    with open(testPosts_loc, 'r') as f:
        for line_number, line_text in enumerate(f):
            if lines_to_read != -1 and line_number >= lines_to_read:
                break
            blog_json = json.loads(line_text)
            date_struct = time.strptime(blog_json['date_gmt'], '%Y-%m-%d %H:%M:%S')
            date_string = time.strftime('%Y-%m-%d', date_struct)
            date_count_map[date_string] += 1

    for date in sorted(date_count_map):
        print 'date = ', date, ', blog_count = ', date_count_map[date]

print_stats()