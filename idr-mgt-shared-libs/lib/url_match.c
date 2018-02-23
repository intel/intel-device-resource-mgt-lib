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
 * url_match.c
 *
 *  Created on: Jul 19, 2017
 *      Author: xin
 */
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>

#include "agent_core_lib.h"

// check if the "url" is starting with "leading_str"
// return: 0 - not match; >0 - the offset of matched url, include any "/" at the end
// 1. it ensures the leading_str "/abc" can pass "/abc/cde" and "/abc/, but fail "/ab" and "/abcd".
//    leading_str "/abc/" can pass "/abc"
// 2. it omit the '/' at the first char
//
int check_url_start(const char* url, int url_len, char * leading_str)
{
    int offset  = 0;
    if(*leading_str == '/') leading_str++;
    if(url_len > 0 && *url =='/')
    {
        url_len --;
        url ++;
        offset ++;
    }

    int len = strlen(leading_str);
    if(len == 0) return 0;


    // ensure leading_str not end with "/"
    if(leading_str[len-1] == '/')
    {
        len --;
        if(len == 0) return 0;
    }

    // equal length
    if(url_len == len)
    {
        if (memcmp(url, leading_str, url_len) == 0)
        {
            return (offset + len);
        }
        else
        {
            return 0;
        }
    }

    if(url_len < len)
        return 0;

    else if(memcmp(url, leading_str, len) != 0)
        return 0;

    else if(url[len] != '/')
        return 0;
    else
        return (offset + len + 1);
}


/*
 * sample 1: /abcd, match /abcd only
 * sample 2: /abcd/ match match "/abcd" and "/abcd/*"
 * sample 3: /abcd*, match any url started with "/abcd"
 * sample 4: /abcd/*, exclude "/abcd"
 *
 */

bool match_url(char * pattern, char * matched)
{
    if(*pattern == '/') pattern ++;
    if(*matched == '/') matched ++;

    int matched_len = strlen(matched);
    if(matched_len == 0)
        return false;

    if(matched[matched_len-1] == '/')
    {
        matched_len --;
        if(matched_len == 0)
            return false;
    }

    int len = strlen(pattern);
    if(len  == 0)
        return false;

    if(pattern[len-1] == '/')
    {
        len --;
        if(strncmp(pattern, matched, len) != 0)
            return false;

        if(len == matched_len)
            return true;

        if(matched_len > len && matched[len] == '/')
            return true;

        return false;

    }
    else if (pattern[len-1] == '*')
    {
        if (pattern[len-2] == '/')
        {
            if(strncmp(pattern, matched, len-1) == 0)
                return true;

            else
                return false;
        }
        else
        {
            return (strncmp(pattern, matched, len-1) == 0);
        }
    }
    else
    {
        return (strcmp(pattern, matched) == 0);
    }
}
