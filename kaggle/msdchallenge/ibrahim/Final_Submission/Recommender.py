#Recommender file
#Team Ubuntu on kaggle challenge - Kunal, Zheng, Matt, Ibrahim


#Python file to recommend songs per user
#File input parameters are the number extensions of the input files/4 for this case
import os
import fileinput
import sys
import multiprocessing
import time

#Returns list of songs along with counts for each user
def song_function(input_file,user_number):
	old_user = int(user_number);
	listy = [];
	for line in open(input_file):
		try:
			user_id = int(line.rstrip('\n').split(' ')[0]);
			song_id = line.rstrip('\n').split(' ')[1];
			count = int(line.rstrip('\n').split(' ')[2]);
		except ValueError:
			continue
		if old_user != user_id:
			yield listy;
			listy = [];
			old_user = user_id;
		listy.append((song_id,count));
	yield listy;

#Recomend song for each user
def song_recommender(file_name,output_file,user_number):
	user_file = open(file_name,'rb')
	out_file = open(output_file,'w')
	num = user_number
	
	for each_song_list in song_function(file_name,user_number):
		song_dict = {}
		new_song_dict = {}
		song_reco = []
		songs,count = zip(*each_song_list)
		totalCount = 0
		counter = 0
		
		for each_song in songs:
			song_dict[each_song] = count[counter]
			counter += 1
		
		song_sorted = sorted(song_dict.items(), key=lambda x:x[1], reverse=True);
		
		for each in song_sorted:
			totalCount += each[1]
		resetter = 0
		countery = 0
		for each_song in song_sorted:
			if len(song_sorted) > 25:
				threshhold = 2
			elif len(song_sorted) > 19:
				threshhold = 3
			elif len(song_sorted) > 12:
				threshhold = 4
			else:
				threshhold = 5
			filePath = ''
			for each in each_song[0]:
				filePath += (each + ('/'))
			try:
				handle = open(filePath+each_song[0]+'.txt','rb')
			except:
				print "oops couldnt find file "+each_song[0]+'\n'
				continue		
			
			for line in handle:
				if threshhold>0:
					song1 = line.rstrip('\n').split(' ')[1]
					song2 = line.rstrip('\n').split(' ')[2]
					song_count = int(line.rstrip('\n').split(' ')[0])
					if song_dict.has_key(song2):
						continue
					if new_song_dict.has_key(song2):
						new_song_dict[song2] = new_song_dict[song2] + (((float(song_count)/popular_song_dict[song1]) + (float(song_count)/popular_song_dict[song2]) ) )
					else:
						new_song_dict[song2] = (( (float(song_count)/popular_song_dict[song1]) + (float(song_count)/popular_song_dict[song2] ) ) )
						threshhold -= 1
				else:
					break
		
		song_list = sorted(new_song_dict.items(), key=lambda x:x[1], reverse=True);
		
		print "Got "+str(len(song_list))+" songs for user "+str(num)
		
		for each_elem in song_list:
			if len(song_reco) == 500:
				break
			if each_elem[0] in songs or each_elem[0] in song_reco:
				continue
			else:
				song_reco.append(each_elem[0])
		
		pop_counter = 0
		while True:
			if len(song_reco) == 500:
				break
			if popular_song_list[pop_counter] in songs:
				pop_counter += 1
				continue
			if popular_song_list[pop_counter] in song_reco:
				pop_counter += 1
			else:
				song_reco.append(popular_song_list[pop_counter])
				pop_counter += 1
		
		out_file.write((' ').join(song_reco)+'\n')
		num += 1
		
		



if __name__ == '__main__':
	print "Loading popular song list"
	popular_songs = open('trainset_song_popularity_sorted_reverse.txt','rb')
	popular_song_dict = {}
	popular_song_list = []
	
	print "Starting preprocessing"
	
	for line in popular_songs:
		popular_song_dict[line.rstrip('\n').split('\t')[0]] = int(line.rstrip('\n').split('\t')[1])
		popular_song_list.append(line.rstrip('\n').split('\t')[0])
	
	print "Done preprocessing"

	print "==============================================================================================================================================="
	print "Starting processes in 1 second"
	print "==============================================================================================================================================="
	
	time.sleep(1)
	ps = []
	num = 1
	"""
	p1 = multiprocessing.Process(target=song_recommender,args=('input'+str(num)+'.txt','output'+str(num)+'.txt',1));
	p1.start()
	"""
	user_number=1
	while True:
		if num==5:
			break
		p1 = multiprocessing.Process(target=song_recommender,args=('input'+str(num)+'.txt','output'+str(num)+'.txt',user_number));
		user_number=0+(25000*num)+1
		p1.start()
		ps.append(p1)
		num+=1
	for t in ps:
		t.join()
	
	print "Done!!"
	
