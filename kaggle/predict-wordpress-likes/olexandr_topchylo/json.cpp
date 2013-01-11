#include <vector>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#include "json.h"

//------------------------------------------------------------------------------------------
//				read_json_int()
//
//  Scans a string for a JSON name/value pair.
//  The value must be integer.
//  Returns the value or 0 if the name was not found.
//------------------------------------------------------------------------------------------
int read_json_int(char* &sz, const char* name)
{
	int len = strlen(name);
	char fullname[256]="\"";
	strcpy(fullname+1,name);
	strcpy(fullname+len+1,"\":");
	sz = strstr(sz,fullname);
	if (!sz) return 0;
	sz += len+3;
	return atoi(sz);
}

//------------------------------------------------------------------------------------------
//				read_json_str2int()
//
//  Scans a string for a JSON name/value pair and converts the value into integer.
//  The value must be a quoted string which contains an integer number.
//  Returns the integer number or 0 if the name was not found.
//------------------------------------------------------------------------------------------
int read_json_str2int(char* &sz, const char* name)
{
	int len = strlen(name);
	char fullname[256]="\"";
	strcpy(fullname+1,name);
	strcpy(fullname+len+1,"\":");
	sz = strstr(sz,fullname);
	if (!sz) return 0;
	sz = strchr(sz+len+3,'\"');
	if (!sz) return 0;
	return atoi(++sz);
}

//------------------------------------------------------------------------------------------
//				read_json_bool()
//
//  Scans a string for a JSON name/value pair.
//  The value must be bool.
//  Returns the value or false if the name was not found.
//------------------------------------------------------------------------------------------
bool read_json_bool(char* &sz, const char* name)
{
	int len = strlen(name);
	char fullname[256]="\"";
	strcpy(fullname+1,name);
	strcpy(fullname+len+1,"\":");
	sz = strstr(sz,fullname);
	if (!sz) return false;
	sz += len+3;
	if (*sz==' ') sz++;
	return (*sz=='t');
}

//------------------------------------------------------------------------------------------
//				read_json_time()
//
//  Scans a string for a JSON name/value pair and converts the value into time_t.
//  The value must be a quoted string in format "YYYY-MM-DD hh:nn:ss" .
//  Returns time_t value or 0 if the name was not found.
//------------------------------------------------------------------------------------------
time_t read_json_time(char* &sz, const char* name)
{
	int len = strlen(name);
	char fullname[256]="\"";
	strcpy(fullname+1,name);
	strcpy(fullname+len+1,"\":");
	sz = strstr(sz,fullname);
	if (!sz) return 0;
	sz = strchr(sz+len+3,'\"');
	if (!sz) return 0;

	tm t;
	t.tm_year = atoi(sz+1)-1900;
	t.tm_mon = atoi(sz+6)-1;
	t.tm_mday = atoi(sz+9);
	t.tm_hour = atoi(sz+12);
	t.tm_min = atoi(sz+15); 
	t.tm_sec = atoi(sz+18);
	sz+=20;

	return mktime(&t);
}

//------------------------------------------------------------------------------------------
//				read_json_hash()
//
//  Scans a string for a JSON array of strings and makes a vector of hashes.
//  Returns the number of strings in the array.
//------------------------------------------------------------------------------------------
int read_json_hash(char* &sz, const char* name, std::vector<unsigned>* hash)
{
	int len = strlen(name);
	char fullname[256]="\"";
	strcpy(fullname+1,name);
	strcpy(fullname+len+1,"\":");
	sz = strstr(sz,fullname);
	if (!sz) return 0;
	sz = strchr(sz+len+3,'[');
	if (!sz) return 0;
	char* end = strchr(sz+1,']');
	if (!end) return 0;

	int count=0;
	while (bool(sz=strchr(sz+1,'\"')) && sz<end) 
	{	
		char* prev = sz++;
		unsigned h = 5381;
		while (*sz!='\"' || *prev=='\\') 
		{    
			prev=sz;
		    h = ((h<<5)^(h>>27))^(unsigned char)*sz++;
		}
		hash->push_back(h);
		count++;
	}
	return count;
}

