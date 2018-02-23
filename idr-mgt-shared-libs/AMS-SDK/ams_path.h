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


#include <stdbool.h>

#define PATH_LEN 256

extern bool ams_set_product_root(char * root_dir);
extern bool ams_locate_product_path(char * execpath, char * bin_path);
extern char * ams_get_product_root(char * path);
extern char * ams_get_product_config_dir(char * path);
extern char * ams_get_product_plugin_dir( char * path);
extern char * ams_get_product_version_dir( char * path);
extern char * ams_get_product_scripts_dir(char * path);
extern char * ams_get_product_config_pathname( char * config_path_name, char * target_type, char * target_id, char * out_path);
extern char * ams_make_temp_dir(char * dir, char * subdir);
extern char * ams_get_temp_root(char * dir);
extern char * ams_get_log_path();
