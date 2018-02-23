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
 * coap_platforms.c
 *
 *  Created on: Jul 3, 2016
 *      Author: xin.wang@intel.com
 */

#include "coap_platforms.h"

uint8_t coap_debug_flags = 0xFFFF;


void copy_net_addr(uip_ipaddr_t * dest, uip_ipaddr_t * src)
{
	memcpy(dest, src, sizeof(uip_ipaddr_t));
}

bool compare_net_addr(uip_ipaddr_t * dest, uip_ipaddr_t * src)
{

	return memcmp(dest, src, sizeof(*src)) == 0;
}


uip_ipaddr_t * new_net_addr(Net_Addr_Type type)
{
	uip_ipaddr_t * addr = (uip_ipaddr_t*) malloc(sizeof(uip_ipaddr_t));
	memset(addr, 0, sizeof(uip_ipaddr_t));
	addr->addr_type = type;
	return addr;
}

void set_addr_ip(uip_ipaddr_t * addr, char * ip, int port)
{
	if(strlen(ip) >= NET_ADDR_RAW_SIZE)
		return;

	addr->addr_type = A_IP_Addr;
	addr->port = port;
	addr->addr_len = strlen(ip);
	strcpy(addr->raw, ip);
}


uint32_t get_platform_time_sec()
{
    struct timespec ts;
    CLOCK_GETTIME(&ts);
    return (ts.tv_sec);}


#ifndef LINUX
//#include <nanokernel.h>

uint32_t get_elpased_ms(uint32_t * last_system_clock)
{
	uint32_t elpased_ms;
	uint32_t now; //161102 = sys_tick_get_32();

	// system clock overrun
	if(now < *last_system_clock)
	{
	  elpased_ms = now + (0xFFFFFFFF - *last_system_clock) + 1;
	}
	else
	{
	  elpased_ms = now - *last_system_clock;
	}

	*last_system_clock = now;

	return elpased_ms;
}

#else

#include <sys/time.h>
#include <pthread.h>
uint32_t get_elpased_ms(uint32_t * last_system_clock)
{
	uint32_t elpased_ms;
    struct timespec ts;
    CLOCK_GETTIME(&ts);

    uint32_t now =  (ts.tv_sec * 1000 + ts.tv_nsec / 1000000);

	// system clock overrun
	if(now < *last_system_clock)
	{
	  elpased_ms = now + (0xFFFFFFFF - *last_system_clock) + 1;
	}
	else
	{
	  elpased_ms = now - *last_system_clock;
	}

	*last_system_clock = now;

	return elpased_ms;
}


//
// if ms is used as unit, a 32 bit long word can be as long as 49 days!!!
//
uint32_t get_platform_time()
{
    struct timespec ts;
    CLOCK_GETTIME(&ts);

    uint32_t now =  (ts.tv_sec * 1000 + ts.tv_nsec / 1000000);

	return(now);
}

void coap_sleep_ms(uint32_t ms)
{
	usleep(ms*1000);
}

void coap_lock(void * lock)
{
	pthread_mutex_t * mutext = (pthread_mutex_t* ) lock;
	pthread_mutex_lock(mutext);
}

void coap_unlock(void * lock)
{
	pthread_mutex_t * mutext = (pthread_mutex_t* ) lock;
	pthread_mutex_unlock(mutext);
}

void * coap_create_lock()
{
	pthread_mutex_t * mutext = (pthread_mutex_t *) malloc(sizeof(pthread_mutex_t));
	pthread_mutex_init(mutext, NULL);
	return (void *) mutext;
}

void coap_free_lock(void * lock)
{
	pthread_mutex_destroy((pthread_mutex_t *) lock);
	free(lock);
}


#endif

#if 0
static struct uip_udp_conn *udp_conn = NULL;

void
coap_init_connection(uint16_t port)
{
  /* new connection with remote host */
  udp_conn = udp_new(NULL, 0, NULL);
  udp_bind(udp_conn, port);
  PRINTF("Listening on port %u\n", uip_ntohs(udp_conn->lport));

  /* initialize transaction ID */
  current_mid = random_rand();
}

#endif
