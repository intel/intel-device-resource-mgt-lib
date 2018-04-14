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
 * NOTE: Don't
 *
 */


#include <stdlib.h>
#ifdef _CRTDBG_MAP_ALLOC
#include <crtdbg.h>
#endif

#include "module.h"
#include "plugin_sdk.h"
#include "sdk_internals.h"

static MODULE_API_1 * UserModuleCallback = NULL;

#define LOG_TAG "IDRM"

int IDRM_WorkingThread(void *param)
{
    IDRM_MOD_HANDLE_DATA* handleData = param;
    int result;
    framework_ctx_t *ctx = (framework_ctx_t*) handleData->framework;
    if(ctx->module_name) prctl (PR_SET_NAME, ctx->module_name);

    while (handleData->stopThread == 0)
    {
        ///
        /// Step 4: run the real message processing in the working thread
        ///
    	uint32_t next_expiry_ms = idrm_process_in_working_thread(ctx);
        if(next_expiry_ms == -1)
        {
        	next_expiry_ms = 30;
        }

        Condition_Wait(ctx->g_working_thread_cond, ctx->g_working_thread_lock, next_expiry_ms);
    }

    return 0;
}


static MODULE_HANDLE IDRM_Create(BROKER_HANDLE broker, const void* configuration)
{
    IDRM_MOD_HANDLE_DATA* result;

    UserModuleCallback = on_get_user_module_apis();

    if(on_get_module_name() == NULL)
    {
        printf("IDRM_Create: on_get_module_name() returned NULL, failed to create\n");
        return NULL;
    }

    if(broker == NULL) /*configuration is not used*/
    {
        ERROR("invalid arg broker=%p", broker);
        result = NULL;
    }
    else
    {
        result = malloc(sizeof(IDRM_MOD_HANDLE_DATA));
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
            }

            ///
            /// Step 1: initialize the restful service framework
            ///
            result->framework = idrm_init_restful_framework(on_get_module_name(), result, broker, true);

            printf("%s: init restful framework success\n", on_get_module_name());

            if(result->framework) on_init_idrm_plugin(result->framework);

            if(UserModuleCallback && UserModuleCallback->Module_Create)
            	UserModuleCallback->Module_Create(broker, configuration);
        }
    }
    return result;
}

static void* IDRM_ParseConfigurationFromJson(const char* configuration)
{
	if(UserModuleCallback && UserModuleCallback->Module_ParseConfigurationFromJson)
		return UserModuleCallback->Module_ParseConfigurationFromJson(configuration);

    return NULL;
}

static void IDRM_FreeConfiguration(void* configuration)
{
	if(UserModuleCallback && UserModuleCallback->Module_FreeConfiguration)
		UserModuleCallback->Module_FreeConfiguration(configuration);
}

static void IDRM_Start(MODULE_HANDLE module)
{
    IDRM_MOD_HANDLE_DATA* handleData = module;
    prctl (PR_SET_NAME, on_get_module_name());

    if (handleData != NULL)
    {
        if (Lock(handleData->lockHandle) != LOCK_OK)
        {
            ERROR("not able to Lock, still setting the thread to finish");
            handleData->stopThread = 1;
        }
        else
        {
            if (ThreadAPI_Create(&handleData->threadHandle, IDRM_WorkingThread, handleData) != THREADAPI_OK)
            {
                ERROR("failed to spawn a thread");
                handleData->threadHandle = NULL;
            }
            (void)Unlock(handleData->lockHandle);
        }
    }

    if(UserModuleCallback && UserModuleCallback->Module_Start)
    	UserModuleCallback->Module_Start(module);
}


static void IDRM_Destroy(MODULE_HANDLE module)
{
    /*first stop the thread*/
    IDRM_MOD_HANDLE_DATA* handleData = module;
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

    idrm_wakeup_working_thread(handleData->framework);

    if (handleData->threadHandle != NULL &&
        ThreadAPI_Join(handleData->threadHandle, &notUsed) != THREADAPI_OK)
    {
        ERROR("unable to ThreadAPI_Join, still proceeding in _Destroy");
    }

    if(UserModuleCallback && UserModuleCallback->Module_Destroy)
    	UserModuleCallback->Module_Destroy(module);

    idrm_cleanup_restful_framework(handleData->framework);

    (void)Lock_Deinit(handleData->lockHandle);
    free(handleData);

}

static void IDRM_Receive(MODULE_HANDLE moduleHandle, MESSAGE_HANDLE messageHandle)
{

	if(idrm_handle_bus_message(moduleHandle, messageHandle))
		return;

	if(UserModuleCallback && UserModuleCallback->Module_Receive)
		UserModuleCallback->Module_Receive(moduleHandle, messageHandle);
}

static const MODULE_API_1 IDRM_API_all =
{
    {MODULE_API_VERSION_1},
    IDRM_ParseConfigurationFromJson,
	IDRM_FreeConfiguration,
    IDRM_Create,
    IDRM_Destroy,
    IDRM_Receive,
    IDRM_Start
};


MODULE_EXPORT const MODULE_API* Module_GetApi(MODULE_API_VERSION gateway_api_version)
{
    const MODULE_API * api;
    if (gateway_api_version >= IDRM_API_all.base.version)
    {
        api= (const MODULE_API*)&IDRM_API_all;
    }
    else
    {
        api = NULL;
    }
    return api;
}

