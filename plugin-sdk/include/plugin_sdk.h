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
 * plugin_sdk.h
 *
 *  Created on: Apr 16, 2017
 *      Author: xwang98
 */

#ifndef APPS_GW_BROKER_PLUGIN_SDK_INCLUDE_PLUGIN_SDK_H_
#define APPS_GW_BROKER_PLUGIN_SDK_INCLUDE_PLUGIN_SDK_H_

#include <stdlib.h>
#include <pthread.h>
#include "azure_c_shared_utility/base64.h"
#include "azure_c_shared_utility/constmap.h"
#include "azure_c_shared_utility/gb_stdio.h"
#include "azure_c_shared_utility/gb_time.h"
#include "azure_c_shared_utility/gballoc.h"
#include "azure_c_shared_utility/map.h"
#include "azure_c_shared_utility/strings.h"
#include "azure_c_shared_utility/doublylinkedlist.h"
#include "azure_c_shared_utility/threadapi.h"
#include "azure_c_shared_utility/lock.h"
#include "azure_c_shared_utility/crt_abstractions.h"
#include "message.h"
#include "broker.h"

#include "plugin_constants.h"

#ifdef __cplusplus
extern "C" {
#endif


typedef enum
{
	T_Get = 1,
	T_Post,
	T_Put,
	T_Del
} rest_action_t;


typedef struct _restful_request
{
	rest_action_t action;
	int payload_fmt;
	int payload_len;
	char * payload;
	char * url;
	char * query;
	char * src_module;
	unsigned long  mid;
	void * user_data;
}restful_request_t;


typedef struct _restful_response
{
	int code;
	int payload_fmt;
	int payload_len;
	char * payload;
	unsigned long  mid;
	char * dest_module;
}restful_response_t;

// The user plugin should call this function once during the initialization
// parameter "separate_thread" set whether the framework is running in a
// working thread rather than the default receiver thread
// return: the framework context pointer
void * init_restful_framework(char * module_name,MODULE_HANDLE, BROKER_HANDLE broker, bool separate_thread);


// register the url that this plugin will serve and handling function
/* url match patterns:
 * sample 1: /abcd, match "/abcd" only
 * sample 2: /abcd/ match match "/abcd" and "/abcd/*"
 * sample 3: /abcd*, match any url started with "/abcd"
 * sample 4: /abcd/*, exclude "/abcd"
 *
 */
typedef bool (*Plugin_Res_Handler) (restful_request_t *request, restful_response_t * response);
bool register_resource_handler(const char * url, Plugin_Res_Handler handler, rest_action_t action);


// The user application should call this function in the Azure Gateway SDK message recieve API
// to allow the framework processing the incoming messages
// return true is framework has handled this message
bool handle_bus_message(MODULE_HANDLE moduleHandle, MESSAGE_HANDLE messageHandle);



// if the user set "separate_thread" parameter in calling init_restful_framework(),
// the application should call process_in_working_thread() timely to handle all the
// framework event.
// there are two situations that app should call this API:
// 1. the working thread received any type of event from user defined wakeup_working_thread()
// 2. after milliseconds as returned by last calling this API
uint32_t process_in_working_thread(void * framework_ctx);


typedef void (*f_wakeup_working_thread) (void * framework_ctx);
void set_working_thread_waker(void * framework_ctx, f_wakeup_working_thread);


typedef void (*Service_Result_Handler) (restful_response_t *response, void * user_data);
int request_bus_service(restful_request_t * request, Service_Result_Handler response_handler, void * user_data, int timeout);


char * rest_action_str(int action);
int action_from_string(char* action);
char * rest_fmt_str(int fmt, char * buffer);

#ifdef __cplusplus
}
#endif

#endif /* APPS_GW_BROKER_PLUGIN_SDK_INCLUDE_PLUGIN_SDK_H_ */
