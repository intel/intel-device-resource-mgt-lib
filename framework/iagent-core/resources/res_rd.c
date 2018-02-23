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
#include <stdio.h>
#include "er-coap-engine.h"
#include "iagent_config.h"
#include "rd.h"


void rd_get_handler(void *request, void *response, uint8_t *buffer,
                            uint16_t preferred_size, int32_t *offset)
{
    char tmp = 0;
    const char *coap_query;
    char * query = NULL;
    bool alloc = false;
    char * payload = NULL;

    int len = coap_get_header_uri_query(request, &coap_query);
    if (len)
    {
    	if (coap_query[len-1] != 0)
    	{
    		query = (char *) malloc (len + 1);
    		memcpy (query, coap_query, len);
    		query[len] = 0;
    		alloc = true;
    	}
	    else
        {
        	query = (char *) coap_query;
        }
    }


    handle_rd_get(query, &payload, true);
    int payload_len= 0;
    if(payload) payload_len = strlen (payload);

	if (payload_len > *offset + preferred_size)
	{
		memcpy (buffer, payload+*offset, preferred_size );
		payload_len = preferred_size;
		*offset += preferred_size;
	}
	else if (payload_len >= *offset)
	{
		memcpy (buffer, payload + *offset, payload_len - *offset);
		payload_len -= *offset;
		*offset = -1;
	}
	else
	{
		payload_len = 0;
		*offset = -1;
	}

    coap_set_payload(response, buffer, payload_len);
    coap_set_header_content_format(response, APPLICATION_JSON);


    if(alloc) free (query);
    if(payload) free (payload);
}

/*---------------------------------------------------------------------------*/
RESOURCE(res_get_rd, "", rd_get_handler, NULL,
         NULL, NULL);
/*---------------------------------------------------------------------------*/
