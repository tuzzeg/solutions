import sys
import fileinput
fullLinesFile = open('KunalsReccomendationsFilledByPop.txt','w')
lineNum = 1
for line in fileinput.input(['KunalsReccomendations.txt']):
	songList = []
	printString = ''
	songs = line.rstrip().split(' ')
	for i in range(len(songs)):
		songList.append(songs[i])
	filler = open('popularSongs.txt','r')
	while len(songList) < 500:
		#print('adding to line ' + str(lineNum))
		testSong = filler.readline().rstrip('\n')
		checkV = True
		for i in range(len(songList)):
			if songList[i] == testSong:
				checkV	= False
		if checkV:
			songList.append(testSong)
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
	filler.close
	lineNum += 1
fullLinesFile.close()
