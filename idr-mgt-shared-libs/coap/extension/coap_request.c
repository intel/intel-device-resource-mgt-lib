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
 * coap_request.c
 *
 *  Created on: Jul 2, 2016
 *      Author: xin.wang@intel.com
 */

#include <stdio.h>
#include <unistd.h>
#include "coap_request.h"
#include "coap_ext.h"



#ifndef COAP_CONTEXT_CONF_MAX_CONTEXTS
#define MAX_CONTEXTS 5
#else
#define MAX_CONTEXTS COAP_CONTEXT_CONF_MAX_CONTEXTS
#endif /* COAP_CONTEXT_CONF_MAX_CONTEXTS */

extern void cleanup_blockwise_list(coap_context_t * coap_ctx);


static coap_context_t coap_contexts[MAX_CONTEXTS] = {0};

void free_blockwise_state(struct request_state_t *state )
{
	if(state->request)
	{
		if(state->request->payload)
		{
			free(state->request->payload);
		}
		free(state->request);
	}

	free(state);
}

/*---------------------------------------------------------------------------*/
coap_context_t *
coap_context_new(uip_ipaddr_t *my_addr)
{
  coap_context_t *ctx = NULL;
  int i;
  for(i = 0; i < MAX_CONTEXTS; i++)
  {
    if(!coap_contexts[i].is_used)
    {
      ctx = &coap_contexts[i];
      break;
    }
  }

  if(ctx == NULL) {
      COAP_WARN("coap-context: no free contexts\n");
    return NULL;
  }

  PRINTF("coap-context: allocated context at slot %d\n", i);

  memset(ctx, 0, sizeof(coap_context_t));
  memcpy(&ctx->my_addr, my_addr, sizeof(*my_addr));


  ctx->is_used = 1;
  ctx->transaction_lock = coap_create_lock();

  ctx->default_retrans_ms = COAP_RESPONSE_TIMEOUT_TICKS;
  ctx->default_retrans_cnt = COAP_MAX_RETRANSMIT;

  return ctx;
}


/*---------------------------------------------------------------------------*/
void
coap_context_close(coap_context_t *coap_ctx)
{
  if(coap_ctx == NULL || coap_ctx->is_used == 0) {
    return;
  }

#ifndef NO_BLOCKWISE_SUPPORT
  cleanup_blockwise_list(coap_ctx);
#endif

  coap_ctx->is_used = 0;
}

/*---------------------------------------------------------------------------
void
coap_ctx_send(coap_context_t *coap_ctx, uint8_t *data,
                  uint16_t length)
{
  //PRINTF("coap_send_message-sent UDP datagram (%u)-\n", length);

  coap_ctx->tx_data(&coap_ctx->addr, data, length);
}
*/

void coap_send_message(coap_context_t *coap_ctx, uip_ipaddr_t *addr, uint16_t port, uint8_t *data,
                       uint16_t length)
{
	coap_ctx->tx_data(coap_ctx, addr, data, length);
}

/*---------------------------------------------------------------------------*/
/*- Internal API ------------------------------------------------------------*/
/*---------------------------------------------------------------------------*/
/*

   Client  Server
      |      |
      |      |
      +----->|     Header: GET (T=CON, Code=0.01, MID=0x7d38)
      | GET  |      Token: 0x53
      |      |   Uri-Path: "temperature"
      |      |
      |      |
      |<- - -+     Header: (T=ACK, Code=0.00, MID=0x7d38)
      |      |
      |      |
      |<-----+     Header: 2.05 Content (T=CON, Code=2.05, MID=0xad7b)
      | 2.05 |      Token: 0x53
      |      |    Payload: "22.3 C"
      |      |
      |      |
      +- - ->|     Header: (T=ACK, Code=0.00, MID=0xad7b)
      |      |

             Figure 20: Confirmable request; separate response


*/
extern int coap_request_handler (void * ctx , void * message);

int
coap_handle_packet(coap_context_t *coap_ctx)
{
	erbium_status_code = NO_ERROR;
	coap_packet_t message[1]; 	/* this way the packet can be treated as pointer as usual */
	coap_transaction_t *transaction = NULL;

	erbium_status_code = coap_parse_message(
			message, coap_ctx->buf, coap_ctx->buf_len);

	if(erbium_status_code == NO_ERROR) {

		/*TODO duplicates suppression, if required by application */
//		pr_info(LOG_MODULE_MAIN, "  Parsed: v %u, t %u, tkl %u, c %u, mid %u\n", message->version,
//				message->type, message->token_len, message->code, message->mid);

		/* handle requests */
		if(message->code >= COAP_GET && message->code <= COAP_DELETE)
		{

#ifndef NO_BLOCKWISE_SUPPORT
			coap_request_handler(coap_ctx, message);
#else
			char  response_buf[COAP_MAX_PACKET_SIZE];
			int len = 0;
			if(serve_request(message, response_buf, &len) && len > 0)
			{
					coap_ctx->tx_data(coap_ctx, &coap_ctx->src_addr, response_buf, len);
			}
#endif

		}
		else
		{

			/* handle responses */
			if(message->type == COAP_TYPE_CON && message->code == 0)
			{
				PRINTF("RX Ping\n");
				erbium_status_code = PING_RESPONSE;

			} else if(message->type == COAP_TYPE_ACK)
			{
				/* transactions are closed through lookup below */
				//PRINTF("ACK\n");

			} else if(message->type == COAP_TYPE_RST)
			{
				PRINTF("RST\n");
				/* cancel possible subscriptions */
			}

			if((transaction = coap_get_transaction_by_mid(coap_ctx, message->mid)))
			{
				/* free transaction memory before callback, as it may create a new transaction */
			    // remove the associated ongoing request from the list
				coap_ongoing_request_t * ongoing_request = transaction->ongoing_request;
			    if(ongoing_request)
			    {
			    	if(message->code != 0 ||  message->type == COAP_TYPE_RST)
			    		run_request_callback(ongoing_request, message);

			    	// this is separate response (code == 0), or obs is enabled, we should not release the callback for request.
					if((message->code != 0 && ongoing_request->observe == 0) || message->type == COAP_TYPE_RST)
					{
						free_ongoing_request(coap_ctx, ongoing_request);
					}
			    }

				coap_clear_transaction(transaction);
			}
			else	// handle separate response situation
			{
				coap_ongoing_request_t * ongoing_request = coap_find_ongoing_request(coap_ctx, message->token_len, message->token);

				if(ongoing_request)
				{
					run_request_callback(ongoing_request, message);

					if(ongoing_request->observe == 0 || message->type == COAP_TYPE_RST)
						free_ongoing_request(coap_ctx, ongoing_request);
				}
			}

			// send ack for separate response in a confirmable message.
			if(message->type == COAP_TYPE_CON)
			{
				// note: this situation should rare, so we malloc the response from stack.
				coap_packet_t * response = (coap_packet_t*) malloc(sizeof(coap_packet_t));
				if(response == NULL) return -1;

				coap_init_message(response, COAP_TYPE_ACK, 0, message->mid);
				send_coap_msg(coap_ctx, response);
				free(response);
			}

		} /* request or response */
	} else { /* parsed correctly */
		//NET_COAP_STAT(recv_err++);
	}



	/* if(new data) */
	return erbium_status_code;
}



/*---------------------------------------------------------------------------*/
/*- Client Part -------------------------------------------------------------*/
/*---------------------------------------------------------------------------*/

// the put callback is guaranteed called only once
// caller should do the user data cleanup once it is called.

int
coap_nonblocking_put_callback(void *callback_data, void *response_msg)
{
	struct request_state_t *state = (struct request_state_t *)callback_data;

	coap_packet_t *request = state->request;
	restful_response_handler response_handler = state->response_callback;
	uint8_t more;
	uint32_t res_block;
	uint8_t block_error;
	uint32_t offset = 0;
	uint16_t size = 0;
	more = 0;
	res_block = 0;
	block_error = 0;
	void * user_data = state->user_data;
	int ret = 0;

	coap_packet_t * response = (coap_packet_t *)response_msg;
	if(response == NULL) {
	    COAP_WARN("%s, Server not responding\n", __FUNCTION__);
		goto cleanup;
	}
	if(response->type == COAP_TYPE_RST) {
	    COAP_WARN("%s, Server RESET\n", __FUNCTION__);
		goto cleanup;
	}


	coap_get_header_block1(response, &res_block, &more, &size, &offset);

	PRINTF("%s: Received #%u%s (%u bytes), code=%d\n", __FUNCTION__, res_block, more ? "+" : "",
			response->payload_len, response->code);

	if(CREATED_2_01  == response->code)
	{
		if(response_handler) response_handler(user_data, response);

		PRINTF("%s: state completed for CREATED_2_01 recieved\n", __FUNCTION__);
		goto cleanup2;
	}

	if(res_block == state->block_num)
	{
		++(state->block_num);
	}
	else
	{
	    COAP_WARN("WRONG BLOCK %u/%u\n", res_block, state->block_num);
		++block_error;
		goto cleanup;
	}

	// according to CoAP, response code CONTINUE_2_31 must have more bit
	if(CONTINUE_2_31 == response->code)
	{
		if(!more)
			goto cleanup;
	}

	// check if finished sending all the payload
	else if(CHANGED_2_04 == response->code )
	{
		if(request->payload_len < (state->block_num*state->block_size))
		{
			if(response_handler) response_handler(user_data, response);
			goto cleanup2;
		}
	}
        else
        {
            COAP_WARN("%s: invalid response code: %d\n", __FUNCTION__, response->code);
              goto cleanup;
        }

	request->mid = coap_get_mid();
	state->transaction = coap_new_transaction(state->transaction->context,
	            request->mid,
		    &state->transaction->addr);

	if(state->transaction) {

		coap_packet_t send_msg[1];
		memcpy(send_msg, state->request, sizeof(send_msg[0]));
		int payload_len = state->block_size;
		more = 1;

		if(payload_len > request->payload_len - (state->block_num*state->block_size))
		{
			payload_len = request->payload_len - (state->block_num*state->block_size);
			more = 0;
		}
		coap_set_payload(send_msg, send_msg->payload + state->block_num*state->block_size, payload_len);
		coap_set_header_block1(send_msg, state->block_num, more, state->block_size);

		coap_set_response_handler(state->transaction, send_msg, coap_nonblocking_put_callback, state);

		state->transaction->packet_len = coap_serialize_message(send_msg,
				state->
				transaction->
				packet);

		if(coap_send_transaction(state->transaction))
                    goto cleanup;

		PRINTF("%s: sent block [%d], more=%d, blocksize=%d, payload len=%d\n", __FUNCTION__,state->block_num,more, state->block_size,payload_len);
	}
	else
	{
		PRINTF("Could not allocate transaction buffer");
		goto cleanup;
	}

	return ret;

cleanup:

   // Tell caller whole request is ended.
if(response_handler) response_handler(user_data, NULL);

cleanup2:

    free_blockwise_state(state);

	PRINTF("%s: blockwise for state %p finished\n", __FUNCTION__, state);

	return ret;
}

/*---------------------------------------------------------------------------*/
// Note:
// 1. the parameter "request" and its "payload" field will be duplicated by this function,
//     so it should be freed when transaction finished.
// 2. need to be aware of the sync issue about the transaction list (added lock)
//
void coap_nonblocking_put(
				coap_context_t *coap_ctx,
				uip_ipaddr_t *dst_addr,
				coap_packet_t *request,
				restful_response_handler request_callback,
				void * user_data)
{
	// check if it needs blockwise transfer

	request->mid = coap_get_mid();
	coap_packet_t send_msg[1];
	uint8_t more;
	uint16_t size;

	if(COAP_MAX_BLOCK_SIZE >= request->payload_len)
	{
		coap_transaction_t * transaction = coap_new_transaction(
					coap_ctx,
					request->mid, dst_addr);
		transaction->packet_len = coap_serialize_message(request, transaction->packet);

		coap_set_response_handler(transaction, request, request_callback, user_data);

		coap_send_transaction(transaction);
		return;
	}

	struct request_state_t *state = malloc(sizeof(struct request_state_t));

	memset(state, 0 , sizeof(*state));

	state->block_num = 0;
	state->block_size = COAP_MAX_BLOCK_SIZE;
	state->response_callback = request_callback;
	state->user_data = user_data;

	state->request = malloc(sizeof(*request));
	memcpy(state->request, request, sizeof(*request));

	// always use confirmable type for blockwise put/post request
	state->request->type = COAP_TYPE_CON ;

	if(request->payload_len)
	{
		state->request->payload = malloc(request->payload_len);
		memcpy(state->request->payload, request->payload, request->payload_len);
		state->request->payload_len = request->payload_len;
	}

	if((state->transaction = coap_new_transaction(coap_ctx,	request->mid, dst_addr)))
	{


		memcpy(send_msg, state->request, sizeof(send_msg[0]));

		coap_set_header_block1(send_msg, state->block_num, 1, COAP_MAX_BLOCK_SIZE);
		coap_set_header_size1(send_msg, state->request->payload_len);

		coap_set_payload(send_msg, send_msg->payload, COAP_MAX_BLOCK_SIZE);
		state->transaction->packet_len = coap_serialize_message(send_msg,
				state->transaction->packet);

		coap_set_response_handler(state->transaction, send_msg, coap_nonblocking_put_callback, state);

		coap_send_transaction(state->transaction);

		PRINTF("%s: blockwise for state %p created\n", __FUNCTION__, state);
	}
}


// for the get callback, the callback can be called multiple times
// the user should treat end of transaction when:
// 1. the response parameter in the calling callback is NULL
// 2. if the more field in the block2 is 0
//
int
coap_nonblocking_request_callback(void *callback_data, void *response_msg)
{
	struct request_state_t *state = (struct request_state_t *)callback_data;

	coap_packet_t *request = state->request;
	restful_response_handler response_handler = state->response_callback;
	uint8_t more;
	uint32_t res_block;
	uint8_t block_error;
	more = 0;
	res_block = 0;
	block_error = 0;
	void * user_data = state->user_data;
	int ret = 0;

	coap_packet_t * response = (coap_packet_t *)response_msg;
	if(!response) {
	    COAP_WARN("%s, Server not responding\n", __FUNCTION__);
		goto cleanup;
	}

	if(response->type == COAP_TYPE_RST) {
	    COAP_WARN("%s, Server RESET\n", __FUNCTION__);
		goto cleanup;
	}

	coap_get_header_block2(response, &res_block, &more, NULL, NULL);

	PRINTF("%s: Received #%u%s (%u bytes)\n", __FUNCTION__, res_block, more ? "+" : "",
			response->payload_len);

	if(res_block == state->block_num)
	{
		if(STOP_REQUEST == response_handler(user_data, response))
		{
			goto cleanup;
		}
		++(state->block_num);
	}
	else
	{
	    COAP_WARN("WRONG BLOCK %u/%u\n", res_block, state->block_num);
		++block_error;
	}

	if(!more)
		goto cleanup2;

	request->mid = coap_get_mid();
	state->transaction = coap_new_transaction(state->transaction->context,
	            request->mid, &state->transaction->addr);
	if(state->transaction) {

		coap_set_response_handler(state->transaction, request, coap_nonblocking_request_callback, state);

		if(state->block_num > 0) {
			coap_set_header_block2(request, state->block_num, 0,
					REST_MAX_CHUNK_SIZE);
		}
		state->transaction->packet_len = coap_serialize_message(request,
				state->
				transaction->
				packet);
		coap_send_transaction(state->transaction);
	} else {
	    COAP_WARN("Could not allocate transaction buffer");
		goto cleanup;
	}

	return ret;

cleanup:

   // Tell caller whole request is ended.
   response_handler(user_data, NULL);

cleanup2:

	free_blockwise_state(state);

	PRINTF("%s: blockwise for state %p finished\n", __FUNCTION__, state);

	return ret;
}


/*---------------------------------------------------------------------------*/
// Note:
// 1. need to be aware of the sync issue about the transaction list
//
void coap_nonblocking_get(
				coap_context_t *coap_ctx,
				uip_ipaddr_t *dst_addr,
				coap_packet_t *request,
				restful_response_handler request_callback,
				void * user_data,
				uint32_t block_num)
{
	uint8_t free_state = 1;
	struct request_state_t *state = malloc(sizeof(struct request_state_t));


	memset(state, 0 , sizeof(*state));

	state->block_num = block_num;
	state->response_callback = request_callback;
	state->user_data = user_data;

	// duplicate the request message, so we can repeat sending it later
	state->request = malloc(sizeof(*request));
	memcpy(state->request, request, sizeof(*request));
	if(request->payload_len)
	{
		state->request->payload = malloc(request->payload_len);
		memcpy(state->request->payload, request->payload, request->payload_len);
		state->request->payload_len = request->payload_len;
	}

	request->mid = coap_get_mid();
	if((state->transaction = coap_new_transaction(
			coap_ctx,
			request->mid, dst_addr))) {

		coap_set_response_handler(state->transaction, request, coap_nonblocking_request_callback, state);

		state->transaction->packet_len = coap_serialize_message(request,
				state->transaction->packet);

		if(coap_send_transaction(state->transaction))
		{
			state->transaction = NULL;
		}
		else
		{
			free_state = 0;
			PRINTF("%s: blockwise for state %p created\n", __FUNCTION__, state);
		}
	}

	if(state && free_state)
	{
		free_blockwise_state(state);
	}
}


// use should NOT set "request_callback" and "user_data"
// if the request type is not confirmable.
//
// the callback may not be called in this situation. That can introduce
// memory leak if user rely on the callback for cleanup of "user_data"
void coap_nonblocking_request(
				coap_context_t *coap_ctx,
				uip_ipaddr_t *dst_addr,
				coap_packet_t *request,
				restful_response_handler request_callback,
				void * user_data)
{

    // ensure the request handler will be working if it is set by user
	if(request_callback)
	{
	    request->type = COAP_TYPE_CON;
	    if(request->token_len == 0)
	    {
	        coap_set_token(request, &request->mid, sizeof(request->mid));
	    }
	}


	if(request->code == COAP_GET)
		coap_nonblocking_get(coap_ctx,dst_addr,request, request_callback,
						user_data, 0);
	else
		coap_nonblocking_put(coap_ctx,dst_addr,request, request_callback,
						user_data);
}


