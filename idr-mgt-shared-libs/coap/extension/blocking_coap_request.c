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
 * blocking_coap_request.c
 *
 *  Created on: Oct 24, 2017
 *      Author: xin.wang@intel.com
 */

#include "coap_ext.h"

// return 0: success
/*---------------------------------------------------------------------------*/
int coap_blocking_request(
        coap_context_t *coap_ctx,
        uip_ipaddr_t *dst_addr,
        coap_packet_t *request,
        restful_response_handler request_callback,
        void * user_data)
{
    uint8_t more = 0;
    uint32_t res_block;
    uint8_t block_error;
    uint16_t block_size = REST_MAX_CHUNK_SIZE;
    int block_num = 0;
    int resend = 0;
    int erbium_status_code = NO_ERROR;
    int ret = -1;
    int default_retrans_ms = coap_ctx->default_retrans_ms;
    int default_retrans_cnt = coap_ctx->default_retrans_cnt;
    uint32_t last_system_clock = 0;

    if(default_retrans_ms == 0) default_retrans_ms = 2000;

    if(coap_ctx->buf == NULL) printf("coap_blocking_request: ctx buffer is NULL\n");

    uint8_t packet[COAP_MAX_PACKET_SIZE];

    more = 0;
    res_block = 0;
    block_error = 0;
    uint32_t elapsed_ms;

    do {
        request->mid = coap_get_mid();

        coap_set_header_block2(request, block_num, 0, block_size);
        PRINTF("coap_blocking_request request url <%s>, mid <%d>\n", request->uri_path ,request->mid);

        int packet_len = coap_serialize_message(request, packet);

        resend = 0;
        int erbium_status_code = NO_ERROR;
resend:

        coap_ctx->tx_data(coap_ctx, dst_addr, packet, packet_len);
        PRINTF("Sent Request #%u (MID %u)\n", block_num, request->mid);

        get_elpased_ms(&last_system_clock);
        int waiting_ms = default_retrans_ms;

recv:
        elapsed_ms = get_elpased_ms(&last_system_clock);
        if(waiting_ms > elapsed_ms)
            waiting_ms -= elapsed_ms;
        else
            waiting_ms = 1;

        ret = coap_ctx->rx_data(coap_ctx, coap_ctx->buf, coap_ctx->buf_size, waiting_ms);


        PRINTF("Response, ret=%d \n", ret);

        if( RX_TIMEOUT == ret)
        {
            if(resend == default_retrans_cnt)
            {
                COAP_WARN("coap_blocking_request: timeout with max retrans (%d)\n", default_retrans_cnt);
                return -1;
            }

            PRINTF("Resend the request\n");

            resend ++;
            goto resend;
        }

        coap_ctx->buf_len = ret;
        coap_packet_t response[1];

        erbium_status_code = coap_parse_message(
                response, coap_ctx->buf, coap_ctx->buf_len);

        PRINTF("coap_blocking_request response url <%s>, mid <%d>\n", request->uri_path ,response->mid);

        if(erbium_status_code != NO_ERROR) {
            COAP_WARN("coap_blocking_request: parse response error: %d\n", erbium_status_code);
            goto recv;
        }

        if(response->code >= COAP_GET && response->code <= COAP_DELETE) {
            COAP_WARN("coap_blocking_request: not response recieved. code=%d\n", response->code);
            goto recv;
        }

        if(response->mid != request->mid){
            COAP_WARN("coap_blocking_request: mid %d in response doesn't match outgoing request %d.\n", response->mid, request->mid);
            goto recv;
        }

        coap_get_header_block2(response, &res_block, &more, NULL, NULL);

        PRINTF("Received #%u%s (%u bytes)\n", res_block, more ? "+" : "",
                response->payload_len);

        if(res_block == block_num) {
            if(STOP_REQUEST == request_callback(user_data, response))
            {
            	ret = 0;
                break;
            }
            ++(block_num);
        } else {
            COAP_WARN("WRONG BLOCK %u/%u\n", res_block, block_num);
            ++block_error;
        }

        if(!more){
            ret = 0;
            break;
        }

        // todo: check the blocksize provided by the server in the response options

    } while(more && block_error < COAP_MAX_ATTEMPTS);

    return ret;
}


void free_coap_request_user_data(coap_request_user_data_t * data)
{
    if(data->payload) free(data->payload);
    free(data);
}

static int blocking_request_handler(coap_request_user_data_t * user_ctx, void *response) {

    coap_packet_t *resp = (coap_packet_t*) response;
    user_ctx->result = Fail;
    char uri_path[2048];


    if (response == NULL) {
        user_ctx->result = Timeout;
        return STOP_REQUEST;
    }

    if (resp->code > CONTENT_2_05 || CREATED_2_01 > resp->code)
    {
        user_ctx->result = Fail;
        return STOP_REQUEST;
    }

    if(resp->payload_len > 0)
    {
        char * buf = malloc(user_ctx->len + resp->payload_len + 1);
        if(buf == NULL)
            return STOP_REQUEST;

        if(user_ctx->len > 0)
        {
            memcpy(buf, user_ctx->payload,user_ctx->len);
            free(user_ctx->payload);
        }
        memcpy(&buf[user_ctx->len], resp->payload, resp->payload_len);

        buf[user_ctx->len + resp->payload_len] = 0;
        user_ctx->payload = buf;
        user_ctx->len = user_ctx->len + resp->payload_len;
    }

    user_ctx->result = Success;

    return 0;
}



static int blocking_request_handler_file(coap_request_user_data_t * user_ctx, void *response) {

    coap_packet_t *resp = (coap_packet_t*) response;

    user_ctx->result = Fail;

    if (response == NULL) {
        return STOP_REQUEST;
    }

    if (resp->code != CONTENT_2_05) {
        return STOP_REQUEST;
    }

    if(resp->payload_len > 0)
    {
        fwrite(resp->payload, 1, resp->payload_len, (FILE *)user_ctx->payload);
        fflush((FILE *) user_ctx->payload);
    }

    user_ctx->result = Success;

    return 0;
}


// save the payload in the returned user data
coap_request_user_data_t * make_blocking_request(
        coap_context_t * coap_ctx,
        coap_packet_t *request, uip_ipaddr_t * server_addr)
{
    int ret=-1;
    coap_request_user_data_t  * user_data = (coap_request_user_data_t*) malloc(sizeof(coap_request_user_data_t));
    if(user_data == NULL)
        return NULL;

    memset (user_data, 0, sizeof(coap_request_user_data_t));
    if(server_addr != NULL){
        ret = coap_blocking_request(coap_ctx,
                server_addr,
                    request,
                    blocking_request_handler,
                    user_data);
    }
    else{
        ret = coap_blocking_request_tcp(coap_ctx,
                    request,
                    blocking_request_handler,
                    user_data);
    }

    if(-1 != ret)
    {
        return user_data;
    }
    else
    {
        free_coap_request_user_data(user_data);
        return NULL;
    }
}


// save the payload to the target file
int make_blocking_request_to_file(
        coap_context_t * coap_ctx,
        coap_packet_t *request,
        char * filename,
        uip_ipaddr_t * server_addr)
{
    int ret = 0;
    coap_request_user_data_t  * user_data = (coap_request_user_data_t*) malloc(sizeof(coap_request_user_data_t));
    if(user_data == NULL)
        return -1;

    FILE *app_package =  fopen(filename, "a+");
    if ((app_package == NULL )) {
        COAP_WARN("fopen %s failed! \n",filename);
        free_coap_request_user_data(user_data);
        return 1;
    }
    memset (user_data, 0, sizeof(coap_request_user_data_t));
    user_data->payload = (char * )app_package;

    if(server_addr != NULL){
        ret = coap_blocking_request(coap_ctx,
                server_addr,
                    request,
                    blocking_request_handler_file,
                    user_data);

    }
    else {
        ret = coap_blocking_request_tcp(coap_ctx,
                    request,
                    blocking_request_handler_file,
                    user_data);
    }

    fclose(app_package);
    user_data->payload = NULL;
    free_coap_request_user_data(user_data);
    return ret;

}
