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

#include "../inc/database_module.h"
#include "dp_cache_server.h"

//azure
#include "azure_c_shared_utility/threadapi.h"
#include "azure_c_shared_utility/lock.h"
#include "broker_rest_convert.h"
#include <parson.h>

extern char g_quit_thread;
extern char * get_module_path(void* address);

#define DATABASE_MESSAGE "database"

static MODULE_HANDLE Database_Create(BROKER_HANDLE broker, const void* configuration)
{
	set_broker_module_id(MODULE_DATABASE);

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

        dp_cache_db_server_init();
    }
    return result;
}

static void* Database_ParseConfigurationFromJson(const char* configuration)
{
    (void)configuration;
    return NULL;
}

static void Database_FreeConfiguration(void* configuration)
{
    (void)configuration;
}

static void Database_Start(MODULE_HANDLE module)
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
            if (ThreadAPI_Create(&handleData->threadHandle, thread_dp_cache_db_server, handleData) != THREADAPI_OK)
            {
                ERROR("failed to spawn a thread");
                handleData->threadHandle = NULL;
            }
            (void)Unlock(handleData->lockHandle);
        }
    }
}

static void Database_Destroy(MODULE_HANDLE module)
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

static void Database_Receive(MODULE_HANDLE moduleHandle, MESSAGE_HANDLE messageHandle)
{
#ifdef RUN_ON_LINUX
    prctl (PR_SET_NAME, "database_receive");
#endif
    CONSTMAP_HANDLE properties = Message_GetProperties(messageHandle); /*by contract this is never NULL*/
    time_t tm;
    int fmt;

    // check if the message is targeted to database
    const char* dest = ConstMap_GetValue(properties, XK_DEST);
    const char* tag = ConstMap_GetValue(properties, XK_TAG);
    if(!tag) goto end;

    const char* mid = ConstMap_GetValue(properties, XK_MID);
    const char* uri = ConstMap_GetValue(properties, XK_URI);
    if (strcmp(tag,TAG_REST_RESP) && uri == NULL) goto end;

    const char *query = ConstMap_GetValue(properties, XK_QUERY);
    const char *action = ConstMap_GetValue(properties, XK_ACTION);
    const char *sfmt = ConstMap_GetValue(properties, XK_FMT);
    if(sfmt) fmt = atoi(sfmt);

    const char *stime = ConstMap_GetValue(properties, XK_TM);
    if (stime) tm = atol(stime);

    const CONSTBUFFER * content = Message_GetContent(messageHandle);

    if(strcmp(tag, TAG_REST_RESP) == 0)
    {
        // todo:
    }
    else if (strcmp(tag, TAG_REST_REQ) == 0)
    {
        if(mid == NULL || uri == NULL) goto end;
        uint32_t id = (uint32_t) atoi(mid);

        const char* src = ConstMap_GetValue(properties, XK_SRC);
        if (check_url_start(uri, strlen(uri), "/dp-cache"))
        {
            if (strcmp(action, ACTION_PUT) == 0 || strcmp(action, ACTION_POST) == 0)
                add_dp_cache_data(uri+9, fmt, tm, content->size, content->buffer);
            else if (strcmp(action, ACTION_GET) == 0)
            {
                // todo:
            }
        }
        else if (check_url_start(uri, strlen(uri), "/db"))
        {
            // todo:
        }
    }
    else if(strcmp(tag, TAG_EVENT) == 0)
    {
        if (check_url_start(uri, strlen(uri), "/ilink"))
        {
            operate_broad_msg(content->size, content->buffer);
        }
    }

end:
    ConstMap_Destroy(properties);
}

static const MODULE_API_1 Database_APIS_all =
{
    {MODULE_API_VERSION_1},
    Database_ParseConfigurationFromJson,
    Database_FreeConfiguration,
    Database_Create,
    Database_Destroy,
    Database_Receive,
    Database_Start
};
#ifdef BUILD_MODULE_TYPE_STATIC
MODULE_EXPORT const MODULE_API* MODULE_STATIC_GETAPI(DATABASE_MODULE)(MODULE_API_VERSION gateway_api_version)
#else
MODULE_EXPORT const MODULE_API* Module_GetApi(MODULE_API_VERSION gateway_api_version)
#endif
{
    const MODULE_API * api;
    if (gateway_api_version >= Database_APIS_all.base.version)
    {
        api= (const MODULE_API*)&Database_APIS_all;
    }
    else
    {
        api = NULL;
    }

    return api;
}

