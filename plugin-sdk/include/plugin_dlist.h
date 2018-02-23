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
 * plugin_dlist.h
 *
 *  Created on: Apr 21, 2017
 *      Author: xin
 */

#ifndef APPS_GW_BROKER_PLUGIN_SDK_INCLUDE_PLUGIN_DLIST_H_
#define APPS_GW_BROKER_PLUGIN_SDK_INCLUDE_PLUGIN_DLIST_H_



#include "azure_c_shared_utility/doublylinkedlist.h"
#include "azure_c_shared_utility/threadapi.h"
#include "azure_c_shared_utility/lock.h"


#ifdef __cplusplus
extern "C" {
#endif


typedef struct dlist_entry_ctx
{
	DLIST_ENTRY list_queue;
	LOCK_HANDLE thread_mutex;

} dlist_entry_ctx_t;

typedef enum
{
	T_No_Message,
	T_Message_Handler,
	T_Bus_Message,
	T_Callback,	// callback_t


	T_User_Message = 1000
} E_Msg_Type;

typedef struct
{
	DLIST_ENTRY entry;
	E_Msg_Type type;
	void * message;
	void * message_handler;
}dlist_node_t;

void dlist_post(dlist_entry_ctx_t *,E_Msg_Type,  void * messageHandle, void * handler);
dlist_node_t * dlist_get(dlist_entry_ctx_t * link);

#ifdef __cplusplus
}
#endif


#endif /* APPS_GW_BROKER_PLUGIN_SDK_INCLUDE_PLUGIN_DLIST_H_ */
