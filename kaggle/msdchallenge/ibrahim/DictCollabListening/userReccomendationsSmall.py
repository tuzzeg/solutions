#should create file with song number as line number; each line contains print of dictionary which should read {otherSong:timesPlayed, otherSong2:times2Played ... }
import sys
import fileinput
from collections import defaultdict
newfile = open("userReccomendationsSmall.txt","w")
user_id=1
songsDict = defaultdict(lambda: defaultdict(int))
for line in fileinput.input(['userHistoriesFixed.txt']):
	temp = line.rstrip('\n')
	history = temp.split()
	x = len(history)
	for i in range(x):
		for j in range(x):
			if i <> j:
				songsDict[history[i]][history[j]] += 1
	print(str(user_id*100/110000)+"%...test..dict...");
	user_id+=1;
userNum = 1
f=open('popularSongs.txt')
for line in fileinput.input(['userHistoriesFixed.txt']):
	userDict = defaultdict(int)
	history = line.rstrip('\n').split()
	x = len(history)
	for i in range(x):
		for key in songsDict[history[i]]:
			if not(key in history):
				userDict[key] += songsDict[history[i]][key]
	sortedDict = [x for x in userDict.iteritems()]
	sortedDict.sort(key=lambda x: x[1], reverse=True)
	printString = ''
	if len(sortedDict)<500:
		for i in range(len(sortedDict)):
			printString += (str(sortedDict[i][0]) + ' ')
		emptySpace = 500 - len(sortedDict)
		isFull = False
		for line in f.readlines():
			if emptySpace <= 0:
				isFull = True
			if not isFull:
				nextSong = line.rstrip('\n')
				if -1 == userDict.get(nextSong, -1):
					printString += (str(nextSong) + ' ')
					emptySpace -= 1
		userNum += 1
		print('less than 500')
	else:
		for i in range(500):
			printString += (str(sortedDict[i][0]) + ' ')
	newfile.write(printString + '\n')
	userNum +=1
	print(str(userNum*100/110000)+"%...recs...");
newfile.close()
f.close()
