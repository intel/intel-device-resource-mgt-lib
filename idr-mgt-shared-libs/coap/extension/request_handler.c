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
 * request_handler.c
 *
 *  Created on: Aug 3, 2017
 *      Author: xin.wang@intel.com
 */
#include "coap_platforms.h"
#include "coap_ext.h"
#include "agent_core_lib.h"


char * get_ip_address(struct   sockaddr_in *sock_addr, char * buffer, int buf_len)
{
	char s[INET6_ADDRSTRLEN];
	int port = -1;
	struct sockaddr_storage *addr = (struct sockaddr_storage*) sock_addr;

	if (AF_INET == addr->ss_family)
	{
		struct sockaddr_in *saddr = (struct sockaddr_in *)addr;
		inet_ntop (saddr->sin_family, &saddr->sin_addr, s, INET6_ADDRSTRLEN);
		port = saddr->sin_port;
	}
	else if (AF_INET6 == addr->ss_family)
	{
		struct sockaddr_in6 *saddr = (struct sockaddr_in6 *)addr;
		inet_ntop (saddr->sin6_family, &saddr->sin6_addr, s, INET6_ADDRSTRLEN);
		port = saddr->sin6_port;
	}
         else
          { 
               strncpy(buffer, "invalid address", buf_len);
               return buffer;
          }

          snprintf (buffer, buf_len, "%s:%d", s, port);
        

return buffer;
}

int add_resource_handler(coap_context_t * coap_ctx, coap_resource_handler_t * handler)
{
	handler->next = coap_ctx->resource_handlers;
	coap_ctx->resource_handlers = handler;

	return 0;
}

int send_coap_msg(coap_context_t * coap_ctx, coap_packet_t * message)
{
    uint32_t block_num = 0;
    uint16_t block_size = REST_MAX_CHUNK_SIZE;
    uint32_t block_offset = 0;
    uint8_t  more = 0;
    char packet[COAP_MAX_PACKET_SIZE];
    char buffer[100];

    PRINTF("sent response to [%s]: code=%d, content format=%d, payload len=%d\n", get_ip_address( &coap_ctx->src_addr.sock_addr, buffer, sizeof(buffer)), message->code, message->content_format, message->payload_len);

    if(coap_get_header_block1 (message, &block_num, &more, &block_size, &block_offset))
    {
        PRINTF("\t block1 num= %lu, size= (%u/%u) @ %lu bytes, more=%d\n",
                (unsigned long)block_num, block_size, REST_MAX_CHUNK_SIZE, (unsigned long)block_offset, more);
    }
	if(coap_get_header_block2 (message, &block_num, &more, &block_size, &block_offset))
	{
		PRINTF("\tblock2 num= %lu, size= (%u/%u) @ %lu bytes, more=%d\n",
				(unsigned long)block_num, block_size, REST_MAX_CHUNK_SIZE, (unsigned long)block_offset, more);
	}


	int len = coap_serialize_message(message, packet);
	if(len > 0)
	{
		coap_ctx->tx_data(coap_ctx, &coap_ctx->src_addr, packet, len);
	}
}

void free_res_blockwise_node(res_block_state_t *res_node)
{
    PRINTF("free blockwise node [%p] for [%s]\n", res_node, res_node->url);
    free(res_node->url);
    free(res_node->buffer);
    free(res_node);

}

void cleanup_blockwise_list(coap_context_t * coap_ctx)
{
    peer_block_state_t * blockwise_list = coap_ctx->blockwise_list;
    while(blockwise_list)
    {
            res_block_state_t * res_block_list = blockwise_list->list;
            res_block_state_t * prev = NULL;
            while(res_block_list)
            {
                	 res_block_state_t * node = res_block_list;

                	 res_block_list = res_block_list->next;

                     free_res_blockwise_node(node);
            }

        //todo: remove the node if no sub nodes
        peer_block_state_t * node = blockwise_list;
        blockwise_list = blockwise_list->next;
        free(node);
    }
}

uint32_t check_blockwise_timeout_ms(coap_context_t * coap_ctx,  int timeout )
{
	time_t now = get_platform_time_sec();
	uint32_t next_timeout = -1;
      char buffer[100];
    peer_block_state_t * blockwise_list = coap_ctx->blockwise_list;
    peer_block_state_t * prev_client_node = NULL;
    int cnt = 0;
    while(blockwise_list)
    {
            res_block_state_t * res_block_list = blockwise_list->list;
            res_block_state_t * prev = NULL;
            while(res_block_list)
            {
                if(now - res_block_list->last_access > timeout )
                {
                	    res_block_state_t * node = res_block_list;
                        if(prev)
                            prev->next = res_block_list->next;
                        else
                        	blockwise_list->list = res_block_list->next;


                        res_block_list = res_block_list->next;
                     free_res_blockwise_node(node);
                     cnt ++;
                }
                else
                {
                	if(next_timeout == -1 || next_timeout > res_block_list->last_access)
                		next_timeout = res_block_list->last_access;

                    prev = res_block_list;
                    res_block_list = res_block_list->next;
                }
            }

        //todo: remove the node if no sub nodes
        if(blockwise_list->list == NULL)
        {
        	peer_block_state_t * empty = blockwise_list;
        	if(prev_client_node)
        		prev_client_node->next = blockwise_list->next;
        	else
        		coap_ctx->blockwise_list = blockwise_list->next;

        	blockwise_list = blockwise_list->next;

        	PRINTF("released the blockwise [%p] for IP [%s]\n", empty, get_ip_address(&empty->peer_addr.sock_addr, buffer, sizeof(buffer)));
        	free(empty);

        }
        else
        {
        	prev_client_node = blockwise_list;
        	blockwise_list = blockwise_list->next;
        }
    }

    if(cnt >0)
    {
    	PRINTF("cleaned %d blockwise resource nodes for timeout\n", cnt);
    }

    return (next_timeout == -1) ?-1:(next_timeout-now)*1000;
}

uint8_t coap_handle_request_internal(coap_context_t * coap_ctx, coap_packet_t * request,
		coap_packet_t * response, char ** out_payload, int * payload_len)
{
    const char *url = NULL;
    char url_str [256];
    int  url_len = coap_get_header_uri_path(request, &url);

    if(url_len == 0 || url_len >= sizeof(url_str))
    {
        return -1;
    }
    memcpy(url_str, url, url_len);
    url_str[url_len] = 0;

	coap_resource_handler_t * resource_handlers = coap_ctx->resource_handlers;
	while(resource_handlers)
	{

		if(match_url(resource_handlers->url, url_str))
		{
			if(request->code == COAP_GET )
			{
				if(resource_handlers->get_handler)
					return resource_handlers->get_handler(request, response, out_payload, payload_len);
			}
			else if(request->code == COAP_POST)
			{
				if(resource_handlers->post_handler)
					return resource_handlers->post_handler(request, response, out_payload, payload_len);
			}
			else if(request->code == COAP_PUT )
			{
				if(resource_handlers->put_handler)
					return resource_handlers->put_handler(request, response, NULL, NULL);
			}
			else if(resource_handlers->other_handler)
			{
				return resource_handlers->other_handler(request, response, NULL, NULL);
			}
		}

		resource_handlers = resource_handlers->next;

	}


    return NOT_IMPLEMENTED_5_01;
}


void add_block(coap_context_t * coap_ctx, res_block_state_t * block)
{
    char buffer[100];
    peer_block_state_t * blockwise_list = coap_ctx->blockwise_list;
    while(blockwise_list)
    {
        if(compare_net_addr(&coap_ctx->src_addr, &blockwise_list->peer_addr))
        {
            break;
        }

        //todo: remove the node if no sub nodes
        blockwise_list = blockwise_list->next;
    }

    if(blockwise_list == NULL)
    {
        blockwise_list = (peer_block_state_t * ) malloc (sizeof(peer_block_state_t ));
        memset(blockwise_list, 0, sizeof(peer_block_state_t ));
        copy_net_addr(&blockwise_list->peer_addr, &coap_ctx->src_addr);

      PRINTF("New blockwise ctx for IP [%s]\n",  get_ip_address(&blockwise_list->peer_addr.sock_addr, buffer, sizeof(buffer)));

        blockwise_list->next = coap_ctx->blockwise_list;
        coap_ctx->blockwise_list = blockwise_list;
    }

    block->next = blockwise_list->list;
    blockwise_list->list = block;
}

res_block_state_t * find_block(coap_context_t * coap_ctx,  char * url, bool is_get, bool remove_if_found)
{
    peer_block_state_t * blockwise_list = coap_ctx->blockwise_list;
    while(blockwise_list)
    {
        if(compare_net_addr(&coap_ctx->src_addr, &blockwise_list->peer_addr))
        {
            res_block_state_t * res_block_list = blockwise_list->list;
            res_block_state_t * prev = NULL;
            while(res_block_list)
            {
                // todo: compare query string as well
                if(is_get == res_block_list->is_get && strcmp(res_block_list->url, url) == 0)
                {
                    if(remove_if_found)
                    {
                        if(prev)
                            prev->next = res_block_list->next;
                        else
                            blockwise_list->list = res_block_list->next;
                    }

                    return res_block_list;
                }
                else
                {
                    prev = res_block_list;
                    res_block_list = res_block_list->next;
                }
            }
        }

        //todo: remove the node if no sub nodes
        blockwise_list = blockwise_list->next;
    }

    return NULL;
}

int set_res_blockwise(coap_context_t * coap_ctx,  char * url_allocated, void * buffer,
		int buffer_size, uint32_t block_num, uint16_t content_fmt)
{

    res_block_state_t * res_block_node = find_block(coap_ctx, url_allocated, true, true);

    if(res_block_node)
    {
        if(res_block_node->buffer) free(res_block_node->buffer);
        free(url_allocated);
    }
    else
    {
        char ip[100] = {0};
        get_ip_address(&coap_ctx->src_addr.sock_addr,ip,sizeof(ip));
        res_block_node = (res_block_state_t*) malloc(sizeof(res_block_state_t));
        memset(res_block_node, 0, sizeof(res_block_state_t));
        res_block_node->url = url_allocated;
        res_block_node->is_get = 1;

        PRINTF("new get type blockwise node [%p] for [%s], ip=%s, buffer size=%d, current block num=%d\n", res_block_node, res_block_node->url, ip, buffer_size, block_num);
    }

    res_block_node->buffer = buffer;
    res_block_node->content_fmt= content_fmt;
    res_block_node->buffer_size = buffer_size;
    res_block_node->block_num = block_num;
    res_block_node->last_access = get_platform_time_sec();

    add_block(coap_ctx, res_block_node);

	return 0;
}

enum
{
	Not_Blockwise_Request = -1,
	Invalid_Blockwise_Request = -2,
	Completed_Blockwise_Request = 0,
	Uncompleted_Blockwise_Request = 1,
                   Get_from_Bockwise_Cache = 3
};

//
// the caller must provide payload with size of  REST_MAX_CHUNK_SIZE in the response
int check_blockwise_transfer_get(coap_context_t * coap_ctx, coap_packet_t * request, coap_packet_t * response)
{
	uint32_t block_num = 0;
	uint16_t block_size = REST_MAX_CHUNK_SIZE;
	uint32_t block_offset = 0;
	uint8_t more = 0;

	char * url_allocated =coap_get_full_url_alloc(request);;
    if(url_allocated == NULL)
    {
        return Not_Blockwise_Request;
    }

	assert(response->payload != NULL);

	res_block_state_t * res_block_list = find_block(coap_ctx, url_allocated, true, false);
	free(url_allocated);
	url_allocated = NULL;

	if(res_block_list == NULL)
	    return Not_Blockwise_Request;

	if(coap_get_header_block2 (request, &block_num, &more, &block_size, &block_offset))
	{
		PRINTF("Blockwise get: block2 num= %lu, blocksize= (%u/%u) @ %lu bytes, more=%d\n",
				(unsigned long)block_num, block_size, REST_MAX_CHUNK_SIZE, (unsigned long)block_offset, more);
		block_size = MIN(block_size, REST_MAX_CHUNK_SIZE);
	}
#if 0
    // only accept request on current block or next block?
    if(block_num != res_block_list->block_num &&  block_num != ((uint32_t) res_block_list->block_num+1))
    {
        COAP_WARN("Blockwise get: block invalid block num %d, current num=%d\n",
				(unsigned long)block_num,  res_block_list->block_num);
       coap_set_status_code(response, BAD_OPTION_4_02);

        return Invalid_Blockwise_Request;
    }
#endif

    // fail it if request out of range
    int toal_blocks =  res_block_list->buffer_size/block_size;
    int payload_len = block_size;
    if(res_block_list->buffer_size%block_size != 0)
    {
        toal_blocks++;
        if(block_num == toal_blocks - 1)
            payload_len = res_block_list->buffer_size%block_size;
    }

    if(block_num >= toal_blocks)
    {
        COAP_WARN("Blockwise get: request block num %d exceed max %d, current num=%d, buffer size=%d\n",
				(unsigned long)block_num,  toal_blocks,  res_block_list->block_num, res_block_list->buffer_size);
       coap_set_status_code(response, REQUEST_ENTITY_TOO_LARGE_4_13);

        return Invalid_Blockwise_Request;
    }

    coap_set_header_block2(response, block_num,
            block_num+1 < toal_blocks,
            block_size);

    coap_set_payload(response,
    		res_block_list->buffer+block_num*block_size,payload_len);

   PRINTF("Blockwise get:  filled data from the cached buffer. payload_len=%d, block num=%d, more=%d\n", payload_len, block_num, ( block_num+1 < toal_blocks));
    res_block_list->last_access = get_platform_time_sec();
    res_block_list->block_num = block_num;

   coap_set_status_code(response, CONTENT_2_05 );
   coap_set_header_content_format(response, res_block_list->content_fmt );


    return Get_from_Bockwise_Cache;

}

//
// the caller must provide payload with size of  REST_MAX_CHUNK_SIZE in the response


int check_blockwise_transfer_put(coap_context_t * coap_ctx, coap_packet_t * request,
        coap_packet_t * response,
        char ** out_payload, int * payload_len)
{
    uint32_t block_num = 0;
    uint16_t block_size = REST_MAX_CHUNK_SIZE;
    uint32_t block_offset = 0;
    uint8_t  more = 0;
    int ret = Invalid_Blockwise_Request;

    if(coap_get_header_block1 (request, &block_num, &more, &block_size, &block_offset))
    {
        PRINTF("Blockwise put: block1 request %lu (%u/%u) @ %lu bytes, more=%d, payload_len=%d\n",
                (unsigned long)block_num, block_size, REST_MAX_CHUNK_SIZE, (unsigned long)block_offset, more, request->payload_len);
        block_size = MIN(block_size, REST_MAX_CHUNK_SIZE);
    }
    else
    {
        return Not_Blockwise_Request;
    }

    if(!more && block_num == 0)
    {
        return Not_Blockwise_Request;
    }

    char * url_allocated =coap_get_full_url_alloc(request);
    if(url_allocated == NULL)
    {
        return Not_Blockwise_Request;
    }

    // remove the found node from list, so later we can directly release the node when completed
    res_block_state_t * res_block_list = find_block(coap_ctx, url_allocated, false, true);

    bool new_node = false;
    int offset = 0;
    if(res_block_list == NULL)
    {
        uint32_t size = 1024 * 16;
        if(block_num != 0 || !more)
        {
        	coap_set_status_code(response, BAD_OPTION_4_02);
        	ret = Invalid_Blockwise_Request;
        	goto end;
        }


        coap_get_header_size1(request, &size);
        res_block_list = (res_block_state_t*) malloc(sizeof(res_block_state_t));
        memset(res_block_list, 0, sizeof(res_block_state_t));
        new_node = true;

        res_block_list->buffer = malloc(size);
        res_block_list->buffer_size = size;
        res_block_list->block_num = 0;
        res_block_list->url = url_allocated;
        url_allocated = NULL;
        res_block_list->is_get = 0;
        res_block_list->last_access = get_platform_time_sec();

        PRINTF("new put type blockwise node [%p] for [%s], buffer size=%d\n", res_block_list, res_block_list->url, size);
    }
    else
    {

		// only accept request on current block or next block?
		if(block_num != res_block_list->block_num &&  block_num != res_block_list->block_num+1)
		{
			ret = Invalid_Blockwise_Request;
			coap_set_status_code(response, BAD_OPTION_4_02);
			COAP_WARN("blockwise put block num %d invalid. node [%p] for [%s] blocknum=%d\n", block_num, res_block_list, res_block_list->url, res_block_list->block_num);
 			goto end;
		}

		// fail it if request out of range
		offset =  block_num * block_size;
		if(offset + request->payload_len > res_block_list->buffer_size)
		{
		    COAP_WARN("blockwise put block num %d over size. node [%p] for [%s]: blocknum=%d, buf size=%d\n", block_num, res_block_list, res_block_list->url, res_block_list->block_num ,res_block_list->buffer_size);
			ret = Invalid_Blockwise_Request;
			coap_set_status_code(response, REQUEST_ENTITY_TOO_LARGE_4_13);
			goto end;
		}
    }

    memcpy(res_block_list->buffer + offset, request->payload, request->payload_len);

    res_block_list->last_access = get_platform_time_sec();
    res_block_list->block_num = block_num;

    coap_set_header_block1(response, block_num,
            more,
            block_size);

    if(!more)
    {
        *out_payload = res_block_list->buffer;
        *payload_len = offset+request->payload_len;
        PRINTF("blockwise put completed! total payload size=%d. node [%p] for [%s]: blocknum=%d\n",  *payload_len, res_block_list, res_block_list->url, res_block_list->block_num);

        free(res_block_list->url);
        free(res_block_list);

        if(url_allocated) free(url_allocated);
        return Completed_Blockwise_Request;
    }

    coap_set_status_code(response, CONTINUE_2_31);

    ret = Uncompleted_Blockwise_Request;

end:

// cleanup
	if( res_block_list)
	{
			// always add the node since we already removed it from the list
			add_block(coap_ctx, res_block_list);
	}

    if(url_allocated != NULL) free(url_allocated);


    return ret;
}


/// return : -1: no response sent

int coap_request_handler (void * ctx , void * message)
{
	coap_packet_t * request = (coap_packet_t *) message;
	coap_context_t * coap_ctx = (coap_context_t*) ctx;

	coap_packet_t response[1];
	char  payload[REST_MAX_CHUNK_SIZE];

	char * out_payload = NULL;
	int out_payload_len = 0;
	int ret = 0;
	char * complete_put_payload = NULL;

	/* handle requests */
	assert(request->code >= COAP_GET && request->code <= COAP_DELETE) ;


	bool alloc = false;
	char * url_str = get_string((char*)request->uri_path, request->uri_path_len, &alloc);
	PRINTF("handle request on [%s], code=%d, type=%d, ctx=[%p]\n", url_str, request->code, request->type, ctx);
	if(alloc) free (url_str);

	/* prepare response */
	if(request->type == COAP_TYPE_CON)
	{
		/* reliable CON requests are answered with an ACK */
		coap_init_message(response, COAP_TYPE_ACK, CONTENT_2_05,
				request->mid);
	}
	else
	{
		/* unreliable NON requests are answered with a NON as well */
		coap_init_message(response, COAP_TYPE_NON, CONTENT_2_05,
				coap_get_mid());
	}

	response->payload = payload;

	if(request->token_len)
	{
		coap_set_token(response, request->token, request->token_len);
	}

	//
	if(request->code == COAP_GET)
    {
         int ret = check_blockwise_transfer_get(coap_ctx,request, response);
	     if(Get_from_Bockwise_Cache == ret ||  Invalid_Blockwise_Request == ret) 
         {
            send_coap_msg(coap_ctx, response);
		    return 0;
            }
    }
    else if(request->code == COAP_POST || request->code == COAP_PUT)
    {

	    int payload_len2 = 0;
	    int ret =  check_blockwise_transfer_put(coap_ctx, request, response, &complete_put_payload, &payload_len2);

	    // still hold part of the payload
	    if(Uncompleted_Blockwise_Request == ret || Invalid_Blockwise_Request == ret)
	    {
            send_coap_msg(coap_ctx, response);
            return 0;
	    }

	    // get complete put/post payload
	    if(Completed_Blockwise_Request == ret)
	    {
	        request->payload = complete_put_payload;
	        request->payload_len = payload_len2;
	    }
    }


	/* call REST framework and check if found and allowed */
	ret = coap_handle_request_internal(coap_ctx, request, response, &out_payload, &out_payload_len);

	// if no resource handled, run the user defined handler
	if(ret == NOT_IMPLEMENTED_5_01 && coap_ctx->request_handler)
	{
		Request_Handler handler = (Request_Handler) coap_ctx->request_handler;
		ret = handler(coap_ctx, message);
	}

	coap_set_status_code(response, ret);


	if(ret == MANUAL_RESPONSE)
	{
		// the user resource doesn't want to send response
	}
	else if( NOT_IMPLEMENTED_5_01 != ret)
	{
		if(request->code == COAP_GET )
		{
			uint32_t block_num = 0;
			uint16_t block_size = REST_MAX_CHUNK_SIZE;
			uint32_t block_offset = 0;
			uint8_t more = 0;



			if(coap_get_header_block2 (request, &block_num,  &more, &block_size, &block_offset))
			{
		PRINTF("handled get request: block2 num= %lu, blocksize= (%u/%u) @ %lu bytes, more=%d\n",
				(unsigned long)block_num, block_size, REST_MAX_CHUNK_SIZE, (unsigned long)block_offset, more);
		block_size = MIN(block_size, REST_MAX_CHUNK_SIZE);

			}

			int payload_len = out_payload_len;
			if(payload_len == 0) payload_len = response->payload_len;

			if((payload_len - block_offset) > block_size)
			{
				more = 1;
				coap_set_header_block2(response, block_num, more, block_size);
				coap_set_header_size2(response, payload_len);

				// if the client set smaller payload than REST_MAX_CHUNK_SIZE,
				// we need to save the payload for later blockwise get
				if(out_payload_len == 0)
				{
					out_payload_len = payload_len;
					out_payload = (char*)malloc(out_payload_len);
					memcpy(out_payload, response->payload, out_payload_len);
				}
			}

			if(out_payload_len != 0)
			{
				//assert(more);  client may request the middle blocks for the first time.
				coap_set_payload(response, out_payload + block_offset,
								  MIN(out_payload_len - block_offset, block_size));
			}
			else if(block_offset != 0)
			{
				assert(!more);
				coap_set_payload(response, response->payload + block_offset,
								  response->payload_len - block_offset);
			}

			send_coap_msg(coap_ctx, response);

			// save the buffer for blockwise transfer request
			if(more)
			{
				char * url_alloc =coap_get_full_url_alloc(request);

				// the allocate url will saved in the context. so no need to free it here
				set_res_blockwise(coap_ctx, url_alloc, out_payload, out_payload_len, block_num,(uint16_t) response->content_format);
				out_payload = NULL;
			}
		}

		// send response for put/post
		else
		{
			send_coap_msg(coap_ctx, response);
		}

	} /* no errors/hooks */
	else if(coap_ctx->response_on_not_found)
	{
	    send_coap_msg(coap_ctx, response);
	}
	else
	{
		ret = -1;
	}

	if(complete_put_payload)
	    free(complete_put_payload);

	if(out_payload)
	{
		free(out_payload);
		out_payload = NULL;
	}
	return (ret);

}
