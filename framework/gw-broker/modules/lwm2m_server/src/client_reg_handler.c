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


#include "lwm2m_server.h"

extern char * generate_register_payload(lwm2m_client_t * client, int * payload_len);
extern sync_ctx_t* g_lwm2m_ctx;

enum {
	CT_LINK = 1540,
	CT_TEXT,
	CT_TLV,
	CT_JSON,
	CT_OPAQUE
};

static int cb_lwm2m_response(void * ctx_data, void * data, int len, unsigned char format)
{
    // todo:
	fprintf(stdout, "> \nRD post success. \n");

    return 0;
}

void prv_monitor_callback(uint16_t clientID,
                                 lwm2m_uri_t * uriP,
                                 int status,
                                 lwm2m_media_type_t format,
                                 uint8_t * data,
                                 int dataLength,
                                 void * userData)
{
    lwm2m_context_t * lwm2mH = (lwm2m_context_t *) userData;
    lwm2m_client_t * targetP;
	MAP_HANDLE propertiesMap = NULL;
	MESSAGE_CONFIG msgConfig;
	char *payload = NULL;
    char *action = "";
	int payload_len = 0;
    propertiesMap = Map_Create(NULL);

    char query [200] ={0};

    if(propertiesMap == NULL)
    {
        ERROR("prv_monitor_callback: unable to create a Map");
        return;
    }


    switch (status)
    {
    case COAP_201_CREATED:

        targetP = (lwm2m_client_t *)lwm2m_list_find((lwm2m_list_t *)lwm2mH->clientList, clientID);
        WARNING("New client [%s] #%d registered.", targetP->name, clientID);
        snprintf(query, sizeof(query), "di=%s", targetP->name);

        //prv_dump_client(targetP);

        set_client_name(clientID, targetP->name);

        action = ACTION_PUT;
        format = CT_JSON;

        // compose the payload of rd registration
        payload = generate_register_payload(targetP, &payload_len);
        LWM2M_LOG_DATA("rd payload:", payload, payload_len);
        break;

    case COAP_202_DELETED:
    	action = ACTION_DEL;
        targetP = (lwm2m_client_t *)lwm2m_list_find((lwm2m_list_t *)lwm2mH->clientList, clientID);
        char * name = get_client_name(clientID);
        if(targetP)
        {
            name = targetP->name;
        }

        if(name)
        {
            snprintf(query, sizeof(query), "di=%s", name);
        }

        WARNING("Client [%s] #%d unregistered.", name?name:"", clientID);

    	del_client_name(clientID);
        break;

    case COAP_204_CHANGED:

        action = ACTION_POST;
        targetP = (lwm2m_client_t *)lwm2m_list_find((lwm2m_list_t *)lwm2mH->clientList, clientID);

    	TraceI(FLAG_LWM2M_LOG, "Client #%d [%s] updated.", clientID, targetP->name);

        snprintf(query, sizeof(query), "di=%s", targetP->name);

        //prv_dump_client(targetP);
        break;

    default:
    	ERROR( "Monitor callback called with an unknown status: %d.", status);
        break;
    }



	if (Map_AddOrUpdate(propertiesMap, XK_TAG, TAG_REST_REQ) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate, [%s]-line[%s]", __FUNCTION__, __LINE__);
		goto end;
	}

	if (Map_AddOrUpdate(propertiesMap, XK_ACTION, action) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate, [%s]-line[%s]", __FUNCTION__, __LINE__);
		goto end;
	}

	if (Map_AddOrUpdate(propertiesMap, XK_SRC, MODULE_LWM2M) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate, [%s]-line[%s]", __FUNCTION__, __LINE__);
		goto end;
	}

	char str[40];
	sprintf(str, "%d", format);
	if (Map_AddOrUpdate(propertiesMap, XK_FMT, str) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate, [%s]-line[%s]", __FUNCTION__, __LINE__);
		goto end;
	}


	if (Map_AddOrUpdate(propertiesMap, XK_URI, "/rd") != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate, [%s]-line[%s]", __FUNCTION__, __LINE__);
		goto end;
	}

	if (query[0] && Map_AddOrUpdate(propertiesMap, XK_QUERY, query) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate, [%s]-line[%s]", __FUNCTION__, __LINE__);
		goto end;
	}

	int id = bh_gen_id(g_lwm2m_ctx);
	char mid[20];
	sprintf(mid, "%d", id);
	if (Map_AddOrUpdate(propertiesMap, XK_MID, mid) != MAP_OK)
	{
		ERROR("unable to Map_AddOrUpdate");
		goto end;
	}

	msgConfig.size = payload_len;
	msgConfig.source = payload;

	msgConfig.sourceProperties = propertiesMap;

	MESSAGE_HANDLE response_Message = Message_Create(&msgConfig);
	if (response_Message == NULL)
	{
		ERROR("unable to create response_Message message");
		goto end;
	}

	//response handle
	bh_wait_response_async(g_lwm2m_ctx,
			id,
			cb_lwm2m_response,
			NULL, 10 * 1000, NULL);

	publish_message_on_broker(response_Message);
	Message_Destroy(response_Message);

end:
    if (payload) free (payload);
    if(propertiesMap) Map_Destroy(propertiesMap);
	return;
}


