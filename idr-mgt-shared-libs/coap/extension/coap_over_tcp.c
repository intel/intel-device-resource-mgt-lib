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
 * coap_over_tcp.c
 *
 *  Created on: Jan 18, 2017
 *      Author: xin.wang@intel.com
 */

#include <string.h>
#include <stdio.h>
#include "er-coap.h"
#include "coap_ext.h"
#include "er-coap-constants.h"
#include "coap_request.h"


extern size_t
coap_serialize_array_option(unsigned int number, unsigned int current_number,
                            uint8_t *buffer, uint8_t *array, size_t length,
                            char split_char);
extern size_t
coap_serialize_int_option(unsigned int number, unsigned int current_number,
                          uint8_t *buffer, uint32_t value);
extern uint16_t coap_log_2(uint16_t value);
extern uint32_t coap_parse_int_option(uint8_t *bytes, size_t length);
extern void
coap_merge_multi_option(char **dst, size_t *dst_len, uint8_t *option,
                        size_t option_len, char separator);

/*
 *
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |Len=15 |  TKL  | Extended Length (32 bits)
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |               |    Code       |  Token (if any, TKL bytes) ...
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |   Options (if any) ...
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |1 1 1 1 1 1 1 1|    Payload (if any) ...
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

 */


int
coap_set_payload_tcp(void *packet, const void *payload, size_t length)
{
  coap_packet_t *const coap_pkt = (coap_packet_t *)packet;

  coap_pkt->payload = (uint8_t *)payload;
  coap_pkt->payload_len = MIN(1024*1024, length);

  return coap_pkt->payload_len;
}

static size_t coap_calc_ext_len_field(int len)
{
	if(len < 13)
		return 0;
	else if(len < (0xFF+13))
		return 1;
	else if(len < (0xFFFF+269))
		return 2;
	else if(len < (0xFFFFFFFF+65805))
		return 4;
	else
		return 0;
}

static size_t coap_max_options_offset(void *packet)
{
	coap_packet_t *const coap_pkt = (coap_packet_t *)packet;
	return 6 + coap_pkt->token_len;
}

size_t
coap_serialize_message_tcp(void *packet, uint8_t ** buffer_out)
{
	coap_packet_t *const coap_pkt = (coap_packet_t *)packet;
	uint8_t buffer[512];

	uint8_t *option = buffer;
	unsigned int current_number = 0;

	/* Serialize options */
	current_number = 0;
	if(0 == coap_pkt->token_len){
		memcpy(coap_pkt->token, &coap_pkt->mid,sizeof(coap_pkt->mid));
		coap_pkt->token_len = sizeof(coap_pkt->mid);
	}
	PRINTF("-Serializing options at %p-\n", option);

	if((coap_pkt->uri_host_len + coap_pkt->uri_path_len + coap_pkt->uri_query_len) > (sizeof(buffer) - 64))
	{
	    COAP_WARN("coap_serialize_message_tcp: the head is over the predefined size. \n");
	    return 0;
	}

	/* The options must be serialized in the order of their number */
	COAP_SERIALIZE_BYTE_OPTION(COAP_OPTION_IF_MATCH, if_match, "If-Match");
	COAP_SERIALIZE_STRING_OPTION(COAP_OPTION_URI_HOST, uri_host, '\0',
			"Uri-Host");
	COAP_SERIALIZE_BYTE_OPTION(COAP_OPTION_ETAG, etag, "ETag");
	COAP_SERIALIZE_INT_OPTION(COAP_OPTION_IF_NONE_MATCH,
			content_format -
			coap_pkt->
			content_format /* hack to get a zero field */,
			"If-None-Match");
	COAP_SERIALIZE_INT_OPTION(COAP_OPTION_OBSERVE, observe, "Observe");
	COAP_SERIALIZE_INT_OPTION(COAP_OPTION_URI_PORT, uri_port, "Uri-Port");
	COAP_SERIALIZE_STRING_OPTION(COAP_OPTION_LOCATION_PATH, location_path, '/',
			"Location-Path");
	COAP_SERIALIZE_STRING_OPTION(COAP_OPTION_URI_PATH, uri_path, '/',
			"Uri-Path");
	COAP_SERIALIZE_INT_OPTION(COAP_OPTION_CONTENT_FORMAT, content_format,
			"Content-Format");
	COAP_SERIALIZE_INT_OPTION(COAP_OPTION_MAX_AGE, max_age, "Max-Age");
	COAP_SERIALIZE_STRING_OPTION(COAP_OPTION_URI_QUERY, uri_query, '&',
			"Uri-Query");
	COAP_SERIALIZE_INT_OPTION(COAP_OPTION_ACCEPT, accept, "Accept");
	COAP_SERIALIZE_STRING_OPTION(COAP_OPTION_LOCATION_QUERY, location_query,
			'&', "Location-Query");
	COAP_SERIALIZE_BLOCK_OPTION(COAP_OPTION_BLOCK2, block2, "Block2");
	COAP_SERIALIZE_BLOCK_OPTION(COAP_OPTION_BLOCK1, block1, "Block1");
	COAP_SERIALIZE_INT_OPTION(COAP_OPTION_SIZE2, size2, "Size2");
	COAP_SERIALIZE_STRING_OPTION(COAP_OPTION_PROXY_URI, proxy_uri, '\0',
			"Proxy-Uri");
	COAP_SERIALIZE_STRING_OPTION(COAP_OPTION_PROXY_SCHEME, proxy_scheme, '\0',
			"Proxy-Scheme");
	COAP_SERIALIZE_INT_OPTION(COAP_OPTION_SIZE1, size1, "Size1");


	/* Pack payload */
	if(coap_pkt->payload_len)
	{
		*option = 0xFF;
		++option;
	}
	uint32_t option_len  = option - &buffer[0];

	uint8_t * p = (uint8_t *) malloc(coap_max_options_offset(packet) + option_len + coap_pkt->payload_len);
	if(p == NULL)
		return 0;
	*buffer_out = p;

	uint8_t  first_4bits;

	*p = (coap_pkt->token_len&0xF);
	uint32_t len = option_len + coap_pkt->payload_len;
	if(len < 13)
	{
		first_4bits = len;
		*p++ |= first_4bits <<4;
	}
	else if(len < (0xFF+13))
	{
		first_4bits = 13;
		*p++ |= first_4bits <<4;
		*p++ = len - 13;
	}
	else if(len < (0xFFFF+269))
	{
		first_4bits = 14;
		*p++ |= first_4bits <<4;
		len -=  269;
		*p = (uint8_t) (len >> 8);
		p++;
		*p = (uint8_t) (len & 0xFF);
		p++;
	}
	else
	{
		first_4bits = 15;
		*p++ |= first_4bits <<4;

		len -=  65805;
		*p++ = (uint8_t) (len >> 24);
		*p++ = (uint8_t) (len >> 16);
		*p++ = (uint8_t) (len >> 8);
		*p++ = (uint8_t) (len & 0xFF);
	}



	*p = coap_pkt->code;
	p++;

	if(coap_pkt->token_len)
		memcpy(p, coap_pkt->token, coap_pkt->token_len);
	p += coap_pkt->token_len;


	memcpy(p, buffer, option_len);
	p += option_len;

	memcpy(p, coap_pkt->payload, coap_pkt->payload_len);
	p += coap_pkt->payload_len;

	return (p - *buffer_out); /* packet length */
}



coap_status_t
coap_parse_message_tcp(void *packet, uint8_t *data, uint16_t data_len)
{
	coap_packet_t *const coap_pkt = (coap_packet_t *)packet;

	/* initialize packet */
	memset(coap_pkt, 0, sizeof(coap_packet_t));

	/* pointer to packet bytes */
	coap_pkt->buffer = data;

	/* parse header fields */
	coap_pkt->version = 1;
	coap_pkt->type = COAP_TYPE_NON;
	coap_pkt->token_len = MIN(COAP_TOKEN_LEN, data[0] & 0xF);
	coap_pkt->mid = 0;


	uint8_t *p = data;
	uint8_t first_4bits = data[0] >> 4;

	uint32_t options_payload_size;
	uint8_t ext_len_field = 0;
	if(first_4bits < 13)
	{
		options_payload_size = first_4bits;
		p++;
	}
	else if(first_4bits == 13)
	{
		ext_len_field = 1;
		options_payload_size = data[1] + 13;
		p += 2;
	}
	else if(first_4bits == 14)
	{
		ext_len_field = 2;
		options_payload_size = (uint16_t)(data[1]<<8) + data[2] + 269;
		p += 3;
	}
	else if(first_4bits == 15)
	{
		ext_len_field = 4;
		options_payload_size = data[1]<<24 + data[2]<<16 + data[3]<<8 + data[4] + 65805;
		p += 5;
	}

	// check the data size is smaller than the size indicated by the packet
	if(ext_len_field + coap_pkt->token_len + 2 + options_payload_size > data_len)
		return BAD_REQUEST_4_00;

	coap_pkt->code = *p ++;
	if(coap_pkt->token_len)
		memcpy(coap_pkt->token, p, coap_pkt->token_len);

	if(coap_pkt->token_len >= 2)
	{
		coap_pkt->mid = *((uint16_t*) (coap_pkt->token));
	}

	p += coap_pkt->token_len;

	uint8_t *current_option = p;
	uint8_t * option_start = p;


	/* parse options */
	memset(coap_pkt->options, 0, sizeof(coap_pkt->options));

	unsigned int option_number = 0;
	unsigned int option_delta = 0;
	size_t option_length = 0;

	while(current_option < data + data_len) {
		/* payload marker 0xFF, currently only checking for 0xF* because rest is reserved */
		if((current_option[0] & 0xF0) == 0xF0) {
			coap_pkt->payload = ++current_option;
			coap_pkt->payload_len = options_payload_size - (coap_pkt->payload - option_start);
			//coap_pkt->payload_len = data_len - (coap_pkt->payload - data);
			break;
		}

		option_delta = current_option[0] >> 4;
		option_length = current_option[0] & 0x0F;
		++current_option;

		/* avoids code duplication without function overhead */
		unsigned int *x = &option_delta;

		do {
			if(*x == 13) {
				*x += current_option[0];
				++current_option;
			} else if(*x == 14) {
				*x += 255;
				*x += current_option[0] << 8;
				++current_option;
				*x += current_option[0];
				++current_option;
			}
		} while(x != (unsigned int*)&option_length && (x = (unsigned int*)&option_length));

		option_number += option_delta;

		PRINTF("OPTION %u (delta %u, len %u): ", option_number, option_delta,
				option_length);

		SET_OPTION(coap_pkt, option_number);

		switch(option_number) {

		case COAP_OPTION_CONTENT_FORMAT:
			coap_pkt->content_format = coap_parse_int_option(current_option,
					option_length);
			PRINTF("Content-Format [%u]\n", coap_pkt->content_format);
			break;
		case COAP_OPTION_MAX_AGE:
			coap_pkt->max_age = coap_parse_int_option(current_option,
					option_length);
			PRINTF("Max-Age [%lu]\n", coap_pkt->max_age);
			break;
		case COAP_OPTION_ETAG:
			coap_pkt->etag_len = MIN(COAP_ETAG_LEN, option_length);
			memcpy(coap_pkt->etag, current_option, coap_pkt->etag_len);
			PRINTF("ETag %u [0x%02X%02X%02X%02X%02X%02X%02X%02X]\n",
					coap_pkt->etag_len, coap_pkt->etag[0], coap_pkt->etag[1],
					coap_pkt->etag[2], coap_pkt->etag[3], coap_pkt->etag[4],
					coap_pkt->etag[5], coap_pkt->etag[6], coap_pkt->etag[7]
			);                 /*FIXME always prints 8 bytes */
			break;
		case COAP_OPTION_ACCEPT:
			coap_pkt->accept = coap_parse_int_option(current_option, option_length);
			PRINTF("Accept [%u]\n", coap_pkt->accept);
			break;
		case COAP_OPTION_IF_MATCH:
			/* TODO support multiple ETags */
			coap_pkt->if_match_len = MIN(COAP_ETAG_LEN, option_length);
			memcpy(coap_pkt->if_match, current_option, coap_pkt->if_match_len);
			PRINTF("If-Match %u [0x%02X%02X%02X%02X%02X%02X%02X%02X]\n",
					coap_pkt->if_match_len, coap_pkt->if_match[0],
					coap_pkt->if_match[1], coap_pkt->if_match[2],
					coap_pkt->if_match[3], coap_pkt->if_match[4],
					coap_pkt->if_match[5], coap_pkt->if_match[6],
					coap_pkt->if_match[7]
			); /* FIXME always prints 8 bytes */
			break;
		case COAP_OPTION_IF_NONE_MATCH:
			coap_pkt->if_none_match = 1;
			PRINTF("If-None-Match\n");
			break;

		case COAP_OPTION_PROXY_URI:
#if COAP_PROXY_OPTION_PROCESSING
			coap_pkt->proxy_uri = (char *)current_option;
			coap_pkt->proxy_uri_len = option_length;
#endif
			PRINTF("Proxy-Uri NOT IMPLEMENTED [%.*s]\n", coap_pkt->proxy_uri_len,
					coap_pkt->proxy_uri);
			coap_error_message = "This is a constrained server (Contiki)";
			return PROXYING_NOT_SUPPORTED_5_05;
			break;
		case COAP_OPTION_PROXY_SCHEME:
#if COAP_PROXY_OPTION_PROCESSING
			coap_pkt->proxy_scheme = (char *)current_option;
			coap_pkt->proxy_scheme_len = option_length;
#endif
			PRINTF("Proxy-Scheme NOT IMPLEMENTED [%.*s]\n",
					coap_pkt->proxy_scheme_len, coap_pkt->proxy_scheme);
			coap_error_message = "This is a constrained server (Contiki)";
			return PROXYING_NOT_SUPPORTED_5_05;
			break;

		case COAP_OPTION_URI_HOST:
			coap_pkt->uri_host = (char *)current_option;
			coap_pkt->uri_host_len = option_length;
			PRINTF("Uri-Host [%.*s]\n", coap_pkt->uri_host_len, coap_pkt->uri_host);
			break;
		case COAP_OPTION_URI_PORT:
			coap_pkt->uri_port = coap_parse_int_option(current_option,
					option_length);
			PRINTF("Uri-Port [%u]\n", coap_pkt->uri_port);
			break;
		case COAP_OPTION_URI_PATH:
			/* coap_merge_multi_option() operates in-place on the IPBUF, but final packet field should be const string -> cast to string */
			coap_merge_multi_option((char **)&(coap_pkt->uri_path),
					&(coap_pkt->uri_path_len), current_option,
					option_length, '/');
			PRINTF("Uri-Path [%.*s]\n", coap_pkt->uri_path_len, coap_pkt->uri_path);
			break;
		case COAP_OPTION_URI_QUERY:
			/* coap_merge_multi_option() operates in-place on the IPBUF, but final packet field should be const string -> cast to string */
			coap_merge_multi_option((char **)&(coap_pkt->uri_query),
					&(coap_pkt->uri_query_len), current_option,
					option_length, '&');
			PRINTF("Uri-Query [%.*s]\n", coap_pkt->uri_query_len,
					coap_pkt->uri_query);
			break;

		case COAP_OPTION_LOCATION_PATH:
			/* coap_merge_multi_option() operates in-place on the IPBUF, but final packet field should be const string -> cast to string */
			coap_merge_multi_option((char **)&(coap_pkt->location_path),
					&(coap_pkt->location_path_len), current_option,
					option_length, '/');
			PRINTF("Location-Path [%.*s]\n", coap_pkt->location_path_len,
					coap_pkt->location_path);
			break;
		case COAP_OPTION_LOCATION_QUERY:
			/* coap_merge_multi_option() operates in-place on the IPBUF, but final packet field should be const string -> cast to string */
			coap_merge_multi_option((char **)&(coap_pkt->location_query),
					&(coap_pkt->location_query_len), current_option,
					option_length, '&');
			PRINTF("Location-Query [%.*s]\n", coap_pkt->location_query_len,
					coap_pkt->location_query);
			break;

		case COAP_OPTION_OBSERVE:
			coap_pkt->observe = coap_parse_int_option(current_option,
					option_length);
			PRINTF("Observe [%lu]\n", coap_pkt->observe);
			break;
		case COAP_OPTION_BLOCK2:
			coap_pkt->block2_num = coap_parse_int_option(current_option,
					option_length);
			coap_pkt->block2_more = (coap_pkt->block2_num & 0x08) >> 3;
			coap_pkt->block2_size = 16 << (coap_pkt->block2_num & 0x07);
			coap_pkt->block2_offset = (coap_pkt->block2_num & ~0x0000000F)
        		<< (coap_pkt->block2_num & 0x07);
			coap_pkt->block2_num >>= 4;
			PRINTF("Block2 [%lu%s (%u B/blk)]\n", coap_pkt->block2_num,
					coap_pkt->block2_more ? "+" : "", coap_pkt->block2_size);
			break;
		case COAP_OPTION_BLOCK1:
			coap_pkt->block1_num = coap_parse_int_option(current_option,
					option_length);
			coap_pkt->block1_more = (coap_pkt->block1_num & 0x08) >> 3;
			coap_pkt->block1_size = 16 << (coap_pkt->block1_num & 0x07);
			coap_pkt->block1_offset = (coap_pkt->block1_num & ~0x0000000F)
        		<< (coap_pkt->block1_num & 0x07);
			coap_pkt->block1_num >>= 4;
			PRINTF("Block1 [%lu%s (%u B/blk)]\n", coap_pkt->block1_num,
					coap_pkt->block1_more ? "+" : "", coap_pkt->block1_size);
			break;
		case COAP_OPTION_SIZE2:
			coap_pkt->size2 = coap_parse_int_option(current_option, option_length);
			PRINTF("Size2 [%lu]\n", coap_pkt->size2);
			break;
		case COAP_OPTION_SIZE1:
			coap_pkt->size1 = coap_parse_int_option(current_option, option_length);
			PRINTF("Size1 [%lu]\n", coap_pkt->size1);
			break;
		default:
			PRINTF("unknown (%u)\n", option_number);
			/* check if critical (odd) */
			if(option_number & 1) {
				coap_error_message = "Unsupported critical option";
				return BAD_OPTION_4_02;
			}
		}

		current_option += option_length;
	}                             /* for */
	PRINTF("-Done parsing-------\n");

	return NO_ERROR;
}



// return 0: success
/*---------------------------------------------------------------------------*/
int coap_blocking_request_tcp(
		coap_context_t *coap_ctx,
		coap_packet_t *request,
		restful_response_handler request_callback,
		void * user_data)
{
	uint8_t more = 0;
	uint32_t res_block;
	uint8_t block_error;
	uint16_t block_size = REST_MAX_CHUNK_SIZE;
	int block_num = 0;
	int ret = -1;
	coap_packet_t response[1];

	if(coap_ctx->buf == NULL)
	    PRINTF("coap_blocking_request_tcp: ctx buffer is NULL\n");

	uint8_t *packet = NULL;

	more = 0;
	res_block = 0;
	block_error = 0;
	int default_retrans_ms = coap_ctx->default_retrans_ms;
    uint32_t last_system_clock = 0;

    if(default_retrans_ms == 0) default_retrans_ms = 2000;

    int waiting_ms = default_retrans_ms;

	do {
		request->mid = coap_get_mid();
		coap_set_token(request, (const uint8_t *)&request->mid, sizeof(request->mid));

		coap_set_header_block2(request, block_num, 0, block_size);

		int packet_len =  coap_serialize_message_tcp(request, &packet);
		if(packet_len == 0)
			goto end;

		// for tcp based coap, no need for the addr.
		// the caller must setup the socket in the coap context
		coap_ctx->tx_data(coap_ctx, NULL, packet, packet_len);
		PRINTF("coap_blocking_request_tcp: Requested #%u (MID %u)\n", block_num, request->mid);

		get_elpased_ms(&last_system_clock);

		free(packet);
		packet = NULL;

		while(1)
		{
            waiting_ms -= get_elpased_ms(&last_system_clock);
            if( waiting_ms <= 0)
            {
                request_callback(user_data, NULL);
                ret = -1;
                goto end;
            }

		    ret = coap_ctx->rx_data(coap_ctx, coap_ctx->buf, coap_ctx->buf_size, waiting_ms);

		    PRINTF("coap_blocking_request_tcp: Response, ret=%d \n", ret);

		    if(ret <= 0)
		    {
		        request_callback(user_data, NULL);
		        ret = -1;
		        goto end;
		    }

            int erbium_status_code = NO_ERROR;

            coap_ctx->buf_len = ret;


            erbium_status_code = coap_parse_message_tcp(
                    response, coap_ctx->buf, coap_ctx->buf_len);

            if(erbium_status_code != NO_ERROR) {
                COAP_WARN("coap_blocking_request_tcp: parse response failed %d\n", erbium_status_code);
                continue;
            }

            if(response->code >= COAP_GET && response->code <= COAP_DELETE) {
                COAP_WARN("coap_blocking_request_tcp: not a response recieved. code=%d\n", response->code);
                continue;
            }

            if(*((uint16_t*)response->token) != request->mid){
                COAP_WARN("coap_blocking_request_tcp: mid in response = %d, not match expected %d\n", *((uint16_t*)response->token), request->mid);
                continue;
            }

            break;
		}

		coap_get_header_block2(response, &res_block, &more, NULL, NULL);

		PRINTF("Received #%u%s (%u bytes)\n", res_block, more ? "+" : "",
				response->payload_len);

		if(res_block == block_num) {
			if(STOP_REQUEST == request_callback(user_data, response))
			{
			    ret = -1;
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

end:
	if(packet)free(packet);
	packet = NULL;
	return ret;
}
