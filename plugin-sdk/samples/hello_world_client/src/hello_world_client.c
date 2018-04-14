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
#include "plugin_sdk.h"

#include "module_entry.h"
extern void hello_response_callback(restful_response_t *response, void * user_data);

int client_WorkingThread(void *param)
{
    static int mid = 0;

    while(1)
    {
        sleep(5);

        restful_request_t request = {0};
        time_t * user_data = (time_t *) malloc(sizeof(time_t));
        time(user_data);
        request.mid = mid ++;
        request.action = T_Get;
        request.url = "/hello";

        printf("client: sending the request\n");
        idrm_request_bus_service(&request, hello_response_callback, user_data, 100);


    }

    return 0;
}

void on_init_idrm_plugin(void * framework)
{

    THREAD_HANDLE threadHandle;

    ThreadAPI_Create(&threadHandle, client_WorkingThread, NULL);


    return;
}


const MODULE_API_1 * on_get_user_module_apis()
{
    return NULL;
}

char * on_get_module_name()
{
    return "hello_client";
}


