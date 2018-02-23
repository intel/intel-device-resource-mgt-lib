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
#include "module_common.h"
#include "logs.h"
#include "plugin_sdk.h"

#ifndef LOG_TAG
#define LOG_TAG "GW"
#endif


static char * g_module_id = NULL;
char * get_broker_module_id()
{
	return g_module_id;
}

/*if the payload buff if malloc by this functin, return 1,  */
bool get_payload(CONSTBUFFER * content, char ** payload)
{
    int payload_len = 0;

    if(content->size == 0)
    {
        *payload = NULL;
        return false;
    }

    if(content->buffer[content->size-1]!= 0)
        payload_len = content->size + 1;
    else
        payload_len = content->size;

    *payload = (char *)malloc(payload_len);
    if (*payload == NULL)
        return false;
    else
    {
        memcpy(*payload, content->buffer, content->size);
        (*payload)[payload_len - 1] = 0;
        return true;
    }
}

void set_broker_module_id(char * module_id)
{
	if(g_module_id)free(g_module_id);

	g_module_id = strdup(module_id);
}



bool set_bus_message_property(MESSAGE_CONFIG *msgConfig, const char * key, const char * value)
{
	MAP_HANDLE properties = msgConfig->sourceProperties; /*by contract this is never NULL*/
	if (Map_AddOrUpdate((MAP_HANDLE)properties, key, value) != MAP_OK)
	{
		return false;
	}
	return true;
}


static void * module_handle_data = NULL;
void set_handle_data(void * data)
{
	module_handle_data = data;
}

IAGENT_HANDLE_DATA * get_handle_data()
{
	return (IAGENT_HANDLE_DATA*) module_handle_data;
}

static void * module_handle_gw_broker = NULL;

void set_handle_gw_broker(void * handle)
{
	module_handle_gw_broker = handle;
}

void * get_handle_gw_broker()
{
	return module_handle_gw_broker;
}




void publish_message_on_broker(MESSAGE_HANDLE msg)
{
	IAGENT_HANDLE_DATA * handleData = get_handle_data();
    if (Lock(handleData->lockHandle) == LOCK_OK)
    {
		(void)Broker_Publish(get_handle_gw_broker(), (MODULE_HANDLE)get_handle_data(), msg);
		(void)Unlock(handleData->lockHandle);
    }
    else
    {
    	(void)Broker_Publish(get_handle_gw_broker(), (MODULE_HANDLE)get_handle_data(), msg);
    }
}

void publish_message_cfg_on_broker(MESSAGE_CONFIG *msgConfig)
{
	MESSAGE_HANDLE BrokerMessage = Message_Create(msgConfig);
	if (BrokerMessage == NULL)
	{
		ERROR("unable to create \"hello world\" message");
		if(msgConfig->sourceProperties)
			Map_Destroy(msgConfig->sourceProperties);

		return;
	}

	publish_message_on_broker(BrokerMessage);
	Message_Destroy(BrokerMessage);

    if (msgConfig->sourceProperties) Map_Destroy(msgConfig->sourceProperties);
}

