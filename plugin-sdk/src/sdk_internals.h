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
 */

#ifndef APPS_GW_BROKER_PLUGIN_SDK_INCLUDE_SDK_INTERNALS_H_
#define APPS_GW_BROKER_PLUGIN_SDK_INCLUDE_SDK_INTERNALS_H_

#include <stdlib.h>
#include "plugin_dlist.h"
#include "agent_core_lib.h"

/// DON'T INCLUDE THIS HEADER IN YOUR APPLICATION SOURCE FILES

#define T_MESSAGE_REQUEST (T_User_Message +1)
#define MAX_RESTFUL_ACTION 5

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

	void * working_thread_waker;

}framework_ctx_t;
extern framework_ctx_t * g_framework_ctx;

void wakeup_working_thread(void * framework);

bool decode_request(MESSAGE_HANDLE messageHandle, restful_request_t * request);
bool decode_response(MESSAGE_HANDLE messageHandle, restful_response_t * response);
MESSAGE_HANDLE encode_response(restful_response_t * response);
MESSAGE_HANDLE encode_request(restful_request_t * request);

#endif /* APPS_GW_BROKER_PLUGIN_SDK_INCLUDE_SDK_INTERNALS_H_ */
