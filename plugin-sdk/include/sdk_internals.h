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
 * sdk_internals.h
 *
 * DON'T INCLUDE THIS HEADER IN YOUR APPLICATION SOURCE FILES
 */


#ifndef APPS_GW_BROKER_PLUGIN_SDK_INCLUDE_SDK_INTERNALS_H_
#define APPS_GW_BROKER_PLUGIN_SDK_INCLUDE_SDK_INTERNALS_H_

#include <stdlib.h>
#include "plugin_dlist.h"
#include "agent_core_lib.h"
#include "azure_c_shared_utility/condition.h"
#include "plugin_sdk.h"



#define T_MESSAGE_REQUEST (T_User_Message +1)
#define T_MESSAGE_EVENT   (T_User_Message +2)


typedef struct _resource_handler_node
{
	struct _resource_handler_node * next;
	char * url;
	unsigned char match_pattern;
	Plugin_Res_Handler res_handlers[MAX_RESTFUL_ACTION];
}resource_handler_node_t;


typedef struct _framework_ctx
{
	resource_handler_node_t * resources_handlers;
	LOCK_HANDLE resources_handlers_lock;

	dlist_entry_ctx_t * internal_queue;
	sync_ctx_t* transactions_ctx;

	void * broker_handle;
	void * module_handle;

	char * module_name;

	COND_HANDLE g_working_thread_cond;
	LOCK_HANDLE g_working_thread_lock;

}framework_ctx_t;

framework_ctx_t * get_framework_ctx();



typedef struct IDRM_MOD_HANDLE_DATA_TAG
{
    THREAD_HANDLE threadHandle;
    LOCK_HANDLE lockHandle;
    int stopThread;
    BROKER_HANDLE broker;
    framework_ctx_t * framework;
}IDRM_MOD_HANDLE_DATA;


bool decode_request(MESSAGE_HANDLE messageHandle, restful_request_t * request);
bool decode_response(MESSAGE_HANDLE messageHandle, restful_response_t * response);
MESSAGE_HANDLE encode_response(restful_response_t * response);
MESSAGE_HANDLE encode_request(restful_request_t * request);

bool decode_event(MESSAGE_HANDLE messageHandle, bus_event_t * event);
MESSAGE_HANDLE encode_event(bus_event_t * event);


#endif /* APPS_GW_BROKER_PLUGIN_SDK_INCLUDE_SDK_INTERNALS_H_ */
