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


#ifndef DP_CACHE_SERVER_H_
#define DP_CACHE_SERVER_H_

#include <time.h>
#include "plugin_dlist.h"

#include "logs.h"
#include "path.h"
#include "misc.h"
#include "parson_ext.h"
#include "string_parser.h"
#include "plugin_constants.h"
#include "module_constants.h"

#include "er-coap-constants.h"
#include "agent_core_lib.h"
#include "ams_constants.h"
#include "dictionary.h"
#include "iniparser.h"

#include "broker_rest_convert.h"


#ifndef LOG_TAG
#define LOG_TAG "DB"
#endif

#define FLAG_DP_CACHE_DB    0x00000080


int dp_cache_db_server_init( );
void operate_broad_msg(int payload_len, const char* payload);
int add_dp_cache_data(const char* uri, int fmt, time_t time, int payload_len, const char* payload );
int thread_dp_cache_db_server(void * param);

#endif
