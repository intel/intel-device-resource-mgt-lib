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


#ifndef __PATH_H__
#define __PATH_H__

//#include "ams_constants.h"

#ifdef __cplusplus
extern "C" {
#endif


// note: the path returned will end with '/'
void path_init(char* exec);

char* get_root_path();
char* get_log_path();
char* get_config_path();
char* get_bin_path();
char* get_tools_path();
char* get_tmp_path();
char* get_bpk_path();
char* get_jeff_path();
char* get_imrt_path();
char* get_trace_path();
char* get_cache_path();
char* get_plugin_path();
char *get_product_config_pathname(char *config_pathname, char* target_type, char * target_id, char * buffer);
char *get_local_config_path(char * cfgname, int len, char * config_name);

extern short g_is_deploy_mode;


#ifdef __cplusplus
}
#endif

#endif
