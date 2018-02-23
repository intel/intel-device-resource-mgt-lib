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
 * interface.c
 *
 *  Created on: Dec 27, 2016
 *      Author: tian, li
 */

#include "ams_sdk_interface.h"
#include "ams_sdk_internal.h"
#include "ams_path.h"
#include "coap_request.h"
extern coap_resource_handler_t ams_sdk_cfg_rd;
extern unsigned short port_num;
#define check_init()  {if(g_ams_context == NULL || g_ams_context->g_init != AMS_Ready) return RET_CODE_NOT_INIT;}

ams_ctx_t * g_ams_context = NULL;


int ams_get_status()
{
    if(g_ams_context == NULL)
        return AMS_Uninit;
    else
        return g_ams_context->g_init;
}

void * ams_init(char * product_name, cfg_change_callback cfg_change_cb, ams_client_status_callback status_cb)
{
	int err = 0;
    uip_ipaddr_t my_addr;
    uint16_t my_port = port_num;
	pthread_t ams_user_handler_tid = 0;
	int socket_coap_client = -1;

    socket_coap_client = create_random_udp_socket (&port_num);
    if(socket_coap_client == -1)
    {
        AMS_LOG("Failed to create coap socket\n");
        return NULL;
    }

	g_ams_context = (ams_ctx_t*)malloc(sizeof(ams_ctx_t));
	memset(g_ams_context, 0, sizeof(*g_ams_context));

	g_ams_context->g_init = AMS_Uninit;
	g_ams_context->p_callback = cfg_change_cb;
	g_ams_context->ams_client_status_cb = status_cb;

	strncpy(g_ams_context->g_software_product_name,product_name, sizeof(g_ams_context->g_software_product_name)-1);

    memcpy(my_addr.raw, "127.0.0.1", sizeof("127.0.0.1"));
    my_addr.addr_type = A_IP_Addr;
    AMS_LOG( "my port is %d\n",port_num);
    g_ams_context->g_coap_ctx = coap_context_new(&my_addr);
    if(g_ams_context->g_coap_ctx == NULL)
    {
    	AMS_LOG("Failed to allocate coap context\n");
    	close(socket_coap_client);
    	free(g_ams_context);
    	g_ams_context = NULL;
    	return NULL;
    }
    g_ams_context->g_coap_ctx->buf = (uint8_t  *) malloc(COAP_MAX_PACKET_SIZE);
    g_ams_context->g_coap_ctx->buf_size = COAP_MAX_PACKET_SIZE;


	pthread_cond_init (&callback_condition_cond, NULL); // = PTHREAD_COND_INITIALIZER;
	pthread_mutex_init(&callback_condition_mutex, NULL); //PTHREAD_MUTEX_INITIALIZER;



	g_ams_context->g_coap_ctx->tx_data = send_data;
	g_ams_context->g_coap_ctx->rx_data = receive_data;
	g_ams_context->g_coap_ctx->socket = socket_coap_client;

    add_resource_handler(g_ams_context->g_coap_ctx, &ams_sdk_cfg_rd);

    if (pthread_create(&ams_user_handler_tid, NULL, thread_user_interface, g_ams_context))
    {
        AMS_LOG( "can't create thread_ilink_sender :[%s]\n", strerror(err));
        err = -1;
        g_ams_context->g_init = AMS_Init_Fail;
    }
    else
    {
        AMS_LOG( "thread_user_interface created successfully\n");
        err = 0;
        g_ams_context->g_init = AMS_Not_Ready;
    }
    return g_ams_context;
}

int ams_set_product_id(char* product_device_id)
{
	int ret=0;
    coap_packet_t request[1];

    check_init();

    memset(request, 0, sizeof(coap_packet_t));

    cJSON * pJsonRoot = NULL;
    pJsonRoot = cJSON_CreateObject();
    if(NULL == pJsonRoot)
    {
    	return -1;
    }
    cJSON_AddStringToObject(pJsonRoot, "product", g_ams_context->g_software_product_name);
    cJSON_AddStringToObject(pJsonRoot, "id", product_device_id);
    char *payload = cJSON_Print(pJsonRoot);

    coap_init_message(request, COAP_TYPE_CON, COAP_POST, coap_get_mid());
    //1. SET URI
    coap_set_header_uri_path(request, URI_SET_PRODUCT_ID);
    coap_set_payload(request, payload, strlen(payload));

    ret = send_ams_coap_request(request);
    cJSON_Delete(pJsonRoot);
    free(payload);
    return ret;
}




int ams_add(char* target_type, char* target_id,bool overwrite_target_id)
{
	int ret;
	check_init();
	ret = ams_configure_checkpoint(T_ADD,target_type,target_id,true);
	return ret;
}


int ams_delete(char* target_type, char* target_id)
{
	int ret=0;

	check_init();
	ret = ams_configure_checkpoint(T_DELETE,target_type,target_id,false);
	return ret;
}


int ams_imediate_cfg_check()
{
	check_init();
	return 0;
}

int ams_register_config_status(void *callback)
{
	int ret=0;
	check_init();

	ret = ams_configure_watcher(T_ADD,"127.0.0.1",port_num);

	g_ams_context->p_callback = callback;

	AMS_LOG("register config status returned %d, port_num=%d\n", ret, port_num);
	return ret;
}


int ams_deregister_config_status()
{
	int ret=0;
	check_init();
	ret = ams_configure_watcher(T_DELETE,"127.0.0.1",port_num);
    g_ams_context->p_callback = NULL;

    return ret;
}
