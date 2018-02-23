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


#ifndef LWM2M_SERVER_H
#define LWM2M_SERVER_H



#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#include <ctype.h>
#include <sys/select.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <sys/stat.h>
#include <errno.h>
#include <signal.h>
#include <inttypes.h>
#include <assert.h>

#include "logs.h"
#include "plugin_constants.h"
#include "plugin_sdk.h"
#include "plugin_dlist.h"
#include "liblwm2m.h"
#include "internals.h"

#include "commandline.h"
#include "connection.h"

#include "agent_core_lib.h"

#include "module.h"
#include "module_common.h"

#include "obs_manager.h"
#include "module_constants.h"
#ifdef __cplusplus
extern "C"
{
#endif

// definition
#define CONSOLE_LOG print_remote

#define FLAG_LWM2M_GWBUS      0x00000001
#define FLAG_LWM2M_LOG		0x00004000
#define FLAG_LWM2M_DATA		0x10000000
#define LWM2M_LOG_DATA(title, buffer, len) if((log_tag_mask[Log4_LWM2M] & FLAG_LWM2M_DATA) && (LOG_INFO   >= log_level)) log_buffer("[LWM2M] " title,buffer, len)

//#define LOG_TAG "LWM2M"

// functions
void print_remote(const char* fmt, ...);
int thread_lwm2m(void * parameter);

void set_client_name(int client_id, char * name);
const char * get_client_name(int client_id);
void del_client_name(int client_id);

MODULE_EXPORT const MODULE_API* MODULE_STATIC_GETAPI(HELLOWORLD_MODULE)(MODULE_API_VERSION gateway_api_version);

#ifdef __cplusplus
}
#endif

#endif /*HELLO_WORLD_H*/
