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


#ifndef AMS_CLIENT_APPS_CLIENT_CLIENT_H_
#define AMS_CLIENT_APPS_CLIENT_CLIENT_H_

#include <stdlib.h>
#include <stdio.h>
#include <pthread.h>
#include <stdint.h>
#include <stdbool.h>
#include <string.h>
#include <stddef.h>

#include <math.h>


#include <time.h>
#include <unistd.h>

#include <ctype.h>
#include <sys/select.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <sys/stat.h>
#include <sys/statfs.h>
#include <errno.h>
#include <signal.h>
#include <time.h>
#include <dirent.h>

#include "ams_sdk_interface.h"

#undef  LOG_TAG
#define LOG_TAG "AMS"

#include "coap_request.h"
#include "cJSON.h"
#include "er-coap-constants.h"
#include "er-coap.h"
#include "rest-engine.h"
#include "er-coap-transactions.h"

#ifndef AMS_LOG
#define AMS_LOG(...) {fprintf(stderr, "[AMS]"  __VA_ARGS__);}
#endif

#define MAX_PACKET_SIZE 1024
#define URI_SET_PRODUCT_ID    "ams/product_id"
#define URI_CONFIG_CHECKPOINT "ams/config_checkpoint"
#define URI_CONFIG_WATCHER    "config_watcher"

#define DEFAULT_CONFIG_MONITOR_URI "ams/config"

/*
 * typedef int (*ams_api_callback)(void * user_data, int result);
 *
typedef struct
{
    void * user_data;
    ams_api_callback api_cb;
}ams_api_callback_t;
*/


typedef struct
{
    cfg_change_callback p_callback;
    ams_client_status_callback ams_client_status_cb;
    coap_context_t *g_coap_ctx ;
    char g_software_product_name[64];
    struct sockaddr g_server_addr;
    socklen_t g_server_sl;
    int g_init;
} ams_ctx_t;
extern ams_ctx_t * g_ams_context;

typedef enum {
	T_ADD,
	T_DELETE,
	T_Max
}checkpoint_type_t;

extern pthread_cond_t  callback_condition_cond;
extern pthread_mutex_t callback_condition_mutex;

int create_random_udp_socket(unsigned short * port);
int send_data(void *, const uip_ipaddr_t *dst_addr, void *buf, int len);
int receive_data(void *, void *buf, int len, int timeout_ms) ;
extern void *thread_user_interface(void * param);
extern int send_ams_coap_request(coap_packet_t *request);
int ams_configure_checkpoint(int operation,char* target_type,char* target_id,bool overwrite_target_id);
int ams_configure_watcher(int operation,char* ip,unsigned short port);

#endif
