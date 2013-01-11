#should create file with users as line number and with the song history as "songTitleA playCountA songTitleB playCountB"
import sys
import fileinput
newfile = open("KunalsReccomendations.txt","w")
test = 'sorted3.txt'
count = 1
currentUser = 1
linecount = 0
currentHistory = ''
for line in fileinput.input([test]):
	temp = line.rstrip('\n')
	triplet = temp.split()
	if count == 1:
		currentUser = triplet[0]
	if currentUser == triplet[0] :
		currentHistory += str(triplet[1]) + ' '
	else:
		newfile.write(currentHistory + '\n')
		currentHistory = str(triplet[1] + ' ')
		linecount +=1
		currentUser = triplet[0]
		#print(str(linecount) + '....'+ currentUser)
		while (linecount < (int(currentUser)-1)):
			newfile.write('\n')
			linecount +=1
	count+=1
newfile.write(currentHistory)
newfile.close()
