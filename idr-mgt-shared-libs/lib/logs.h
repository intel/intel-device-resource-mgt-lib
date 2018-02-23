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


#ifndef __LOGS_H_
#define __LOGS_H_

#include <stdarg.h>
#include <stdio.h>

#ifdef __cplusplus
extern "C" {
#endif

#define LOG_BUF_SIZE      1024
#define LOG_MASK_SIZE     10

#ifndef LOG_MY_MASK_ID
#define LOG_MY_MASK_ID     0
#endif

enum{
	LOG_VERBOSE,
	LOG_INFO,
	LOG_DEBUG,
	LOG_WARN,
	LOG_ERROR
};




extern unsigned long log_tag_mask[LOG_MASK_SIZE];
extern unsigned char log_level;


#define LOG_SET(flag)  ((log_tag_mask[LOG_MY_MASK_ID]) & (flag))
#define LOGV_SET(flag)  ((log_tag_mask[LOG_MY_MASK_ID]) & (flag) && (LOG_VERBOSE >= log_level))
#define LOGI_SET(flag)  ((log_tag_mask[LOG_MY_MASK_ID]) & (flag) && (LOG_INFO >= log_level))


#define ERROR(...) if((LOG_ERROR   >= log_level)) log_print("E:" LOG_TAG, __VA_ARGS__)
#define WARNING(...) if((LOG_WARN  >= log_level)) log_print("W:" LOG_TAG, __VA_ARGS__)


#define Trace(flag, level, tag, ...) if((log_tag_mask[LOG_MY_MASK_ID] & flag) && (LOG_VERBOSE >= level)) log_print(tag, __VA_ARGS__)

#define TraceD(flag,...) if((log_tag_mask[LOG_MY_MASK_ID] & flag) && (LOG_DEBUG   >= log_level)) log_print("D:" LOG_TAG, __VA_ARGS__)
#define TraceI(flag,...) if((log_tag_mask[LOG_MY_MASK_ID] & flag) && (LOG_INFO >= log_level)) log_print("I:" LOG_TAG, __VA_ARGS__)
#define TraceV(flag,...) if((log_tag_mask[LOG_MY_MASK_ID] & flag) && (LOG_VERBOSE >= log_level)) log_print("V:" LOG_TAG, __VA_ARGS__)

#define LOG_DATA(flag,title, buffer, len) if(len >0 && (log_tag_mask[LOG_MY_MASK_ID]  & flag) && (LOG_DEBUG   >= log_level)) log_buffer(title,buffer, len)

#define LOG_MSG(message)  if((LOG_WARN   >= log_level)) log_print("E:" LOG_TAG, "%s, function: %s,line: %d", message, __FUNCTION__, __LINE__);
#define LOG_ME()  if((LOG_WARN   >= log_level)) log_print("E:" LOG_TAG, "error condition in function %s, line %d", __FUNCTION__, __LINE__);
#define LOG_RETURN(ret)  { if(LOG_WARN   >= log_level)  log_print("E:" LOG_TAG, "return for error condition. function: %s, line: %d", __FUNCTION__, __LINE__); return ret;}
#define LOG_GOTO(message, label)  { if(LOG_WARN   >= log_level)  log_print("E:" LOG_TAG, "%s, leave for error condition. function: %s, line: %d",  message, __FUNCTION__, __LINE__); goto label;}


void log_init(const char* app, const char* log_path, const char* log_cfg_path);
void log_setlevel(int level);
void log_set_tag_mask(int tag_mask);
void log_clear_tag_mask(int tag_mask);
void log_print(const char* tag, const char* fmt, ...);
void log_check_refresh(int upload);
void log_gen();
char* log_to();
char * now_str(char *s );
void log_buffer(char * title, void * buffer,   int length);
FILE * log_get_handle();



/*
 * The ranage 0x00100000 -- 0x00010000 is reserved for library
 */


#ifndef LOG_FLAG_TASK
#define LOG_FLAG_TASK 0x00100000
#endif

#ifdef __cplusplus
}
#endif

#endif
