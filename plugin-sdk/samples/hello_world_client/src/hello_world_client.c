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
#ifdef _CRTDBG_MAP_ALLOC
#include <crtdbg.h>
#endif

#include "module.h"

#include "plugin_sdk.h"
#include "../inc/hello_world.h"

int sock = -1;
int g_quit_thread = 0;
void * g_framework = NULL;
COND_HANDLE g_working_thread_cond = NULL;
LOCK_HANDLE g_working_thread_lock = NULL;

extern void working_thread_waker(void * framework);
extern void hello_response_callback(restful_response_t *response, void * user_data);


typedef struct HELLOWORLD_HANDLE_DATA_TAG
{
    THREAD_HANDLE threadHandle;
    LOCK_HANDLE lockHandle;
    int stopThread;
    BROKER_HANDLE broker;

}HELLOWORLD_HANDLE_DATA;

#define HELLOWORLD_MESSAGE "hello world"


int helloWorldThread(void *param)
{
    HELLOWORLD_HANDLE_DATA* handleData = param;
    int result;
    int mid = 0;

    while (handleData->stopThread == 0)
    {
        ///
        /// Step 4: run the real message processing in the working thread
        ///
    	uint32_t next_expiry_ms = process_in_working_thread(g_framework);
        if(next_expiry_ms == -1)
        {
        	next_expiry_ms = 30;
        }

        Condition_Wait(g_working_thread_cond, g_working_thread_lock, next_expiry_ms);

        restful_request_t request = {0};
        time_t * user_data = (time_t *) malloc(sizeof(time_t));
        time(user_data);
        request.mid = mid ++;
        request.action = T_Get;
        request.url = "/hello";
        request_bus_service(&request, hello_response_callback, user_data, 100);
    }

    return 0;
}


static MODULE_HANDLE HelloWorld_Create(BROKER_HANDLE broker, const void* configuration)
{
    HELLOWORLD_HANDLE_DATA* result;

    if(broker == NULL) /*configuration is not used*/
    {
        //LogError("invalid arg broker=%p", broker);
        result = NULL;
    }
    else
    {
        result = malloc(sizeof(HELLOWORLD_HANDLE_DATA));
        if(result == NULL)
        {
            //LogError("unable to malloc");
        }
        else
        {
            result->lockHandle = Lock_Init();
            if(result->lockHandle == NULL)
            {
                //LogError("unable to Lock_Init");
                free(result);
                result = NULL;
            }
            else
            {
                result->stopThread = 0;
                result->broker = broker;
                result->threadHandle = NULL;
            }

            g_working_thread_cond = Condition_Init();
            g_working_thread_lock = Lock_Init();

            ///
            /// Step 1: initialize the restful service framework
            ///
            g_framework = init_restful_framework("hello_client", result, broker, true);

            ///
            /// Step 2: add the working thread waker to the restful service framework
            ///
            set_working_thread_waker(g_framework, working_thread_waker);
        }
    }
    return result;
}

static void* HelloWorld_ParseConfigurationFromJson(const char* configuration)
{
	(void)configuration;
    return NULL;
}

static void HelloWorld_FreeConfiguration(void* configuration)
{
	(void)configuration;
}

static void HelloWorld_Start(MODULE_HANDLE module)
{
    HELLOWORLD_HANDLE_DATA* handleData = module;
    if (handleData != NULL)
    {
        if (Lock(handleData->lockHandle) != LOCK_OK)
        {
            //LogError("not able to Lock, still setting the thread to finish");
            handleData->stopThread = 1;
        }
        else
        {
            if (ThreadAPI_Create(&handleData->threadHandle, helloWorldThread, handleData) != THREADAPI_OK)
            {
                //LogError("failed to spawn a thread");
                handleData->threadHandle = NULL;
            }
            (void)Unlock(handleData->lockHandle);
        }
    }
}

static void HelloWorld_Destroy(MODULE_HANDLE module)
{
    /*first stop the thread*/
    HELLOWORLD_HANDLE_DATA* handleData = module;
    int notUsed;
    if (Lock(handleData->lockHandle) != LOCK_OK)
    {
        //LogError("not able to Lock, still setting the thread to finish");
        handleData->stopThread = 1;
    }
    else
    {
        handleData->stopThread = 1;
        Unlock(handleData->lockHandle);
    }

    working_thread_waker(g_framework);

    if (handleData->threadHandle != NULL &&
        ThreadAPI_Join(handleData->threadHandle, &notUsed) != THREADAPI_OK)
    {
        //LogError("unable to ThreadAPI_Join, still proceeding in _Destroy");
    }
    
    (void)Lock_Deinit(handleData->lockHandle);
    free(handleData);
}

static void HelloWorld_Receive(MODULE_HANDLE moduleHandle, MESSAGE_HANDLE messageHandle)
{

    ///
    /// Step 3: call the the bus message handler of restful service framework
    ///

	handle_bus_message(moduleHandle, messageHandle);
}

static const MODULE_API_1 HelloWorld_API_all =
{
    {MODULE_API_VERSION_1},
    HelloWorld_ParseConfigurationFromJson,
	HelloWorld_FreeConfiguration,
    HelloWorld_Create,
    HelloWorld_Destroy,
    HelloWorld_Receive,
    HelloWorld_Start
};

#ifdef BUILD_MODULE_TYPE_STATIC
MODULE_EXPORT const MODULE_API* MODULE_STATIC_GETAPI(HELLOWORLD_MODULE)(MODULE_API_VERSION gateway_api_version)
#else
MODULE_EXPORT const MODULE_API* Module_GetApi(MODULE_API_VERSION gateway_api_version)
#endif
{
    const MODULE_API * api;
    if (gateway_api_version >= HelloWorld_API_all.base.version)
    {
        api= (const MODULE_API*)&HelloWorld_API_all;
    }
    else
    {
        api = NULL;
    }
    return api;
}
