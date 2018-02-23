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


#include "message.h"


typedef struct operate_user_data
{
	char request_id[20];		// the bus message id
	char requester[100];
	char *  action;
} operate_user_data_t;

extern dlist_entry_ctx_t g_request_queue;
extern lwm2m_context_t * g_lwm2mH;

lwm2m_client_t *getClientbyID(lwm2m_context_t * contextP, int id)
{
	lwm2m_client_t * client = (lwm2m_client_t *) lwm2m_list_find((lwm2m_list_t *)contextP->clientList, id);
	return client;
}

static lwm2m_client_t * getClientByName(lwm2m_context_t * contextP,
                                            char * name)
{
    lwm2m_client_t * targetP;

    targetP = contextP->clientList;
    while (targetP != NULL && strcmp(name, targetP->name) != 0)
    {
        targetP = targetP->next;
    }

    return targetP;
}

#define MAX_URI_TOKEN_LEN 100
int get_uri_token(const char * uri, char *buffer)
{
	char *p = buffer;
	int cnt = 0;
	if(*uri == 0) return 0;
	if(*uri == '/') uri ++;
	if(*uri == 0) return 0;

	while(*uri != 0 && *uri != '/' )
	{
		cnt ++;
		if(cnt == MAX_URI_TOKEN_LEN)
		{
			*p = 0;
			return cnt;  // to check if should minus 1
		}

		*p++ = * uri ++;
	}
	*p = 0;
	return cnt;
}

lwm2m_context_t * get_lwm2m_context()
{
	return g_lwm2mH;
}

static lwm2m_observation_t * prv_findObservationByURI(lwm2m_client_t * clientP,
        lwm2m_uri_t * uriP)
{
	lwm2m_observation_t * targetP;

	    targetP = clientP->observationList;
	    while (targetP != NULL)
	    {
	        if (targetP->uri.objectId == uriP->objectId
	         && targetP->uri.flag == uriP->flag
	         && targetP->uri.instanceId == uriP->instanceId
	         && targetP->uri.resourceId == uriP->resourceId)
	        {
	            return targetP;
	        }

	        targetP = targetP->next;
	    }

	    return targetP;
}

static void request_result_callback(uint16_t clientID,
                                lwm2m_uri_t * uriP,
                                int status,
                                lwm2m_media_type_t format,
                                uint8_t * data,
                                int dataLength,
                                void * userData)
{
	MAP_HANDLE propertiesMap = NULL;

    fprintf(stdout, "\r\nClient #%d /%d", clientID, uriP->objectId);
	char uri_str[100] = {0};
	uri_depth_t  depth = 0;
	uri_toString(uriP, uri_str, sizeof(uri_str), &depth);

    lwm2m_client_t * clientP = getClientbyID(get_lwm2m_context(), clientID);

    TraceI(FLAG_LWM2M_LOG, "recieved response from device: [%s] #%d, uri=%s, status:(%s)",
    		clientP?clientP->name:"[!!]", clientID, uri_str,
    		coap_status_to_string(status));

    output_data(log_get_handle(), format, data, dataLength, 1);
    //LWM2M_LOG_DATA("response payload:", data, dataLength);


    if(userData)
    {
    	operate_user_data_t * ctx_data = (operate_user_data_t* )userData;

    	// remove the obs user data when cancel is done
    	if(clientP && strcmp(ctx_data->action, "obs_cancel" )== 0)
    	{
    		// note: need to slove the static declaration of prv_findObservationByURI()
    		lwm2m_observation_t * observeP = prv_findObservationByURI(clientP, uriP);
    		if(observeP && observeP->userData)
    		{
    			obs_user_data_t *obs_ctx = observeP->userData;

				// todo: next more study on safe removal of the obs context data
    			WARNING("freeing the observe user context [%p], client=%s, uri=%s",obs_ctx, obs_ctx->client_uuid, obs_ctx->uri);
				remove_obs_user_data(obs_ctx);
				release_obs_user_data(obs_ctx);
    		}
    	}

    	// publish the response to the bus
        MESSAGE_CONFIG msgConfig;
        propertiesMap = Map_Create(NULL);
        if(propertiesMap == NULL)
        {
            ERROR("unable to create a Map");
            goto end;
        }

		if (Map_AddOrUpdate(propertiesMap, XK_TAG, TAG_REST_RESP) != MAP_OK)
		{
			ERROR("unable to Map_AddOrUpdate");
			goto end;
		}

		if (Map_AddOrUpdate(propertiesMap, XK_MID, ctx_data->request_id) != MAP_OK)
		{
			ERROR("unable to Map_AddOrUpdate");
			goto end;
		}
		if (Map_AddOrUpdate(propertiesMap, XK_DEST, ctx_data->requester) != MAP_OK)
		{
			ERROR("unable to Map_AddOrUpdate");
			goto end;
		}

		char str[40];
		sprintf(str, "%d", format);
		if (Map_AddOrUpdate(propertiesMap, XK_FMT, str) != MAP_OK)
		{
			ERROR("unable to Map_AddOrUpdate");
			goto end;
		}
		sprintf(str, "%d", status);
		if (Map_AddOrUpdate(propertiesMap, XK_RESP_CODE, str) != MAP_OK)
		{
			ERROR("unable to Map_AddOrUpdate");
			goto end;
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
			ERROR("unable to create response_Message message");
			goto end;
		}

		publish_message_on_broker(response_Message);
		Message_Destroy(response_Message);

		Map_Destroy(propertiesMap);

		propertiesMap = NULL;
    }


end:
	if(userData) free(userData);

	if(propertiesMap) Map_Destroy((CONSTMAP_HANDLE)propertiesMap);
}


static void prv_print_error(uint8_t status)
{
    CONSOLE_LOG( "Error: ");
    print_status(stdout, status);
    CONSOLE_LOG( "\r\n");
}


void check_bus_message(lwm2m_context_t * contextP)
{

	char name[MAX_URI_TOKEN_LEN];
	lwm2m_uri_t uri;
	int result;

	dlist_node_t * node = dlist_get(&g_request_queue);
	if(node == NULL)
		return;

	E_Msg_Type type = node->type;
	MESSAGE_HANDLE messageHandle = (MESSAGE_HANDLE) node->message;
	free(node);

	CONSTMAP_HANDLE properties = Message_GetProperties(messageHandle); /*by contract this is never NULL*/

	const char* action = ConstMap_GetValue(properties, XK_ACTION);
	const char* uri_str = ConstMap_GetValue(properties, XK_URI);
	const char* obs = ConstMap_GetValue(properties, XK_OBS);

	TraceV(FLAG_LWM2M_GWBUS, "Recieved GW bus message, action=%s, uri=%s, obs=%s",
	        action?action:"",uri_str?uri_str:"", obs?obs:"");

	if(uri_str[0] == '/') uri_str++;

	int PRE_LWM2M_LEN = strlen("lwm2m/");
	if(strlen(uri_str) <= PRE_LWM2M_LEN)
	{
        WARNING("check_bus_message: URI not right, %s", uri_str);
		goto end;
	}

	assert(strncmp(uri_str, "lwm2m/", PRE_LWM2M_LEN) == 0);
	uri_str += PRE_LWM2M_LEN;

	int len = get_uri_token(uri_str, name);
	if(len == 0)
	{
	    WARNING("check_bus_message: GET URI TOKEN FAILED. %s", uri_str);
		goto end;
	}

	uri_str += len;

    result = lwm2m_stringToUri(uri_str, strlen(uri_str), &uri);
    if (result == 0)
    {
        WARNING("check_bus_message: failed to parse uri [%s]", uri_str);
        goto end;
    }


	lwm2m_client_t *client = getClientByName(contextP, name);
	if(client == NULL)
	{
	    WARNING("failed to find client [%s]", name);
		goto end;
	}

	const char* src = ConstMap_GetValue(properties, XK_SRC);
	const char* mid = ConstMap_GetValue(properties, XK_MID);

	if(mid == NULL)
	{
	    WARNING("mid from bus message is NULL");
	    goto end;
	}

	if(obs && strcmp(action, ACTION_GET) == 0)
	{
		obs_user_data_t * obs_ctx = find_obs_user_data(client->name, uri_str);
		if(strcmp(obs, "1") == 0 || strcmp(obs, "true") == 0)
		{
			// start obs
			obs_user_data_t * user_data = new_obs_user_data(name, uri_str);
			const char* publish = ConstMap_GetValue(properties, XK_PUBLISH);
			if(publish)
				user_data->publish_point = strdup(publish);

			if(src)
				user_data->requester = strdup(src);
			user_data->request_id = strdup(mid);

		    result = lwm2m_observe(contextP, client->internalID,
		    		&uri, obs_notify_callback, user_data);

		    if (result == 0)
		    {
		        WARNING("Observe [%s:%s] OK", client->name,uri_str );
		    }
		    else
		    {
		        WARNING("Observe [%s:%s]  error", client->name,uri_str);
		    }
		}
		else
		{
			// remove obs
			operate_user_data_t *user_data = (operate_user_data_t*) malloc(sizeof(operate_user_data_t));
			if(user_data == NULL)
			{
			    LOG_MSG("Malloc failed");
				goto end;
			}

			user_data->action = "obs_cancel";

			strncpy(user_data->requester, src, sizeof(user_data->requester));
			strncpy(user_data->request_id, mid, sizeof(user_data->request_id));

			result =  lwm2m_observe_cancel(contextP, client->internalID,
			                         &uri,
									 request_result_callback,
									 user_data);
		    if (result == 0)
		    {
		        WARNING("Remove Observe [%s:%s] OK", client->name,uri_str );
		    }
		    else
		    {
		        WARNING("Remove Observe [%s:%s]  error", client->name,uri_str);
		    	free(user_data);
		    }


		}
	}
	else
	{
		operate_user_data_t * user_data = NULL;

		// send reponse to bus only when the source addr and mid is available in the bus message
		if(src && mid)
		{
			user_data = (operate_user_data_t*) malloc(sizeof(operate_user_data_t));
			if(user_data == NULL)
			{
			    LOG_MSG("Malloc failed");
				goto end;
			}

			strncpy(user_data->requester, src, sizeof(user_data->requester));
			strncpy(user_data->request_id, mid, sizeof(user_data->request_id));
		}

		if(strcmp(action, ACTION_PUT) == 0 )   // write
		{
			user_data->action = "write";
			int fmt = LWM2M_CONTENT_TEXT;
			const char* fmt_str = ConstMap_GetValue(properties, XK_FMT);
			if(fmt_str)
				fmt=atoi(fmt_str);
			const CONSTBUFFER * payload = Message_GetContent(messageHandle);
		    result = lwm2m_dm_write(contextP, client->internalID, &uri, fmt,
		    		(uint8_t *)payload->buffer, payload->size, request_result_callback, user_data);

		    if (result == 0)
		    {
		        TraceI(FLAG_LWM2M_GWBUS, "write [%s] OK", uri_str);
		    }
		    else
		    {
		        WARNING("write [%s] failed", uri_str);
		    	if(user_data) free (user_data);
		        prv_print_error(result);
		    }
		}
		else if( strcmp(action, ACTION_POST) == 0)	// exec
		{
			user_data->action = "exec";
			const CONSTBUFFER * payload = Message_GetContent(messageHandle);
		    result = lwm2m_dm_execute(contextP, client->internalID, &uri, LWM2M_CONTENT_TEXT,
		    		(uint8_t *)payload->buffer, payload->size, request_result_callback, user_data);

		    if (result == 0)
		    {
		        TraceI(FLAG_LWM2M_GWBUS, "exec  [%s] OK", uri_str);
		    }
		    else
		    {
		        WARNING("exec [%s] failed", uri_str);
		    	if(user_data) free (user_data);
		        prv_print_error(result);
		    }

		}
		else if(strcmp(action, ACTION_GET) == 0)	// read
		{
			user_data->action = "read";
		    result = lwm2m_dm_read(contextP, client->internalID, &uri, request_result_callback, user_data);

		    if (result == 0)
		    {
		        TraceI(FLAG_LWM2M_GWBUS, "get  [%s] OK", uri_str);
		    }
		    else
		    {
		        WARNING("get [%s] failed", uri_str);
		    	if(user_data) free (user_data);
		        prv_print_error(result);
		    }
		}
		else
		{
			if(user_data) free (user_data);
			goto end;
		}
	}


end:

	ConstMap_Destroy(properties);
	Message_Destroy(messageHandle);

}
