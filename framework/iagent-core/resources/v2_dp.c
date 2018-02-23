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
#include "connection.h"

extern connection_t *g_passive_endpoint_conn;

int handler_dp_put (void * request_coap, void * response, char ** out_payload, int * payload_len)
{

	coap_packet_t * coap_message = (coap_packet_t* ) request_coap;
	int url_len;
	char *url=NULL;
	url_len = coap_get_header_uri_path(coap_message, (const char **)&url);

    // the /dp url accept two schemes:
    // 1. dp/ep/[device id]/[resource]
    // 2. dp/[resource]  (the source address must in the connection list)
    TraceI(FLAG_CLOUD_MSG, "dp from passive device. url=%s, format=%d, payload=%s, payload_len=%d",
                url, coap_message->content_format, coap_message->payload, coap_message->payload_len);

    char deviceid[100];

    if (url_len > 3 && check_url_start(url+3, url_len-3, "ep/"))
    {
        iUrl_t i_url = {0};
        if (!parse_iUrl_body2(url+3, url_len-3, &i_url))
            LOG_RETURN();

        OnDataPoint(i_url.device, i_url.res_uri, coap_message->content_format, coap_message->payload, coap_message->payload_len);
        free_iUrl_body(&i_url);
    }
    else
    {
        connection_t *connection =  connection_find(g_passive_endpoint_conn,
        		&g_endpoint_coap_ctx->src_addr.sock_addr, g_endpoint_coap_ctx->src_addr.addr_len);
        if (connection == NULL)
        {
            WARNING("passive device connection was NULL!");
            return BAD_REQUEST_4_00;
        }

        char *name = find_client_by_connection(connection, deviceid, sizeof(deviceid));
        TraceI(FLAG_CLOUD_MSG, "passice device name is %s", name);

        if(name)
        {
            bool alloc = false;
            char *uri = get_string(url+2, url_len-2, &alloc);
            TraceI(FLAG_CLOUD_MSG, "passive device dp url: %s", uri);
            OnDataPoint(name, uri,
                        coap_message->content_format,
                        coap_message->payload, coap_message->payload_len);
            if(alloc) free(uri);
        }
    }

    return CHANGED_2_04;
}

coap_resource_handler_t resource_dp = {NULL, "dp*", NULL, handler_dp_put, handler_dp_put, NULL};

