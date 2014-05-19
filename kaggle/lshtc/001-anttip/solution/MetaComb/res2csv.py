import sys
x=1
print 'Id,Predicted'
for line in open(sys.argv[1],'r'):
	print str(x)+','+line[:-1].replace(",", " ").replace("  ", " ").strip()
	x+=1
