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

#include "lwm2m_server.h"

extern sync_ctx_t* g_lwm2m_ctx;

dlist_entry_ctx_t g_request_queue;

static void* Lwm2mServer_ParseConfigurationFromJson(const char* configuration)
{
	(void)configuration;
    return NULL;
}

static void Lwm2mServer_FreeConfiguration(void* configuration)
{
	(void)configuration;
}

static MODULE_HANDLE Lwm2mServer_Create(BROKER_HANDLE broker, const void* configuration)
{
	IAGENT_HANDLE_DATA* result;

	set_broker_module_id(MODULE_LWM2M);
    DList_InitializeListHead(&(g_request_queue.list_queue));
    g_request_queue.thread_mutex = Lock_Init();
    //pthread_mutex_init(&(g_request_queue.thread_mutex), NULL);

    if (broker == NULL) /*configuration is not used*/
    {
        ERROR("invalid arg broker=%p", broker);
        result = NULL;
    }
    else
    {
    	set_handle_gw_broker(broker);

        result = malloc(sizeof(IAGENT_HANDLE_DATA));
        if(result == NULL)
        {
            ERROR("unable to malloc");
        }
        else
        {
            result->lockHandle = Lock_Init();
            if(result->lockHandle == NULL)
            {
                ERROR("unable to Lock_Init");
                free(result);
                result = NULL;
            }
            else
            {
                result->stopThread = 0;
                result->broker = broker;
				result->threadHandle = NULL;

				set_handle_data(result);
            }
        }
	}
    return result;
}

static void Lwm2mServer_Start(MODULE_HANDLE module)
{
	IAGENT_HANDLE_DATA* handleData = module;
	if (handleData != NULL)
	{
		if (Lock(handleData->lockHandle) != LOCK_OK)
		{
			ERROR("not able to Lock, still setting the thread to finish");
			handleData->stopThread = 1;
		}
		else
		{
			// Start lwm2m thread
			if (ThreadAPI_Create(&handleData->threadHandle, thread_lwm2m, handleData) != THREADAPI_OK)
			{
				ERROR("failed to spawn a thread");
				handleData->threadHandle = NULL;
			}
			(void)Unlock(handleData->lockHandle);
		}
	}
}

static void Lwm2mServer_Destroy(MODULE_HANDLE module)
{
    /*first stop the thread*/
	IAGENT_HANDLE_DATA* handleData = module;
    int notUsed;
    if (Lock(handleData->lockHandle) != LOCK_OK)
    {
        ERROR("not able to Lock, still setting the thread to finish");
        handleData->stopThread = 1;
    }
    else
    {
        handleData->stopThread = 1;
        Unlock(handleData->lockHandle);
    }

    if(ThreadAPI_Join(handleData->threadHandle, &notUsed) != THREADAPI_OK)
    {
        ERROR("unable to ThreadAPI_Join, still proceeding in _Destroy");
    }
    
    (void)Lock_Deinit(handleData->lockHandle);
    free(handleData);
}

static void Lwm2mServer_Receive(MODULE_HANDLE moduleHandle, MESSAGE_HANDLE messageHandle)
{
#ifdef RUN_ON_LINUX
    prctl (PR_SET_NAME, "lwm2m_receive");
#endif
	CONSTMAP_HANDLE properties = Message_GetProperties(messageHandle); /*by contract this is never NULL*/

	// check if the message is targeted to iagent
	const char* dest = ConstMap_GetValue(properties, XK_DEST);
	if(dest && strcmp(dest, MODULE_LWM2M) != 0) goto end;

	const char* tag = ConstMap_GetValue(properties, XK_TAG);
	if(!tag) goto end;

	const char* mid = ConstMap_GetValue(properties, XK_MID);
	if(mid == NULL) goto end;

	const char* uri = ConstMap_GetValue(properties, XK_URI);
    if (strcmp(tag,TAG_REST_RESP) && uri == NULL) goto end;

	const char* action = ConstMap_GetValue(properties, XK_ACTION);
	const char* response = ConstMap_GetValue(properties, XK_RESP_CODE);

	const CONSTBUFFER * content = Message_GetContent(messageHandle);

	if(strcmp(tag,TAG_REST_RESP) == 0 && (dest && strcmp(dest, MODULE_LWM2M) == 0))
	{
		int id = atoi(mid);

		if (response)
		{
			bh_feed_response(g_lwm2m_ctx, id, (void *)content->buffer, content->size, T_Broker_Message_Handle);
		}
	}
	else if(strcmp(tag,TAG_REST_REQ) == 0)
	{
		const char* uri = ConstMap_GetValue(properties, XK_URI);
		if (uri == NULL) goto end;

		if(!check_url_start(uri, strlen(uri), "/lwm2m"))
			goto end;

		const char* mid = ConstMap_GetValue(properties, XK_MID);
		if (mid == NULL) goto end;

		LWM2M_LOG_DATA("Receive bus message", content->buffer, content->size);

		// post it to the lwm2m2 thread for processing
		dlist_post(&g_request_queue, T_Bus_Message, Message_Clone(messageHandle), NULL);

		wakeup_lwm2m_thread();
	}


end:
	ConstMap_Destroy(properties);
}

static const MODULE_API_1 Lwm2mServer_APIS_all =
{
	{MODULE_API_VERSION_1},
	Lwm2mServer_ParseConfigurationFromJson,
	Lwm2mServer_FreeConfiguration,
	Lwm2mServer_Create,
	Lwm2mServer_Destroy,
	Lwm2mServer_Receive,
	Lwm2mServer_Start
};

#ifdef BUILD_MODULE_TYPE_STATIC
MODULE_EXPORT const MODULE_API* MODULE_STATIC_GETAPI(HELLOWORLD_MODULE)(MODULE_API_VERSION gateway_api_version)
#else
MODULE_EXPORT const MODULE_API* Module_GetApi(MODULE_API_VERSION gateway_api_version)
#endif
{
	const MODULE_API * api;
	if (gateway_api_version >= Lwm2mServer_APIS_all.base.version)
	{
		api= (const MODULE_API*)&Lwm2mServer_APIS_all;
	}
	else
	{
		api = NULL;
	}

	return api;
}
