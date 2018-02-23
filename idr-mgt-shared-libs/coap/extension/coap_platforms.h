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
 * coap_platforms.h
 *
 *  Created on: Jul 3, 2016
 *      Author: xin.wang@intel.com
 */

#ifndef COAP_PLATFORMS_H_
#define COAP_PLATFORMS_H_
#ifdef __cplusplus
extern "C" {
#endif
#include <stdint.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include "list_coap.h"
#include <stdbool.h>

#define LINUX
#include <sys/select.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <time.h>
#include<sys/time.h>

#if defined(CLOCK_MONOTONIC)
#define CLOCK_GETTIME(t) clock_gettime(CLOCK_MONOTONIC, t);
#pragma message("CLOCK_MONOTONIC enabled, that is expected!")
#else
#define CLOCK_GETTIME(t) clock_gettime(CLOCK_REALTIME, t);
#pragma message("CLOCK_MONOTONIC disabled, that is unexpected!")
#endif



#define COAP_TRANS_LOCK(ctx) coap_lock(ctx->transaction_lock)
#define COAP_TRANS_UNLOCK(ctx )  coap_unlock(ctx->transaction_lock)

/* REST_MAX_CHUNK_SIZE is the max size of payload.
 * The maximum buffer size that is provided for resource responses and must be respected due to the limited IP buffer.
 * Larger data must be handled by the resource and will be sent chunk-wise through a TCP stream or CoAP blocks.
 */
#ifndef REST_MAX_CHUNK_SIZE
#define REST_MAX_CHUNK_SIZE     1024
#endif


#ifndef MIN
#define MIN(a, b) ((a) < (b) ? (a) : (b))
#endif /* MIN */

#define CLOCK_SECOND 1000
#define NET_ADDR_RAW_SIZE 32
typedef enum
{
	A_Raw,
	A_Sock_Addr,
	A_IP_Addr,
	A_Custom
} Net_Addr_Type;

typedef struct net_addr {
	Net_Addr_Type addr_type;
	union {
		char     raw[NET_ADDR_RAW_SIZE];
		struct   sockaddr_in sock_addr;
	};
	uint16_t port;
	uint16_t addr_len;
}net_addr_t;

#define uip_ipaddr_t net_addr_t

#define memb_free(x, y)  free(x)

void set_addr_ip(uip_ipaddr_t *, char * ip, int port);
uip_ipaddr_t * new_net_addr(Net_Addr_Type type);
void copy_net_addr(uip_ipaddr_t * dest, uip_ipaddr_t * src);
bool compare_net_addr(uip_ipaddr_t * dest, uip_ipaddr_t * src);

uint32_t get_elpased_ms(uint32_t * last_system_clock);
uint32_t get_platform_time();
uint32_t get_platform_time_sec();

void coap_sleep_ms(uint32_t ms);
void coap_lock(void *);
void coap_unlock(void *);
void * coap_create_lock();
void  coap_free_lock(void *);

void *xalloc(uint32_t size);

#define os_malloc   malloc/*xalloc*/
#define os_free     free/*bfree*/

#ifdef __cplusplus
}
#endif
#endif /* COAP_PLATFORMS_H_ */
