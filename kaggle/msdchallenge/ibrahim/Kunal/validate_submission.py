#!/usr/bin/env python
"""
Thierry Bertin-Mahieux (2012) Columbia University
tb2332@columbia.edu

Code to validate a submission file for the Million Song Dataset
Challenge on Kaggle. Requires an internet connection.
This code is developed under python 2.7 (Ubuntu machine).

Copyright 2012, Thierry Bertin-Mahieux

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
"""
__author__ = 'Thierry Bertin-Mahieux <tb2332@columbia.edu>'
__date__ = 'Sun Mar 11 18:39:03 EDT 2012'


import os
import sys
import time
import urllib2


# Number of predicted songs required per user.
CUTOFF = 500

# Million Song Dataset website file directory.
HTML_PREFIX = 'http://labrosa.ee.columbia.edu/millionsong/sites/default/files/'

# Canonical list of users for the contest, there should be predictions for
# each user, one user per line, users are in the same order as in this file.
CANONICAL_USER_LIST = '%s%s' % (HTML_PREFIX,
                                'challenge/canonical/kaggle_users.txt')

# Canonical list of songs and their integer index.
CANONICAL_SONG_LIST = '%s%s' % (HTML_PREFIX,
                                'challenge/canonical/kaggle_songs.txt')


def load_list_from_the_web(url):
    """Grab a text file, return each line in a list."""
    print '---retrieveing url %s...' % url
    t1 = time.time()
    stream = urllib2.urlopen(url)
    data = [l.strip() for l in stream.readlines()]
    stream.close()
    print '    DONE! It took %d seconds.' % int(time.time() - t1)
    return data


def print_error_message(msg, line_num=None):
    """Formatted error message."""
    prefix = 'ERROR! '
    if line_num:
        prefix += '[line %d] ' % line_num
    print '%s%s' % (prefix, msg)


def validate_one_line(line, line_num, min_max_song_indexes):
    """Make sure an individual line looks valid, return True if so."""
    is_valid = True
    min_index, max_index = min_max_song_indexes
    assert min_index == 1, 'Problem, minimum song index is not 1.'
    # Line too small or empty?
    if len(line) < 500:
        print_error_message("Line too short! (%d characters)" % len(line),
                            line_num)
        is_valid = False
    parts = line.split(' ')
    # Not the right number of items per line?
    if len(parts) != CUTOFF:
        msg = "Line should have %d one-space-separated elements, " % CUTOFF
        msg += "found %d" % len(parts)
        print_error_message(msg, line_num)
        is_valid = False
    for song_index in parts:
        # Is the song an integer?
        try:
            index = int(song_index)
        except ValueError:
            if len(song_index) == 18 and song_index[:2] == 'SO':
                msg = 'Predicted songs should be integers, not SO...'
                msg += 'Found: %s' % song_index
                print_error_message(msg, line_num)
            else:
                msg = 'Found non-integer song ID: %s' % song_index
                print_error_message(msg, line_num)
            is_valid = False
            break
        # Is it 0-indexed instead of 1?
        if index == 0:
            msg = 'Found song index 0, song indexes start at 1.'
            print_error_message(msg, line_num)
            is_valid = False
            break
        # Is the index a valid integer?
        elif index < 1 or index > max_index:
            msg = 'Found song index %d, ' % index
            msg += 'it should be between 1 and %d.' % max_index
            print_error_message(msg, line_num)
            is_valid = False
            break
    # Are there song duplicates?
    if is_valid:
        if len(set(parts[1:])) != len(parts[1:]):
            msg = 'There is at least one song ID duplicate.'
            print_error_message(msg, line_num)
            is_valid = False
    # Done.
    return is_valid


def main(argv):
    """Validate the submission from canonical files fetched online."""

    # Sanity check on the file.
    submission_filename = argv[1]
    if not os.path.isfile(submission_filename):
        print 'ERROR: file %s does not exist.' % submission_filename
        die_with_usage()

    # Fetch data files.
    users = load_list_from_the_web(CANONICAL_USER_LIST)
    songs_and_indexes = load_list_from_the_web(CANONICAL_SONG_LIST)

    # Check user file.
    assert len(users) == 110000, 'Problem with the online user file.'
    for user in users:
        assert len(user) == 40, '%s' % (
            'Problem with the online user file (user: %s).' % user, )

    print '***************************************'
    print '**********ANALYZING SUBMISSION*********'

    # Extract indexes from the list of songs.
    indexes = [int(line.split(' ')[1]) for line in songs_and_indexes]
    min_index = min(indexes)
    max_index = max(indexes)
    msg_song_file_prob = 'Problem with the online song file, aborting.'
    assert min_index == 1, msg_song_file_prob
    assert max_index == len(indexes), msg_song_file_prob
    min_max_index = (min_index, max_index)

    # Keep stats
    submission_is_valid = True

    # Go through each line, validates it, keep some stats.
    line_number = 0
    fIn = open(submission_filename, 'r')
    for line in fIn.xreadlines():
        line_number += 1
        submission_is_valid = validate_one_line(line.strip(),
                                                line_number,
                                                min_max_index)
        if not submission_is_valid:
            fIn.close()
            sys.exit(0)
    fIn.close()

    # Final message.
    if submission_is_valid:
        print '***************************************'
        print 'Awesome, your submission is good to go!'
        sys.exit(0)    


def die_with_usage():
    """Help menu."""
    print 'MSD CHallenge: script to validate your submission to Kaggle.'
    print '(you need an internet connection)'
    print '------------------------------------------------------------'
    print ''
    print 'python validate_submission.py <submission file>'
    print ''
    print 'ARGS'
    print '   <submission file>   File to be uploaded to Kaggle.'
    sys.exit(0)


if __name__ == '__main__':

    # Display the help menu and quit?
    HELP_KEYWORDS = ('help', '-help', '--help')
    if len(sys.argv) < 2 or sys.argv[1].lower() in HELP_KEYWORDS:
        die_with_usage()

    main(sys.argv)
