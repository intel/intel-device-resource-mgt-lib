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



#ifndef APPS_IAGENT_CORE_IAGENT_BSP_LINUX_H__
#define APPS_IAGENT_CORE_IAGENT_BSP_LINUX_H__

//iagent


#include <pthread.h>
#include <stdbool.h>
#include <string.h>
#include <stdlib.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <unistd.h>
#include <fcntl.h>
#include <printf.h>
#include <errno.h>
#include <sys/stat.h>
#include <linux/prctl.h>
#include <sys/select.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/prctl.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include<sys/time.h>

typedef struct _sync
{
    pthread_mutex_t condition_mutex;
    pthread_cond_t  condition_cond;
    pthread_condattr_t cond_attr;
    unsigned char locked;
    unsigned char waiting;
}sync_t, *ptr_sync_t;





#endif
