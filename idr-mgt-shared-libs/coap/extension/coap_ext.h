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
 * coap_ext.h
 *
 *  Created on: Jan 18, 2017
 *      Author: xin.wang@intel.com
 */

#ifndef COAP_EXTENSION_COAP_EXT_H_
#define COAP_EXTENSION_COAP_EXT_H_

#include "er-coap.h"
#include "coap_request.h"

#ifdef __cplusplus
extern "C" {
#endif



#define COAP_DEBUG 1
extern uint8_t coap_debug_flags;
#if COAP_DEBUG
#include <stdio.h>
#define PRINTF(...) {if((coap_debug_flags&0x80000000)  != 0) printf("[coap] " __VA_ARGS__);}
#define COAP_WARN(...) {if((coap_debug_flags&0x40000000) == 0) printf("[!coap] " __VA_ARGS__);}

#define PRINT6ADDR(addr) PRINTF("[%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x]", ((uint8_t *)addr)[0], ((uint8_t *)addr)[1], ((uint8_t *)addr)[2], ((uint8_t *)addr)[3], ((uint8_t *)addr)[4], ((uint8_t *)addr)[5], ((uint8_t *)addr)[6], ((uint8_t *)addr)[7], ((uint8_t *)addr)[8], ((uint8_t *)addr)[9], ((uint8_t *)addr)[10], ((uint8_t *)addr)[11], ((uint8_t *)addr)[12], ((uint8_t *)addr)[13], ((uint8_t *)addr)[14], ((uint8_t *)addr)[15])
#define PRINTLLADDR(lladdr) PRINTF("[%02x:%02x:%02x:%02x:%02x:%02x]", (lladdr)->addr[0], (lladdr)->addr[1], (lladdr)->addr[2], (lladdr)->addr[3], (lladdr)->addr[4], (lladdr)->addr[5])
#else
#define PRINTF(...)
#define COAP_WARN(...)
#define PRINT6ADDR(addr)
#define PRINTLLADDR(addr)
#endif



//
// blocking_coap_request.c
//
typedef struct coap_request_user_data
{
    int len;
    int code;
    char * payload;
    enum{
        Fail = 0,
        Success = 1,
        Timeout = 2
    }result;
}coap_request_user_data_t;

void free_coap_request_user_data(coap_request_user_data_t * data);
coap_request_user_data_t * make_blocking_request(
        coap_context_t * coap_ctx,
        coap_packet_t *request, uip_ipaddr_t * server_addr);
int make_blocking_request_to_file(
        coap_context_t * coap_ctx,
        coap_packet_t *request,
        char * filename,
        uip_ipaddr_t * server_addr);
int coap_blocking_request(
                        coap_context_t *coap_ctx,
                        uip_ipaddr_t *dst_addr,
                        coap_packet_t *request,
                        restful_response_handler request_callback,
                        void * user_data);

coap_status_t coap_parse_message_tcp(void *packet, uint8_t *data, uint16_t data_len);
int coap_blocking_request_tcp(coap_context_t *coap_ctx,coap_packet_t *request,
		restful_response_handler request_callback,void * user_data);

size_t coap_serialize_message_tcp(void *packet, uint8_t ** buffer_out);
int coap_set_payload_tcp(void *packet, const void *payload, size_t length);
uint8_t coap_is_request(coap_packet_t * coap_message);

uint16_t coap_find_mid(uint8_t *buffer);
uint8_t coap_find_code(uint8_t *buffer);
void coap_change_mid(uint8_t *buffer, uint16_t id);

int add_resource_handler(coap_context_t * coap_ctx, coap_resource_handler_t * handler);
uint32_t check_blockwise_timeout_ms(coap_context_t * coap_ctx,  int timeout_sec);
int set_res_blockwise(coap_context_t * coap_ctx,  char * url_allocated, void * buffer,
        int buffer_size, uint32_t block_num, uint16_t content_fmt);
int send_coap_msg(coap_context_t * coap_ctx, coap_packet_t * message);

char * coap_get_full_url_alloc(coap_packet_t * request);


#ifdef __cplusplus
}
#endif
#endif /* COAP_EXTENSION_COAP_EXT_H_ */
