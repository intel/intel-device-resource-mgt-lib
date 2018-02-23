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


int handler_rd_monitor_get (void * request_coap, void * response, char ** out_payload, int * payload_len)
{

	coap_packet_t * coap_message = (coap_packet_t* ) request_coap;

	return NOT_IMPLEMENTED_5_01;


}

int handler_rd_monitor_put (void * request_coap, void * response_coap, char ** out_payload, int * payload_len)
{

	coap_packet_t * coap_message = (coap_packet_t* ) request_coap;
	coap_packet_t * response = (coap_packet_t* ) response_coap;

    char s[INET6_ADDRSTRLEN];
    char buffer[200] = {0};
    int port;
    struct sockaddr_storage *addr = (struct sockaddr_storage*) &g_endpoint_coap_ctx->src_addr.sock_addr;
    if (AF_INET == addr->ss_family)
    {
        struct sockaddr_in *saddr = addr;
        inet_ntop(saddr->sin_family, &saddr->sin_addr, s, INET6_ADDRSTRLEN);
        port = saddr->sin_port;
    }
    else if (AF_INET6 == addr->ss_family)
    {
        struct sockaddr_in6 *saddr = (struct sockaddr_in6 *)addr;
        inet_ntop(saddr->sin6_family, &saddr->sin6_addr, s, INET6_ADDRSTRLEN);
        port = saddr->sin6_port;
    }
    else
    {
        return INTERNAL_SERVER_ERROR_5_00;
    }
    sprintf(buffer, "coap://%s:%d/", s, port);

    int id = handle_rd_monitor_put(coap_message->payload, buffer);
    if (id != -1)
    {
        sprintf(response->payload, "%d", id);
        response->payload_len = strlen(response->payload);
    }
    else
        return INTERNAL_SERVER_ERROR_5_00;

    // ensure the response for /rd/monitor put is sent before we publish the RD status
    if(id != -1)
        do_rd_monitor_scan(id);

    return CHANGED_2_04;
}


coap_resource_handler_t resource_rd_monitor = {NULL, "rd/monitor", handler_rd_monitor_get, handler_rd_monitor_put, handler_rd_monitor_put, NULL};
