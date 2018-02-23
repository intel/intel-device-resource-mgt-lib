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
#include "azure_c_shared_utility/xlogging.h"

#include "plugin_sdk.h"
#include "../../hello_world_service/inc/hello_world.h"

int sock = -1;
int g_quit_thread = 0;
void * g_framework = NULL;
#define MAX_PACKET_SIZE 128

extern bool res_hello_handler (restful_request_t *request, restful_response_t * response);
extern int create_socket(const char * portStr, int addressFamily);
extern void working_thread_waker(void * framework);


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
    struct timeval tv;
    int result;
    unsigned int sock_errs = 0;
    fd_set readfds;

    sock = create_socket((const char*) "4567", (int) AF_INET);

    while(sock == -1)
    {
		sleep(1);
    	sock = create_socket((const char*) "4567", (int) AF_INET);
	}


    while (handleData->stopThread == 0)
    {
        ///
        /// Step 4: run the real message processing in the working thread
        ///
    	uint32_t next_expiry_ms = process_in_working_thread(g_framework);
        if(next_expiry_ms != -1)
        {
            tv.tv_sec = next_expiry_ms / 1000;
            tv.tv_usec = (next_expiry_ms % 1000) * 1000;
        }
        else
        {
            tv.tv_sec = 30;
            tv.tv_usec = 0;
        }

        // we use a socket to receive the wakeup signal for handling arriving request
        // in this working thread.
        FD_ZERO(&readfds);
        FD_SET(sock,&readfds);
        result = select(FD_SETSIZE, &readfds, 0, 0, &tv);

		if (result <= 0) continue;

		if (FD_ISSET(sock, &readfds))
		{
			struct sockaddr_storage addr;
			socklen_t addrLen;
			char buffer[MAX_PACKET_SIZE];

			addrLen = sizeof(addr);
			int numBytes = recvfrom(sock, buffer, MAX_PACKET_SIZE, 0, (struct sockaddr *)&addr, &addrLen);

			if (numBytes == -1)
			{
				//LogError( "Error in recvfrom() for socket, errno = %d\r\n", errno);
				continue;
			}

			// This is the wake signal from wakeup_working_thread() indicating a service request arrived.
			// You can also use other wakeup mechanism, such as conditional signal.
			if (numBytes == 5 && strncmp((const char*) buffer, "wake", 4) == 0) {
				continue;
			}

			// do your own stuff here..

		}
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


            ///
            /// Step 1: initialize the restful service framework
            ///
            g_framework = init_restful_framework("hello", result, broker, true);

            ///
            /// Step 2: add the resource and handler to the restful service framework
            ///
            register_resource_handler("/hello", res_hello_handler, T_Get);

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
