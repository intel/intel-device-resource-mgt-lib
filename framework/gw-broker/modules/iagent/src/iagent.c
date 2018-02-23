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
#include "iagent_base.h"
#include "agent_core_lib.h"
#include "iagent.h"
#include "rd.h"

//external
#include "rest-engine.h"

//azure
#include "azure_c_shared_utility/threadapi.h"
#include "azure_c_shared_utility/lock.h"
#include "broker_rest_convert.h"
#include <parson.h>

extern int thread_iagent_core(void * param);
extern char * get_module_path(void* address);
extern char g_quit_thread;

#define IAGENT_MESSAGE "iagent"

static MODULE_HANDLE Iagent_Create(BROKER_HANDLE broker, const void* configuration)
{
	set_broker_module_id(MODULE_AGENT);

    IAGENT_HANDLE_DATA* result;
    if ((broker == NULL))
    {
        ERROR("invalid arg broker=%p", broker);
        result = NULL;
    }
    else
    {
        set_handle_gw_broker((void *)broker);

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
        init_agent();
    }
    return result;
}

static void* Iagent_ParseConfigurationFromJson(const char* configuration)
{
    (void)configuration;
    return NULL;
}

static void Iagent_FreeConfiguration(void* configuration)
{
    (void)configuration;
}

static void Iagent_Start(MODULE_HANDLE module)
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
            if (ThreadAPI_Create(&handleData->threadHandle, thread_iagent_core, handleData) != THREADAPI_OK)
            {
                ERROR("failed to spawn a thread");
                handleData->threadHandle = NULL;
            }
            (void)Unlock(handleData->lockHandle);
        }
    }
}

static void Iagent_Destroy(MODULE_HANDLE module)
{
    /*first stop the thread*/
    IAGENT_HANDLE_DATA* handleData = module;
    int notUsed;

    g_quit_thread = 1;
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

static void Iagent_Receive(MODULE_HANDLE moduleHandle, MESSAGE_HANDLE messageHandle)
{
#ifdef RUN_ON_LINUX
    prctl (PR_SET_NAME, "iagent_receive");
#endif
    CONSTMAP_HANDLE properties = Message_GetProperties(messageHandle); /*by contract this is never NULL*/

    // check if the message is targeted to iagent
    const char* dest = ConstMap_GetValue(properties, XK_DEST);
    if(dest && strcmp(dest, MODULE_AGENT) != 0)
        goto end;

    const char* tag = ConstMap_GetValue(properties, XK_TAG);
    if(!tag) goto end;

    const char* mid = ConstMap_GetValue(properties, XK_MID);
    const char* uri = ConstMap_GetValue(properties, XK_URI);
    if (strcmp(tag,TAG_REST_RESP) && uri == NULL) goto end;

    const char* action = ConstMap_GetValue(properties, XK_ACTION);
    const char* sfmt = ConstMap_GetValue(properties, XK_ACTION);

    const CONSTBUFFER * content = Message_GetContent(messageHandle);
    if(strcmp(tag,TAG_EVENT) == 0)
    {
        if(check_url_start(uri, strlen(uri), "/rd/"))
        {
            agent_post_message_handler(handle_bus_rd_event, Message_Clone(messageHandle));
        }
        else if(check_url_start(uri, strlen(uri),"/dp/"))
        {
            agent_post_message_handler(handle_data_point, Message_Clone(messageHandle));
        }
        goto end;
    }

    else if(strcmp(tag,TAG_REST_RESP) == 0)
    {
    	// response is not for me.
    	if(strcmp(dest, MODULE_AGENT) != 0)
    		goto end;

        if(!mid) goto end;
        int id = atoi(mid);

        TraceI(FLAG_DUMP_MESSAGE, "iagent recieved response. mid=%s, payload len=%d", mid, content->size);

        // trigger the callback function cb_foward_response_to_client()
        bh_feed_response(get_outgoing_requests_ctx(), id, messageHandle, 0, T_Broker_Message_Handle);

    }
    else if(strcmp(tag,TAG_REST_REQ) == 0)
    {
    	if(!mid) goto end;
    	uint32_t id = (uint32_t) atoi(mid);

    	const char* src = ConstMap_GetValue(properties, XK_SRC);
        if(check_url_start(uri, strlen(uri), "/rd/"))
        {
        	if(check_url_start(uri, strlen(uri), "/rd/monitor")  && strcmp(action, ACTION_POST) == 0 )
		    {
			   agent_post_message_handler(handle_bus_rd_monitor_post, Message_Clone(messageHandle));
		    }
        	else if(strcmp(action, ACTION_POST) == 0 || strcmp(action, ACTION_PUT) == 0)
            {
                agent_post_message_handler(handle_bus_rd_event, Message_Clone(messageHandle));
            }
            else if(strcmp(action, ACTION_GET) == 0)
            {
                agent_post_message_handler(handle_bus_rd_get, Message_Clone(messageHandle));
            }
            else if(strcmp(action, ACTION_DEL) == 0)
            {
                agent_post_message_handler(handle_bus_rd_delete, Message_Clone(messageHandle));
            }

        }
        else if(check_url_start(uri, strlen(uri), "/cal/"))
        {
            agent_post_message_handler(handle_bus_calibration, Message_Clone(messageHandle));
        }
        else if(check_url_start(uri, strlen(uri),  "/refresher"))
        {
            agent_post_message_handler(handle_refresher_data, Message_Clone(messageHandle));
        }
        else if(check_url_start(uri, strlen(uri),  "/ep/"))
        {
        	char ep_name[100];
        	char * p = strchr(uri + 4, '/');
        	if(p )
        	{
        		int len = (p - uri - 4 );
        		if(len >= sizeof(ep_name)) goto end;
        		memcpy(ep_name, uri + 4, len);
        		ep_name[len] = 0;
        		handle_bus_endpoint(messageHandle, ep_name, (char *)(p+1), id, (char *)src);
        	}
        }
#ifdef BUILTIN_IBROKER
        else if(check_url_start(uri, strlen(uri),  "/ibroker/"))
        {
            const char *tm = ConstMap_GetValue(properties, XK_TM);
        	handle_bus_ibroker(messageHandle, (char *)(uri+9), (char *)src, (char*)tm);
        }
#endif
        else if(find_restful_service(uri, strlen(uri)))
        {
            handle_bus_default(messageHandle);
        }
    }

end:
    ConstMap_Destroy(properties);
}

static const MODULE_API_1 Iagent_APIS_all =
{
    {MODULE_API_VERSION_1},
    Iagent_ParseConfigurationFromJson,
    Iagent_FreeConfiguration,
    Iagent_Create,
    Iagent_Destroy,
    Iagent_Receive,
    Iagent_Start
};
#ifdef BUILD_MODULE_TYPE_STATIC
MODULE_EXPORT const MODULE_API* MODULE_STATIC_GETAPI(IAGENT_MODULE)(MODULE_API_VERSION gateway_api_version)
#else
MODULE_EXPORT const MODULE_API* Module_GetApi(MODULE_API_VERSION gateway_api_version)
#endif
{
    const MODULE_API * api;
    if (gateway_api_version >= Iagent_APIS_all.base.version)
    {
        api= (const MODULE_API*)&Iagent_APIS_all;
    }
    else
    {
        api = NULL;
    }

    return api;
}

