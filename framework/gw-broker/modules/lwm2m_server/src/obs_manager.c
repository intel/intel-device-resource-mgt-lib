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

static obs_user_data_t * g_obs_user_data = NULL;




obs_user_data_t * get_obs_user_data()
{
	return g_obs_user_data;
}


obs_user_data_t * find_obs_user_data(const char * ep, const char * url)
{
	obs_user_data_t * node = get_obs_user_data();

	while(node)
	{
		if(strcmp(ep, node->client_uuid) == 0 && strcmp(url, node->uri) == 0)
			return node;

		node = node->next;
	}

	return NULL;
}

void remove_obs_user_data(obs_user_data_t * node)
{
	obs_user_data_t * current = g_obs_user_data;
	obs_user_data_t * prev = NULL;

	while(current)
	{
		if(node == current)
		{
			if(prev)
				prev->next = current->next;
			else
				g_obs_user_data = current->next;

			return;
		}
		prev = current;
		current = current->next;
	}
}

void release_obs_user_data(obs_user_data_t *obs_ctx)
{
	if(obs_ctx->client_uuid) free(obs_ctx->client_uuid);
	if(obs_ctx->publish_point) free(obs_ctx->publish_point);
	if(obs_ctx->request_id) free(obs_ctx->request_id);
	if(obs_ctx->requester) free(obs_ctx->requester);
	if(obs_ctx->uri) free(obs_ctx->uri);

	free(obs_ctx);
}

obs_user_data_t * new_obs_user_data(const char * ep, const char * url)
{
	obs_user_data_t * node = (obs_user_data_t *)malloc(sizeof(obs_user_data_t));
	if(node == NULL)
		return NULL;

	memset(node, 0, sizeof(*node));
	node->client_uuid = strdup(ep);
	node->uri = strdup(url);
	node->status = S_Not_Observed;

	node->next = g_obs_user_data;
	g_obs_user_data = node;

	return node;
}

// todo: implement it
// return: the length of new payload. 0 - conversion failed
int  tlv_to_json(char * payload, int len, char ** new_payload)
{

	return 0;
}

void obs_notify_callback(uint16_t clientID,
                                lwm2m_uri_t * uriP,
                                int count,
                                lwm2m_media_type_t format,
                                uint8_t * data,
                                int dataLength,
                                void * userData)
{
	MAP_HANDLE propertiesMap = NULL;
	MESSAGE_CONFIG msgConfig;
	char * orginal_payload = NULL;
	char uri_str[100] = {0};
	char * client_name = "";
	uri_depth_t  depth = 0;
	uri_toString(uriP, uri_str, sizeof(uri_str), &depth);

	lwm2m_client_t * clientP = getClientbyID(get_lwm2m_context(), clientID);
	if(clientP) client_name = clientP->name;

    TraceI(FLAG_LWM2M_LOG, "notify from device: [%s] #%d, uri=%s number=%d",
            client_name, clientID, uri_str,
    		count);
    //LWM2M_LOG_DATA("response payload:", data, dataLength);
    output_data(log_get_handle(), format, data, dataLength, 1);


    obs_user_data_t * obs_data = (obs_user_data_t*) userData;

    //
    // send response for the first notification
    //
    if(!obs_data->first_reported)
    {
    	// remove the user data when obs failed
	    if(data == NULL)
	    {
	        WARNING("obs_notify_callback: obs failed, client=%s, uri=%s",client_name, uri_str);
	    	remove_obs_user_data(obs_data);
	    	release_obs_user_data(obs_data);
	    }
	    else
	    {
	        WARNING("obs_notify_callback: obs successed, client=%s, uri=%s",client_name, uri_str);
	    	obs_data->first_reported = 1;
	    }

    	// todo: send response to the requester

        propertiesMap = Map_Create(NULL);
        if(propertiesMap == NULL)
        {
            LOG_MSG("unable to create a Map");
            goto end;
        }

		if (Map_AddOrUpdate(propertiesMap, XK_TAG, TAG_REST_RESP) != MAP_OK)
		{
		    LOG_MSG("unable to Map_AddOrUpdate");
			goto end;
		}

		if (Map_AddOrUpdate(propertiesMap, XK_MID, obs_data->request_id) != MAP_OK)
		{
		    LOG_MSG("unable to Map_AddOrUpdate");
			goto end;
		}
		if (Map_AddOrUpdate(propertiesMap, XK_DEST, obs_data->requester) != MAP_OK)
		{
		    LOG_MSG("unable to Map_AddOrUpdate");
			goto end;
		}

        char str[100];
		if (Map_AddOrUpdate(propertiesMap, XK_FMT, rest_fmt_str(format, str)) != MAP_OK)
		{
		    LOG_MSG("unable to Map_AddOrUpdate");
			goto end;
		}

		// refer to observe.c and prv_obsRequestCallback() for the value of "count" paramater
		sprintf(str, "%d", count==0?COAP_205_CONTENT:count);
		if (Map_AddOrUpdate(propertiesMap, XK_RESP_CODE, str) != MAP_OK)
		{
			ERROR("unable to Map_AddOrUpdate");
			goto end;
		}

		msgConfig.size = 0;
		msgConfig.source = NULL;

		msgConfig.sourceProperties = propertiesMap;

		MESSAGE_HANDLE response_Message = Message_Create(&msgConfig);
		if (response_Message == NULL)
		{
			ERROR("unable to create response_Message message");
			goto end;
		}

		publish_message_on_broker(response_Message);
		Message_Destroy(response_Message);

	    Map_Destroy(propertiesMap);
		propertiesMap = NULL;
    }

    if(userData == NULL)
    {
    	LOG_MSG("userData is NULL\n");
    	goto end;
    }

    //
    // report data
    //

    propertiesMap = Map_Create(NULL);
    if(propertiesMap == NULL)
    {
        LOG_MSG("unable to create a Map");
        goto end;
    }


    // cover the payload from TLV format to JSON format
    if(format == LWM2M_CONTENT_TLV)
    {
    	char * new_payload;
    	int len = tlv_to_json(data, dataLength, &new_payload);
    	if(len)
    	{
    		orginal_payload = data;
    		data = new_payload;
    		dataLength = len;
    		format = LWM2M_CONTENT_JSON;
    	}
    }

    char str[100];
	if (Map_AddOrUpdate(propertiesMap, XK_FMT, rest_fmt_str(format, str)) != MAP_OK)
	{
	    LOG_MSG("unable to Map_AddOrUpdate");
		goto end;
	}

	// when the client request an obs on a resource, it can define a publish point.
	// then the data will sent with a url beginning with publish point as request.
	// otherwise the data will be broadcasted as EVENT type.
    if(obs_data->publish_point == NULL)
    {
    	char buffer[256];
    	sprintf(buffer, "/dp/lwm2m/%s%s", obs_data->client_uuid, obs_data->uri);
    	// broadcast data at /refresher/lwm2m/[:client-id]/object/instance/res by default
    	if (Map_AddOrUpdate(propertiesMap, XK_URI, buffer) != MAP_OK)
    	{
    	    LOG_MSG("unable to Map_AddOrUpdate");
    		goto end;
    	}

    	if (Map_AddOrUpdate(propertiesMap, XK_TAG, TAG_EVENT) != MAP_OK)
    	{
    	    LOG_MSG("unable to Map_AddOrUpdate");
    		goto end;
    	}
    }
    else
    {
    	char buffer[256];

    	sprintf(buffer, "%s%s/lwm2m/%s%s", obs_data->publish_point[0]=='/'?"":"/",
    			obs_data->publish_point,
    			obs_data->client_uuid, obs_data->uri);

    	// send request to publish point  /[publish-point]/lwm2m/[:client-id/././.
    	if (Map_AddOrUpdate(propertiesMap, XK_URI, buffer) != MAP_OK)
    	{
    	    LOG_MSG("unable to Map_AddOrUpdate");
    		goto end;
    	}
    	if (Map_AddOrUpdate(propertiesMap, XK_ACTION, ACTION_POST) != MAP_OK)
    	{
    	    LOG_MSG("unable to Map_AddOrUpdate");
    		goto end;
    	}
    	// No dest addr set here, so every module on the bus can check
    	// if the url can be locally handled.
    	//
    	// we don't set the source addr, so the reciver is not required
    	// to send response
    	if (Map_AddOrUpdate(propertiesMap, XK_TAG, TAG_REST_REQ) != MAP_OK)
    	{
    	    LOG_MSG("unable to Map_AddOrUpdate");
    		goto end;
    	}
    }
	if(data)
	{
		msgConfig.size = dataLength;
		msgConfig.source = data;
	}
	else
	{
		msgConfig.size = 0;
		msgConfig.source = NULL;
	}

	msgConfig.sourceProperties = propertiesMap;

	MESSAGE_HANDLE response_Message = Message_Create(&msgConfig);
	if (response_Message == NULL)
	{
	    LOG_MSG("unable to create response_Message message");
		goto end;
	}

	publish_message_on_broker(response_Message);
	Message_Destroy(response_Message);

    obs_data->last_report = time(NULL);

end:
	if(propertiesMap) Map_Destroy(propertiesMap);

	// note: "data" holds the new allocated payload when "orginal_payload" is set
	if(orginal_payload) free(data);
}


