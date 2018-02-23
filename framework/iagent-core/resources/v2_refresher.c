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


int handler_refresher_get (void * request_coap, void * response, char ** out_payload, int * payload_len)
{

	coap_packet_t * coap_message = (coap_packet_t* ) request_coap;

	return NOT_IMPLEMENTED_5_01;


}

int handler_refresher_put (void * request_coap, void * response_coap, char ** out_payload, int * payload_len)
{

	coap_packet_t * coap_message = (coap_packet_t* ) request_coap;
	coap_packet_t * response = (coap_packet_t* ) response_coap;

    int id=-1;
    int status = NOT_IMPLEMENTED_5_01;

	char buffer[256] = {0};
	char s[INET6_ADDRSTRLEN];
	int port;
	struct sockaddr_storage *addr = (struct sockaddr_storage*) &g_endpoint_coap_ctx->src_addr.sock_addr;

	if (AF_INET == addr->ss_family)
	{
		struct sockaddr_in *saddr = (struct sockaddr_in *)addr;
		inet_ntop (saddr->sin_family, &saddr->sin_addr, s, INET6_ADDRSTRLEN);
		port = saddr->sin_port;
	}
	else if (AF_INET6 == addr->ss_family)
	{
		struct sockaddr_in6 *saddr = (struct sockaddr_in6 *)addr;
		inet_ntop (saddr->sin6_family, &saddr->sin6_addr, s, INET6_ADDRSTRLEN);
		port = saddr->sin6_port;
	}

	sprintf (buffer, "coap://%s:%d", s, port);
	id = handle_data_observing(coap_message->payload, buffer);

	if (id == -1)
	{
		status = INTERNAL_SERVER_ERROR_5_00;
		LOG_MSG("observing failed");

	}
	else
	{
		status = CHANGED_2_04;
		sprintf (response->payload, "%d", id);
		coap_set_payload(response, response->payload, strlen(response->payload));
	}

	return status;
}

coap_resource_handler_t resource_refresher = {NULL, "refresher", handler_refresher_get, handler_refresher_put, handler_refresher_put, NULL};

