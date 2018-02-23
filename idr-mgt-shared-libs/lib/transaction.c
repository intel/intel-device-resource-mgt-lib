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
 * transaction.c
 *
 *  Created on: Oct 22, 2016
 *      Author: xwang98
 */

#include "agent_core_lib.h"


/*typedef*/ struct sync_node_data
{
    void * response;
    int  response_len;
    ptr_sync_t sync_obj;
    uint8_t  status;
    uint8_t  response_fmt;
};


/*typedef*/ struct async_node_data
{
    void * context_data;
    bh_async_callback cb;
    bh_post_callback work_thread;
};

typedef struct sync_node
{
    struct sync_node * next;
    //struct sync_node * prev;
    uint32_t id;
    unsigned char sync_type; // 1.sync  2. async
    uint32_t  timeout;
    union
    {
        struct async_node_data cb_data;
        struct sync_node_data  sync_data;
    };
}sync_node_t;

//#define PRINTX(...) printf(__VA_ARGS__)
#define PRINTX(...)

#ifdef CTX_BUFFER_CECK

// it is used for checking if the user freed the data in the callback function
int g_trans_ctx_malloc_cnt = 0;
void * trans_malloc_ctx(int len)
{
	if(len == 0)
		return NULL;

	g_trans_ctx_malloc_cnt++;
	return malloc(len);
}


void   trans_free_ctx(void * ctx)
{
	if(ctx == NULL) return;

	g_trans_ctx_malloc_cnt --;
	free(ctx);
}
#endif


sync_ctx_t * create_sync_ctx()
{
	sync_ctx_t* node = (sync_ctx_t *)malloc(sizeof(sync_ctx_t));
	memset((void*) node, 0, sizeof (*node));

	node->ctx_lock = bh_bsp_create_syncobj();

	bh_get_elpased_ms((uint32_t*)&node->last_check);

	return node;
}
void delete_sync_ctx(sync_ctx_t * ctx)
{
    bh_bsp_delete_syncobj(ctx->ctx_lock);

	free(ctx);
}


static void add_sync_node(sync_ctx_t* sync_ctx, sync_node_t* node)
{
	bh_bsp_lock(sync_ctx->ctx_lock);

	if(sync_ctx->list == NULL)
	{
	    sync_ctx->list = node;
		node->next = NULL;
	}
	else
	{
		node->next = (sync_node_t * ) sync_ctx->list;
		sync_ctx->list = node;
	}

	sync_ctx->cnt ++;

	PRINTX("trans[%p]: add node [%p], id=%d total num=%d\n", sync_ctx, node, node->id, sync_ctx->cnt);
	bh_bsp_unlock(sync_ctx->ctx_lock);
}


static sync_node_t * remove_sync_node(sync_ctx_t * sync_ctx, unsigned long id)
{
	bh_bsp_lock(sync_ctx->ctx_lock);
	sync_node_t * prev = NULL;
	sync_node_t * current = (sync_node_t * )sync_ctx->list;
	while(current)
	{
		if(id == current->id)
		{
			if(prev)
				prev->next = current->next;
			else
				sync_ctx->list = current->next;

			sync_ctx->cnt --;

			PRINTX("trans[%p]: removed node [%p], id=%d, total num=%d\n",sync_ctx, current, current->id, sync_ctx->cnt);
			break;
		}
		else
		{
			prev = current;
			current = current->next;
		}
	}

	bh_bsp_unlock(sync_ctx->ctx_lock);

	return current;

}
unsigned long bh_gen_id(sync_ctx_t * ctx)
{
	// skip -1 as valid ID
	unsigned long id;
	bh_bsp_lock(ctx->ctx_lock);
	if(ctx->new_id == -1) ctx->new_id = 0;
	id= ctx->new_id++;
	bh_bsp_unlock(ctx->ctx_lock);

	return id;


}

//return: -1 – timeout, 0: no response, >0: response length
int bh_wait_response(sync_ctx_t* sync_ctx, uint32_t id, void ** response, uint32_t timeout)
{
	sync_node_t * node = (sync_node_t *)malloc(sizeof(sync_node_t));
	memset((void*) node, 0, sizeof (*node));

	if(id == -1)
		node->id = bh_gen_id(sync_ctx);
	else
		node->id = id;
	node->sync_type = 1;
	node->sync_data.sync_obj = bh_bsp_create_syncobj();

	add_sync_node(sync_ctx, node);

	// Todo: need ref count to avoid other thread free this node bet
	int ret = -1;
	bh_bsp_lock(node->sync_data.sync_obj);
	bh_bsp_wait(node->sync_data.sync_obj, timeout, 1);

	sync_node_t* node_found = remove_sync_node(sync_ctx, node->id);
	if(node_found)
	{
		assert(node_found == node);

		*response = node_found->sync_data.response;

		ret = node_found->sync_data.response_len;

		bh_bsp_delete_syncobj(node_found->sync_data.sync_obj);
		free(node_found);
	}
	else // waker now hold the node
	{
		node->sync_data.status = S_PENDING_DELETE;
		bh_bsp_unlock(node->sync_data.sync_obj);
	}

	return ret;
}


void bh_feed_transaction(void * transaction, void * response,  uint32_t len, uint8_t format)
{
	sync_node_t * node = (sync_node_t *) transaction;

	if(node->sync_type == 2)
	{
		// the work thread api should post the callback to the target thread
		assert(node->cb_data.work_thread == NULL);
		if(node->cb_data.cb)
		{
			node->cb_data.cb(node->cb_data.context_data, response, len, format);
		}

	}

	free(node);

	return;
}

void bh_feed_response(sync_ctx_t * sync_ctx, uint32_t id, void * response,  uint32_t len, uint8_t format)
{
	//printf("In bh_feed_response. mid=%d, response=%s, len=%d, fmt=%d\n", id, response, len, format);
	sync_node_t * node = remove_sync_node(sync_ctx,id);
	if(node == NULL)
	{
		printf("In bh_feed_response, node == NULL, mid = %d\n", id);
		return;
	}
	if(node->sync_type == 1)
	{
		bh_bsp_lock(node->sync_data.sync_obj);

		if(node->sync_data.status == S_PENDING_DELETE)
		{
			bh_bsp_delete_syncobj(node->sync_data.sync_obj);
			free(node);
			return;
		}

		node->sync_data.response_len = 0;

		if(len)
		{
			node->sync_data.response = malloc(len);
			if(node->sync_data.response == NULL)
			{
				printf("mid=%d, response == NULL\n", id);
			}
			else
			{
				node->sync_data.response_len = len;
				memcpy(node->sync_data.response, response, len);
			}
		}
		node->sync_data.response_fmt = format;

		add_sync_node(sync_ctx, node);

		// call sequence important here: ensure
		bh_bsp_wakeup(node->sync_data.sync_obj, 0);
	}
	else if(node->sync_type == 2 && node->cb_data.work_thread)
	 {
		 callback_t * cb = (callback_t*) malloc(sizeof(callback_t));
		 memset(cb, 0, sizeof(callback_t));
		 cb->len = len;
		 cb->format = format;
		 if(len){
			 cb->data = malloc(len);
			 memcpy(cb->data, response, len);
		 }
		 else
		 {
			 // user may use len=0 for passing messageHandle
			 cb->data = response;
		 }
		 cb->transaction = node;
		 bh_post_callback work_thread = node->cb_data.work_thread;
		 node->cb_data.work_thread = NULL;
		 work_thread(cb);
		 return;
	 }
	 else
	 {
		 return bh_feed_transaction(node, response, len, format);
	 }
}


bool bh_update_trans_timer(sync_ctx_t* sync_ctx)
{
    bh_bsp_lock(sync_ctx->ctx_lock);
    uint32_t elpased_ms = bh_get_elpased_ms((uint32_t*)&sync_ctx->last_check);
    bool has_timeout = 0;
    sync_node_t * current = (sync_node_t * )sync_ctx->list;
    while(current)
    {
        if(current->timeout > elpased_ms)
        {
            current->timeout -= elpased_ms;
        }
        else
        {
            has_timeout = true;
            current->timeout = 0;
        }
        current = current->next;
    }

    bh_bsp_unlock(sync_ctx->ctx_lock);

    return has_timeout;
}

//
// ctx_data： context  buffer allocated by the caller. It will be auto released by the framework.
// return: the id of the transaction node
uint32_t bh_wait_response_async(sync_ctx_t* sync_ctx, uint32_t id,
		/*bh_async_callback*/void* cb,
		void* ctx_data,
		uint32_t timeout,
		void * worker_thread)
{

    bh_update_trans_timer(sync_ctx);

	sync_node_t * node = (sync_node_t *)malloc(sizeof(sync_node_t));
	memset((void*) node, 0, sizeof (*node));

	if(id == -1)
	{
		node->id = bh_gen_id(sync_ctx);
		id = node->id;
	}
	else
	{
		node->id = id;
	}
	node->sync_type = 2;
	node->cb_data.context_data = ctx_data;
	node->cb_data.work_thread = worker_thread;
	node->cb_data.cb = cb;
	node->timeout = timeout;

	add_sync_node(sync_ctx, node);
	return id;
}

#define MOVE_NEXT(prev, current) {prev = current; current = current->next;}
static uint32_t bh_remove_expired_trans(sync_ctx_t* sync_ctx, sync_node_t ** expired_list)
{
	bh_bsp_lock(sync_ctx->ctx_lock);
    uint32_t elpased_ms = bh_get_elpased_ms((uint32_t*)&sync_ctx->last_check);

    uint32_t nearest = -1;

    sync_node_t * expired = NULL;
	sync_node_t * prev = NULL;
	sync_node_t * current = (sync_node_t * )sync_ctx->list;
	while(current)
	{
		if(current->timeout > elpased_ms)
		{
			current->timeout -= elpased_ms;

			if(nearest == -1)
			{
				nearest = current->timeout;
			}
			else if(nearest > current->timeout)
			{
				nearest = current->timeout;
			}

			MOVE_NEXT(prev, current);
		}
		else if(current->sync_type == 1)
		{
		    MOVE_NEXT(prev, current);
		}
		else
		{
			sync_node_t * sv_node = current->next;
			current->timeout = 0;
			if(prev)
				prev->next = current->next;
			else
				sync_ctx->list = current->next;

			current->next = expired;
			expired = current;

			current=sv_node;
			sync_ctx->cnt --;

			PRINTX("trans[%p]: expired, removed node [%p], id=%d, total num=%d\n",sync_ctx, expired, expired->id, sync_ctx->cnt);
		}
	}

	bh_bsp_unlock(sync_ctx->ctx_lock);

	if(expired_list) *expired_list = expired;

	return nearest;
}


uint32_t bh_handle_expired_trans(sync_ctx_t* sync_ctx)
{
	 sync_node_t * expired_list = NULL;
	 int count = 0;

	 uint32_t timeout = bh_remove_expired_trans(sync_ctx, &expired_list);

	 while(expired_list)
	 {
		 sync_node_t *node = expired_list;
		 expired_list = expired_list->next;

		 PRINTX("trans [%p]: handle expired node [%p], id=%d, next=[%p]\n",sync_ctx, node, node->id, node->next);

		 if(node->sync_type == 2)
		 {
			 if(node->cb_data.work_thread)
			 {
				 callback_t * cb = (callback_t*) malloc(sizeof(callback_t));
				 memset(cb, 0, sizeof(callback_t));
				 cb->format = T_Empty;
				 cb->transaction = node;
				 bh_post_callback work_thread = node->cb_data.work_thread;
				 node->cb_data.work_thread = NULL;
				 work_thread(cb);

				 // should NOT be referred any more
				 node = NULL;
			 }
			 else
			{
				 if(node->cb_data.cb)
					 node->cb_data.cb(node->cb_data.context_data, NULL, 0, 0);
			}
		}

		 if(node)
		     free(node);
	 }

	 return timeout;
}

/// when the worker thread recieved the callback_t node,
/// call this function to handle it.
void execute_callback_node(callback_t * cb)
{
	if(cb->transaction)
	{
		bh_feed_transaction(cb->transaction, cb->data, cb->len, cb->format);
	}

	// note: user may set the len as 0 for bus message messageHandle
	//       then we don't need to release the handle it self.
	if(cb->len && cb->data) free(cb->data);
	free(cb);
}
