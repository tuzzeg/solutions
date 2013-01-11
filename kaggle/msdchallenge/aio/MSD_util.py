### MSD_util.py: MSD challenge utilities 
### Copyright (C) 2012  Fabio Aiolli

#    This program is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.

#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.

#    You should have received a copy of the GNU General Public License
#    along with this program.  If not, see <http://www.gnu.org/licenses/>.

# Scientific results produced using the software provided shall
# acknowledge the use of this software. Please cite as  

#       F. Aiolli, A Preliminary Study on a Recommender System for the Million Songs Dataset Challenge
#       Preference Learning: Problems and Applications in AI (PL-12), ECAI-12 Workshop, Montpellier
#       http://www.ke.tu-darmstadt.de/events/PL-12/papers/08-aiolli.pdf

# Moreover shall the author be informed about the
# publication.

import random,time,math
import sys,os

def song_to_count(if_str):
    stc=dict()
    with open(if_str,"r") as f:
        for line in f:
            _,song,_=line.strip().split('\t')
            if song in stc:
                stc[song]+=1
            else:
                stc[song]=1
    return stc

def user_to_count(if_str):
    utc=dict()
    with open(if_str,"r") as f:
        for line in f:
            user,_,_=line.strip().split('\t')
            if user in utc:
                utc[user]+=1
            else:
                utc[user]=1
    return utc

def sort_dict_dec(d):
    return sorted(d.keys(),key=lambda s:d[s],reverse=True)

def song_to_users(if_str,set_users=None, ratio=1.0):
    stu=dict()
    with open(if_str,"r") as f:
        for line in f:
            if random.random()<ratio:
                user,song,_=line.strip().split('\t')
                if not set_users or user in set_users:
                    if song in stu:
                        stu[song].add(user)
                    else:
                        stu[song]=set([user])
    return stu

def user_to_songs(if_str):
    uts=dict()
    with open(if_str,"r") as f:
        for line in f:
            user,song,_=line.strip().split('\t')
            if user in uts:
                uts[user].add(song)
            else:
                uts[user]=set([song])
    return uts

def load_unique_tracks(if_str):
    ut=[]
    with open(if_str,"r") as f:
        for line in f:
            a_id,s_id,a,s=line.strip().split('<SEP>')
            ut.append((a_id,s_id,a,s))
    return ut

def load_users(if_str):
    with open(if_str,"r") as f:
        u=map(lambda line: line.strip(),f.readlines())
    return u

def song_to_idx(if_str):
     with open(if_str,"r") as f:
         sti=dict(map(lambda line: line.strip().split(' '),f.readlines()))
     return sti

def unique_users(if_str):
    u=set()
    with open(if_str,"r") as f:
        for line in f:
            user,_,_=line.strip().split('\t')
            if user not in u:
                u.add(user)
    return u 

def save_recommendations(r,songs_file,ofile):
    print "Loading song indices from " + songs_file
    s2i=song_to_idx(songs_file)
    print "Saving recommendations"
    f=open(ofile,"w")
    for r_songs in r:
        indices=map(lambda s: s2i[s],r_songs)
        f.write(" ".join(indices)+"\n")
    f.close()
    print "Ok."
