/*
 * Copyright (C) 2017 Intel Corporation.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * string_parser.cpp
 *
 *  Created on: Jan 21, 2017
 *      Author: xwang98
 */

#include "string_parser.h"

#if 0
/*
#include <map>
void parse_query_string(const char * query, std::map <std::string, std::string> & map_query)
{
	char * query_str = strdup(query);
	if(NULL == query_str)
	{
		return;
	}

	char * p = query_str;
	char * key = p;
	char * value = NULL;

    while(*p == ' ') p++;

	while(*p)
	{
		if(*p == '=')
		{
			*p = '\0';
			p++;
			while(*p == ' ') p++;
			value = p;
		}
		else if(*p == '&')
		{
			*p = '\0';
			if(value == NULL)
				goto end;

		    map_query[key] = value;
		    p++;
		    while(*p == ' ') p++;
		    value = NULL;
		    key = p;
		}
		else
		{
			p++;
		}
	}
end:
	free(query_str);
}

*/
#endif

// parse format [token1, token2] into a char * array which end with NULL pointer.
char ** parse_string_array(char * str, char delimiter)
{

	int cnt = 0;
	char ** l = NULL;
	char * value  = strdup(str);
	char * p = value;

	char * start;
	char * end;
    int i = 0;

	while(*p == ' ') p ++;
	if(*p != '[')
		goto end;
	p++;
	while(*p == ' ')p++;

	end = strrchr(p, ']');
	if(end == NULL) return NULL;
	*end = 0;
	if(*p == 0)goto end;

	start = p;
	while(*p)
	{
		if(*p == delimiter) cnt ++;
		p++;
	}

	l = (char **)malloc(sizeof(char **) * (cnt +2));
	memset(l, 0, sizeof(char **) * (cnt +2));
	p = value;
	while(*p)
	{
		if(*p == delimiter)
		{
			*p = 0;
			l[i++] = strdup(start);
			p++;
			while(*p == ' ')p++;
			start = p;
		}
		else
		{
			p++;
		}
	}
	l[i++] = strdup (start);
	assert(i == (cnt+1));

end:
	free (value);
	return l;
}


void free_str_list(char ** str_list)
{
	char ** p = str_list;
	while(*p)
	{
		free(*p);
		p++;
	}
	free(str_list);
}


// parse format [token1, token2] into a char * array which end with NULL pointer.
void * parse_string_array_to_Json(char * str, char delimiter)
{

	JSON_Value *json_root = NULL;
	char * value  = strdup(str);
	char * p = value;
	char * start;
	char * end;
    int i = 0;
	while(*p == ' ') p ++;
	if(*p != '[')
		goto end;
	p++;
	while(*p == ' ')p++;

	end = strrchr(p, ']');
	if(end == NULL) return NULL;
	*end = 0;
	if(*p == 0)goto end;

	json_root = json_value_init_array();
	start = p;
	p = value;
	while(*p)
	{
		if(*p == delimiter)
		{
			*p = 0;
			json_array_append_string(json_array(json_root), start);
			p++;
			while(*p == ' ')p++;
			start = p;
		}
		else
		{
			p++;
		}
	}
	if(start[0])
		json_array_append_string(json_array(json_root), start);

end:
	free (value);
	return json_root;
}
