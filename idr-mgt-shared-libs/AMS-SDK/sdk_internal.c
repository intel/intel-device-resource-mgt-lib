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
 * sdk_internal.c
 *
 *  Created on: Jan 25, 2017
 *      Author: tianli
 */

#include <linux/prctl.h>
#include <sys/time.h>
#include <sys/prctl.h>
#include "ams_sdk_interface.h"
#include "ams_sdk_internal.h"
#include "coap_request.h"

pthread_cond_t  callback_condition_cond;
pthread_mutex_t callback_condition_mutex;
unsigned short port_num = 0;
bool debug_ams = false;

uint8_t ams_response_handler(void* user_data,void *response)
{
    int g_coap_result = -1;

    if(user_data == NULL)
        return -1;

    ams_ctx_t * ctx = (ams_ctx_t*)user_data;

	if (response)
	{
		coap_packet_t *resp = (coap_packet_t*)response;
		if(resp->code == CHANGED_2_04 || resp->code == CONTENT_2_05)
		{
			g_coap_result = RET_CODE_OK;
		}
		else
		{
			g_coap_result = RET_CODE_AMS_FAIL;
			AMS_LOG("AMS response_handler: response error: %d\n", resp->code);

		}

        if(ctx->g_init != AMS_Ready)
        {
            ctx->g_init = AMS_Ready;

            AMS_LOG("AMS response_handler: first time connected to AMS\n");

            //always register the config change water when connected to the ams client
            if(g_ams_context->p_callback)
                ams_configure_watcher(T_ADD,"127.0.0.1",port_num);

            if(ctx->ams_client_status_cb)
                ctx->ams_client_status_cb(ctx->g_init);
        }
	}
	else
	{
	    if(ctx->g_init != AMS_Not_Ready)
	    {
	        ctx->g_init = AMS_Not_Ready;
	        if(ctx->ams_client_status_cb)
	            ctx->ams_client_status_cb(ctx->g_init);
	    }

	    AMS_LOG("AMS response_handler: timeout\n");

	    g_coap_result = RET_CODE_AMS_FAIL;
	}
	return 0;
}

int create_random_udp_socket(unsigned short * port)
{

	int s32_ret = 0;
	int cc = 0;
	struct   sockaddr_in sockaddr_bind_temp;
	int   sock_random;
	sock_random = socket(AF_INET, SOCK_DGRAM | SOCK_CLOEXEC, IPPROTO_UDP);
	if (sock_random == 0)
	{
		//        LOG_printf(&trace, "socket open failed");
		return -1;
	}

	cc = 1;
	if (setsockopt(sock_random, SOL_SOCKET, SO_BROADCAST, (const char *)(&cc), sizeof(int)) < 0)
	{
		cc = -1;
		return -5;
	}
	// Set socket option to allow us to broadcast
	cc = 1;
	if (setsockopt(sock_random, SOL_SOCKET, SO_REUSEADDR, (const char *)(&cc), sizeof(int)) < 0)
	{
		cc = -1;
		return -5;
	}

	memset(&sockaddr_bind_temp, 0, sizeof(struct sockaddr_in));
	sockaddr_bind_temp.sin_family = AF_INET;

	sockaddr_bind_temp.sin_addr.s_addr = INADDR_ANY;
	sockaddr_bind_temp.sin_port=0;//= htons(UDP_LOCAL_PORT);
	s32_ret = bind(sock_random, (struct sockaddr *)&sockaddr_bind_temp, sizeof(sockaddr_bind_temp));
	if (s32_ret  < 0)
	{
		return -3;
	}
	struct sockaddr_in ss;
	int len = sizeof(ss);
	getsockname(sock_random,(struct sockaddr*)&ss,&len);
	*port = ntohs(ss.sin_port);

	return sock_random;
}


int send_data(void * ctx, const uip_ipaddr_t *dst_addr, void *buf, int len)
{
	coap_context_t * coap_ctx = (coap_context_t *) ctx;

	// this is ensure the we used IP addr for calling coap_nonblocking_request
	if (dst_addr->addr_type == A_IP_Addr)
	{

        struct   sockaddr_in sockaddr_send;
        sockaddr_send.sin_family = AF_INET;
        sockaddr_send.sin_addr.s_addr = inet_addr(dst_addr->raw);
        sockaddr_send.sin_port = htons(dst_addr->port);
        return sendto(coap_ctx->socket, buf, len, 0, (struct sockaddr *)&sockaddr_send,sizeof(struct sockaddr_in));
	}
	else
	{
	    assert(dst_addr->addr_type == A_Sock_Addr);
	    return sendto (coap_ctx->socket, buf, len, 0, (struct sockaddr *)&dst_addr->sock_addr,dst_addr->addr_len);
	}
}



int receive_data(void * ctx, void *buf, int len, int timeout_ms)
{
	coap_context_t * coap_ctx = (coap_context_t *) ctx;
    int numBytes = 0;
    struct timeval tv;
    tv.tv_usec = (timeout_ms % 1000) * 1000;
    tv.tv_sec = timeout_ms / 1000;

    socklen_t addrLen;
    addrLen = sizeof(coap_ctx->src_addr.sock_addr);

    setsockopt(coap_ctx->socket, SOL_SOCKET, SO_RCVTIMEO, (char*) &tv,
            sizeof(struct timeval));

    numBytes = recvfrom(coap_ctx->socket, buf, len, 0,
            (struct sockaddr *) &coap_ctx->src_addr.sock_addr, &addrLen);

    coap_ctx->src_addr.addr_len = addrLen;
    coap_ctx->src_addr.addr_type = A_Sock_Addr;

    if (0 > numBytes)
    {
        if(errno != EINTR && EAGAIN != errno) AMS_LOG("[%p] Error in receive_data(): %d\r\n", coap_ctx, errno);
        return RX_TIMEOUT;
    }
    else if (0 < numBytes)
    {
        return numBytes;
    }

    return RX_TIMEOUT;
}





int send_ams_coap_request(coap_packet_t *request)
{
    struct timeval now;
    struct timespec timeToWait;
    int ret = 0;
    uip_ipaddr_t addr;


    set_addr_ip(&addr, "127.0.0.1", AMS_CLIENT_COAP_SERVER_PORT);
    coap_nonblocking_request(g_ams_context->g_coap_ctx, &addr, request, ams_response_handler,(void*)g_ams_context);

    return 0;
}



int ams_configure_watcher(int operation,char* ip,unsigned short port)
{
	int ret=0;
    coap_packet_t request[1];
    memset(request, 0, sizeof(coap_packet_t));

    cJSON * pJsonRoot = NULL;
    pJsonRoot = cJSON_CreateObject();
    if(NULL == pJsonRoot)
    {
    	return -1;
    }
    if(operation == T_ADD){
    	cJSON_AddStringToObject(pJsonRoot, "action", "add");
    }
    else{
    	cJSON_AddStringToObject(pJsonRoot, "action", "delete");
    }
    cJSON_AddStringToObject(pJsonRoot, "product", g_ams_context->g_software_product_name);
    if(ip != NULL){
    	cJSON_AddStringToObject(pJsonRoot, "ip", ip);
    }
    if(port != 0){
    	cJSON_AddNumberToObject(pJsonRoot, "port",  port);
    }
    char *payload = cJSON_Print(pJsonRoot);

    coap_init_message(request, COAP_TYPE_CON, COAP_POST, coap_get_mid());
    //1. SET URI
    coap_set_header_uri_path(request, URI_CONFIG_WATCHER);

    coap_set_token(request, (const uint8_t *)&request->mid, sizeof(request->mid));

    coap_set_payload(request, payload, strlen(payload));

    ret = send_ams_coap_request(request);
    cJSON_Delete(pJsonRoot);
    free(payload);
    return ret;
}



int ams_configure_checkpoint(int operation,char* target_type,char* target_id,bool overwrite_target_id)
{
	int ret=0;
    coap_packet_t request[1];
    memset(request, 0, sizeof(coap_packet_t));

    cJSON * pJsonRoot = NULL;
    pJsonRoot = cJSON_CreateObject();
    if(NULL == pJsonRoot)
    {
    	return -1;
    }
    if(operation == T_ADD){
    	cJSON_AddStringToObject(pJsonRoot, "action", "add");
    }
    else{
    	cJSON_AddStringToObject(pJsonRoot, "action", "delete");
    }
    cJSON_AddStringToObject(pJsonRoot, "product", g_ams_context->g_software_product_name);
    cJSON_AddStringToObject(pJsonRoot, "target_type", target_type);
    cJSON_AddStringToObject(pJsonRoot, "target_id",  target_id);
    if(true == overwrite_target_id){
    	cJSON_AddStringToObject(pJsonRoot, "overwrite",  "true");
    }
    else{
    	cJSON_AddStringToObject(pJsonRoot, "overwrite",  "false");
    }
    char *payload = cJSON_Print(pJsonRoot);

    coap_init_message(request, COAP_TYPE_CON, COAP_POST, coap_get_mid());

    //1. SET URI
    coap_set_header_uri_path(request, URI_CONFIG_CHECKPOINT);
    coap_set_payload(request, payload, strlen(payload));

    ret = send_ams_coap_request(request);
    cJSON_Delete(pJsonRoot);

    free(payload);

    return ret;
}

void ping_ams()
{
    int ret=0;
    coap_packet_t request[1];
    memset(request, 0, sizeof(coap_packet_t));
    coap_init_message(request, COAP_TYPE_CON, COAP_GET, coap_get_mid());
    coap_set_header_uri_path(request, "ams/version");
    send_ams_coap_request(request);
}


void *thread_user_interface(void * param)
{
    char threadname[100];
    sprintf(threadname, "ams-%s-%d", g_ams_context->g_software_product_name, port_num);
    prctl (PR_SET_NAME, threadname);

	time_t last_register = 0;
	ams_ctx_t * ams_ctx = (ams_ctx_t*)param;

	coap_context_t * ctx = (coap_context_t *)ams_ctx->g_coap_ctx;
	while(1)
	{
        uint32_t next_timeout = coap_check_transactions(ctx);

        if (ams_ctx->g_init == AMS_Not_Ready && (time(NULL) - last_register) > 15)
        {
            // ping the ams client
            ping_ams();

            if (next_timeout > 15000)
            {
                next_timeout = 15000;
            }
            last_register = time(NULL);
        }

        int buf_len = ctx->rx_data(ctx, ctx->buf, ctx->buf_size, next_timeout);
        if(RX_TIMEOUT == buf_len)
                continue;

        ctx->buf_len = buf_len;

        coap_handle_packet(ctx);
	}
}

