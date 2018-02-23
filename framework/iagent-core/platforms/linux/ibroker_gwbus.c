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
#include <string.h>
#include <ctype.h>

//iagent
#include "iagent_base.h"
#include "agent_core_lib.h"
#include "broker_rest_convert.h"
#include "ilink_message.h"

//external
#include "er-coap.h"
#include "coap_ext.h"
#include "message.h"



static int cb_foward_ibroker_response_to_bus(void *ctx_data, void *data, int len, unsigned char format)
{
    module_to_endpoint_call_t *ctx = (module_to_endpoint_call_t *) ctx_data;

    if (data)
    {
        assert(format == T_Coap_Parsed);
        coap_packet_t *coap_message = (coap_packet_t *) data;

        MESSAGE_HANDLE bus_msg = coap_to_bus_msg(coap_message, ctx->origin_bus_msg_id, ctx->source_module_id);
        publish_message_on_broker(bus_msg);
        Message_Destroy(bus_msg);

    }
    else
    {
        LOG_MSG("ibroker response TIMEOUT");
        // maybe to send a error response for timeout
    }

    if (ctx_data)
    {
        trans_free_ctx(ctx_data);
    }
    return 0;
}

void send_bus_request_to_ibroker(void *message,  char *src_module, char *tm)
{
    coap_packet_t *coap_message = (coap_packet_t *) message;
    module_to_endpoint_call_t *data = (module_to_endpoint_call_t*) trans_malloc_ctx(sizeof (module_to_endpoint_call_t));
    memset (data, 0, sizeof(*data));

    assert(coap_message->token_len == sizeof (uint32_t));
    data->origin_bus_msg_id = *((uint32_t*)coap_message->token);
    if (src_module) strcpy (data->source_module_id, src_module);

    uint32_t msg_id = bh_gen_id(get_outgoing_requests_ctx());

    ilink_message_t msg[1];
    ilink_vheader_t vheader;

    init_ilink_message(msg, COAP_OVER_TCP);
    ilink_set_req_resp(msg, 1, msg_id);

    memset (&vheader, 0, sizeof (vheader));

    vheader_set_value_s(&vheader, K_TIME, tm);

    char *self_id = get_self_agent_id();
    vheader_set_value_s(&vheader, K_AGENT_ID, self_id);
    ilink_set_vheader(msg, &vheader);
    coap_set_token(coap_message, (const uint8_t *)&msg_id, sizeof (msg_id));

    char *packet = NULL;
    size_t packet_len = coap_serialize_message_tcp(coap_message, (uint8_t **)&packet);

    if (packet_len)
    {
        //set async callback for broker response
        bh_wait_response_async(get_outgoing_requests_ctx(),
                msg_id,
                cb_foward_ibroker_response_to_bus,
                data,
                10*1000,
                NULL);

        ilink_set_payload(msg, packet, packet_len);
        ilink_msg_send(msg);
        free(packet);
    }

    vheader_destroy(&vheader);
    reset_ilink_message(msg);
}
