import sys
import linecache
import fileinput
fullLinesFile = open('KunalsReccomendationsFilledByPop2.txt','w')
lineNum = 1
for line in fileinput.input(['KunalsReccomendationsFilledByPop.txt']):
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
				print('added'+str(testSong) + " to line " + str(lineNum))
				songList.append(testSong)
			itersongs += 1
	for i in range(500):
		printString += (str(songList[i]) + ' ')
	fullLinesFile.write(printString.lstrip(' ') + '\n')
	lineNum += 1
fullLinesFile.close()
