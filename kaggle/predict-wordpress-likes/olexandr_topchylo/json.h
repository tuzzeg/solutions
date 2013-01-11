#include <vector>

#ifndef JSON_H
#define JSON_H

int    read_json_int(char* &sz, const char* name);
int    read_json_str2int(char* &sz, const char* name);
bool   read_json_bool(char* &sz, const char* name);
time_t read_json_time(char* &sz, const char* name);
int    read_json_hash(char* &sz, const char* name, std::vector<unsigned>* hash);

#endif