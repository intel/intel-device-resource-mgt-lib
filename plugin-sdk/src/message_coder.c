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


#include "plugin_sdk.h"
#include "agent_core_lib.h"
#define ERROR printf



char * REST_ACTION_STR[] =
{
		ACTION_GET,
		ACTION_POST,
		ACTION_PUT,
		ACTION_DEL
};

char * rest_action_str(int action)
{
	if (action >= T_Get && action<=T_Del)
		return REST_ACTION_STR[action - T_Get];

	return "unknown";
}

int action_from_string(char* action)
{
	for(int i=0;i<COUNT_OF(REST_ACTION_STR);i++)
	{
		if(strcmp(REST_ACTION_STR[i], action) == 0)
			return (i + T_Get);
	}
	return -1;
}

char * rest_fmt_str(int fmt, char * buffer)
{
	static char internal[30];
	if(buffer == NULL) buffer = &internal[0];
	sprintf(buffer, "%d", fmt);
	return buffer;
}


bool decode_request(MESSAGE_HANDLE messageHandle, restful_request_t * request)
{
	bool ret = true;
	CONSTMAP_HANDLE properties = Message_GetProperties(messageHandle); /*by contract this is never NULL*/

	request->url = (char*)ConstMap_GetValue(properties, XK_URI);
	if(request->url == NULL) ret = false;

	const CONSTBUFFER * content = Message_GetContent(messageHandle);
	request->payload = (char*)content->buffer;
	request->payload_len = content->size;

	request->src_module = (char*)ConstMap_GetValue(properties, XK_SRC);

	const char * fmt = ConstMap_GetValue(properties, XK_FMT);
	if(fmt)
		request->payload_fmt = atoi(fmt);
	else
	{
		request->payload_fmt = -1;
		ret = false;
	}

	request->query = (char*)ConstMap_GetValue(properties, XK_QUERY);
	const char* msg_action = ConstMap_GetValue(properties, XK_ACTION);
	if(msg_action != NULL)
	{
		request->action = action_from_string((char*)msg_action);
	}
	else
	{
		request->action = -1;
		ret = false;
	}
	const char* mid = ConstMap_GetValue(properties, XK_MID);
	if(mid)
	{
		request->mid = atoi(mid);
	}
	else
	{
		request->mid = -1;
	}
	ConstMap_Destroy(properties);

	return ret;
}

bool decode_response(MESSAGE_HANDLE messageHandle, restful_response_t * response)
{
	CONSTMAP_HANDLE properties = Message_GetProperties(messageHandle); /*by contract this is never NULL*/
	const char* mid = ConstMap_GetValue(properties, XK_MID);
	if(mid) response->mid = atoi(mid);
	const CONSTBUFFER * content = Message_GetContent(messageHandle);
	response->payload = (char*)content->buffer;
	response->payload_len = content->size;
	response->dest_module = (char*)ConstMap_GetValue(properties, XK_DEST);

	const char* format = ConstMap_GetValue(properties, XK_FMT);
	if(format != NULL)
		response->payload_fmt = atoi(format);
	else
		response->payload_fmt = -1;


	ConstMap_Destroy(properties);

	return true;
}


MESSAGE_HANDLE encode_response(restful_response_t * response)
{
	MESSAGE_HANDLE messageHandle = NULL;
    int len;
    char buffer[100];
    MESSAGE_CONFIG msgConfig[1];

    MAP_HANDLE propertiesMap = Map_Create(NULL);
    if(propertiesMap == NULL)
    {
        ERROR("unable to create a Map");
        goto end;
    }

    // set message tag
	if (Map_AddOrUpdate(propertiesMap, XK_TAG, TAG_REST_RESP) != MAP_OK)
	{
		ERROR("unable to set TAG_REST_RESP");
		goto end;
	}


	sprintf(buffer, "%d", response->code);
	if(Map_AddOrUpdate(propertiesMap, XK_RESP_CODE, buffer) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate");
		goto end;
	}


    sprintf(buffer, "%d", response->mid);
    if(Map_AddOrUpdate(propertiesMap, XK_MID, buffer) != MAP_OK)
    {
        ERROR("unable to Map_AddOrUpdate");
        goto end;
    }

	// set payload fmt
	if (response->payload_fmt != -1 && Map_AddOrUpdate(propertiesMap, XK_FMT, rest_fmt_str(response->payload_fmt,buffer)) != MAP_OK)
	{
		ERROR("unable to set payload format\n");
		goto end;
	}

	// set source module
	if (response->dest_module && Map_AddOrUpdate(propertiesMap, XK_DEST, response->dest_module) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate");
		goto end;
	}

	// The message has only properties and no content.
	msgConfig->size = response->payload_len;
	msgConfig->source = response->payload;
	msgConfig->sourceProperties = propertiesMap;

	messageHandle = Message_Create((const MESSAGE_CONFIG *)msgConfig);
	if (messageHandle == NULL)
	{
		ERROR("unable to create  message\n");
		goto end;
	}


end:
	if(propertiesMap) Map_Destroy(propertiesMap);

	return messageHandle;
}


MESSAGE_HANDLE encode_request(restful_request_t * request)
{
	MESSAGE_HANDLE messageHandle = NULL;
    int len;
    char buffer[100];
    MESSAGE_CONFIG msgConfig[1];

    MAP_HANDLE propertiesMap = Map_Create(NULL);
    if(propertiesMap == NULL)
    {
        ERROR("unable to create a Map");
        goto end;
    }

    // set message tag
	if (Map_AddOrUpdate(propertiesMap, XK_TAG, TAG_REST_REQ) != MAP_OK)
	{
		ERROR("unable to set TAG_REST_RESP");
		goto end;
	}


	// set payload fmt
	if (request->payload_fmt != -1 && Map_AddOrUpdate(propertiesMap, XK_FMT, rest_fmt_str(request->payload_fmt,buffer)) != MAP_OK)
	{
		ERROR("unable to set payload format\n");
		goto end;
	}

	// set source module
	if (request->src_module && Map_AddOrUpdate(propertiesMap, XK_SRC, request->src_module) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate");
		goto end;
	}

    sprintf(buffer, "%d", request->mid);
    if(Map_AddOrUpdate(propertiesMap, XK_MID, buffer) != MAP_OK)
    {
        ERROR("unable to Map_AddOrUpdate");
        goto end;
    }

	// set uri

	if (request->url && Map_AddOrUpdate(propertiesMap, XK_URI, request->url) != MAP_OK)
	{
        ERROR("unable to Map_AddOrUpdate");
		goto end;
	}

	// set qeury
	if (request->query && Map_AddOrUpdate(propertiesMap, XK_QUERY, request->query) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate");
		goto end;
	}

	if ( Map_AddOrUpdate(propertiesMap, XK_ACTION, rest_action_str(request->action)) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate");
		goto end;
	}

	// The message has only properties and no content.
	msgConfig->size = request->payload_len;
	msgConfig->source = request->payload;
	msgConfig->sourceProperties = propertiesMap;

	messageHandle = Message_Create((const MESSAGE_CONFIG *)msgConfig);
	if (messageHandle == NULL)
	{
		ERROR("unable to create  message\n");
		goto end;
	}


end:
	if(propertiesMap) Map_Destroy(propertiesMap);

	return messageHandle;
}



MESSAGE_HANDLE encode_event(bus_event_t * request)
{
    MESSAGE_HANDLE messageHandle = NULL;
    int len;
    char buffer[100];
    MESSAGE_CONFIG msgConfig[1];

    MAP_HANDLE propertiesMap = Map_Create(NULL);
    if(propertiesMap == NULL)
    {
        ERROR("unable to create a Map");
        goto end;
    }

    // set message tag
    if (Map_AddOrUpdate(propertiesMap, XK_TAG, TAG_EVENT) != MAP_OK)
    {
        ERROR("unable to set TAG_REST_RESP");
        goto end;
    }


    // set payload fmt
    if (request->payload_fmt != -1 && Map_AddOrUpdate(propertiesMap, XK_FMT, rest_fmt_str(request->payload_fmt,buffer)) != MAP_OK)
    {
        ERROR("unable to set payload format\n");
        goto end;
    }

    // set source module
    if (request->src_module && Map_AddOrUpdate(propertiesMap, XK_SRC, request->src_module) != MAP_OK)
    {
        ERROR("unable to Map_AddOrUpdate");
        goto end;
    }


    // set uri

    if (request->url && Map_AddOrUpdate(propertiesMap, XK_URI, request->url) != MAP_OK)
    {
        ERROR("unable to Map_AddOrUpdate");
        goto end;
    }

    // set qeury
    if (request->query && Map_AddOrUpdate(propertiesMap, XK_QUERY, request->query) != MAP_OK)
    {
        ERROR("unable to Map_AddOrUpdate");
        goto end;
    }


    // The message has only properties and no content.
    msgConfig->size = request->payload_len;
    msgConfig->source = request->payload;
    msgConfig->sourceProperties = propertiesMap;

    messageHandle = Message_Create((const MESSAGE_CONFIG *)msgConfig);
    if (messageHandle == NULL)
    {
        ERROR("unable to create  message\n");
        goto end;
    }


end:
    if(propertiesMap) Map_Destroy(propertiesMap);

    return messageHandle;
}



bool decode_event(MESSAGE_HANDLE messageHandle, bus_event_t * request)
{
    bool ret = true;
    CONSTMAP_HANDLE properties = Message_GetProperties(messageHandle); /*by contract this is never NULL*/

    request->url = (char*)ConstMap_GetValue(properties, XK_URI);
    if(request->url == NULL) ret = false;

    const CONSTBUFFER * content = Message_GetContent(messageHandle);
    request->payload = (char*)content->buffer;
    request->payload_len = content->size;

    request->src_module = (char*)ConstMap_GetValue(properties, XK_SRC);

    const char * fmt = ConstMap_GetValue(properties, XK_FMT);
    if(fmt)
        request->payload_fmt = atoi(fmt);
    else
    {
        request->payload_fmt = -1;
        ret = false;
    }

    request->query = (char*)ConstMap_GetValue(properties, XK_QUERY);

    ConstMap_Destroy(properties);

    return ret;
}

