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


#ifndef __SCREDSI__H
#define __SCREDSI__H
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>
#include <stdio.h>
#include <errno.h>
#include <stdlib.h>

#ifdef __cplusplus
extern "C"{
#endif

typedef struct redis_client{
	int fd;
}redis_client;
typedef struct redis_response{
	char * data;
	int len;
}redis_response;


// public api
redis_client * redis_connect(const char*host,const char *port);
redis_response * redis_command(redis_client *c,const char*fmt,...);

int redis_publish(redis_client *c, char* topic, char* content);
int redis_set(redis_client *c, char* key, char* value);
int redis_get(redis_client *c, char* key, char* value);
int redis_del(redis_client *c, char* key);



#ifdef __cplusplus
}
#endif
#endif
