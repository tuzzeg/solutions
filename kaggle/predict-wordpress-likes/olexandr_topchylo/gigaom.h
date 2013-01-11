#include <fstream>
#include <set>
#include <string>
#include <vector>
#include <map>
#include <list>
#include <locale>
#include <conio.h>
#include <ctime>
#include <time.h>
#include <limits.h>
#include <string.h>
#include <stdlib.h>

#include <iostream>
#include <iterator>
#include <algorithm>

#include <boost/multi_index_container.hpp>
#include <boost/multi_index/ordered_index.hpp>
#include <boost/multi_index/member.hpp>
#include <boost/foreach.hpp>


#ifndef MAIN_H
#define MAIN_H

using namespace std;
using namespace ::boost;
using namespace ::boost::multi_index;

#define foreach BOOST_FOREACH 
#define reverse_foreach BOOST_REVERSE_FOREACH
#define MAX(a,b) (a>b?a:b)
#define MIN(a,b) (a<b?a:b)

#define STO 100
#define SECS_IN_DAY (24*3600)
#define SECS_IN_WEEK (7*24*3600)

typedef map<unsigned, unsigned> MUU;
typedef multimap<unsigned, unsigned> MmUU;
typedef map<int, int> MII;
typedef map<int, float> MCands;
typedef multimap<float, int> MAns;
typedef multimap<int, int> MmII;

struct Like {
	int uid;    // user ID
	int bid;	// blog ID
	int pid;	// post ID
	time_t time;
	Like(int uid,int bid,int pid,int time):uid(uid),bid(bid),pid(pid),time(time) {}
};

struct StatU {
	int uid;
	int bid;
	int num_likes;
	StatU(int uid,int bid,int num_likes):uid(uid),bid(bid),num_likes(num_likes) {}
};

struct StatB {	
	int bid;
	int num_posts;
	int num_likes;
	StatB(int bid,int num_posts,int num_likes):bid(bid),num_posts(num_posts),num_likes(num_likes) {}
};

struct Post {	
	int bid;
	int pid;
	time_t time;
	vector<unsigned> hash;  // hashes of keywords from "tags" and "categories" sections
	Post(int bid, int pid, time_t time, vector<unsigned> hash): bid(bid),pid(pid),time(time),hash(hash){}
};

struct Blog {
	int old_posts;
	int old_likes;
	int new_posts;
	int new_likes;
	int test_posts;
	Blog(){};
	Blog(int old_posts,int old_likes,int new_posts,int new_likes,int test_posts):old_posts(old_posts), old_likes(old_likes), new_posts(new_posts), new_likes(new_likes), test_posts(test_posts){}
};
typedef map<int,Blog> MBlog;

typedef multi_index_container<
	Like, 
	indexed_by<	
		ordered_non_unique<member<Like,int,&Like::uid> >, 
		ordered_non_unique<member<Like,int,&Like::pid> >,
		ordered_non_unique<member<Like,int,&Like::bid> >
	> 
> MICL;

typedef multi_index_container<
	StatU, 
	indexed_by<	
		ordered_non_unique<member<StatU,int,&StatU::uid> >, 
		ordered_non_unique<member<StatU,int,&StatU::bid> >
	> 
> MICU;

typedef multi_index_container<
	StatB, 
	indexed_by<	
		ordered_unique<member<StatB,int,&StatB::bid> >
	> 
> MICB;

typedef multi_index_container<
	Post, 
	indexed_by<	
		ordered_non_unique<member<Post,int,&Post::bid> >,
		ordered_unique<member<Post,int,&Post::pid> >
	> 
> MICP;

void read_stat_u(char const* file_name);
void read_stat_b(char const* file_name);
void read_likes(char const* file_name);
time_t read_posts(char const* file_name, MICP* micp);
time_t split();
void solve(vector<int> test_u, MICP test_p, time_t train_end, time_t test_end);
void save_solution(char const* file_name); 
double calc_map100(time_t my_train_end);
void find_top100(time_t train_end, MICP test_p);
double perf(double* xx);
void calc_score_t();

#endif
