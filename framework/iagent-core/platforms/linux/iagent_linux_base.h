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


#ifndef APPS_IAGENT_CORE_IAGENT_LINUX_BASE_H__
#define APPS_IAGENT_CORE_IAGENT_LINUX_BASE_H__


#ifdef __cplusplus
extern "C"
{
#endif

//external
#include "er-coap.h"
#include "connection.h"
#include "ilink_message.h"

#define IBROKER_SERVER_PORT 5000
#define IBROKER_SERVER_PORT_STR "5000"
#define I_COAP_SERVER_PORT 5683
#define I_COAP_SERVER_PORT_STR "5683"

#define SERVER_IP "127.0.0.1"

typedef struct coap_endpoint_call
{
    uint16_t origin_coap_id;
    uint32_t origin_ilink_id;
    uint8_t has_ilink_mid;
    uint8_t token_len;
    uint8_t token[8];
} coap_endpoint_call_t;


typedef struct module_to_endpoint_call
{
	uint32_t  origin_bus_msg_id;
	char      source_module_id[100];
} module_to_endpoint_call_t;


extern int g_socket_ibroker;
extern int g_socket_coap_ep;



// coap_endpoint
extern coap_context_t * g_endpoint_coap_ctx;

int send_ilink_request_to_ep(char *epname, ilink_message_t *ilink_msg, coap_packet_t * coap_message);
int send_bus_request_to_ep(char *epname, coap_packet_t * coap_message, uint32_t bus_msg_id, char * src_module);
void send_bus_request_to_ibroker(void *message,  char *src_module, char *tm);

// coap_server
int forward_coap_request_to_bus(coap_context_t * coap_ctx, coap_packet_t * coap_message);
void publish_cloud_request_to_gw_bus(ilink_message_t *ilink_msg, void * coap_parsed);


//ports_handler
int get_coap_ep_sock();
bool wakeup_ports_thread();

//ibroker_send
bool wakeup_main_thread(int sock);
void ilink_init();
void ilink_msg_send(ilink_message_t *msg);

extern void *thread_ilink_sender(void *arg);
extern void *thread_ilink_port_handler(void *arg);


#ifdef __cplusplus
}
#endif

#endif
