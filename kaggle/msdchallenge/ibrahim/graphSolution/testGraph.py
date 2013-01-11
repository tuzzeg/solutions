import networkx as nx
G = nx.Graph()
lineCount = 1
import fileinput
user_id = 1
for line in fileinput.input(["userHistoriesTESTFILE.txt"]):
    history = line.rstrip('\n').split()
    x = len(history)
    for i in range(x):
        songDict = {}
        if i in G:
            songDict = eval(db[str(i)])
        for j in range(x):
            if i != j:
                if  str(j) in songDict:
                    songDict[j] += 1
                else:
                    songDict[j] = 1
        db[str(i)] = str(songDict)
    print(str(len(db.keys())))
    user_id += 1
user_id = 1
db.close
