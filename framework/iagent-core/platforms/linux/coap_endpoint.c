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
#include "rd.h"
#include "broker_rest_convert.h"
#include "logs.h"

//external
#include "er-coap.h"
#include "coap_ext.h"
#include "coap_request.h"
#include "er-coap-transactions.h"
#include "er-coap-constants.h"
#include "message.h"


coap_context_t *g_endpoint_coap_ctx = NULL;
extern coap_resource_handler_t resource_ibroker;
extern coap_resource_handler_t resource_rd;
extern coap_resource_handler_t resource_refresher;
extern coap_resource_handler_t resource_rd_monitor;
extern coap_resource_handler_t resource_dp;
extern coap_resource_handler_t ams_cfg_rd;
extern coap_resource_handler_t resource_version;
extern coap_resource_handler_t resource_reset;
extern coap_resource_handler_t resource_ilink;

static int endpoint_send(void *ctx, const uip_ipaddr_t *dst_addr, void *buf, int len)
{
    coap_context_t *coap_ctx = (coap_context_t *) ctx;
    assert (dst_addr->addr_type == A_Sock_Addr);
    return sendto (coap_ctx->socket, buf, len, 0, (struct sockaddr *)&dst_addr->sock_addr,dst_addr->addr_len);
}

static int iagent_coap_request_handler (void * ctx , void * message)
{

	coap_context_t * coap_ctx = (coap_context_t*) ctx;

	return forward_coap_request_to_bus(coap_ctx, (coap_packet_t*)message);
}


void init_endpoint_coap_ctx()
{
    assert (g_endpoint_coap_ctx == NULL);

    uip_ipaddr_t my_addr;
    g_endpoint_coap_ctx = coap_context_new(&my_addr);

    if (NULL == g_endpoint_coap_ctx) LOG_RETURN();

    g_socket_coap_ep = create_socket((const char*) I_COAP_SERVER_PORT_STR,(int) AF_INET);
    while (g_socket_coap_ep == -1)
    {
        g_socket_coap_ep = create_socket((const char*) I_COAP_SERVER_PORT_STR,(int) AF_INET);

        if (g_socket_coap_ep < 0)
            fprintf (stdout, "Error in thread_ilink_port_handler: port %s is used\r\n", I_COAP_SERVER_PORT_STR);

        sleep (2);
    }

    g_endpoint_coap_ctx->socket = g_socket_coap_ep;
    g_endpoint_coap_ctx->request_handler = iagent_coap_request_handler;
    g_endpoint_coap_ctx->tx_data = endpoint_send;

    add_resource_handler(g_endpoint_coap_ctx, &resource_rd);
    add_resource_handler(g_endpoint_coap_ctx, &resource_refresher);
    add_resource_handler(g_endpoint_coap_ctx, &resource_rd_monitor);
    add_resource_handler(g_endpoint_coap_ctx, &resource_dp);
    add_resource_handler(g_endpoint_coap_ctx, &ams_cfg_rd);
    add_resource_handler(g_endpoint_coap_ctx, &resource_version);
    add_resource_handler(g_endpoint_coap_ctx, &resource_reset);
    add_resource_handler(g_endpoint_coap_ctx, &resource_ilink);
#ifdef BUILTIN_IBROKER
    add_resource_handler(g_endpoint_coap_ctx, &resource_ibroker);
#endif
}

// callback function
// it is callled when the outgoing coap request is responded
//static int cb_foward_ep_response_to_cloud(void *ctx_data, void *data, int len, unsigned char format)
static int cb_foward_ep_response_to_cloud(void *ctx_data, void *data)
{
    coap_endpoint_call_t *ctx = (coap_endpoint_call_t *) ctx_data;
    ilink_message_t  msg;

    init_ilink_message(&msg, COAP_OVER_TCP);
    msg.has_mid = ctx->has_ilink_mid;
    msg.msgID = ctx->origin_ilink_id;
    msg.is_req = 0;

    if(data)
    {
        // cover the coap over UDP format to coap over TCP format
        coap_packet_t *coap_message = (coap_packet_t *)data;
        if (ctx->token_len)
            coap_set_token(coap_message, ctx->token, ctx->token_len);
        else if (ctx->has_ilink_mid)
            coap_set_token(coap_message, (const uint8_t*)&ctx->origin_ilink_id, sizeof(ctx->origin_ilink_id));

        uint8_t *packet = NULL;
        size_t packet_len = coap_serialize_message_tcp(coap_message, &packet);
        if(packet_len == 0) LOG_GOTO("ep response packet len = 0.", end);
        ilink_set_payload(&msg, packet, packet_len);
        ilink_msg_send(&msg);
        reset_ilink_message(&msg);

        free (packet);
    }
    else
    {
        // todo:
        LOG_MSG("ep response data was NULL!");
        // maybe to send a error response for timeout
    }

end:
    if(ctx_data)
    {
        trans_free_ctx(ctx_data);
    }

    return 0;
}


int send_ilink_request_to_ep(char *epname, ilink_message_t *ilink_msg, coap_packet_t *coap_message)
{
    uint8_t packet[COAP_MAX_PACKET_SIZE];
    connection_t *conn = get_endpoint_conn((const char *) epname);

    if (conn == NULL)
    {
        // todo: return broker not_found
        LOG_RETURN(0);
    }

    void *device = find_device((const char *) epname);
    if (device == NULL || is_passive_device(device))
    {
        WARNING("reject access to passive device %s\n", epname);
        LOG_RETURN(0);
    }

    // the coap over tcp use token to match request and response
    // the process here will keep the token in the request to ep
    // and the ep should send back the token in the response
    coap_status_t coap_error_code = NO_ERROR;
    const char *url = NULL;
    int url_len = coap_get_header_uri_path(coap_message, &url);
    if(url == NULL) LOG_RETURN(0);

    // check if the target resource is defined separate url
    bool url_alloc = false;
    char *seperate_url = NULL;
    char *separate_params = NULL;
    char *url_str = get_string((char*)url, url_len, &url_alloc);
    if(check_seperate_url(epname, url_str, coap_message->code, &seperate_url, &separate_params))
    {
        coap_set_header_uri_path(coap_message, seperate_url);
        if(separate_params)
            coap_set_header_uri_query(coap_message, separate_params);
    }
    if(url_alloc) free(url_str);

    unsigned long msg_id = bh_gen_id(get_outgoing_requests_ctx());
//    coap_message->mid = coap_get_mid();
    coap_set_token(coap_message, (const uint8_t *)&msg_id, sizeof(msg_id));

    coap_endpoint_call_t *data = (coap_endpoint_call_t *)trans_malloc_ctx(sizeof(coap_endpoint_call_t));
    memset (data, 0, sizeof(*data));
    data->origin_coap_id = coap_message->mid;
    data->token_len = coap_message->token_len;
    memcpy (data->token, coap_message->token, coap_message->token_len);

    data->has_ilink_mid = ilink_msg->has_mid;
    if (ilink_msg->has_mid)
    {
        data->origin_ilink_id = ilink_msg->msgID;
    }

    uip_ipaddr_t addr;
    memset (&addr, 0, sizeof(addr));
    addr.addr_type = A_Sock_Addr;
    memcpy (addr.raw, &(conn->addr), conn->addrLen);
    addr.addr_len = conn->addrLen;

    coap_message->mid = coap_get_mid();
    coap_message->type = COAP_TYPE_CON;
    coap_transaction_t *transaction = coap_new_transaction(
            g_endpoint_coap_ctx,
            coap_message->mid, &addr);

    coap_set_response_handler(transaction, coap_message,cb_foward_ep_response_to_cloud, data );

    transaction->packet_len = coap_serialize_message(coap_message, transaction->packet);
    coap_send_transaction(transaction);

    if(separate_params) free(separate_params);
    if(seperate_url) free(seperate_url);

    return 0;
}



static int cb_foward_ep_response_to_bus(void *ctx_data, void *data)
{

    module_to_endpoint_call_t *ctx = (module_to_endpoint_call_t *) ctx_data;

    if (data)
    {
        coap_packet_t *coap_message = (coap_packet_t *) data;

        MESSAGE_HANDLE bus_msg = coap_to_bus_msg(coap_message, ctx->origin_bus_msg_id, ctx->source_module_id);
        publish_message_on_broker(bus_msg);
        Message_Destroy(bus_msg);

    }
    else
    {
        LOG_MSG("ep response data was NULL");
        // maybe to send a error response for timeout
    }

    if (ctx_data)
    {
        trans_free_ctx(ctx_data);
    }
    return 0;
}


int send_bus_request_to_ep(char *epname, coap_packet_t *coap_message, uint32_t bus_msg_id, char *src_module)
{
    connection_t *conn = get_endpoint_conn((const char *) epname);
    if(conn == NULL)
    {
        // todo: return broker not_found
        LOG_RETURN(0);
    }

    void *device = find_device((const char *) epname);
    if(device == NULL || is_passive_device(device))
    {
        WARNING("reject access from bus to passive device %s\n", epname);
        LOG_RETURN(0);
    }

    module_to_endpoint_call_t *data = (module_to_endpoint_call_t*) trans_malloc_ctx(sizeof(module_to_endpoint_call_t));
    memset (data, 0, sizeof(*data));
    data->origin_bus_msg_id = bus_msg_id;
    if(src_module) strcpy (data->source_module_id, src_module);

    coap_set_token(coap_message, (const uint8_t *) &bus_msg_id, sizeof(bus_msg_id));
    coap_message->mid = coap_get_mid();

    uip_ipaddr_t addr;
    memset (&addr, 0, sizeof(addr));
    addr.addr_type = A_Sock_Addr;
    memcpy (addr.raw, &conn->addr, conn->addrLen);
    addr.addr_len = conn->addrLen;

    coap_transaction_t *transaction = coap_new_transaction(
            g_endpoint_coap_ctx,
            coap_message->mid, &addr);

    coap_set_response_handler(transaction, coap_message, cb_foward_ep_response_to_bus, data);

    transaction->packet_len = coap_serialize_message(coap_message, transaction->packet);

    if (coap_send_transaction(transaction))
    {
        trans_free_ctx(data);
    }

    return 0;
}


