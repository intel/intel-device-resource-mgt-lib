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


#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "path.h"
#include "misc.h"
#include "ams_path.h"

// note: the path returned must end with '/'

static char root_path[PATH_LEN] = {0};

static char bin_path[PATH_LEN] = {0};
static char config_path[PATH_LEN] = {0};
static char generic_path[PATH_LEN] = {0};

short g_is_deploy_mode  = 1;
extern char * getExecPath (char * path,size_t dest_len, char * argv0);

void path_init(char* exec)
{
	char buf[PATH_LEN];
	char* s;
	int n;

	getExecPath(buf, sizeof(buf), exec);

	ams_locate_product_path(buf, bin_path);

	ams_get_product_root(root_path);

         ams_get_product_config_dir(config_path);

	make_full_dir(get_log_path());
	make_full_dir(get_config_path());

    make_full_dir(get_tmp_path());
    make_full_dir(get_cache_path());
    make_full_dir(get_plugin_path());

}

char* get_root_path()
{
	return  (root_path);
}

char* get_log_path()
{
	return ams_get_log_path();
}

char* get_config_path()
{
	return config_path;
}

char *get_product_config_pathname(char *config_pathname, char* target_type, char * target_id, char * buffer)
{
    return ams_get_product_config_pathname(config_pathname, target_type, target_id, buffer);
}

char* get_bin_path()
{
	return bin_path;
}

char* get_tools_path()
{
    ams_get_product_root(generic_path);
    strcat(generic_path,  "upgrade_tool/");
    return generic_path;
}

char* get_tmp_path()
{
	ams_get_temp_root(generic_path);
	strcat(generic_path, "iagent/");
	return generic_path;
}

char* get_bpk_path()
{
    ams_get_product_root(generic_path);
    strcat(generic_path,  "applets/");
    return generic_path;}

char* get_jeff_path()
{
    ams_get_product_root(generic_path);
    strcat(generic_path,  "jeff/");
    return generic_path;}

char* get_imrt_path()
{
    ams_get_product_root(generic_path);
    strcat(generic_path,  "imrt/");
    return generic_path;
}

char* get_trace_path()
{
    ams_get_product_root(generic_path);
    strcat(generic_path,  "trace/");
    return generic_path;
}

char* get_cache_path()
{
    ams_get_product_root(generic_path);
    strcat(generic_path,  "data_cache/");
    return generic_path;
}

char* get_plugin_path()
{
    return ams_get_product_plugin_dir(generic_path);
}

char * get_local_config_path(char * cfgname, int len, char * config_name)
{
	snprintf(cfgname, len, "%sproduct/%s",get_root_path(), config_name);
	return cfgname;
}
