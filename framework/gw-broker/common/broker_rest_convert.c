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
#include "broker_rest_convert.h"
#include "coap_ext.h"
#include "coap_request.h"
#include "logs.h"
#include "plugin_sdk.h"


bool setup_bus_restful_message(MESSAGE_CONFIG *msgConfig, char * tag, int fmt, char * url_path,
		char * query, int code, void * payload, int payload_len)
{
    const char * option_result = NULL;
    int len;
    char buffer[100];


    MAP_HANDLE propertiesMap = Map_Create(NULL);
    if(propertiesMap == NULL)
    {
        ERROR("unable to create a Map");
        goto end;
    }

    // set message tag
	if (Map_AddOrUpdate(propertiesMap, XK_TAG, tag) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate");
		goto end;
	}

	// set rest action
	if (strcmp(tag, TAG_REST_REQ) == 0 &&
	     Map_AddOrUpdate(propertiesMap, XK_ACTION, rest_action_str(code)) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate");
		goto end;
	}
	else if (strcmp(tag, TAG_REST_RESP) == 0)
	{
		sprintf(buffer, "%d", code);
		if(Map_AddOrUpdate(propertiesMap, XK_RESP_CODE, buffer) != MAP_OK)
		{
			ERROR("unable to Map_AddOrUpdate");
			goto end;
		}
	}

	// set uri

	if (url_path && Map_AddOrUpdate(propertiesMap, XK_URI, url_path) != MAP_OK)
	{
        ERROR("unable to Map_AddOrUpdate");
		goto end;
	}


	// set qeury
	if (query && Map_AddOrUpdate(propertiesMap, XK_QUERY, query) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate");
		goto end;
	}


	// set payload fmt
	if (fmt != -1 && Map_AddOrUpdate(propertiesMap, XK_FMT, rest_fmt_str(fmt,buffer)) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate");
		goto end;
	}

	// set source module
	if (Map_AddOrUpdate(propertiesMap, XK_SRC, get_broker_module_id()) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate");
		goto end;
	}

	// The message has only properties and no content.
	msgConfig->size = payload_len;
	msgConfig->source = payload;
	msgConfig->sourceProperties = propertiesMap;

	return true;


end:
	if(propertiesMap) Map_Destroy(propertiesMap);

    return false;
}

bool coap_to_broker_msg_config(coap_packet_t *coap_message, MESSAGE_CONFIG *msgConfig)
{
    const char * option_result = NULL;
    int len;
    char buffer[100];

    MAP_HANDLE propertiesMap = Map_Create(NULL);
    if(propertiesMap == NULL)
    {
        ERROR("unable to create a Map");
        goto end;
    }

    // set message tag
	if (Map_AddOrUpdate(propertiesMap, XK_TAG, coap_is_request(coap_message)?TAG_REST_REQ:TAG_REST_RESP) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate");
		goto end;
	}

	// set rest action
	if (Map_AddOrUpdate(propertiesMap, XK_ACTION, rest_action_str(coap_message->code)) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate");
		goto end;
	}

	// set uri
	len = coap_get_header_uri_path(coap_message, &option_result);

	if (len > 0 )
	{
	    // url may not end with 0
	    int len2 = len + 1/*add for end mark*/ + 1/*add for '/'*/;
		char * tmp = malloc(len2);
		tmp[0]='/';
		memcpy (tmp+1, option_result, len);
		tmp[len2-1] = 0;

		if (Map_AddOrUpdate(propertiesMap, XK_URI, tmp) != MAP_OK)
		{
			free(tmp);
			goto end;
		}
		free(tmp);
	}

	// set qeury
	len = coap_get_header_uri_query(coap_message, &option_result);
	if (len > 0 )
	{
		char * tmp = malloc(len+1);
		memcpy(tmp, option_result, len);
		tmp[len] = 0;
		if (Map_AddOrUpdate(propertiesMap, XK_QUERY, tmp) != MAP_OK)
		{
			free(tmp);
			ERROR("unable to Map_AddOrUpdate");
			goto end;
		}
		free(tmp);
	}

	// set payload fmt
	if (Map_AddOrUpdate(propertiesMap, XK_FMT, rest_fmt_str(coap_message->content_format,buffer)) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate");
		goto end;
	}

	// set source module
	if (Map_AddOrUpdate(propertiesMap, XK_SRC, get_broker_module_id()) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate");
		goto end;
	}

	msgConfig->size = coap_message->payload_len;
	msgConfig->source = coap_message->payload;
	msgConfig->sourceProperties = propertiesMap;

	return 1;

end:
	Map_Destroy(propertiesMap);

    return false;
}


MESSAGE_HANDLE coap_to_bus_msg(coap_packet_t *coap_message, unsigned long id, const char * dest_module)
{
    MESSAGE_HANDLE BrokerMessage = NULL;
    MESSAGE_CONFIG msgConfig = {0};

    if(!coap_to_broker_msg_config(coap_message, &msgConfig))
    	return NULL;

    char buf[20];
    snprintf(buf, sizeof(buf), "%ld", id);
	if (id != -1 && Map_AddOrUpdate(msgConfig.sourceProperties , XK_MID, buf) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate");
		goto end;
	}
	if (dest_module && Map_AddOrUpdate(msgConfig.sourceProperties , XK_DEST, dest_module) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate");
		goto end;
	}

	BrokerMessage = Message_Create(&msgConfig);
	if (BrokerMessage == NULL)
	{
		ERROR("unable to create \"hello world\" message");
		goto end;
	}

end:
	// The properities has been copied when called function "Message_Create", so, it should be release even the broker message created successfully
	if(msgConfig.sourceProperties)
		Map_Destroy(msgConfig.sourceProperties);

	return BrokerMessage;
}

// the caller should pay attention:
// the payload in coap_message actually referred from the broker_msg
bool convert_bus_msg_to_coap(MESSAGE_HANDLE brokre_msg, coap_packet_t *coap_message)
{
    const CONSTBUFFER * content = Message_GetContent(brokre_msg); /*by contract, this is never NULL*/
    CONSTMAP_HANDLE properties = Message_GetProperties(brokre_msg); /*by contract this is never NULL*/

    const char *tag = ConstMap_GetValue(properties, XK_TAG);
    if(!tag) goto end;
    const char *mid = ConstMap_GetValue(properties, XK_MID);
    if(mid == NULL) goto end;
    uint32_t id = atoi(mid);
    const char *uri = ConstMap_GetValue(properties, XK_URI);
    const char *query = ConstMap_GetValue(properties, XK_QUERY);
    const char *fmt = ConstMap_GetValue(properties, XK_FMT);
    const char *src = ConstMap_GetValue(properties, XK_SRC);

    if(strcmp(tag,TAG_REST_RESP) == 0)
    {
        const char* result = ConstMap_GetValue(properties, XK_RESP_CODE);
        if(result == NULL) goto end;
        coap_init_message(coap_message, COAP_TYPE_ACK, atoi(result), id);
    }
    else if(strcmp(tag,TAG_REST_REQ) == 0)
    {
        const char* action = ConstMap_GetValue(properties, XK_ACTION);
        if(action == NULL) goto end;
        int n_action = action_from_string((char *)action);
        if(n_action == -1) goto end;
        coap_init_message(coap_message, COAP_TYPE_CON, n_action, id);

        // 1) the ilink use coap over tcp which use token for match the request and response
        // 2) and the mid is 2 bytes only which can't match the transaction id in 4 bytes
        coap_set_token(coap_message, (const uint8_t *)&id, sizeof(id));
    }
    else
    {
        goto end;
    }

    if(fmt!=NULL)
    {
        coap_set_header_content_format(coap_message, atoi(fmt));

        // todo: to handle it more carefully for UDP and TCP respectively
        coap_set_payload_tcp(coap_message,content->buffer,content->size);
    }
    if (uri)
        coap_set_header_uri_path((void *)coap_message, uri);
    if (query)
        coap_set_header_uri_query((void *)coap_message, query);
    if (src)
        coap_set_header_uri_path((void *)coap_message, src);

    ConstMap_Destroy(properties);
    return true;

end:
    ConstMap_Destroy(properties);
    return false;
}


