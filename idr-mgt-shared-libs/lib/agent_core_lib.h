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
 * agent_core_lib.h
 *
 *  Created on: Dec 11, 2016
 *      Author: xwang98
 */

#ifndef APPS_IAGENT_CORE_LIB_AGENT_CORE_LIB_H_
#define APPS_IAGENT_CORE_LIB_AGENT_CORE_LIB_H_


#include "iagent_bsp.h"
#include "misc.h"
#include "logs.h"


#ifdef __cplusplus
extern "C" {
#endif

#define S_PENDING_DELETE  1

#define COUNT_OF(x)  (sizeof(x)/sizeof(x[0]))



/************************************************************************/
/*                                                                      */
/*                           message_queue.c                              */
/*                                                                      */
/************************************************************************/


typedef void (*bh_msg_handler) (void *msg);

typedef struct _msg_t
{
    struct _msg_t * next;
    struct _msg_t * prev;
    unsigned short tag;
    unsigned long len;
    void * body;
    time_t  time;
    bh_msg_handler msg_handler;
} msg_t;


typedef struct _msg_queue_t
{
    pthread_mutex_t condition_mutex;
    pthread_cond_t  condition_cond;
    pthread_condattr_t cond_attr;
    unsigned int cnt;
    unsigned int max;
    unsigned int drops;
    msg_t * head;
    msg_t * tail;
} msg_queue_t;


//message_queue
#define MSG_NOW time(NULL)
msg_queue_t* create_queue();
void release_queue(msg_queue_t * queue);
bool post_msg(msg_queue_t *queue, void *body, unsigned int len);
bool post_msg2(msg_queue_t *queue, msg_t *msg);
msg_t * new_msg( void *body, unsigned int len, time_t time, unsigned short tag, void * handler);
void free_msg(msg_t *msg);
msg_t *get_msg(msg_queue_t *queue, int timeout);
msg_t *get_msg_call_handler(msg_queue_t *queue, int timeout);


/************************************************************************/
/*                                                                      */
/*                           transaction.c                              */
/*                                                                      */
/************************************************************************/

#ifndef sync_t
//typedef int sync_t;
#endif

enum{
	T_Empty,
	T_Raw,
	T_Coap_Raw,
	T_Coap_Parsed,
	T_iLink_Parsed,
	T_Broker_Message_Handle,

	T_Trans_User_Fmt = 100
};


typedef int (*bh_async_callback) (void * ctx, void * data, int len, unsigned char format);

typedef struct _callback
{
	void * data;
	int len;
	unsigned char format;
	void * transaction;
} callback_t;
void execute_callback_node(callback_t * cb);

typedef void (*bh_post_callback) (callback_t * callback);


typedef struct sync_ctx
{
    ptr_sync_t ctx_lock;
    unsigned int cnt;
    void * list;
    time_t last_check;
    uint32_t new_id;
}sync_ctx_t;

//transaction
//#define CTX_BUFFER_CECK 1
#ifdef CTX_BUFFER_CECK
void * trans_malloc_ctx(int len);
void   trans_free_ctx(void *);
#else
#define trans_malloc_ctx malloc
#define trans_free_ctx free
#endif

sync_ctx_t * create_sync_ctx();
void delete_sync_ctx(sync_ctx_t* ctx);
int bh_wait_response(sync_ctx_t * sync_ctx, uint32_t id, void ** response, uint32_t timeout);
void bh_feed_response(sync_ctx_t * sync_ctx, uint32_t id, void * response,  uint32_t len, uint8_t format);
uint32_t bh_wait_response_async(sync_ctx_t * sync_ctx, uint32_t id, /*bh_async_callback*/void* cb, void* ctx_data, uint32_t timeout, void * worker_thread);

uint32_t bh_handle_expired_trans(sync_ctx_t* sync_ctx);
unsigned long bh_gen_id(sync_ctx_t * ctx);
void execute_callback_node(callback_t * cb);
sync_ctx_t* get_outgoing_requests_ctx();



/************************************************************************/
/*                                                                      */
/*                           task.c                                     */
/*                                                                      */
/************************************************************************/

typedef int (*bh_task_handler) (void * task);
typedef int (*bh_task_close_handler) (void * task);


typedef struct _bh_task
{
	struct _bh_task * next;
	char* task_type;
	char * task_data;
	bh_task_handler handler;
	bh_task_close_handler close_handler;
	int repeat_interval_secs;  // 0 - no repeat, delete it after execution
	time_t next_execution;
	unsigned int exec_count;
    bool task_data_is_allocated;
}bh_task_t;

void * bh_init_task_scheduler();
bh_task_t * bh_new_task(char* task_name, void * data, bool auto_free_data, int repeat_duration, void * task_handler);
void bh_delete_task(bh_task_t * task);
bh_task_t * bh_schedule_task(void * task_ctx,bh_task_t * task, int secs_from_now);
int bh_execute_task(void * task_ctx,bh_task_t * task);
void bh_wait_for_task(void * task_ctx,int seconds);
bh_task_t * bh_remove_task_by_data(void * task_ctx,void * data_ptr);
bh_task_t * bh_remove_task_by_name(void * task_ctx, char * name);


/************************************************************************/
/*                                                                      */
/*                           url_match.c                              */
/*                                                                      */
/************************************************************************/

extern int check_url_start(const char* url, int url_len, char * leading_str);
bool match_url(char * pattern, char * matched);


/************************************************************************/
/*                                                                      */
/*                           path_util.c                              */
/*                                                                      */
/************************************************************************/
char * getExecPath (char * path,size_t dest_len, char * argv0);
char * get_module_path(void* address);


#ifdef __cplusplus
}
#endif

#endif /* APPS_IAGENT_CORE_LIB_AGENT_CORE_LIB_H_ */
