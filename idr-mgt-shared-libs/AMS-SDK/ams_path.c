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
 * path.c
 *
 *  Created on: Dec 29, 2016
 *      Author: xwang98
 *
 *
 *  Path:
 *  /ams_client
 *          /product.ams
 *      	/product/
 *      	/config/
 *
 *  /iagent
 *          /product.ams
 *      	/product/
 *      			/triggers/
 *      			/manifest
 *      			/
 *      	/config/
 *      	       /target_type/
 *      	            /target_type/target_id
 *      	       /modbus_meta/
 *      	/log
 *      	/plugin
 *      			/modbus
 *      					/product
 *      						/triggers
 *      						/manifest
 *      					/config
 *	/imrt
 *			/product.ams
 *			/product/
 *	/some_app
 *			/product.ams
 *			/product/
 *
 */

#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <stdio.h>
#include "ams_constants.h"
#include "ams_path.h"


static char path_buffer[PATH_LEN] = {0};
static char g_product_root[PATH_LEN] = {0};

// return true if this function malloc a new buffer
bool make_string(void * data1, int data_len, char ** str_out)
{
	bool ret = false;
	char * data = (char *)data1;
	if(data[data_len-1] == 0)
		*str_out = data;
	else
	{
		*str_out = malloc(data_len+1);
		memcpy(*str_out, data, data_len);
		(*str_out)[data_len] = 0;
		ret = true;
	}

	return ret;
}


/*
 * Under the root dir of each product, there is a file "product.ams"
 * That is used to identify the root dir.
 */

#define PRODUCT_FILE "product.ams"

// set the root dir for the current product
// the return value indicate the presence of PRODUCT_FILE under the root dir
// note: the user software can either call AMS restful api to the its product root dir,
//       or call ams_locate_product_path() to locate the product root dir.
bool ams_set_product_root(char * root_dir)
{
	char path[PATH_LEN] ;
	strcpy(g_product_root, root_dir);
	int len = strlen(g_product_root);
	if(len == 0 || g_product_root[len-1] != '/')
		g_product_root[len] = '/';

	strcpy(path,g_product_root );
	strcpy(path, PRODUCT_FILE);

	return (access(path, 0) == 0);
}


bool ams_locate_product_path(char * execpath, char * bin_path)
{
	char path[PATH_LEN] ;
	strcpy(path, execpath);


	// the ams client default dir is current binary folder
	strcpy(g_product_root, execpath);
	char * p = strrchr(g_product_root, '/'); // remove *.so
	if(p)
	{
		*(p+1) = 0;
	    if(bin_path) strcpy(bin_path, g_product_root);

	    char * p2 = strrchr(g_product_root, '/');
	    if(p2) 
	    {
	    	*(p2+1) = 0;
	    }
    }

	p = strrchr(path, '/');
	while(p)
	{
		strcpy(p+1, PRODUCT_FILE);
		if(access(path, 0) == 0)
		{
			*(p) = 0;
			strcpy(g_product_root, path);
			strcat(g_product_root, "/");
			return true;
		}

		// back to upper dir
		*(p) = 0;
		p = strrchr(path, '/');
	}

	return false;
}



char * ams_get_temp_root(char * dir)
{
	if(dir == NULL) dir = &path_buffer[0];
	strcpy(dir, g_product_root);
	char *p = strrchr(dir, '/');
	if(p)
		strcat(p+1, "temp/");
	else
		strcat(dir, "temp/");

	return dir;
}

char * ams_make_temp_dir(char * dir, char * subdir)
{
	char path[PATH_LEN];
	char * p = ams_get_temp_root(dir);
	if(subdir)
		strcat(p, subdir);
	else
		strcat(p, "dir");

	strcpy(path, p);
	int i = 1;
	while (0 == access(path, 0) && i < 10000)
	{
		//file exists;
		sprintf(path, "%s_%d", p, i);
		i++;
	}

	if(i == 10000)
		return NULL;

	if(dir == NULL) dir = &path_buffer[0];
	strcpy(dir, path);
	strcat(dir, "/");
	return dir;
}

char * ams_get_temp_pathname(char * pathname, char * prefix)
{
	char path[PATH_LEN];
	char * p = ams_get_temp_root(pathname);
	if(prefix)
		strcat(p, prefix);
	else
		strcat(p, "file");

	strcpy(path, p);
	int i = 1;
	while (0 == access(path, 0) && i < 10000)
	{
		//file exists;
		sprintf(path, "%s_%d", p, i);
		i++;
	}

	if(i == 10000)
		return NULL;

	if(pathname == NULL) pathname = &path_buffer[0];
	strcpy(pathname, path);

	return pathname;
}

char * ams_get_product_root(char * path)
{
	if(path == NULL) path = &path_buffer[0];
	strcpy(path, g_product_root);
	return path;
}


char * ams_get_product_config_dir( char * path)
{
	if(path == NULL) path = &path_buffer[0];
	ams_get_product_root(path);
	strcat(path, "config/");
	return path;
}

// the file path for a configuration file:
// [root config dir]/[:target_type]/[:target_id]/[:config_path_name]
//
// note: when target type is TT_DEVICE, there is no section of "target id".
//       this is because some module need extra efforts to get the iagent id. let's make it easy.
//
//       If the target type start with prefix "ONE_", there is no subfolders for the target id.
//       It is specialized design for the type with one instance on the gateway.
char * ams_get_product_config_pathname( char * config_path_name, char * target_type, char * target_id, char * out_path)
{
	if(out_path == NULL) out_path = &path_buffer[0];
	ams_get_product_config_dir(out_path);
	strcat(out_path, target_type);
	strcat(out_path, "/");
	if(strcmp(TT_DEVICE, target_type) != 0 && target_id && *target_id != 0
	        && (strncmp("ONE-",target_type, 4) != 0))
	{
		strcat(out_path, target_id);
		if(*config_path_name != '/'){
			strcat(out_path, "/");
		}
	}

	strcat(out_path, config_path_name);
	return out_path;
}

char * ams_get_product_plugin_dir(char * path)
{
	if(path == NULL) path = &path_buffer[0];
	ams_get_product_root(path);
	strcat(path, "plugins/");
	return path;
}

char * ams_get_product_version_dir(char * path)
{
	if(path == NULL) path = &path_buffer[0];
	ams_get_product_root(path);
	strcat(path, "version/");
	return path;
}

char * ams_get_product_scripts_dir(char * path)
{
	if(path == NULL) path = &path_buffer[0];
	ams_get_product_root(path);
	strcat(path, "ams/triggers/");
	return path;
}


char* ams_get_log_path()
{
	char *path_name = ams_get_product_root(NULL);
	strcat(path_name,"logs/");
	return path_name;

}

