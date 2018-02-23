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
 * test_linux.c
 *
 *  Created on: Jul 3, 2016
 *      Author: wangxin
 */



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
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <sys/stat.h>
#include <pthread.h>
#include "../er-coap/er-coap.h"
#include "coap_request.h"



#define MAX_PACKET_SIZE 1024
//#define PRINTF(...)
#define PRINTF(...) printf(__VA_ARGS__)
#define ERROR(...)



socklen_t g_server_sl;
struct sockaddr g_server_addr;

int g_sock;


int create_socket()
{
    int s = -1;
    struct addrinfo hints;
    struct addrinfo *res;
    struct addrinfo *p;

    memset(&hints, 0, sizeof hints);
    hints.ai_family = AF_INET6;
    hints.ai_socktype = SOCK_DGRAM;
    hints.ai_flags = AI_PASSIVE;

    if (0 != getaddrinfo(NULL, "123456", &hints, &res))
    {
        return -1;
    }

    for(p = res ; p != NULL && s == -1 ; p = p->ai_next)
    {
        s = socket(p->ai_family, p->ai_socktype, p->ai_protocol);
        if (s >= 0)
        {
            if (-1 == bind(s, p->ai_addr, p->ai_addrlen))
            {
                close(s);
                s = -1;
            }
        }
    }

    freeaddrinfo(res);

    return s;
}


int get_server_addr()
{
    struct addrinfo hints;
    struct addrinfo *servinfo = NULL;
    struct addrinfo *p;
    int s;
    struct sockaddr *sa;
    socklen_t sl;

    memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_DGRAM;

    if (0 != getaddrinfo("localhost", "5555", &hints, &servinfo) || servinfo == NULL)
    	return 0;

    // we test the various addresses
    s = -1;
    for(p = servinfo ; p != NULL && s == -1 ; p = p->ai_next)
    {
        s = socket(p->ai_family, p->ai_socktype, p->ai_protocol);
        if (s >= 0)
        {
            sa = p->ai_addr;
            sl = p->ai_addrlen;
            if (-1 == connect(s, p->ai_addr, p->ai_addrlen))
            {
                close(s);
                s = -1;
            }
        }
    }
    if (s >= 0)
    {
    	memcpy(&g_server_addr,sa, sl);
    	g_server_sl = sl;
    	close(s);
    }

    if (NULL != servinfo) {
        free(servinfo);
    }

    return s;
}

void tx(const uip_ipaddr_t *dst_addr, uint16_t port, const void *buf, int len)
{
    /* assert (g_sock != -1); */

    /*
    struct sockaddr_in dst_addr;
    dst_addr.sin_family = AF_INET;
    dst_addr.sin_addr.s_addr = inet_addr(g_addr);
    dst_addr.sin_port = htons(5683);
    */

    sendto(g_sock, buf, len, 0, (struct sockaddr *) &g_server_addr, g_server_sl/*sizeof(g_addr)*/);
}

uint8_t response_handler(void* user_data, void *response)
{
	if (response)
	{
		coap_packet_t *resp = (coap_packet_t*)response;
		PRINTF("response_handler, payload: %.*s\n", resp->payload_len, resp->payload);
	}
	return 0;
}

int rx(void *buf, int len, int timeout_ms)
{
	int numBytes = 0;
	struct timeval tv;
	tv.tv_usec = 0;
	tv.tv_sec = timeout_ms % 1000;

	struct sockaddr_storage addr;
	socklen_t addrLen;
	addrLen = sizeof(addr);

	/*
	 * We retrieve the data received
	 */
	setsockopt(g_sock, SOL_SOCKET, SO_RCVTIMEO, (char*) &tv, sizeof(struct timeval));
	numBytes = recvfrom(g_sock, buf, MAX_PACKET_SIZE, 0, (struct sockaddr *) &addr, &addrLen);
	if (0 > numBytes) {

		fprintf(stderr, "Error in recvfrom(): %d\r\n", errno);
		return RX_TIMEOUT;

	} else if (0 < numBytes) {

		char s[INET6_ADDRSTRLEN];
		in_port_t port;

		if (AF_INET == addr.ss_family) {
			struct sockaddr_in *saddr = (struct sockaddr_in *) &addr;
			inet_ntop(saddr->sin_family, &saddr->sin_addr, s, INET6_ADDRSTRLEN);
			port = saddr->sin_port;
		} else if (AF_INET6 == addr.ss_family) {
			struct sockaddr_in6 *saddr = (struct sockaddr_in6 *) &addr;
			inet_ntop(saddr->sin6_family, &saddr->sin6_addr, s,
					INET6_ADDRSTRLEN);
			port = saddr->sin6_port;
		}
		fprintf(stderr, "%d bytes received from [%s]:%hu\r\n", numBytes, s,
				ntohs(port));

		return numBytes;
	}

	return RX_TIMEOUT;
}

void *transaction_task(void *argument)
{
	coap_context_t * ctx = (coap_context_t *)argument;

	while(1)
	{
        uint32_t next_timeout = coap_check_transactions();

        ctx->buf_len = ctx->rx_data(ctx, ctx->buf, sizeof(ctx->buf), next_timeout);
        if(RX_TIMEOUT == ctx->buf_len)
        		continue;
        coap_handle_packet(ctx);
	}
	return NULL;
}


int main()
{
	uip_ipaddr_t my_addr, dst_addr;
	uint16_t my_port = 1000, dst_port=5699;
	unsigned int payload[1025];

	memcpy(my_addr.raw, "127.0.0.1", sizeof("127.0.0.1"));
//	memcpy(dst_addr.raw, "127.0.0.1", sizeof("127.0.0.1"));
	set_addr_ip(&dst_addr, "127.0.0.1", dst_port);


	coap_packet_t * message = malloc(sizeof(coap_packet_t));

	get_server_addr();

	g_sock = create_socket();
	if(g_sock == -1)
		return -1;

	coap_context_t * ctx = coap_context_new( &my_addr);

	ctx->tx_data = tx;
	ctx->rx_data = rx;

	coap_init_message(message, COAP_TYPE_CON, COAP_GET, coap_get_mid());
	coap_set_header_uri_path(message, "/test");
	payload[1024]=0;
	for(int i=0;i<1024;i++)
	{
		payload[i]=i;
		payload[1024]+=i;
	}
	coap_set_payload(message,(char *)payload, 1025*sizeof(unsigned int));

	/*
	//
	// Test 1: blocking request*/
	PRINTF("blocking_send request payload len  : %d\n",1025*sizeof(unsigned int) );
	//coap_blocking_request(ctx, &dst_addr, dst_port, message, response_handler, NULL);
	//coap_blocking_request(ctx, &dst_addr,  message, response_handler, NULL);

	//
	//
	// Test 2: non-blocking request
	pthread_t thread;
	pthread_create(&thread, NULL, transaction_task, ctx);
//	 coap_nonblocking_request(ctx, &dst_addr, dst_port,  message, response_handler, NULL);
	coap_nonblocking_request(ctx, &dst_addr,  message, response_handler, NULL);
	message = NULL;

	while(1)
	{
		sleep(1000);
	}

	return 0;
}
