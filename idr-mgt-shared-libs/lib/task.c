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
 * ams_task.c
 *
 *  Created on: Dec 25, 2016
 *      Author: xwang98
 */

#define LOG_TAG                     "TASK"

#include "agent_core_lib.h"


typedef struct _task_ctx
{
    pthread_mutex_t taskqueue_condition_mutex;
    pthread_cond_t  taskqueue_condition_cond;
    pthread_condattr_t cond_attr;
    bh_task_t * g_task_list;
    bh_task_t * g_current_task;
} task_ctx_t;



void * bh_init_task_scheduler()
{
    task_ctx_t * task_ctx = (task_ctx_t*) malloc(sizeof(task_ctx_t));
    if(task_ctx == NULL) return NULL;

    memset(task_ctx, 0, sizeof(*task_ctx));
    errno = pthread_condattr_init (&task_ctx->cond_attr);
    if (errno) {
 	   perror ("pthread_condattr_init");
 	   return NULL;
    }

    errno = pthread_condattr_setclock (&task_ctx->cond_attr, CLOCK_MONOTONIC);
    if (errno) {
 	   perror ("pthread_condattr_setclock");
    }

    pthread_mutex_init(&task_ctx->taskqueue_condition_mutex, NULL); //PTHREAD_MUTEX_INITIALIZER;
    pthread_cond_init(&task_ctx->taskqueue_condition_cond, &task_ctx->cond_attr); // = PTHREAD_COND_INITIALIZER;


    return task_ctx;
}


bh_task_t * bh_new_task(char* task_name, void * data, bool auto_free_data, int repeat_duration, void * task_handler)
{

	bh_task_t * task = (bh_task_t*) malloc(sizeof(bh_task_t));
	if(task == NULL) return NULL;

	assert(task_name != NULL);

	memset(task, 0, sizeof(*task));
	task->task_type = strdup(task_name);
	task->task_data = data;
	task->repeat_interval_secs = repeat_duration;
	task->handler = (bh_task_handler) task_handler;
	task->task_data_is_allocated = auto_free_data;

	return task;
}

void bh_delete_task(bh_task_t * task)
{
    if(task->close_handler)
    {
        task->close_handler(task);
    }

	if(task->task_data_is_allocated && task->task_data)
	{
		free(task->task_data);
	}

	TraceV(LOG_FLAG_TASK,"free task [%s]\n",task->task_type);

	if(task->task_type) free(task->task_type);
	free(task);
}

bh_task_t * bh_schedule_task(void * task_ctx, bh_task_t * task, int secs_from_now)
{
    task_ctx_t * ctx = (task_ctx_t*) task_ctx;

	task->next_execution = bh_get_tick_sec() + secs_from_now;
	TraceV(LOG_FLAG_TASK,"schedule_task:type:%s,time:%d\n",
			task->task_type,task->next_execution);

	pthread_mutex_lock(&ctx->taskqueue_condition_mutex);

	//put the task into the list in time order
	task->next = NULL;
	if(ctx->g_task_list == NULL || task->next_execution <= ctx->g_task_list->next_execution)
	{
		task->next  = ctx->g_task_list;
		ctx->g_task_list = task;
	}
	else
	{
		bh_task_t * t = ctx->g_task_list;

		assert(t->next_execution < task->next_execution);

		while(1)
		{
			if(t->next == NULL)
			{
				t->next = task;
				break;
			}
			else if(task->next_execution < t->next->next_execution)
			{
				task->next = t->next;
				t->next = task;

				break;
			}
			t = t->next;
		}
	}


	//signal if it is the first task in the time order
	pthread_cond_signal( &ctx->taskqueue_condition_cond );


	pthread_mutex_unlock(&ctx->taskqueue_condition_mutex);

	return NULL;
}


int bh_execute_task(void * task_ctx, bh_task_t * task) // ?
{
	int ret = -1;
	task_ctx_t * ctx = (task_ctx_t*) task_ctx;

	ctx->g_current_task = task;
	TraceV(LOG_FLAG_TASK,"execute_task:type:%s,time:%d\n",
			task->task_type,task->next_execution);

	ret = task->handler(task);
	task->exec_count ++;

	ctx->g_current_task = NULL;

	return ret;
}

// if the earliest task is ready to run, then remove it from the task list
// and return it to caller.
bh_task_t * bh_check_task(void * task_ctx, int * secs_to_first_task)
{
	bh_task_t * ret = NULL;
	task_ctx_t * ctx = (task_ctx_t*) task_ctx;

	pthread_mutex_lock(&ctx->taskqueue_condition_mutex);

	time_t now = bh_get_tick_sec();

	if(ctx->g_task_list == NULL)
		*secs_to_first_task = 120;
	else if(ctx->g_task_list->next_execution <= now)
	{
		ret = ctx->g_task_list;
		ctx->g_task_list = ctx->g_task_list->next;
	}
	else
	{
		*secs_to_first_task = ctx->g_task_list->next_execution - now;
	}

	pthread_mutex_unlock(&ctx->taskqueue_condition_mutex);

	return ret;
}

void bh_wait_for_task(void * task_ctx, int seconds)
{
    task_ctx_t * ctx = (task_ctx_t*) task_ctx;
	pthread_mutex_lock(&ctx->taskqueue_condition_mutex);

    struct timespec timeToWait;
    clock_gettime (CLOCK_MONOTONIC, &timeToWait);
    timeToWait.tv_sec += seconds;

    pthread_cond_timedwait(&ctx->taskqueue_condition_cond, &ctx->taskqueue_condition_mutex, &timeToWait);

	pthread_mutex_unlock(&ctx->taskqueue_condition_mutex);

}

// find the task by the data pointer
bh_task_t * bh_remove_task_by_data(void * task_ctx, void * data_ptr)
{
	bh_task_t * ret = NULL;
	task_ctx_t * ctx = (task_ctx_t*) task_ctx;
	pthread_mutex_lock(&ctx->taskqueue_condition_mutex);

	bh_task_t * t = ctx->g_task_list;
	bh_task_t * prev = NULL;
	while(t)
	{
		if(t->task_data == data_ptr)
		{
			if(prev)
			{
				prev->next = t->next;
			}
			else
			{
			    ctx->g_task_list = t->next;
			}
			ret = t;
			break;
		}
		prev = t;
		t = t->next;
	}

	pthread_mutex_unlock(&ctx->taskqueue_condition_mutex);

	return ret;
}


// find the task by the data pointer
bh_task_t * bh_remove_task_by_name(void * task_ctx, char * name)
{
    bh_task_t * ret = NULL;
    task_ctx_t * ctx = (task_ctx_t*) task_ctx;
    pthread_mutex_lock(&ctx->taskqueue_condition_mutex);

    bh_task_t * t = ctx->g_task_list;
    bh_task_t * prev = NULL;
    while(t)
    {
        if(strcmp(t->task_type, name) == 0)
        {
            if(prev)
            {
                prev->next = t->next;
            }
            else
            {
                ctx->g_task_list = t->next;
            }
            ret = t;
            break;
        }
        prev = t;
        t = t->next;
    }

    pthread_mutex_unlock(&ctx->taskqueue_condition_mutex);

    return ret;
}




int thread_task_polling(void * param)
{
	int secs_to_first_task = 0;
	bh_task_t * current;
	task_ctx_t * ctx = (task_ctx_t*) param;
	TraceI(LOG_FLAG_TASK, "enter thread_task_polling thread\n");

	while(1)
	{
		current  = bh_check_task(ctx, &secs_to_first_task);
		if(current)
		{
			int result = bh_execute_task(ctx, current);

			if(current->repeat_interval_secs == 0)
			    bh_delete_task(current);
			else
			    bh_schedule_task(ctx, current,current->repeat_interval_secs);
		}
		else
		{
		    bh_wait_for_task(ctx, secs_to_first_task);
		}
	}
}

#if 0
int sample_task(bh_task_t *task)
{
	int s32_ret = -1;
    char query_str[256] = {0};
    sprintf(query_str, "client_uuid=%s", get_ams_client_id());

	coap_packet_t message[1];
    coap_init_message(message, COAP_TYPE_CON, COAP_GET, coap_get_mid());
    coap_set_header_uri_path(message, "/ams/v1/product/changes");
    coap_set_header_uri_query(message, query_str);

    coap_request_user_data_t * data = ams_blocking_request(message);

    if(data && data->result == Success && data->payload)
    {
    	s32_ret = handle_software_query(data->payload);
    }
	if(-2 == s32_ret){//-2:download failed,0:download success,-1:404 not found means no changes
		if(g_download_software_interval<TIME_INTERVAL_DOWNLOAD_SOFTWARE_MAX){
			g_download_software_interval+=10;
		}
		TraceV(LOG_FLAG_TASK,"%s failed,current_time:%d,next interval changed to: %d\n",
				task_type_string[task->task_type],bh_get_tick_sec()-g_process_start_time ,g_download_software_interval);
	}
	else{
		g_download_software_interval = TIME_INTERVAL_DOWNLOAD_SOFTWARE_S;
	}
    task->repeat_interval_secs = g_download_software_interval;
    if(data)free_coap_request_user_data(data);

    return 0;

}

#endif
