/*
*	GigaOM.cpp
*
*   Program for 
*	GigaOM WordPress Challenge, Powered by Splunk
*	(http://www.kaggle.com/c/predict-wordpress-likes)
*
*	Copyright (C) 2012  Olexandr Topchylo
*
*	This program is free software: you can redistribute it and/or modify    
*	it under the terms of the GNU General Public License as published by       
*	the Free Software Foundation, either version 3 of the License, or          
*	(at your option) any later version.                                        
*
*/

#include "gigaom.h"
#include "json.h"
#include "pso.h"

MICU stat_u;
MICB stat_b;
MICL likes;
MICP test_p, train_p, my_test_p;
vector<int> train_u, test_u, my_test_u;
vector<int> top_posts;		
vector<vector<int> > solution; 
time_t test_end, train_end, my_train_end;
vector<double> pdf(35/6.*SECS_IN_DAY+1);

double x[2];   // model tuning parameters
int max_hashes = 10;
ofstream myout("myout.txt");

//---------------------------------------------------------------------------
//                  main()
//---------------------------------------------------------------------------
int main()
{	    
	myout << clock()/CLOCKS_PER_SEC << "s\t start";

	// read data	
	read_stat_u("kaggle-stats-user.json");	
	read_stat_b("kaggle-stats-blog.json");
	read_likes("trainUsers.json");			    
	test_end = read_posts("testPosts.json", &test_p);			
	train_end = read_posts("trainPosts.json", &train_p);	
	
	// split train set into my_train and my_test sets
	my_train_end = split(); 

	calc_score_t();

	// find optimal model parameters x[] for my_test set
	find_top100(my_train_end, my_test_p);
	srand(1);
	PSO(perf, 2, 0., 100., 100, x); 
	

	myout << endl << clock()/CLOCKS_PER_SEC << "s\t Optimal parameters: " << x[0] << "\t" << x[1];

	// make solution for original test set 
	max_hashes = 100;
	find_top100(train_end, test_p);
	solve(test_u, test_p, train_end, test_end);

	// make submission file 	
	save_solution("solution.csv"); 

	myout << endl << clock()/CLOCKS_PER_SEC << "s\t end";
}

//---------------------------------------------------------
//				calc_score_t()
//---------------------------------------------------------
void calc_score_t()
{
	myout << endl << clock()/CLOCKS_PER_SEC << "s\t calc_score_t";
	
	time_t time0 = my_train_end; 

	int count_p=0;
	for (MICP::iterator itp=train_p.begin(); itp!=train_p.end(); itp++) 
	{
		time_t time = itp->time;
		if (time>time0) continue;
		int pid = itp->pid;
		vector<int> deltas;
		for (MICL::nth_index<1>::type::iterator it = likes.get<1>().lower_bound(pid); it!=likes.get<1>().upper_bound(pid); ++it) 
		{
			time_t time2 = it->time;
			int delta = time2-time;
			if (delta>=35/6.*SECS_IN_DAY) continue;  
			if (delta<0) 
			{
				deltas.clear();
				break;
			}
			deltas.push_back(delta);
		}
		int total=deltas.size();
		if (total==0) continue;

		count_p++;
		// calc pdf		
		foreach (int sec, deltas) 
			pdf[sec] += 1./total;			
	}
	// normalize pdf
	double prev_pdf=0;
	for (int i=0; i<35/6.*SECS_IN_DAY; i++) 
	{
		pdf[i] = prev_pdf + pdf[i]/count_p;
		prev_pdf = pdf[i];
	}
}

//--------------------------------------------------------------------------------------
//                  perf()
//
//  Generates a solution for my_test set and given xx[], and returns -MAP@100
//--------------------------------------------------------------------------------------
double perf(double* xx)
{
	x[0] = xx[0];
	x[1] = xx[1];
	solve(my_test_u, my_test_p, my_train_end, train_end); 
	double map = calc_map100(my_train_end); 
	myout << "\t" << x[0] << "\t" << x[1] << "\t" << map;
	return -map;     
}

//--------------------------------------------------------------------------------------
//				read_likes()
//
//  Reads trainUsers.json and generates containers train_u and likes
//--------------------------------------------------------------------------------------
void read_likes(char const* file_name)
{
	myout << endl << clock()/CLOCKS_PER_SEC << "s\t read_likes: "  << file_name << flush;	
	ifstream infile(file_name);
	string line;
	getline(infile, line, '\n');
	while (!infile.eof())
	{
		int size = line.size();
		char* buf = new char[size];
		line.copy(buf, size);		
		char* sz = buf;
		buf[size-1]=0;
		bool in_test = read_json_bool(sz, "inTestSet");
		int uid = read_json_str2int(sz, "uid");
		train_u.push_back(uid);
		if (in_test) test_u.push_back(uid);
		int bid = read_json_str2int(sz, "blog");
		while(bid) 
		{
			int pid = read_json_str2int(sz, "post_id");
			time_t time = read_json_time(sz, "like_dt");
			likes.insert(Like(uid, bid, pid, time));
			bid = read_json_str2int(sz, "blog");
		}
		getline(infile, line, '\n');
		delete[] buf;		
	}
	infile.close();
}
//--------------------------------------------------------------------------------------
//				read_stat_u()
//
// Reads kaggle-stats-users*.json and generates container stat_u
//--------------------------------------------------------------------------------------
void read_stat_u(char const* file_name)
{
	myout << endl << clock()/CLOCKS_PER_SEC << "s\t read_stat_u: "  << file_name << flush;
	ifstream infile(file_name);
	string line;
	getline(infile, line, '\n');
	while (!infile.eof())
	{
		int size = line.size();		
		char* buf = new char[size];
		line.copy(buf, size);
		char* sz = buf;
		buf[size-1]=0;
		int uid = read_json_int(sz, "user_id");		
		int bid = read_json_str2int(sz, "blog_id");	
		while(bid) 
		{
			int num_likes = read_json_str2int(sz, "likes"); 
			stat_u.insert(StatU(uid, bid, num_likes));	
			bid = read_json_str2int(sz, "blog_id");	
		}
		getline(infile, line, '\n');
		delete[] buf;						
	}
	infile.close();
}
//--------------------------------------------------------------------------------------
//				read_stat_b()
//
// Reads kaggle-stats-blogs*.json and generates container stat_b
//--------------------------------------------------------------------------------------
void read_stat_b(char const* file_name)
{
	myout << endl << clock()/CLOCKS_PER_SEC << "s\t read_stat_b: "  << file_name << flush;
	string line;
	ifstream infile(file_name);
	getline(infile, line, '\n');
	while(!infile.eof())
	{	    
		int size = line.size();
		char* buf = new char[size];
		line.copy(buf, size);		
		char* sz = buf;

		int bid = read_json_int(sz, "blog_id");
		int num_posts = read_json_int(sz, "num_posts");
		int num_likes = read_json_int(sz, "num_likes");
		stat_b.insert(StatB(bid, num_posts, num_likes));
		
		delete[] buf;		
		getline(infile, line, '\n');
	}
	infile.close();
}

//--------------------------------------------------------------------------------------
//				read_posts()			       
//
//   Reads *Posts.json and generates a MICP container.
//   Returns the maximum value of a "date_gmt" node. 
//--------------------------------------------------------------------------------------
time_t read_posts(char const* fname, MICP* micp)
{
	myout << endl << clock()/CLOCKS_PER_SEC << "s\t read_posts: "  << fname << " ";
	time_t max=0, min=LONG_MAX;
	string line;
	int count = 0;
	ifstream infile(fname);		
	getline(infile, line, '\n');
	while (!infile.eof()) 
	{
		int size = line.size();						
		char* buf = new char[size];					
		line.copy(buf, size);						
		char* sz = buf;								

		time_t time = read_json_time(sz, "date_gmt"); 
		max = MAX(time, max);						
		int bid = read_json_str2int(sz, "blog");		
		int pid = read_json_str2int(sz, "post_id");	
		vector<unsigned> hash;
		read_json_hash(sz, "tags", &hash);
		read_json_hash(sz, "categories", &hash);
		micp->insert(Post(bid, pid, time, hash));
		if (++count % 20000 == 0)  myout << "." << flush; // progress bar
		getline(infile, line, '\n');
		delete[] buf;
	}
	infile.close();

	return max;
}


//--------------------------------------------------------------------------------------
//				save_solution()
// 
//  Saves solution to file
//--------------------------------------------------------------------------------------
void save_solution(char const* file_name) 
{
	myout << endl << clock()/CLOCKS_PER_SEC << "s\t save_solution";

	ofstream out(file_name);
	int num_tests = test_u.size();
	out << "\"posts\"" << endl;
	for (int n=0; n<num_tests; n++) 
	{		
		for (int j=0; j<STO; j++) 
			out << solution[n][j] << " ";
		out << endl;
	}
	out.close();
}

//--------------------------------------------------------------------------------------
//					         calc_map100()
//
//   Returns Mean Average Precision @100 of solution[][] 
//--------------------------------------------------------------------------------------
double calc_map100(time_t my_train_end)
{
	int tests = my_test_u.size();
	int examples = 0;
	double total_sum=0;
	for (int i=0; i<tests; i++) 
	{	
		int uid  = my_test_u[i];						
		vector<int> elem;  // "likes" in my_test set
		for (MICL::iterator it=likes.lower_bound(uid); it!=likes.upper_bound(uid); it++) 
			if (it->time > my_train_end) 
				elem.push_back(it->pid);
		int elements = elem.size();
		double denominator = MIN(elements,STO);
		double sum = 0;
		int right_count=0;
		for (int k=0; k<STO; k++) 
		{
			int b = solution[i][k];
			if (!b) break;
			bool is_right=false;
			for (int j=0; j<elements; j++) 
			{
				if (elem[j]==b) 
				{
					is_right = true;
					break;
				}
			}
			if (is_right) 
			{
				right_count++;
				sum += right_count/(k+1.);
			}
		}
		double avg_precision = (denominator? sum/denominator : 0);
		total_sum += avg_precision;		
		examples++;
	}

	return examples ? total_sum/examples : 0;	
}
//---------------------------------------------------------------------------------------
//				split()
//
// 	Splits train set into my_train and my_test(validation) sets.
//  Returns the time of splitting.
//---------------------------------------------------------------------------------------
time_t split()
{
	myout << endl << clock()/CLOCKS_PER_SEC << "s\t split";

	// Split train period into my_train and my_test periods, retaining proportion 5:1.
	time_t my_train_end = train_end - 35/6.*SECS_IN_DAY;        

	// my_train set consists of the posts and likes which were created before my_train_end.
	// Remaining data are considered as my_test set.

	// generate my_test_u from train_u
	int count = train_u.size();
	for (int i=0; i<count; i++) 
	{
		int uid = train_u[i];
		int likes_before=0, likes_after=0;
		for (MICL::iterator it=likes.lower_bound(uid); it!=likes.upper_bound(uid); it++) 
		{
			if (it->time <= my_train_end) 
				likes_before++;
			else		 		          
				likes_after++;	
		}
		// The "my_test" users are restricted to users who have "liked" at least 1 post 
		// in the my_test period and at least 5 posts in the my_train period.
		if (likes_before>=5 && likes_after>0) 
			my_test_u.push_back(uid);
	}

	// generate my_test_p from train_p
	for (MICP::iterator it=train_p.begin(); it!=train_p.end(); it++) 
		if (it->time > my_train_end) 
			my_test_p.insert(Post(it->bid,it->pid,it->time,it->hash));

	return my_train_end;
}

//--------------------------------------------------------------------------------------
//				find_top100()
//
//  Generates top 100 posts, for users who don't like very many blogs 
//--------------------------------------------------------------------------------------
void find_top100(time_t train_end, MICP test_p)
{
	myout << endl << clock()/CLOCKS_PER_SEC << "s\t find_top100";

	MAns top_blogs;
	for (MICB::iterator it=stat_b.begin(); it!=stat_b.end(); it++) 
	{
		int bid = it->bid;
		int old_posts = it->num_posts;
		int old_likes = it->num_likes;
		int new_likes=0;

		set<int> posts;
		for (MICL::nth_index<2>::type::iterator it2=likes.get<2>().lower_bound(bid); it2!=likes.get<2>().upper_bound(bid); ++it2) 
		{
			if (it2->time > train_end) continue;
			posts.insert(it2->pid);
			new_likes++;   			
		}
		int new_posts = posts.size();
		float r_new = new_likes/(new_posts+10.);
		float r_old = old_likes/(old_posts+10.);
		top_blogs.insert(pair<float,int>(r_new+r_old, bid));   		
	}

	// select 100 posts from top blogs		
	top_posts.clear();
	for (MAns::reverse_iterator rit=top_blogs.rbegin(); rit!=top_blogs.rend(); rit++) 
	{
		int bid = rit->second;
		for (MICP::iterator it2=test_p.lower_bound(bid); it2!=test_p.upper_bound(bid); it2++) 		
			top_posts.push_back(it2->pid);
		if (top_posts.size()>=STO) break;
	}
}

//--------------------------------------------------------------------------------------
//				solve()
//
//  Generates a list of posts-canditates, calculates their scores, sorts and 
//  saves top 100 candidates to solution[][]
//--------------------------------------------------------------------------------------
void solve(vector<int> test_u, MICP test_p, time_t train_end, time_t test_end)
{	
	myout << endl << clock()/CLOCKS_PER_SEC << "s\t solve ";

	float period = test_end - train_end;
	int num_tests = test_u.size();
	solution.resize(num_tests); 

	for (int n=0; n<num_tests; n++) 
	{               
		solution[n].resize(STO,0); 
		int uid  = test_u[n];	// user ID			

		// Generate a hash/frequency map of keywords for the user
		MUU u_hash; // [hash] frequency
		for (MICL::iterator it=likes.lower_bound(uid); it!=likes.upper_bound(uid); it++) 
		{
			if (it->time > train_end) continue;			
			int pid = it->pid;		
			for (MICP::nth_index<1>::type::iterator it2 = train_p.get<1>().lower_bound(pid); it2!=train_p.get<1>().upper_bound(pid); ++it2) 
				foreach (unsigned h, it2->hash)
					u_hash[h]++;
		}
		
		// Sort hashes by frequencies
		MmUU sorted_hash; // [frequency] hash
		for (MUU::iterator it=u_hash.begin(); it!=u_hash.end(); it++)
			sorted_hash.insert(pair<unsigned,unsigned>(it->second, it->first));
				
		// Collect statistics of 'likes' of the user on every blog
		map<int, pair<int, int> > user_blogs;  // [bid] <new_blog_likes,old_blog_likes>

		// statistics on 'new' likes (based on train set)
		int new_total_likes=0;
		for (MICL::iterator it=likes.lower_bound(uid); it!=likes.upper_bound(uid); it++) 
		{
			if (it->time > train_end) continue;
			user_blogs[it->bid].first++;
			new_total_likes++;			
		}

		// statistics on 'old' likes (based on long term statistics)
		int old_total_likes=0;
		for (MICU::iterator it=stat_u.lower_bound(uid); it!=stat_u.upper_bound(uid); it++) 
		{
			user_blogs[it->bid].second += it->num_likes;
			old_total_likes += it->num_likes;
		}

		// All post from these blogs are considered as candidates to the solution
		MCands cands; // [pid] score
		for (map<int,pair<int,int> >::iterator it=user_blogs.begin(); it!=user_blogs.end(); it++) 
		{
			int bid = it->first; // blog ID

			// Calculate scoreB. It is common for all posts in the blog
			int test_posts = test_p.count(bid);
			if (!test_posts) continue;
			int new_blog_likes = it->second.first;
			int old_blog_likes = it->second.second;
			float r_new = new_blog_likes / (new_total_likes + 10.);  
			float r_old = old_blog_likes / (old_total_likes + 10.);  
			float scoreB = (r_new + r_old) / (test_posts + 1.);  

			for (MICP::iterator it2=test_p.lower_bound(bid); it2!=test_p.upper_bound(bid); it2++) 
			{
				int pid = it2->pid;  // post ID

				// Calculate scoreT
				int delta = MIN(test_end-it2->time, 35/6.*SECS_IN_DAY-1);
				if (delta<=0) delta =0;
				float scoreT = pdf[delta];
				
				// Calculate scoreH. It is proportional to the similarity between the test post and the posts which were "liked" by the user
				float hashes = 0;
				int num_best_hashes = 0;
				float max_freq = 0.01;				
				for (MmUU::reverse_iterator rit=sorted_hash.rbegin(); rit!=sorted_hash.rend(); rit++) 
				{		
					unsigned h1 = rit->second;
					unsigned freq = rit->first;
					if (num_best_hashes==0) max_freq = freq; 				
					foreach (unsigned h2, it2->hash)
						if (h1==h2) { 
							hashes += freq;	
							break; 
						} 
					// limit the max number of hashes to reduce computation time
					if (++num_best_hashes >= max_hashes) break;  
				}
				float scoreH = hashes / max_freq;

				// calculate total score
				cands[pid] = 10000*scoreB + x[0]*scoreT + x[1]*scoreH;
			}
		}
		if (n%250 == 0) myout << "." << flush;  // progress bar
		
		// Sort candidates and save to solution[][]
		if (cands.size()>0) 
		{
			MAns ans;
			for (MCands::iterator it=cands.begin(); it!=cands.end(); ++it) 
				ans.insert(pair<float, int>(it->second, it->first));

			int num=0;
			for (MAns::reverse_iterator rit=ans.rbegin(); rit!=ans.rend(); rit++) 
			{
				solution[n][num]=rit->second;
				if (++num==STO) break;
			}
		}
		
		// add top100 posts		
		int num = cands.size();
		for (int i=0; i<STO-num; i++)
			solution[n][num+i] = top_posts[i];
	}
}
