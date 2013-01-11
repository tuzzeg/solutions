#should create file with song number as line number; each line contains print of dictionary which should read {otherSong:timesPlayed, otherSong2:times2Played ... }
import fileinput
import anydbm
db = anydbm.open('songsTest.db', 'c')
user_id = 1
for line in fileinput.input(["userHistoriesTESTFILE.txt"]):
    history = line.rstrip('\n').split()
	x = len(history)
	for i in range(x):
		songDict = {}
		if db.has_key(str(i)):
			songDict = eval(db[str(i)])
		for j in range(x):
			if i <> j:
				if songDict.has_key(j):
					songDict[j] +=1
				else:
					songDict[j] = 1
		db[str(i)]=str(songDict)
	print(str(len(db.keys())))
    user_id+=1
user_id = 1
db.close
