import sys
import linecache
import fileinput
fullLinesFile = open('KunalsReccomendationsFilledByMatts.txt','w')
lineNum = 1
for line in fileinput.input(['KunalsReccomendations.txt']):
	songList = []
	printString = ''
	songs = line.rstrip().split(' ')
	for i in range(len(songs)):
		songList.append(songs[i])
	while len(songList) < 500:
		#print('adding to line ' + str(lineNum))
		fillSongs = linecache.getline('userFullRecs2.txt', lineNum).rstrip('\n').split()
		itersongs = 0
		while len(songList) < 500:
			testSong = fillSongs[itersongs]
			checkV = True
			for i in range(len(songList)):
				if songList[i] == testSong:
					checkV	= False
			if checkV:
				songList.append(testSong)
			itersongs += 1
	incSong = 1
	while len(songList) < 500:
		filler = open('popularSongs.txt','r')
		#print('adding to line ' + str(lineNum))
		testSong = filler.readline().rstrip('\n')
		checkV = True
		for i in range(len(songList)):
			if songList[i] == testSong:
				checkV	= False
		if checkV:
			songList.append(testSong)
		filler.close
	incSong = 1
	while len(songList) < 500:
		#print('adding to line ' + str(lineNum))
		testSong = incSong
		checkV = True
		for i in range(len(songList)):
			if songList[i] == testSong:
				checkV	= False
		if checkV:
			songList.append(testSong)
		incSong +=1
	for i in range(500):
		printString += (str(songList[i]) + ' ')
	fullLinesFile.write(printString.lstrip(' ') + '\n')
	lineNum += 1
fullLinesFile.close()
