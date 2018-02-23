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

#ifdef BUILTIN_IBROKER

static int cb_foward_cloud_response_to_client(void *ctx_data, void *data, int len, unsigned char format)
{
    coap_request_ctx_t *addr = (coap_request_ctx_t *) ctx_data;

    if (data)
    {
       // covert coap response from tcp based to udp based, then send to the original client
        assert(format == T_Coap_Parsed);
        coap_packet_t *coap_message = (coap_packet_t *) data;

        coap_message->type = COAP_TYPE_ACK;
        coap_message->mid = addr->origin_id;

        if (addr->origin_token_len > 0)
            coap_set_token(coap_message, addr->origin_token, addr->origin_token_len);

        char packet[COAP_MAX_PACKET_SIZE];
        size_t packet_len = coap_serialize_message(coap_message, packet);

        if (packet_len)
        {
            sendto (g_socket_coap_ep, packet, packet_len, 0,
                    (struct sockaddr *)&addr->addr,
                    addr->addrLen);
        }
        else
        {
            LOG_MSG("Cann't serialize the packet from cloud response");
        }
    }
    else
    {
        LOG_MSG("cloud response data to client was NULL");
        // maybe to send a error response for timeout

    }
    if (ctx_data)
    {
        trans_free_ctx(ctx_data);
    }

    return 0;
}



int handler_ibroker_get_put (void * request_coap, void * response, char ** out_payload, int * payload_len)
{

	coap_packet_t * coap_message = (coap_packet_t* ) request_coap;

	 int url_len;
	char *url=NULL;
	url_len = coap_get_header_uri_path(coap_message, (const char **)&url);
	url[url_len] = '\0';

	// forward the request to the cloud and strip the "ibroker/"
	coap_set_header_uri_path(coap_message, url+8);

	// todo:
	ilink_message_t msg[1];
	char *request;
	uint32_t request_len;
	coap_request_ctx_t *ctx_data = (coap_request_ctx_t*) trans_malloc_ctx(sizeof(coap_request_ctx_t));

	memset (ctx_data, 0, sizeof(*ctx_data));
	memcpy (&ctx_data->addr, &g_endpoint_coap_ctx->src_addr.sock_addr, g_endpoint_coap_ctx->src_addr.addr_len);
	ctx_data->addrLen = g_endpoint_coap_ctx->src_addr.addr_len;
	ctx_data->origin_token_len = coap_message->token_len;

	if(coap_message->token_len)
		memcpy (ctx_data->origin_token, coap_message->token, coap_message->token_len);

    ctx_data->origin_id = coap_message->mid;
	// compose request
	unsigned long msg_id = bh_gen_id(get_outgoing_requests_ctx());
	init_ilink_message(msg, COAP_OVER_TCP);
	ilink_set_req_resp(msg, 1, msg_id);

	// coap over tcp use token for match the request and response
	coap_set_token(coap_message, (const uint8_t *)&msg_id, sizeof(msg_id));
	char *packet = NULL;
	size_t packet_len = coap_serialize_message_tcp((void*)coap_message, (uint8_t **)&packet);

	if (packet_len)
	{
		ilink_set_payload(msg, packet, packet_len);
		ilink_msg_send(msg);
		free(packet);
	}

	//set async callback for broker response
	bh_wait_response_async(get_outgoing_requests_ctx(),
			msg_id,
			cb_foward_cloud_response_to_client,
			ctx_data,
			10*1000,
			NULL);

	reset_ilink_message(msg);

	return MANUAL_RESPONSE;

}

coap_resource_handler_t resource_ibroker = {NULL, "ibroker/", handler_ibroker_get_put, handler_ibroker_get_put, handler_ibroker_get_put, handler_ibroker_get_put};


#endif
