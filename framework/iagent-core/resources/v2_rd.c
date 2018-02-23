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




#include "coap_platforms.h"
#include "er-coap.h"
#include "agent_core_lib.h"
#include "iagent_base.h"

extern connection_t *g_passive_endpoint_conn;

int handler_rd_get (void * request_coap, void * response_coap, char ** out_payload, int * payload_len)
{

	coap_packet_t * request = (coap_packet_t* ) request_coap;
	coap_packet_t * response = (coap_packet_t* ) response_coap;
    char tmp = 0;
    const char *coap_query;
    char * query = NULL;
    bool alloc = false;
    char * payload = NULL;

    int len = coap_get_header_uri_query(request, &coap_query);
    if (len)
    {
    	if (coap_query[len-1] != 0)
    	{
    		query = (char *) malloc (len + 1);
    		memcpy (query, coap_query, len);
    		query[len] = 0;
    		alloc = true;
    	}
	    else
        {
        	query = (char *) coap_query;
        }
    }


    handle_rd_get(query, &payload, true);
    if(payload)
    {
    	len = strlen (payload);
    	if(len > REST_MAX_CHUNK_SIZE)
    	{
    		*out_payload = payload;
    		* payload_len = len;
    	}
    	else
    	{
    		memcpy(response->payload, payload, len);
    		response->payload_len = len;
     		 if(payload) free (payload);
    	}
    }

    coap_set_header_content_format(response, APPLICATION_JSON);


    if(alloc) free (query);

    return CONTENT_2_05;

}

int handler_rd_put (void * request_coap, void * response_coap, char ** out_payload, int * payload_len)
{

	coap_packet_t * coap_message = (coap_packet_t* ) request_coap;
	coap_packet_t * response = (coap_packet_t* ) response_coap;

    connection_t *connection =  connection_find(g_passive_endpoint_conn,
    		&g_endpoint_coap_ctx->src_addr.sock_addr, g_endpoint_coap_ctx->src_addr.addr_len);
    if(connection == NULL)
    {
        connection = connection_new_incoming(g_passive_endpoint_conn, g_endpoint_coap_ctx->socket,
        		(struct sockaddr *) &g_endpoint_coap_ctx->src_addr.sock_addr, g_endpoint_coap_ctx->src_addr.addr_len);

        TraceI(FLAG_CLOUD_MSG, "rd create new connection");
    }
    if(handle_rd_on_connection(coap_message->payload, coap_message->payload_len, connection))
    {
        connection->next = g_passive_endpoint_conn;
        g_passive_endpoint_conn = connection;
        TraceI(FLAG_CLOUD_MSG, "handle_rd_on_connection. sock=%d", connection->sock);
    }
    else
    {
        free(connection);
    }

    return CHANGED_2_04;

}

int handler_rd_del (void * request_coap, void * response_coap, char ** out_payload, int * payload_len)
{
	coap_packet_t * coap_message = (coap_packet_t* ) request_coap;
	coap_packet_t * response = (coap_packet_t* ) response_coap;
    bool result = false;

    char tmp = 0;
    const char *coap_query;
    char * query = NULL;
    bool alloc = false;

    if(coap_message->code != COAP_DELETE)
    	return NOT_IMPLEMENTED_5_01;

    int len = coap_get_header_uri_query(coap_message, &coap_query);
    if (len)
    {
        if (coap_query[len-1] != 0)
        {
            query = (char *) malloc (len + 1);
            memcpy (query, coap_query, len);
            query[len] = 0;
            alloc = true;
        }
        else
        {
            query = (char *) coap_query;
        }
    }


    if (query && *query)
    	result = handle_rd_delete((const char *)query);
    else
    	LOG_MSG("URL didn't take query. We don't know which di to delete");

    int reponse_code = result? DELETED_2_02:NOT_FOUND_4_04;

    if(alloc) free (query);

    return reponse_code;

}


coap_resource_handler_t resource_rd = {NULL, "rd", handler_rd_get, handler_rd_put, handler_rd_put, handler_rd_del};

