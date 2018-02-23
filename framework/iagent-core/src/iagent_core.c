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


//iagent
#include "iagent_base.h"
//external
#ifdef RUN_AS_BROKER_MODULE
#include "broker_rest_convert.h"
#endif


#ifndef BUILTIN_IBROKER
#pragma message ("disable ibroker")
#endif


static time_t g_process_start_time;
char tbuf_process_start_time[32];
char tbuf_conection_status_change_time[32];
pthread_t ilink_sender_tid;
pthread_t ilink_ping_tid;
pthread_t ilink_recv_tid;

char g_quit_thread = 0;



static int init(char *exec)
{
	path_init(exec);

	init_agent();
    return 0;
}
#ifdef RUN_AS_BROKER_MODULE
int thread_iagent_core(void * param)
{
#ifdef RUN_ON_LINUX
    prctl (PR_SET_NAME, "iagent_core");
#endif

	IAGENT_HANDLE_DATA* handleData = param;
#else
int main(int argc, char *argv[])
{
	init(argv[0]);

#endif

    fprintf(stdout, "Starting iagent-core\n");



    int err;
    pthread_t ilink_sender_tid = 0;
    pthread_t ilink_handler_tid = 0;
    pthread_t ilink_ping_tid = 0;
    pthread_t ilink_recv_tid = 0;

    // time
    struct timeval tv;
    struct tm *tm_tmp;

    g_process_start_time = time(NULL);
    tm_tmp = localtime(&g_process_start_time);
    strftime(tbuf_process_start_time, 32, "[%y-%m-%d %H:%M:%S]\n", tm_tmp);
    strftime(tbuf_conection_status_change_time, 32, "[%y-%m-%d %H:%M:%S]\n", tm_tmp);

    //create thread
#ifdef BUILTIN_IBROKER
    if (pthread_create (&ilink_sender_tid, NULL, thread_ilink_sender, NULL))
    {
        ERROR( "can't create thread_ilink_sender :[%s]\n", strerror(err));
        goto out;
    }
    else
    {
        TraceI(FLAG_INIT, "thread_ilink_sender successfully\n");
    }
#else
    WARNING ("IBROKER is disabled in this version");
#endif


    if (pthread_create (&ilink_recv_tid, NULL, thread_ilink_port_handler, NULL))
    {
        ERROR( "can't create thread_ilink_port_handler :[%s]\n", strerror(err));
        return -1;
    }
    else
    {
        TraceI(FLAG_INIT, "thread_ilink_port_handler created successfully\n");
    }


    while (g_quit_thread == 0)
    {
        sleep(2000);
    }

out:
    return -1;
}
