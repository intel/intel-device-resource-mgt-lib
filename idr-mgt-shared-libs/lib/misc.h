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


#ifndef __MISC_H__
#define __MISC_H__
#include <stdbool.h>
#ifdef __cplusplus
extern "C" {
#endif

#ifndef MIN
#define MIN(a,b) (a)<(b)?(a):(b)
#endif

// http://stackoverflow.com/questions/1640423/error-cast-from-void-to-int-loses-precision
#define PTR_TO_INT(arg)  (*((int*)(&arg)))

#define MACRO_STR(x) MICRO_DEF(x)
#define MICRO_DEF(x) #x

#define SECONDS_TO_DAYS(s) ((s)/(24*60*60))
#define SECONDS_TO_MINUTES(s) ((s)/(60))
#define SECONDS_TO_HOURS(s) ((s)/(60*60))
#define SECONDS_OF_DAY (24*60*60)

#ifndef _PLATFORM
#define SZ_PLATFORM "unknown"
#else
#define SZ_PLATFORM MACRO_STR(_PLATFORM)
#endif

#define COUNT_OF(x) (sizeof(x)/sizeof(x[0]))

#ifndef FIELD_OFFSET
#define FIELD_OFFSET(type, field)    ((LONG)(LONG_PTR)&(((type *)0)->field))
#endif

#ifndef Add2Ptr
#define Add2Ptr(P,I) ((PVOID)((PUCHAR)(P) + (I)))
#endif

#ifndef ROUND_TO_SIZE
#define ROUND_TO_SIZE(_length, _alignment)    \
            (((_length) + ((_alignment)-1)) & ~((_alignment) - 1))
#endif

#ifndef FlagOn
#define FlagOn(_F,_SF)        ((_F) & (_SF))
#endif

#ifndef FlagOff
#define FlagOff(_F,_SF)        (((_F) & (_SF)) == 0)
#endif


extern unsigned long get_file_size(const char *path , time_t * modi_time);
extern unsigned long dir_files_size(char * path);
extern void make_full_dir(char * path);
extern int create_socket_r();
extern int already_running(const char *filename);

extern int ini_get_int(const char* cfgname, char * key, int def);

// value=NULL: unset the key from the file
extern bool set_ini_key(char *cfgname , const char * key, const char * value);

extern void prv_output_buffer(char * buffer,    int length);
extern int load_file_to_memory(const char *filename, char **result);
extern char * get_string(char * data, int data_len, bool *allocated);
extern int copy_to_found_char(char * source, int source_len, char c, char * dest, int dest_len);


#ifdef __cplusplus
}
#endif

#endif // __MISC_H_
