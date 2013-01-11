Instructions 
------------

To reproduce results you need:
1) OS Windows (XP or later)
2) installed MinGW  -  Minimalist GNU for Windows with C++ compiler (http://mingw.org)
3) installed Boost library, ver.1.50 or later (http://boost.org)

The source code contains 6 files:
	GigaOM.cpp
	json.cpp
	pso.cpp
	gigaom.h
	json.h
	pso.h

The command to make executable file gigaom.exe:

	g++ GigaOM.cpp json.cpp pso.cpp -o gigaom



For making prediction for the 2nd (final) dataset the program needs following data files in the same directory:
	kaggle-stats-user.json
	kaggle-stats-blog.json
	trainUsers.json
	testPosts.json
	trainPosts.json

If you want to run the program for the 1st dataset you have to rename preliminary two kaggle-stats files:
	ren kaggle-stats-users-20111123-20120423.json kaggle-stats-user.json
	ren kaggle-stats-blogs-20111123-20120423.json kaggle-stats-blog.json



The program generates file solution.csv which contains a solution.
Also the program writes some auxiliary information to myout.txt during its work.
Approximate work time on an average computer: 4 hours.
