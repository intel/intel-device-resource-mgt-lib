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

#include "plugin_dlist.h"
#include "agent_core_lib.h"


dlist_entry_ctx_t * create_dlist()
{
	dlist_entry_ctx_t * internal_queue = (dlist_entry_ctx_t *) malloc(sizeof(dlist_entry_ctx_t));
	memset(internal_queue, 0, sizeof(*internal_queue));
	DList_InitializeListHead(&(internal_queue->list_queue));
	internal_queue->thread_mutex = Lock_Init();
	internal_queue->thread_cond = Condition_Init();
}

void free_dlist(dlist_entry_ctx_t * dlist)
{
	Condition_Deinit(dlist->thread_cond);

	Lock_Deinit(dlist->thread_mutex);

	free(dlist);
}

void dlist_post(dlist_entry_ctx_t * link, E_Msg_Type type,  void * messageHandle , void * handler)
{
	dlist_node_t * node = (dlist_node_t*) malloc(sizeof(dlist_node_t));
	if(node == NULL)
		return;

	memset(node, 0, sizeof(*node));
	node->message = messageHandle;
	node->type = type;
	node->message_handler= handler;
	Lock(link->thread_mutex);
	DList_InsertTailList(&(link->list_queue), (PDLIST_ENTRY) node);

	if(link->thread_cond)
		Condition_Post(link->thread_cond);

	Unlock(link->thread_mutex);
}

typedef void (*handler) (void * msg);

dlist_node_t * dlist_get(dlist_entry_ctx_t * link)
{
	Lock(link->thread_mutex);
	dlist_node_t * node = (dlist_node_t*) DList_RemoveHeadList(&(link->list_queue));
	Unlock(link->thread_mutex);

	if(node == (dlist_node_t *)link)
		return NULL;

	if(node->type == T_Callback)
	{
		execute_callback_node((callback_t *) node->message);
	}
	else if(node->type == T_Message_Handler)
	{
		((handler)(node->message_handler))(node->message);
	}
	else
	{
		return node;
	}

	free(node);
	return NULL;
}
