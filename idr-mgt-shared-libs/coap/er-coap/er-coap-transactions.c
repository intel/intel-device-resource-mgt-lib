/*
 * Copyright (c) 2013, Institute for Pervasive Computing, ETH Zurich
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * This file is part of the Contiki operating system.
 */

/**
 * \file
 *      CoAP module for reliable transport
 * \author
 *      Matthias Kovatsch <kovatsch@inf.ethz.ch>
 */

/*
 *  modified on: Jul 2, 2016
 *      Author: xin.wang@intel.com
 *      changes: 1. ported to linux. 2. support response handler for request
 */


#include "er-coap.h"
#include "list_coap.h"
#include "coap_request.h"
#include "er-coap-transactions.h"


#define COAP_DEBUG 1

#if COAP_DEBUG
#include <stdio.h>
#define PRINTF(...) printf("[coap] " __VA_ARGS__)
#define PRINT6ADDR(addr) PRINTF("[%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x]", ((uint8_t *)addr)[0], ((uint8_t *)addr)[1], ((uint8_t *)addr)[2], ((uint8_t *)addr)[3], ((uint8_t *)addr)[4], ((uint8_t *)addr)[5], ((uint8_t *)addr)[6], ((uint8_t *)addr)[7], ((uint8_t *)addr)[8], ((uint8_t *)addr)[9], ((uint8_t *)addr)[10], ((uint8_t *)addr)[11], ((uint8_t *)addr)[12], ((uint8_t *)addr)[13], ((uint8_t *)addr)[14], ((uint8_t *)addr)[15])
#define PRINTLLADDR(lladdr) PRINTF("[%02x:%02x:%02x:%02x:%02x:%02x]", (lladdr)->addr[0], (lladdr)->addr[1], (lladdr)->addr[2], (lladdr)->addr[3], (lladdr)->addr[4], (lladdr)->addr[5])
#else
#define PRINTF(...)
#define PRINT6ADDR(addr)
#define PRINTLLADDR(addr)
#endif

#if !defined(COAP_TRANS_LOCK)
#define COAP_TRANS_LOCK(ctx)
#define COAP_TRANS_UNLOCK(ctx )
#endif



/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/*- Internal API ------------------------------------------------------------*/
/*---------------------------------------------------------------------------*/

list_t coap_get_transactions(coap_context_t * context)
{
	return (list_t) &context->transactions;
}

coap_transaction_t *
coap_new_transaction(coap_context_t * context,uint16_t mid, uip_ipaddr_t *addr)
{
  coap_transaction_t *t = (coap_transaction_t*) malloc(sizeof(coap_transaction_t));

	if(t)
	{
		memset(t,0, sizeof(*t));

		t->mid = mid;
		t->retrans_counter = 0;
		t->context = context;
		t->retrans_timer = context->default_retrans_ms;

		/* save client address */
		memcpy(&t->addr, addr, sizeof(*addr));

		// update the existing transactions timeout before adding a new one
		coap_update_transactions(context, NULL);

		list_t transactions_list = coap_get_transactions(context);

		COAP_TRANS_LOCK(context);
		list_add_coap(transactions_list, (list_t*)t); /* list itself makes sure same element is not added twice */
		COAP_TRANS_UNLOCK(context);
		PRINTF("new transaction: [%p], mid=%u\n", t, mid);
	}
	else
	{
		PRINTF("new transaction failed\n");
	}

  return t;
}

coap_transaction_t *
coap_new_transaction_ipaddr(coap_context_t * context,uint16_t mid, char *ip, uint16_t port)
{
	uip_ipaddr_t addr;
	memset(&addr, 0, sizeof(uip_ipaddr_t));
	set_addr_ip(&addr, ip, port);
	return coap_new_transaction(context, mid, &addr);
}


/*---------------------------------------------------------------------------*/
// return indicates if the transaction was freed
uint8_t
coap_send_transaction(coap_transaction_t *t)
{
  uint8_t clear = 0;
  coap_context_t * ctx = t->context;

  if(COAP_TYPE_CON ==
     ((COAP_HEADER_TYPE_MASK & t->packet[0]) >> COAP_HEADER_TYPE_POSITION))
  {
    if(t->retrans_counter < ctx->default_retrans_cnt) {
  	  t->retrans_timer = ctx->default_retrans_ms ;
      t->retrans_counter ++;

      PRINTF("Sending transaction %u, retrans cnt: %d\n", t->mid, t->retrans_counter);
      ctx->tx_data(ctx, &t->addr, t->packet, t->packet_len);

    }
    else
    {
      /* timed out */
    	coap_ongoing_request_t * ongoing_request = t->ongoing_request;

      if(ongoing_request && ongoing_request->callback) {
    	  restful_response_handler callback = ongoing_request->callback;
        callback(ongoing_request->callback_data, NULL);
      }

	    // remove the associated ongoing request from the list
	    if(t->ongoing_request)
	    {
	    	free_ongoing_request(t->context, t->ongoing_request );
			t->ongoing_request = NULL;
	    }

	  coap_clear_transaction(t);


      clear = 1;
    }
  }
  else
  {
	  PRINTF("Sending NON-CON transaction %u, retrans cnt: %d\n", t->mid, t->retrans_counter);
	  ctx->tx_data(ctx, &t->addr, t->packet, t->packet_len);

	    // remove the associated ongoing request from the list
	    if(t->ongoing_request)
	    {
	    	free_ongoing_request(t->context, t->ongoing_request );
			t->ongoing_request = NULL;
	    }
    coap_clear_transaction(t);
    clear = 1;
  }

  return clear;
}

/*---------------------------------------------------------------------------*/
void
coap_clear_transaction(coap_transaction_t *t)
{
  if(t) {
    PRINTF("Freeing transaction %u: %p\n", t->mid, t);
    list_t transactions_list = coap_get_transactions(t->context);

    COAP_TRANS_LOCK(t->context);
    list_remove_coap(transactions_list, (list_t*)t);


    coap_ongoing_request_t * ongoing_request = t->ongoing_request;
	if(ongoing_request )
	{
		assert ( ongoing_request->transaction == t);
		ongoing_request->transaction = NULL;
	}
	COAP_TRANS_UNLOCK(t->context);

    free(t);
  }
}

/// note for the thread safe issue:
/// it is possible one thread is referring a transaction which is
/// returned from coap_get_transaction_by_mid(), another thread
/// release it.
/// although we can protect the list through lock, but it is difficult
/// to protect above situation. so we require the callers to ensure
/// the transaction destroy must be done in the same thread, that means
/// the hanle_coap_packet() and coap_check_transactions() must be called
/// in one thread.

coap_transaction_t *
coap_get_transaction_by_mid(coap_context_t * context, uint16_t mid)
{
  coap_transaction_t *t = NULL;
  list_t transactions_list = coap_get_transactions(context);

  COAP_TRANS_LOCK(context);
  for(t = (coap_transaction_t *)list_head(transactions_list); t; t = t->next) {
    if(t->mid == mid) {
      COAP_TRANS_UNLOCK(context);

      PRINTF ("Found transaction for MID %u: %p\n", mid, t);
      return t;
    }
  }

  COAP_TRANS_UNLOCK(context);
  PRINTF ("Failed to find transaction for MID %u\n", mid);
  return NULL;
}

/*---------------------------------------------------------------------------*/
#define EXPIRY_HANDLING_NUM 10
uint32_t coap_check_transactions(coap_context_t * context)
{
  coap_transaction_t *t = NULL;
  coap_transaction_t * expired_trans[EXPIRY_HANDLING_NUM] = {0};
  coap_transaction_t * prev = NULL;

  // set default timer at 60s even no transaction exist
  uint32_t nearest_timeout = 60000;
  uint8_t  idx = 0;
  uint32_t elpased_ms = get_elpased_ms(&context->last_checktime);
  list_t transactions_list = coap_get_transactions(context);

  COAP_TRANS_LOCK(context);
  for(t = (coap_transaction_t *)list_head(transactions_list); t; )
  {
    if(elpased_ms >= t->retrans_timer)
    {
    	if(prev) prev->next = t->next;
    	expired_trans[idx++] = t;

    	// only handle EXPIRY_HANDLING_NUM
    	if(idx == EXPIRY_HANDLING_NUM)
    	{
    		// the caller should continue to call this function for remaining expired trans
    		nearest_timeout = 0;
    		break;
    	}
    }
    else
    {
    	t->retrans_timer -= elpased_ms;
    	prev = t;
    	if(nearest_timeout > t->retrans_timer)
           	nearest_timeout = t->retrans_timer;

    }

    t = t->next;
  }

  COAP_TRANS_UNLOCK(context);

  int i = 0;
  for(i=0;i<idx;i++)
  {
	  // coap_send_transaction can cause create or delete transaction into the list,
	  // NEVER calling this function during traverse the trans list, here we remove
	  // all expired transaction from list before calling this function
	  if(!coap_send_transaction(expired_trans[i]))
	  {
		  COAP_TRANS_LOCK(context);
		  list_add_coap(transactions_list, (list_t*)expired_trans[i]);
		  COAP_TRANS_UNLOCK(context);

		  if(nearest_timeout > expired_trans[i]->retrans_timer)
		       	nearest_timeout = expired_trans[i]->retrans_timer;
	  }
  }

  return nearest_timeout;
}


/*---------------------------------------------------------------------------*/

// return the nearest transaction timeout
// so the caller can use it for set timer
uint32_t
coap_update_transactions(coap_context_t * context, uint16_t * expired_num)
{
  coap_transaction_t *t = NULL;
  uint16_t expired = 0;
  uint32_t nearest_timeout = -1;
  uint32_t elpased_ms = get_elpased_ms(&context->last_checktime);
  list_t transactions_list = coap_get_transactions(context);

  COAP_TRANS_LOCK(context);
  for(t = (coap_transaction_t *)list_head(transactions_list); t; t = t->next) {
    if(elpased_ms >= t->retrans_timer) {
    	t->retrans_timer = 0;
    	expired ++;
    }
    else
    {
    	t->retrans_timer -= elpased_ms;
    }

    if(nearest_timeout == -1)
    	nearest_timeout = t->retrans_timer;
    else if(nearest_timeout > t->retrans_timer)
    	nearest_timeout = t->retrans_timer;
  }
  COAP_TRANS_UNLOCK(context);

  if(expired_num) *expired_num = expired;

  return nearest_timeout;
}



///////////////////////////////////////////////////////////////////////////
//
//    ONGOING REQUEST FUNCTIONS
//
//
///////////////////////////////////////////////////////////////////////////

list_t coap_get_ongoing_requests(coap_context_t * context)
{
	return (list_t) &context->ongoing_requests;
}


void coap_set_response_handler(coap_transaction_t * transaction, coap_packet_t *coap_message,void * callback, void *callback_data)
{
	uint32_t observe;

	if(coap_message->token_len == 0)
	{
		PRINTF("coap_set_response_handler: token len = 0!!\n");
		return;
	}
	coap_ongoing_request_t *t;
/*	 following code is problemtic - for the blockwse get/put
	t = coap_find_ongoing_request(transaction->context, coap_message->token_len, coap_message->token);

	if(t != coap_message)
	{
		PRINTF("coap_set_response_handler: found token from ongoing list!!\n");
		if(t->transaction == transaction)
		{
			assert (transaction->ongoing_request == t);
			return;
		}

		if(t->transaction)
		{
			t->transaction->ongoing_request = NULL;
			t->transaction = transaction;
			return;
		}
		else
		{
			assert(1);
		}
	}
*/
	t = (coap_ongoing_request_t*) malloc(sizeof(coap_ongoing_request_t));

	if(t == NULL) return;

	memset(t,0, sizeof(*t));
	t->callback = callback;
	t->callback_data = callback_data;
	t->token_len = MIN(coap_message->token_len, sizeof(t->token));
	t->observe = coap_get_header_observe(coap_message, &observe);
	memcpy(t->token, coap_message->token, t->token_len);

	COAP_TRANS_LOCK(transaction->context);
	transaction->ongoing_request = t;
	t->transaction = transaction;
	COAP_TRANS_UNLOCK(transaction->context);

	PRINTF("malloc ongoing request ctx [%p], attached to transaction [%p]\n", t, transaction);

	list_t requests = coap_get_ongoing_requests(transaction->context);

	COAP_TRANS_LOCK(transaction->context);
	list_add_coap(requests, (list_t*)t); /* list itself makes sure same element is not added twice */
	COAP_TRANS_UNLOCK(transaction->context);
}

coap_ongoing_request_t * coap_find_ongoing_request(coap_context_t * context, int token_len, char * token)
{
	coap_ongoing_request_t *t = NULL;
	list_t requests = coap_get_ongoing_requests(context);

	  COAP_TRANS_LOCK(context);
	  for(t = (coap_ongoing_request_t *)list_head(requests); t; t = t->next) {
	    if(t->token_len == token_len && memcmp(token, t->token, token_len) == 0) {
	      COAP_TRANS_UNLOCK(context);
	      //PRINTF ("Found coap_ongoing_request for MID %u: %p\n", mid, t);
	      return t;
	    }
	  }

	  COAP_TRANS_UNLOCK(context);
	  PRINTF ("Failed to find coap_ongoing_request for token\n");
	  return NULL;
}

void run_request_callback(coap_ongoing_request_t * ongoing_request, void * message)
{
	restful_response_handler callback;
	void *callback_data;

	if(ongoing_request == NULL) return;

	callback = ongoing_request->callback;
	/* check if someone registered for the response */
	if(callback)
	{
		callback(ongoing_request->callback_data, message);
	}
}

void free_ongoing_request(coap_context_t * context, coap_ongoing_request_t * ongoing_request)
{
	list_t requests = coap_get_ongoing_requests(context);
	COAP_TRANS_LOCK(context);
	list_remove_coap(requests, (list_t*)ongoing_request);
	COAP_TRANS_UNLOCK(context);

	PRINTF("free ongoing request ctx [%p], deattached to transaction [%p]\n", ongoing_request, ongoing_request->transaction);

	COAP_TRANS_LOCK(context);
	if(ongoing_request->transaction)
	{
		coap_transaction_t * t = (coap_transaction_t*)ongoing_request->transaction;
		t->ongoing_request = NULL;
	}
	COAP_TRANS_UNLOCK(context);

	free(ongoing_request);
}
