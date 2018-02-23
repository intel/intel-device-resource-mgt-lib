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


#ifndef APPS_IAGENT_CORE_IAGENT_BASE_H_
#define APPS_IAGENT_CORE_IAGENT_BASE_H_



#include "iagent_config.h"

#include "iniparser.h"


#include "agent_core_lib.h"

#include "platforms/linux/iagent_linux_base.h"
#include "ilink_message.h"

#include "plugin_dlist.h"

#include "logs.h"
#include "path.h"
#include "misc.h"
#include "parson_ext.h"
#include "string_parser.h"
#include "plugin_constants.h"
#include "module_constants.h"



#ifdef __cplusplus
extern "C" {
#endif


#define URI_IBROKER_DP "/ibroker/dp"


#ifndef LOG_TAG
#define LOG_TAG "iAgent"
#endif


#define FLAG_DEVICE_REG     0x00000001
#define FLAG_INIT           0x00000002
#define FLAG_CLOUD_CONNECT  0x00000004
#define FLAG_DATA_POINT     0x00000008

#define FLAG_CLOUD_MSG      0x00000010
#define FLAG_DUMP_MESSAGE   0x00000020
#define FLAG_CLOUD_CONFIG   0x00000040
#define FLAG_BUS_MESSAGE    0x00000080

#define FLAG_PING_MESSAGE   0x00000100

#define FLAG_RAW_MESSAGE    0x40000000


typedef struct
{
    struct sockaddr_storage  addr;
    socklen_t addrLen;
    uint32_t  origin_id;
    uint8_t   origin_token[8];
    uint8_t   origin_token_len;
    char * url;
} coap_request_ctx_t;


int init_agent();


extern cloud_status_e g_cloud_status;

struct req
{

};

typedef struct req request_t;
typedef struct req response_t;


int iLinkRequest(request_t* request, response_t** response, int timeout);
int iLinkRequestRaw(request_t* request, response_t ** response, int timeout);
int iLinkRequestCB(request_t* request, /*bh_async_callback*/void* cb, int timeout);

int iRegisterRes (char resource, /*bh_async_callback*/void* cb, bool workerThread);
int iRegisterSvcModule (char* module, char* resource);
int iLookupRes (char* resource);

int iRegisterEndpoint (char* ep, char* resource, char* IP, int port);
int iLookupEndpoint(char* ep, char* resource, char* IP, int *port);

bool handshake_done();

const char * ilink_status_text(int status);


char * my_ini_filename();
void set_cloud_connection_status(cloud_status_e status);
cloud_status_e get_cloud_connection_status();


#ifdef __cplusplus
}
#endif


#endif /* APPS_IAGENT_CORE_IAGENT_BASE_H_ */
