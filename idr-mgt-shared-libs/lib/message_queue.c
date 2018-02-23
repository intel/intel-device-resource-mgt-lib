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
 * message_queue.c
 *
 *  Created on: Oct 25, 2016
 *      Author: xwang98
 */

//system
#include <sys/time.h>

//iagent
//#include "ilink_message.h"
#include "agent_core_lib.h"
#ifdef RUN_ON_LINUX
#include "logs.h"
#endif

#define DEFAULT_QUEUE_MAX 100

msg_queue_t* create_queue()
{
    msg_queue_t *queue = (msg_queue_t*) malloc(sizeof(msg_queue_t));
    if(queue == NULL)
        return NULL;

    memset((void*)queue, 0, sizeof(*queue));

    queue->max = DEFAULT_QUEUE_MAX;
    errno = pthread_condattr_init (&queue->cond_attr);
    if (errno) {
 	   perror ("pthread_condattr_init");
 	   return NULL;
    }

    errno = pthread_condattr_setclock (&queue->cond_attr, CLOCK_MONOTONIC);
    if (errno) {
 	   perror ("pthread_condattr_setclock");
    }
    pthread_mutex_init(&queue->condition_mutex, NULL); //PTHREAD_MUTEX_INITIALIZER;
    pthread_cond_init(&queue->condition_cond, &queue->cond_attr); // = PTHREAD_COND_INITIALIZER;

    return queue;
}

void release_queue(msg_queue_t * queue)
{
    pthread_mutex_destroy(&queue->condition_mutex);
    pthread_cond_destroy(&queue->condition_cond);
    pthread_condattr_destroy(&queue->cond_attr);

    free(queue);
}

bool post_msg2(msg_queue_t *queue, msg_t *msg)
{
	if(queue->cnt >= queue->max)
	{
		free_msg(msg);
		queue->drops ++;
		return false;
	}

    pthread_mutex_lock(&queue->condition_mutex);

    if (queue->cnt == 0)
    {
        assert (queue->head == NULL);
        assert (queue->tail == NULL);
        queue->head = queue->tail = msg;
        msg->next = msg->prev = NULL;
        queue->cnt = 1;

        pthread_cond_signal( &queue->condition_cond );
    }
    else
    {
        msg->next = NULL;
        msg->prev = queue->tail;
        queue->tail->next = msg;
        queue->tail = msg;
        queue->cnt  ++;
    }

    pthread_mutex_unlock( &queue->condition_mutex );

    return true;
}


bool post_msg(msg_queue_t *queue, void *body, unsigned int len)
{
    msg_t *msg = (msg_t*) malloc(sizeof(msg_t));
    if(msg == NULL)
    {
    	if(body) free(body);
    	return false;
    }

    memset(msg,0, sizeof(msg_t));
    msg->len = len;
    msg->body = body;
    msg->time = time(NULL);

    return post_msg2(queue, msg);
}


msg_t * new_msg( void *body, unsigned int len, time_t t, unsigned short tag, void * handler)
{
    msg_t *msg = (msg_t*) malloc(sizeof(msg_t));
    if(msg == NULL)
        return NULL;
    memset(msg,0, sizeof(msg_t));
    msg->len = len;
    msg->body = body;
   	msg->time = t;
    msg->tag = tag;
    msg->msg_handler = (bh_msg_handler) handler;

    return msg;
}

void free_msg(msg_t *msg)
{
	if(msg->body && msg->len)
		free(msg->body);

	free(msg);
}


msg_t *get_msg(msg_queue_t *queue, int timeout)
{
    msg_t *msg = NULL;
    pthread_mutex_lock(&queue->condition_mutex);

    struct timespec timeToWait;

    if( queue->cnt == 0)
    {
        assert (queue->head == NULL);
        assert (queue->tail == NULL);
/*
        struct timeval now;
        gettimeofday(&now,NULL);
        timeToWait.tv_sec = now.tv_sec + timeout;
        timeToWait.tv_nsec = now.tv_usec * 1000;
*/
        clock_gettime (CLOCK_MONOTONIC, &timeToWait);
        timeToWait.tv_sec += timeout;
        pthread_cond_timedwait(&queue->condition_cond, &queue->condition_mutex, &timeToWait);
    }

    if (queue->cnt == 0)
    {
        assert (queue->head == NULL);
        assert (queue->tail == NULL);
    }
    else if (queue->cnt == 1)
    {
        assert (queue->head == queue->tail);

        msg = queue->head;
        queue->head = queue->tail = NULL;
        queue->cnt = 0;
    }
    else
    {
        msg = queue->head;
        queue->head = queue->head->next;
        queue->head->prev = NULL;
        queue->cnt--;
    }

    pthread_mutex_unlock(&queue->condition_mutex);




    return msg;
}


// run the msg handler if it is available in the message
msg_t *get_msg_call_handler(msg_queue_t *queue, int timeout)
{
	msg_t * msg ;
	time_t now;
	time_t last = bh_get_tick_sec();
	while(timeout >=0 && (msg = get_msg(queue, timeout)) && msg->msg_handler)
	{
		msg->msg_handler(msg);
		free_msg(msg);

		now = bh_get_tick_sec();
		timeout -= (now - last);
		last = now;
	}

	return msg;
}
