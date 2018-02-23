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


#ifndef APPS_GW_BROKER_COMMON_MODULE_COMMON_H_
#define APPS_GW_BROKER_COMMON_MODULE_COMMON_H_

#include <errno.h>

#include "azure_c_shared_utility/base64.h"
#include "azure_c_shared_utility/constmap.h"
#include "azure_c_shared_utility/crt_abstractions.h"
#include "azure_c_shared_utility/gb_stdio.h"
#include "azure_c_shared_utility/gb_time.h"
#include "azure_c_shared_utility/gballoc.h"
#include "azure_c_shared_utility/map.h"
#include "azure_c_shared_utility/strings.h"
#include "azure_c_shared_utility/doublylinkedlist.h"
#include "azure_c_shared_utility/threadapi.h"
#include "azure_c_shared_utility/lock.h"

#include "message.h"
#include "broker.h"

#include "module_constants.h"

#ifdef __cplusplus
extern "C"
{
#endif




void set_handle_gw_broker(void * handle);
void set_handle_data(void * data);
char * get_broker_module_id();
void set_broker_module_id(char * module_id);

bool set_bus_message_property(MESSAGE_CONFIG *msgConfig, const char * key, const char * value);

bool get_payload(CONSTBUFFER * content, char ** payload);


void publish_message_cfg_on_broker(MESSAGE_CONFIG *msgConfig);


typedef struct IAGENT_HANDLE_DATA_TAG
{
    THREAD_HANDLE threadHandle;
    LOCK_HANDLE lockHandle;
    int stopThread;
    BROKER_HANDLE broker;

}IAGENT_HANDLE_DATA;


#ifdef __cplusplus
}
#endif

#endif /* APPS_GW_BROKER_COMMON_MODULE_COMMON_H_ */
