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
 * coap_request.h
 *
 *  Created on: Jul 2, 2016
 *      Author: xin.wang@intel.com
 */

#ifndef SRC_COAP_REQUEST_H_
#define SRC_COAP_REQUEST_H_

#include "coap_platforms.h"
#include "../er-coap/er-coap.h"
#include "../er-coap/er-coap-transactions.h"
#include "../rest-engine/rest-engine.h"

#ifdef __cplusplus
extern "C" {
#endif




// return
// STOP_REQUEST : stop the whole request procedure
#define STOP_REQUEST -1




typedef coap_packet_t rest_request_t;
typedef coap_packet_t rest_response_t;

#define COAP_CONTEXT_NONE  NULL

void coap_init_engine(void);
coap_context_t * coap_context_new(uip_ipaddr_t *my_addr);

/*---------------------------------------------------------------------------*/
/*- Client Part -------------------------------------------------------------*/
/*---------------------------------------------------------------------------*/

extern int coap_nonblocking_request_callback(void *callback_data, void *response);

//
// request_state_t is designed for a block-wise request over multiple transactions
//
struct request_state_t {
  coap_transaction_t *transaction;
  coap_packet_t *request;
  restful_response_handler response_callback;
  void * user_data;
  uint32_t block_num;
  uint16_t block_size;
};



void coap_nonblocking_put(
				coap_context_t *coap_ctx,
				uip_ipaddr_t *dst_addr,
				coap_packet_t *request,
				restful_response_handler request_callback,
				void * user_data);

void coap_nonblocking_get(
				coap_context_t *coap_ctx,
				uip_ipaddr_t *dst_addr,
				coap_packet_t *request,
				restful_response_handler request_callback,
				void * user_data,
				uint32_t block_num);

// this API cover both get, put and post
void coap_nonblocking_request(
				coap_context_t *coap_ctx,
				uip_ipaddr_t *dst_addr,
				coap_packet_t *request,
				restful_response_handler request_callback,
				void * user_data);

int
coap_handle_packet(coap_context_t *coap_ctx);

uint8_t coap_is_request(coap_packet_t * coap_message);




#ifdef __cplusplus
}
#endif

#endif /* SRC_COAP_REQUEST_H_ */
