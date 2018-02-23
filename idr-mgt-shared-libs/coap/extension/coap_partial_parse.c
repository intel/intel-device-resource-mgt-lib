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
 * coap_partial_parse.c
 *
 *  Created on: Nov 2, 2016
 *      Author: xin.wang@intel.com
 */
#include <stdint.h>

#include "er-coap.h"

int
coap_parse_partial(void *packet, uint8_t *data, uint16_t data_len)
{
  coap_packet_t *const coap_pkt = (coap_packet_t *)packet;

  /* initialize packet */
  memset(coap_pkt, 0, sizeof(coap_packet_t));

  /* pointer to packet bytes */
  coap_pkt->buffer = data;

  /* parse header fields */
  coap_pkt->version = (COAP_HEADER_VERSION_MASK & coap_pkt->buffer[0])
    >> COAP_HEADER_VERSION_POSITION;
  coap_pkt->type = (COAP_HEADER_TYPE_MASK & coap_pkt->buffer[0])
    >> COAP_HEADER_TYPE_POSITION;
  coap_pkt->token_len =
    MIN(COAP_TOKEN_LEN,
        (COAP_HEADER_TOKEN_LEN_MASK & coap_pkt->
         buffer[0]) >> COAP_HEADER_TOKEN_LEN_POSITION);
  coap_pkt->code = coap_pkt->buffer[1];
  coap_pkt->mid = coap_pkt->buffer[2] << 8 | coap_pkt->buffer[3];

  if(coap_pkt->version != 1) {
    coap_error_message = "CoAP version must be 1";
    return BAD_REQUEST_4_00;
  }

  uint8_t *current_option = data + COAP_HEADER_LEN;

  memcpy(coap_pkt->token, current_option, coap_pkt->token_len);

  return 0;
}


uint16_t coap_find_mid(uint8_t *buffer)
{
	return (buffer[2] << 8 | buffer[3]);
}

void coap_change_mid(uint8_t *buffer, uint16_t id)
{
	buffer[3] = id & 0xFFFF;
	buffer[2] = (id >> 8) & 0xFFFF;
}

uint8_t coap_find_code(uint8_t *buffer)
{
	return (buffer[1]);
}


uint8_t coap_is_request(coap_packet_t * coap_message)
{
	if (coap_message->code >= COAP_GET && coap_message->code <= COAP_DELETE)
		return 1;
	else
		return 0;
}



char * coap_get_full_url_alloc(coap_packet_t * request)
{
	const char *url = NULL;
	const char * query = NULL;
	int  url_len = coap_get_header_uri_path(request, &url);
	int query_len = coap_get_header_uri_query(request, &query);

	if(url_len == 0)
		return NULL;

	char * url_alloc = (char*)malloc(url_len  + 1 + query_len + 1);
	memcpy(url_alloc, url, url_len);
	url_alloc[url_len] = 0;

	// make the url looks like /abc?e=f
	if(query_len != 0)
	{
		strcat(url_alloc, "&");
		memcpy(url_alloc + strlen(url_alloc),  query, query_len);
		url_alloc[url_len  + 1 + query_len] = 0;
	}

	return url_alloc;
}
