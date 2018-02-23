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
 *      An abstraction layer for RESTful Web services (Erbium).
 *      Inspired by RESTful Contiki by Dogan Yazar.
 * \author
 *      Matthias Kovatsch <kovatsch@inf.ethz.ch>
 */

#include <string.h>
//#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include "rest-engine.h"
#include "list_coap.h"
#include "er-coap-engine.h"
#include "coap_ext.h"
/*---------------------------------------------------------------------------*/
LIST(restful_services);
LIST(restful_periodic_services);

/* the discover resource is automatically included for CoAP */
extern resource_t res_well_known_core;


extern service_callback_t service_cbk;

/* avoid initializing twice */
static uint8_t initialized = 0;

/*---------------------------------------------------------------------------*/
/*- REST Engine API ---------------------------------------------------------*/
/*---------------------------------------------------------------------------*/
/**
 * \brief Initializes and starts the REST Engine process
 *
 * This function must be called by server processes before any resources are
 * registered through rest_activate_resource().
 */
void rest_init_engine_from_array(rest_engine_init_t * service_array)
{
  if(initialized) {
      //printf("REST engine process already running - double initialization?\n");
    return;
  }

  initialized = 1;
  list_init_coap(restful_services);

  coap_set_service_callback(rest_invoke_restful_service);

  /* Start the RESTful server implementation. */
  rest_activate_resource(&res_well_known_core,       ".well-known/core");
  if(service_array != NULL)
  {
	  int i=0;
	  while((service_array+i)->res !=0)
	  {
		  rest_activate_resource((service_array+i)->res,    (service_array+i)->uri);
		  i++;
	  }

  }
}
/*---------------------------------------------------------------------------*/
/*- REST Engine API ---------------------------------------------------------*/
/*---------------------------------------------------------------------------*/
/**
 * \brief Initializes and starts the REST Engine process
 *
 * This function must be called by server processes before any resources are
 * registered through rest_activate_resource().
 */
void rest_init_engine(void)
{
  if(initialized) {
      //printf("REST engine process already running - double initialization?\n");
    return;
  }

  initialized = 1;
  list_init_coap(restful_services);

  coap_set_service_callback(rest_invoke_restful_service);

  /* Start the RESTful server implementation. */
  rest_activate_resource(&res_well_known_core, ".well-known/core");


}
/*---------------------------------------------------------------------------*/
/**
 * \brief Makes a resource available under the given URI path
 * \param resource A pointer to a resource implementation
 * \param path The URI path string for this resource
 *
 * The resource implementation must be imported first using the
 * extern keyword. The build system takes care of compiling every
 * *.c file in the ./resources/ sub-directory (see example Makefile).
 */
void rest_activate_resource(resource_t *resource, char *path)
{
  resource->url = path;
  list_add_coap/*list_add*/(restful_services, resource);

  printf("Activating: %s\n", resource->url);

  /* Only add periodic resources with a periodic_handler and a period > 0. */
  if((resource->flags & IS_PERIODIC) && resource->periodic->periodic_handler
     && resource->periodic->period) {
    printf("Periodic resource: %p (%s)\n", resource->periodic,
           resource->periodic->resource->url);
    list_add_coap/*list_add*/(restful_periodic_services, resource->periodic);
  }
}
/*---------------------------------------------------------------------------*/
/*- Internal API ------------------------------------------------------------*/
/*---------------------------------------------------------------------------*/
list_t rest_get_resources(void)
{
  return restful_services;
}


uint8_t find_restful_service(const char *url, int url_len)
{
    resource_t *resource = NULL;
    for(resource = (resource_t *)list_head(restful_services); resource; resource = resource->next)
    {
        /* if the web service handles that kind of requests and urls matches */
        url_len = coap_get_header_uri_path(resource, &url);
        if((url_len == strlen(resource->url)
            || (url_len > strlen(resource->url)
            && (resource->flags & HAS_SUB_RESOURCES)
            && url[strlen(resource->url)] == '/'))
            && strncmp(resource->url, url, strlen(resource->url)) == 0)
        {
        	return 1;
        }
    }
    return 0;
}



/*---------------------------------------------------------------------------*/
int rest_invoke_restful_service(void *request, void *response,uint8_t *buffer,
                            uint16_t buffer_size, int32_t *offset)
{
    uint8_t found = 0;
    uint8_t allowed = 1;

    resource_t *resource = NULL;
    const char *url = NULL;
    int url_len;

    for(resource = (resource_t *)list_head(restful_services); resource; resource = resource->next)
    {
        /* if the web service handles that kind of requests and urls matches */
        url_len = coap_get_header_uri_path(request, &url);
        if((url_len == strlen(resource->url)
            || (url_len > strlen(resource->url)
            && (resource->flags & HAS_SUB_RESOURCES)
            && url[strlen(resource->url)] == '/'))
            && strncmp(resource->url, url, strlen(resource->url)) == 0)
        {
            found = 1;
            rest_resource_flags_t method = coap_get_rest_method(request);

            printf("/%s, method %u, resource->flags %u\n", resource->url, (uint16_t)method, resource->flags);

            if((method & METHOD_GET) && resource->get_handler != NULL)
            {
                /* call handler function */
                resource->get_handler(request, response, buffer, buffer_size, offset);
            }
            else if((method & METHOD_POST) && resource->post_handler != NULL)
            {
                /* call handler function */
                resource->post_handler(request, response, buffer, buffer_size, offset);
            }
            else if((method & METHOD_PUT) && resource->put_handler != NULL)
            {
                /* call handler function */
                resource->put_handler(request, response, buffer, buffer_size, offset);
            }
            else if((method & METHOD_DELETE) && resource->delete_handler != NULL)
            {
                /* call handler function */
                resource->delete_handler(request, response, buffer, buffer_size, offset);
            }
            else
            {
                allowed = 0;
                coap_set_status_code(response, METHOD_NOT_ALLOWED_4_05);
            }
            break;
        }
    }
    if(!found)
    {
        coap_set_status_code(response, NOT_FOUND_4_04);
    }
    else if(allowed)
    {
        /* final handler for special flags */
        //if(resource->flags & IS_OBSERVABLE) {
        //	coap_observe_handler(resource, request, response);
    }

    return (found & allowed);
}


// need to design the buffer management mechanism
// 1. for ilink the payload can be large, better let the
// 2.
int serve_request(coap_packet_t *message, char * response_packet, int *len)
{
	coap_status_t erbium_status_code = NO_ERROR;
	coap_packet_t response[1];

	/* handle requests */
	assert(message->code >= COAP_GET && message->code <= COAP_DELETE) ;

	uint32_t block_num = 0;
	uint16_t block_size = REST_MAX_CHUNK_SIZE;
	uint32_t block_offset = 0;
	int32_t new_offset = 0;

	/* prepare response */
	if(message->type == COAP_TYPE_CON) {
		/* reliable CON requests are answered with an ACK */
		coap_init_message(response, COAP_TYPE_ACK, CONTENT_2_05,
				message->mid);
	} else {
		/* unreliable NON requests are answered with a NON as well */
		coap_init_message(response, COAP_TYPE_NON, CONTENT_2_05,
				coap_get_mid());
		/* mirror token */
	} if(message->token_len) {
		coap_set_token(response, message->token, message->token_len);
		/* get offset for blockwise transfers */
	}
	if(coap_get_header_block2
			(message, &block_num, NULL, &block_size, &block_offset)) {
		printf("Blockwise: block request %lu (%u/%u) @ %lu bytes\n",
				(unsigned long)block_num, block_size, REST_MAX_CHUNK_SIZE, (unsigned long)block_offset);
		block_size = MIN(block_size, REST_MAX_CHUNK_SIZE);
		new_offset = block_offset;
	}

	/* invoke resource handler */
	/* call REST framework and check if found and allowed */
	if(service_cbk
		&&(service_cbk(message, response, response_packet + COAP_MAX_HEADER_SIZE, block_size, &new_offset))
		&& (erbium_status_code == NO_ERROR)) {

				/* TODO coap_handle_blockwise(request, response, start_offset, end_offset); */

				/* resource is unaware of Block1 */
				if(IS_OPTION(message, COAP_OPTION_BLOCK1)
						&& response->code < BAD_REQUEST_4_00
						&& !IS_OPTION(response, COAP_OPTION_BLOCK1)) {
					printf("Block1 NOT IMPLEMENTED\n");

					erbium_status_code = NOT_IMPLEMENTED_5_01;
					coap_error_message = "NoBlock1Support";

					/* client requested Block2 transfer */
				} else if(IS_OPTION(message, COAP_OPTION_BLOCK2)) {

					/* unchanged new_offset indicates that resource is unaware of blockwise transfer */
					if(new_offset == block_offset) {
						printf
						("Blockwise: unaware resource with payload length %u/%u\n",
								response->payload_len, block_size);
						if(block_offset >= response->payload_len) {
							printf
							("handle_incoming_data(): block_offset >= response->payload_len\n");

							response->code = BAD_OPTION_4_02;
							coap_set_payload(response, "BlockOutOfScope", 15); /* a const char str[] and sizeof(str) produces larger code size */
						} else {
							coap_set_header_block2(response, block_num,
									response->payload_len -
									block_offset > block_size,
									block_size);
							coap_set_payload(response,
									response->payload + block_offset,
									MIN(response->payload_len -
											block_offset, block_size));
						} /* if(valid offset) */

						/* resource provides chunk-wise data */
					} else {
						printf("Blockwise: blockwise resource, new offset %ld\n",
								(long)new_offset);
						coap_set_header_block2(response, block_num,
								new_offset != -1
								|| response->payload_len > block_size, block_size);

						if(response->payload_len > block_size)
							coap_set_payload(response, response->payload, block_size);

					} /* if(resource aware of blockwise) */

					/* Resource requested Block2 transfer */
				} else if(new_offset != 0) {
					printf
					("Blockwise: no block option for blockwise resource, using block size %u\n",
							COAP_MAX_BLOCK_SIZE);

					coap_set_header_block2(response, 0, new_offset != -1 ||(response->payload_len > COAP_MAX_BLOCK_SIZE),
							COAP_MAX_BLOCK_SIZE);

					if(response->payload_len > COAP_MAX_BLOCK_SIZE)
						coap_set_payload(response, response->payload, COAP_MAX_BLOCK_SIZE);

				} /* blockwise transfer handling */
			} /* no errors/hooks */
			/* successful service callback */
			/* serialize response */
	else
	{
		erbium_status_code = NOT_IMPLEMENTED_5_01;
		coap_error_message = "NoServiceCallbck"; /* no 'a' to fit into 16 bytes */
	} /* if(service callback) */

	if(erbium_status_code == NO_ERROR)
	{
		int packet_len = coap_serialize_message(response,
				response_packet);
		if(packet_len == 0)
		{
			erbium_status_code = PACKET_SERIALIZATION_ERROR;
		}
		else
		{
			* len = packet_len;
		}
	}

	if(erbium_status_code != NO_ERROR)
	{
		coap_set_status_code(response, erbium_status_code);
		coap_set_payload(response, coap_error_message,
				strlen(coap_error_message));
		* len = coap_serialize_message(response, response_packet);
	}

	return (response->code != NOT_IMPLEMENTED_5_01);
}

int serve_request_from_tcp(coap_packet_t* message, char **response_packet, int *len)

{
	coap_status_t erbium_status_code = NO_ERROR;
	coap_packet_t response[1];
	char buffer[1024*4]; //

	/* handle requests */
	assert(message->code >= COAP_GET && message->code <= COAP_DELETE) ;
	int32_t offset = 0;
	int packet_len;

		/* prepare response */
	if(message->type == COAP_TYPE_CON) {
			/* reliable CON requests are answered with an ACK */
		coap_init_message(response, COAP_TYPE_ACK, CONTENT_2_05,
					message->mid);
	} else {
			/* unreliable NON requests are answered with a NON as well */
		coap_init_message(response, COAP_TYPE_NON, CONTENT_2_05,
					coap_get_mid());
			/* mirror token */
	}

	if(message->token_len) {
		coap_set_token(response, message->token, message->token_len);
	}

	/* invoke resource handler */
    if((service_cbk)
		    &&(service_cbk(message, response, buffer + COAP_MAX_HEADER_SIZE, sizeof(buffer), &offset)))
    {
        coap_set_payload_tcp(response, response->payload, offset);
    }
   else
   {
		erbium_status_code = NOT_IMPLEMENTED_5_01;
		coap_error_message = "NoServiceCallbck"; /* no 'a' to fit into 16 bytes */

	} /* if(service callback) */

	if(erbium_status_code == NO_ERROR)
	{
		packet_len = coap_serialize_message_tcp(response,
					(uint8_t **)response_packet);
		if(packet_len == 0)
		{
			erbium_status_code = PACKET_SERIALIZATION_ERROR;
		}
		else
		{
			* len = packet_len;
		}
	}

	if(erbium_status_code != NO_ERROR)
	{
		coap_set_status_code(response, erbium_status_code);
		coap_set_payload(response, coap_error_message,
					strlen(coap_error_message));
		* len = coap_serialize_message_tcp(response, (uint8_t **)response_packet);
	}
	return (response->code != NOT_IMPLEMENTED_5_01);


}

#if NOT_USED
/*-----------------------------------------------------------------------------------*/
PROCESS_THREAD(rest_engine_process, ev, data, buf, user_data)
{
  PROCESS_BEGIN();

  /* pause to let REST server finish adding resources. */
  PROCESS_PAUSE();

  /* initialize the PERIODIC_RESOURCE timers, which will be handled by this process. */
  periodic_resource_t *periodic_resource = NULL;

  for(periodic_resource =
        (periodic_resource_t *)list_head(restful_periodic_services);
      periodic_resource; periodic_resource = periodic_resource->next) {
    if(periodic_resource->periodic_handler && periodic_resource->period) {
      printf("Periodic: Set timer for /%s to %lu\n",
             periodic_resource->resource->url, periodic_resource->period);
      etimer_set(&periodic_resource->periodic_timer,
                 periodic_resource->period, &rest_engine_process);
    }
  }
  while(1) {
    PROCESS_WAIT_EVENT();

    if(ev == PROCESS_EVENT_TIMER) {
      for(periodic_resource =
            (periodic_resource_t *)list_head(restful_periodic_services);
          periodic_resource; periodic_resource = periodic_resource->next) {
        if(periodic_resource->period
           && etimer_expired(&periodic_resource->periodic_timer)) {

          printf("Periodic: etimer expired for /%s (period: %lu)\n",
                 periodic_resource->resource->url, periodic_resource->period);

          /* Call the periodic_handler function, which was checked during adding to list. */
          (periodic_resource->periodic_handler)();

          etimer_reset(&periodic_resource->periodic_timer);
        }
      }
    }
  }

  PROCESS_END();
}
/*---------------------------------------------------------------------------*/
#endif
