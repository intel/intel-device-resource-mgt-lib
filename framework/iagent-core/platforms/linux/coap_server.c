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


#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>
#include <stdbool.h>
#include <ctype.h>
#include <sys/time.h>

//iagent
#include "iagent_base.h"
#include "rd.h"
#include "connection.h"

//external
#include "er-coap.h"
#include "coap_ext.h"

//azure
#ifdef RUN_AS_BROKER_MODULE
#include "module.h"
#include "azure_c_shared_utility/threadapi.h"
#include "azure_c_shared_utility/lock.h"
#include "broker_rest_convert.h"
#endif

connection_t *g_passive_endpoint_conn = NULL;




void remove_connection_from_list(connection_t *conn)
{
    if (g_passive_endpoint_conn == NULL)
    {
        return;
    }

    if (conn == g_passive_endpoint_conn)
    {
        g_passive_endpoint_conn = g_passive_endpoint_conn->next;
        return;
    }

    connection_t *prev = g_passive_endpoint_conn;
    connection_t *current = g_passive_endpoint_conn->next;

    while (current)
    {
        if (conn == current)
        {
            prev->next = current->next;
            return;
        }
        prev = current;
        current = current->next;
    }

    WARNING( "g_passive_endpoint_conn didn't include current connection [%p]", conn);
}





#ifdef RUN_AS_BROKER_MODULE
static int cb_foward_bus_response_to_client(void *ctx_data, void *data, int len, unsigned char format)
{
    coap_request_ctx_t *addr = (coap_request_ctx_t *) ctx_data;

    char * url_alloc =addr->url;

    if (data)
    {
        coap_packet_t coap_message[1];
        char buffer[COAP_MAX_PACKET_SIZE];

        // covert the broker message to coap packet
        convert_bus_msg_to_coap(data, coap_message);

        // recover the original request id
        coap_message->mid = addr->origin_id;
        if(addr->origin_token_len)
            coap_set_token(coap_message, addr->origin_token, addr->origin_token_len);


		///if(coap_message->code == COAP_GET )

		uint32_t block_num = 0;
		uint16_t block_size = REST_MAX_CHUNK_SIZE;
		uint32_t block_offset = 0;
		uint8_t more = 0;
		char ip[100] = {0};


		g_endpoint_coap_ctx->src_addr.addr_type = A_Sock_Addr;
		memcpy(&g_endpoint_coap_ctx->src_addr.sock_addr, &addr->addr,  addr->addrLen);
		g_endpoint_coap_ctx->src_addr.addr_len = addr->addrLen;


		get_ip_address(&g_endpoint_coap_ctx->src_addr.sock_addr,ip,sizeof(ip));

		int payload_len = coap_message->payload_len;



		if(payload_len > block_size && url_alloc)
		{
	        TraceI(FLAG_BUS_MESSAGE, "foward bus response to coap client..payload=%d too long, blockwise started..",payload_len);

	        //printf("foward bus response to coap client [%s]..payload=%d too long, blockwise started..\n",ip, payload_len);


			more = 1;
			coap_set_header_block2(coap_message, block_num, more, block_size);
			coap_set_header_size2(coap_message, payload_len);

			coap_message->payload_len = block_size;

			// the allocate url will saved in the context. so no need to free it here
			char * out_payload = (char*) malloc(payload_len);

			memcpy(out_payload, coap_message->payload, payload_len);

			set_res_blockwise(g_endpoint_coap_ctx, url_alloc, out_payload, payload_len, block_num,(uint16_t) coap_message->content_format);

			// url_alloc will be stored and release later
			url_alloc = NULL;
		}




		send_coap_msg(g_endpoint_coap_ctx, coap_message);
    }
    else
    {
        LOG_MSG("bus request timeout..bus response data to client was NULL");
        // maybe to send a error response for timeout

    }

    if(url_alloc) free(url_alloc);

    if (ctx_data)
    {
        trans_free_ctx(ctx_data);
    }



    return 0;
}

// the callback function to forward the response from gw-broker module to the cloud
static int cb_foward_bus_response_to_cloud(void *ctx_data, void *data, int len, unsigned char format)
{
    coap_endpoint_call_t *ctx = (coap_endpoint_call_t *) ctx_data;
    TraceI(FLAG_CLOUD_MSG, "cb_foward_bus_response_to_cloud, len=%d, format=%d\n", len, format);

    if (data)
    {
        coap_packet_t coap_message[1];
        char *buffer = NULL;

        // covert the broker message to coap packet
        convert_bus_msg_to_coap(data, coap_message);

        // recover the original request id
        coap_message->mid = ctx->origin_coap_id;
        if (ctx->token_len)
            coap_set_token(coap_message, ctx->token, ctx->token_len);
        else if (ctx->has_ilink_mid)
            coap_set_token(coap_message, (const uint8_t *)&ctx->origin_ilink_id, sizeof(ctx->origin_ilink_id));

        int packet_len = coap_serialize_message_tcp(coap_message, (uint8_t **)&buffer);

        if (packet_len != 0)
        {
            // send caop response to the orginal client
            ilink_message_t msg;
            init_ilink_message(&msg, COAP_OVER_TCP);
            msg.has_mid = ctx->has_ilink_mid;
            msg.msgID = ctx->origin_ilink_id;
            msg.is_req = 0;

            ilink_set_payload(&msg, buffer, packet_len);
            ilink_msg_send(&msg);

            free(buffer);
            TraceV(FLAG_CLOUD_MSG, "cb_foward_bus_response_to_cloud, sent response to cloud. ilink mid=%d\n", msg.msgID);
        }
        else
        {
            LOG_MSG("Cann't serialize the packet from cloud response");
        }
    }
    else
    {
        LOG_MSG("bus response data to cloud was NULL");
        // maybe to send a error response for timeout

    }

    if (ctx_data)
    {
        trans_free_ctx(ctx_data);
    }

    return 0;
}


void publish_cloud_request_to_gw_bus(ilink_message_t *ilink_msg, void *coap_parsed)
{
    coap_packet_t *coap_message = (coap_packet_t *) coap_parsed;
    char new_url[256];

    const char *url = NULL;
    int url_len = coap_get_header_uri_path(coap_message, &url);
    if (url == NULL) LOG_RETURN();

    // check if the target resource is defined separate url
    bool url_alloc = false;
    char *seperate_url = NULL;
    char *separate_params = NULL;
    iUrl_t iurl_body = {0};

    if (parse_iUrl_body((IURL_BODY)url, &iurl_body))
    {
        char *url_str = get_string((char *)url, url_len, &url_alloc);

		char * alias = get_alias_url_from_di(iurl_body.device, new_url, sizeof(new_url));
		if(alias)
		{
			strcat(new_url, iurl_body.res_uri);

			TraceI(FLAG_BUS_MESSAGE, "publish_cloud_request_to_gw_bus: new url:%s, original: [%s]", new_url, url);

			coap_set_header_uri_path(coap_message, new_url);
		}

		else if (check_seperate_url(iurl_body.device, iurl_body.res_uri, coap_message->code, &seperate_url, &separate_params))
        {
            coap_set_header_uri_path(coap_message, seperate_url);
            if (separate_params)
                coap_set_header_uri_query(coap_message, separate_params);
        }

        if (url_alloc) free(url_str);
        free_iUrl_body(&iurl_body);
    }

    // forward to gw-broker for extension service
    coap_endpoint_call_t *data = (coap_endpoint_call_t *) trans_malloc_ctx(sizeof(coap_endpoint_call_t));
    memset (data, 0, sizeof(*data));
    data->has_ilink_mid = ilink_msg->has_mid;

    if (ilink_msg->has_mid)
        data->origin_ilink_id = ilink_msg->msgID;
    else
        TraceV(FLAG_CLOUD_MSG, "publish_cloud_request_to_gw_bus: ilink message didn't take mid");

    data->token_len = coap_message->token_len;
    if(coap_message->token_len)
        memcpy(data->token, coap_message->token, coap_message->token_len);
    else
        TraceV(FLAG_CLOUD_MSG, "publish_cloud_request_to_gw_bus: ilink message didn't take token");

    unsigned long broker_msg_id = bh_gen_id(get_outgoing_requests_ctx());
    MESSAGE_HANDLE broker_msg = coap_to_bus_msg(coap_message, broker_msg_id, NULL);

    // set async callback for broker response
    bh_wait_response_async(get_outgoing_requests_ctx(),
            broker_msg_id,
            cb_foward_bus_response_to_cloud,
            data,
            10*1000,
            NULL);

    // send the broker message
    publish_message_on_broker(broker_msg);

    Message_Destroy(broker_msg);

    if (separate_params) free(separate_params);
    if (seperate_url) free(seperate_url);
}


int forward_coap_request_to_bus(coap_context_t * coap_ctx, coap_packet_t * coap_message)
{

    coap_status_t coap_error_code = NO_ERROR;
    int len = 0;
    assert (coap_message->code >= COAP_GET && coap_message->code <= COAP_DELETE);

	char url[256];
	memset (url,0,256);
	char *strip_url = NULL;
	int url_len;
	char *url_path=NULL;
	char new_url[256];

	//the uri path get from coap package don't include "/0" flag
	url_len = coap_get_header_uri_path(coap_message, (const char **)&url_path);
	if(url_path == NULL)  LOG_RETURN(BAD_REQUEST_4_00);

	if((url_len+1) >= sizeof(url)) LOG_RETURN(BAD_REQUEST_4_00);

	memcpy(url,url_path,url_len);
	url[url_len] = 0;
	TraceI(FLAG_BUS_MESSAGE, "forward_coap_request_to_bus url=%s, url_len = %d, code=%d, payload len=%d",
				url, url_len, coap_message->code, coap_message->payload_len);

	// try to replace the "dev" with actually standard name
	if(check_url_start(url, url_len, "dev/")) // Bugzilla-2429
	{
		check_url_generic_dev(coap_message, url, sizeof(url));
	}

	if (check_url_start(url, url_len,  "ep"))
	{
		TraceI(FLAG_BUS_MESSAGE, "ep not support for url [%s]", url);
		return BAD_REQUEST_4_00;
	}

	// check if the target resource is defined separate url

	char *seperate_url = NULL;
	char *separate_params = NULL;
	iUrl_t iurl_body = {0};
	if(parse_iUrl_body(url, &iurl_body))
	{

		char * alias = get_alias_url_from_di(iurl_body.device, new_url, sizeof(new_url));
		if(alias)
		{
			strcat(new_url, iurl_body.res_uri);

			TraceI(FLAG_BUS_MESSAGE, "coap server: new url:%s, original: [%s]", new_url, url);

			coap_set_header_uri_path(coap_message, new_url);
		}
		else if(check_seperate_url(iurl_body.device, iurl_body.res_uri, coap_message->code, &seperate_url, &separate_params))
		{
			coap_set_header_uri_path(coap_message, seperate_url);
			if(separate_params)
				coap_set_header_uri_query(coap_message, separate_params);
		}

		free_iUrl_body(&iurl_body);
	}


	// forward to gw-broker for extension service
	coap_request_ctx_t *ctx_data = (coap_request_ctx_t*) trans_malloc_ctx(sizeof(coap_request_ctx_t));
	memcpy(&ctx_data->addr, &coap_ctx->src_addr.sock_addr, coap_ctx->src_addr.addr_len);
	ctx_data->addrLen = coap_ctx->src_addr.addr_len;
	ctx_data->origin_id = coap_message->mid;
	ctx_data->origin_token_len = coap_message->token_len;
	ctx_data->url =coap_get_full_url_alloc(coap_message);


	if (coap_message->token_len > 0)
		memcpy(ctx_data->origin_token, coap_message->token, coap_message->token_len);

	unsigned long broker_msg_id = bh_gen_id(get_outgoing_requests_ctx());

	// convert the request to the broker message

	MESSAGE_HANDLE broker_msg = coap_to_bus_msg(coap_message, broker_msg_id, NULL);

	// set async callback for broker response
	bh_wait_response_async(get_outgoing_requests_ctx(),
			broker_msg_id,
			cb_foward_bus_response_to_client,
			ctx_data,
			10*1000,
			NULL);

	// send the broker message
	publish_message_on_broker(broker_msg);
	Message_Destroy(broker_msg);

	if(separate_params) free(separate_params);
	if(seperate_url) free(seperate_url);

	// MANUAL_RESPONSE tells the coap stack not respond ack immedately
	//                 the coap response will be sent to client when the bus response is back
	return MANUAL_RESPONSE;

}



#endif


