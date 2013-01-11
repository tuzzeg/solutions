#should create file with users as line number and with the song history as "songTitleA playCountA songTitleB playCountB"
import sys
import fileinput
newfile = open("userHistoriesTrainingSet.txt","w")
count = 1
currentUser = 1
currentHistory = ''
songDict = {}
counter = 1
for line in fileinput.input(['kaggle_songs.txt']):
	song = line.rstrip('\n').split()
	songDict[song[0]]=song[1]
	print(str(counter*100/400000) + 'ish % of songs')
	counter +=1
counter = 1
for line in fileinput.input(['train_triplets.txt']):
	temp = line.rstrip('\n')
	triplet = temp.split()
	if count == 1:
		currentUser = triplet[0]
	if currentUser == triplet[0] :
		currentHistory += str(songDict[triplet[1]]) + '\t'
	else:
		newfile.write(currentHistory + '\n')
		currentHistory = str(songDict[triplet[1]]) + '\t'
		currentUser = triplet[0]
		print(str(counter*100/1000000) + 'ish % of users')
		counter +=1
newfile.write(currentHistory + '\n')
newfile.close()
