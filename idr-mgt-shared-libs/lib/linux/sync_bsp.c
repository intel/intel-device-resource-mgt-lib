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


#include <stdlib.h>
#include <errno.h>
#include <sys/time.h>
#include "iagent_bsp.h"

#if defined(CLOCK_MONOTONIC)
#define CLOCK_GETTIME(t) clock_gettime(CLOCK_MONOTONIC, t);
#pragma message("CLOCK_MONOTONIC enabled, that is expected!")
#else
#define CLOCK_GETTIME(t) clock_gettime(CLOCK_REALTIME, t);
#pragma message("CLOCK_MONOTONIC disabled, that is unexpected!")
#endif

void bh_bsp_lock(ptr_sync_t sync_obj)
{
    pthread_mutex_lock(&sync_obj->condition_mutex);
    sync_obj->locked = 1;
}

void bh_bsp_unlock(ptr_sync_t sync_obj)
{
    sync_obj->locked = 0;
    pthread_mutex_unlock(&sync_obj->condition_mutex);
}

time_t bh_bsp_get_time()
{
    time_t t;
    time (&t);
    return t;
}


// 返回自系统开机以来的毫秒数（tick）
tick_time_t bh_get_tick_ms()
{
    struct timespec ts;
    CLOCK_GETTIME(&ts);

    return (ts.tv_sec * 1000 + ts.tv_nsec / 1000000);
}

// 返回自系统开机以来的秒数（tick）
// note: CLOCK_REALTIME will return calendar time
tick_time_t bh_get_tick_sec()
{
    struct timespec ts;
    CLOCK_GETTIME(&ts);
    return (ts.tv_sec);
}

ptr_sync_t bh_bsp_create_syncobj()
{

   ptr_sync_t sync_obj = (ptr_sync_t) malloc(sizeof(sync_t));
   if(sync_obj == NULL)
       return NULL;

    memset(sync_obj, 0, sizeof(*sync_obj));

    errno = pthread_condattr_init (&sync_obj->cond_attr);
    if (errno) {
 	   perror ("pthread_condattr_init");
 	   return NULL;
    }

    errno = pthread_condattr_setclock (&sync_obj->cond_attr, CLOCK_MONOTONIC);
    if (errno) {
 	   perror ("pthread_condattr_setclock");
    }

    pthread_mutex_init (&sync_obj->condition_mutex, NULL); //PTHREAD_MUTEX_INITIALIZER;
    pthread_cond_init (&sync_obj->condition_cond, &sync_obj->cond_attr); // = PTHREAD_COND_INITIALIZER;

    return sync_obj;

}

void bh_bsp_delete_syncobj(ptr_sync_t sync_obj)
{
    if(sync_obj->locked)
        bh_bsp_unlock(sync_obj);
    
    pthread_mutex_destroy (&sync_obj->condition_mutex);
    pthread_cond_destroy (&sync_obj->condition_cond);
    pthread_condattr_destroy(&sync_obj->cond_attr);
    free(sync_obj);
}

int bh_bsp_wait(ptr_sync_t sync_obj, int timeout_ms, bool hold_lock)
{
    struct timespec timeToWait;
    clock_gettime (CLOCK_MONOTONIC, &timeToWait);

    timeToWait.tv_nsec += (timeout_ms%1000) * 1000000;
    timeToWait.tv_sec += timeout_ms/1000;
    timeToWait.tv_sec += timeToWait.tv_nsec/(1000*1000*1000);
    timeToWait.tv_nsec %= (1000*1000*1000);

    sync_obj->waiting = 1;
    int ret = pthread_cond_timedwait(&sync_obj->condition_cond, &sync_obj->condition_mutex, &timeToWait);
    sync_obj->waiting = 0;

    if(!hold_lock) bh_bsp_unlock(sync_obj);
    
    if(ETIMEDOUT == ret) 
        return -1;
    else
        return 0;

}

void bh_bsp_wakeup(ptr_sync_t sync_obj,  bool hold_lock)
//    if(sync_obj->locked == 0)
//        hb_bsp_lock(sync_obj);
{
    pthread_cond_signal( &sync_obj->condition_cond );
    
    if(!hold_lock)
    	bh_bsp_unlock(sync_obj);

}


#include <sys/time.h>
#include <pthread.h>
uint32_t bh_get_elpased_ms(uint32_t * last_system_clock)
{
    uint32_t elpased_ms;
    tick_time_t now = bh_get_tick_ms();

    // system clock overrun
    if(now < *last_system_clock)
    {
      elpased_ms = now + (0xFFFFFFFF - *last_system_clock) + 1;
    }
    else
    {
      elpased_ms = now - *last_system_clock;
    }

    *last_system_clock = now;

    return elpased_ms;
}
