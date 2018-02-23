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


/*
 * ports_handler.c
 *
 * 1. Handle the data from ilink (TCP), coap end points (UDP), coap client(UDP)
 * 2. Collaborate with ibroker sender thread on the ilink connection status
 *
 *  Created on: Oct 25, 2016
 *      Author: xwang98
 *
 * Design notes:
 *  1. socket thread sync: sender and reciver threads sync up through
 *     the global variable : g_cloud_status
 *  2. connect(), accept() are blocking operations. We move the socket
 *     create, close and connect into the sender thread
 *  3. after the socket is connected, only after handshaking is finished
 *     other modules can send messages to cloud
 *  4. multiple endpoints: use the connection.c functions under /external/wakaama/samples/shared
 *  5. coap client: send the response to source of the request
 *
 */

#include <time.h>
#include "iagent_base.h"
#include "ilink_message.h"
#include "broker_rest_convert.h"
#include "logs.h"
#include "rd.h"

//external
#include "er-coap.h"
#include "rest-engine.h"
#include "ams_constants.h"
#include "er-coap-transactions.h"
#include "ams_sdk_interface.h"

//azure
#ifdef RUN_AS_BROKER_MODULE
#include "azure_c_shared_utility/doublylinkedlist.h"
dlist_entry_ctx_t g_internal_queue;
#endif

int g_socket_coap_ep = -1;
extern int g_cloud_sock;


extern char g_quit_thread;
cloud_status_e g_cloud_status = iReady_To_Connect;

extern resource_t res_get_core_version;
extern resource_t res_get_ilink;
extern resource_t res_set_reset;

extern void init_endpoint_coap_ctx();
extern void check_internal_message();
extern void ResRefresherExpiry(uint32_t *next_expiry_ms);
extern int check_ping_timeout();


void cfg_change_cb(const char *product_name, const char *target_type, const char *target_id, const char *cfg_file_name)
{
    TraceI (FLAG_CLOUD_CONFIG, ">>>>>cfg_change_cb1. type=%s, id=%s, file=%s\n", target_type, target_id?target_id:"", cfg_file_name);
    if(cfg_file_name !=NULL && target_type != NULL && strcmp(target_type, TT_DEVICE_ON_GW) == 0)
    {
        // todo: we can't do it in the ams thread.
        //       post a message to rd thread and do it there.
        TraceI (FLAG_CLOUD_CONFIG, ">>>>> calling reload_client_config\n");
        reload_client_config((char*)target_id);
    }
}


int my_ams_client_status_callback(int result)
{
    if(result == AMS_Ready)
    {
        char *iagent_id = load_iagent_id();
        if (iagent_id)
        {
            WARNING("ams client found. set the product id [%s] and device id to ams", get_self_agent_id());
            ams_set_product_id(get_self_agent_id());
            ams_add(TT_DEVICE, iagent_id,true);

            // for download the device.cfg and group.cfg for the iagent itself
            iagent_register_AMS();
        }
        else
        {
            ams_add(TT_DEVICE, "",false);
        }
    }
}

int init_agent()
{
    load_iagent_id();

    ilink_init();

#ifdef RUN_AS_BROKER_MODULE
    DList_InitializeListHead(&(g_internal_queue.list_queue));
    g_internal_queue.thread_mutex = Lock_Init();
#endif

    ams_init(SW_IAGENT, cfg_change_cb, my_ams_client_status_callback);

    TraceI (FLAG_INIT, "ams_init done\n");

    rest_activate_resource(&res_get_core_version, "iagent/version");
    rest_activate_resource(&res_get_ilink, "ilink");
    rest_activate_resource(&res_set_reset, "reset");

    // create the iagent client in the RD and register to AMS.
    // NOTE: the AMS client may not be ready at this moment,
    // so we need to register AMS again in the AMS ready callback.
    init_resource_directory();

    return 0;
}


int get_coap_ep_sock()
{
    return g_socket_coap_ep;
}


bool wakeup_ports_thread()
{
    return  wakeup_main_thread(get_coap_ep_sock());
}

//struct sockaddr_in sockaddr_send;
//int g_cmd_from_stdin_or_remote = 0  ;//0 stdin,1:remote UDP command


int handle_cloud_packet(char *message, int string_len, recv_context_t *ctx)
{
    int result = -1;
    char *pos = message;

    while (string_len >0)
    {
        result = on_byte_arrive((unsigned char)*pos, ctx);
        string_len--;
        pos ++;

        // if current in payload recieiving, do payload copy
        if (result == 2)
        {
            int len = string_len;
            if (0 == on_payload_block(ctx, pos, &len))
            {
                handle_ilink_message(ctx);
            }
            string_len -= len;
            pos += len;
        }
        else if (0 == result)
        {
            handle_ilink_message(ctx);
        }
    }
    return result;
}


void *thread_ilink_port_handler(void *arg)
{
#ifdef RUN_ON_LINUX
    prctl (PR_SET_NAME, "iagent_port_handler");
#endif
    int result = 0, i = 0, opt = 0;
    unsigned int sock_errs = 0;
    fd_set readfds;
    struct timeval tv;
    uint8_t * buffer = (uint8_t * ) malloc(COAP_MAX_PACKET_SIZE);

    int address_family = AF_INET;
    static recv_context_t ibroker_ctx;

    init_endpoint_coap_ctx();
    g_endpoint_coap_ctx->buf = buffer;
    g_endpoint_coap_ctx->buf_size = COAP_MAX_PACKET_SIZE;


    load_configured_endpoints();

    load_configured_data_monitors();


    while (g_quit_thread == 0)
    {

        bool on_cloud_sock = false;

        // put the internal message handling before expire checks, it may finish some transactions
        check_internal_message();

        // check timeout for all async transactions
        uint32_t next_expiry_ms = bh_handle_expired_trans(get_outgoing_requests_ctx());
#ifdef BUILTIN_IBROKER
        // check timeout for ID provisionning
        if (get_cloud_connection_status() == iCloud_Provisioning)
        {
            int next = check_provision_timeout();
            if (next >0 && (next_expiry_ms== -1 || next < next_expiry_ms))
                next_expiry_ms = next;
        }

        // check timeout for the handshaking
        if (get_cloud_connection_status() == iCloud_Handshaking
                && !handshake_done())
        {
            int next = check_handshake_timeout();
            if (next >0 && (next_expiry_ms== -1 || next < next_expiry_ms))
                next_expiry_ms = next;
        }

        // check timeout for the ping
        if (get_cloud_connection_status() == iReady_For_Work)
        {
            int next = check_ping_timeout();
            if (next >0 && (next_expiry_ms== -1 || next < next_expiry_ms))
                next_expiry_ms = next;
        }
#endif
        //reconnet cloud after the connection failed
        if (get_cloud_connection_status() == iError_In_Connection)
            set_cloud_connection_status (iReady_To_Connect);

        uint32_t next = coap_check_transactions(g_endpoint_coap_ctx);
        if (next != -1 && (next_expiry_ms== -1 || next < next_expiry_ms))
            next_expiry_ms = next;

        next = check_blockwise_timeout_ms(g_endpoint_coap_ctx, 5);
        if (next != -1 && (next_expiry_ms== -1 || next < next_expiry_ms))
            next_expiry_ms = next;

        // run the regular resource reading process,
        // and reschedule for the nearest resource refresher timeout
        // !!! note: put it after the transaction check, so timeout of read transaction
        //           can trigger immediate the new read transaction
        ResRefresherExpiry(&next_expiry_ms);

        if (next_expiry_ms != -1)
        {
            tv.tv_sec = next_expiry_ms / 1000;
            tv.tv_usec = (next_expiry_ms % 1000) * 1000;
        }
        else
        {
            tv.tv_sec = 30;
            tv.tv_usec = 0;
        }

        // reset socket
        FD_ZERO(&readfds);
        if (g_cloud_status >= iSocket_Connected)
        {
            FD_SET(g_cloud_sock,&readfds);
            on_cloud_sock = true;
        }
        FD_SET(g_socket_coap_ep, &readfds);

        result = select (FD_SETSIZE, &readfds, 0, 0, &tv);

        if (result < 0)
        {
            if (errno != EINTR)
            {
                ERROR ("iagent: Error in select(): %d\r\n", errno);
            }
        }
        else if (result == 0)  //timeout
        {
        }
        else if (result > 0)
        {

            int numBytes;

            if (on_cloud_sock && FD_ISSET(g_cloud_sock, &readfds))
            {
                numBytes = recv(g_cloud_sock, buffer, COAP_MAX_PACKET_SIZE, 0);
                if(numBytes == 0)
                {
                    set_cloud_connection_status (iReady_To_Connect);
                }
                else if(numBytes == -1)
                {
                    if(errno == ECONNRESET)
                        set_cloud_connection_status (iReady_To_Connect);
                    else
                        sock_errs ++;
                }
                else
                {
                    handle_cloud_packet((char*)buffer, numBytes, &ibroker_ctx);
                }
            }
            else if (FD_ISSET(g_socket_coap_ep, &readfds))
            {
                struct sockaddr_storage addr;
                socklen_t addrLen;

                addrLen = sizeof(addr);
                numBytes = recvfrom(g_socket_coap_ep, buffer, COAP_MAX_PACKET_SIZE, 0,
                		(struct sockaddr *)&g_endpoint_coap_ctx->src_addr.sock_addr, &addrLen);

                if (numBytes == -1)
                {
                   ERROR( "Error in recvfrom() for g_socket_coap_ep, errno = %d\r\n", errno);
                }
                else
                {
                    // To make main thread select on g_cloud_sock immediately
                    if (numBytes == 5 && strncmp((const char*) buffer, "wake", 4) == 0)
                    {
                        continue;
                    }
                    g_endpoint_coap_ctx->buf_len = numBytes;
                    g_endpoint_coap_ctx->src_addr.addr_len = addrLen;
                    g_endpoint_coap_ctx->src_addr.addr_type = A_Sock_Addr;

                    coap_handle_packet(g_endpoint_coap_ctx);

                }
            }

        }
    }
    close(g_socket_coap_ep);
    //connection_free(connList);
    return 0;
}

