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
 * url_util.c
 *
 *  Created on: Dec 10, 2016
 *      Author: xwang98
 */


#include <ctype.h>
#include "agent_core_lib.h"
#include "string_parser.h"


#define COAP_NO_ERROR                   (uint8_t)0x00
#define COAP_IGNORE                     (uint8_t)0x01

#define COAP_201_CREATED                (uint8_t)0x41
#define COAP_202_DELETED                (uint8_t)0x42
#define COAP_204_CHANGED                (uint8_t)0x44
#define COAP_205_CONTENT                (uint8_t)0x45
#define COAP_400_BAD_REQUEST            (uint8_t)0x80
#define COAP_401_UNAUTHORIZED           (uint8_t)0x81
#define COAP_402_BAD_OPTION             (uint8_t)0x82
#define COAP_404_NOT_FOUND              (uint8_t)0x84
#define COAP_405_METHOD_NOT_ALLOWED     (uint8_t)0x85
#define COAP_406_NOT_ACCEPTABLE         (uint8_t)0x86
#define COAP_500_INTERNAL_SERVER_ERROR  (uint8_t)0xA0
#define COAP_501_NOT_IMPLEMENTED        (uint8_t)0xA1
#define COAP_503_SERVICE_UNAVAILABLE    (uint8_t)0xA3


#define CODE_TO_STRING(X)   case X : return #X

const char* coap_status_to_string(int status)
{
	static char buffer[20];
    switch(status)
    {
    CODE_TO_STRING(COAP_NO_ERROR);
    CODE_TO_STRING(COAP_IGNORE);
    CODE_TO_STRING(COAP_201_CREATED);
    CODE_TO_STRING(COAP_202_DELETED);
    CODE_TO_STRING(COAP_204_CHANGED);
    CODE_TO_STRING(COAP_205_CONTENT);
    CODE_TO_STRING(COAP_400_BAD_REQUEST);
    CODE_TO_STRING(COAP_401_UNAUTHORIZED);
    CODE_TO_STRING(COAP_404_NOT_FOUND);
    CODE_TO_STRING(COAP_405_METHOD_NOT_ALLOWED);
    CODE_TO_STRING(COAP_406_NOT_ACCEPTABLE);
    CODE_TO_STRING(COAP_500_INTERNAL_SERVER_ERROR);
    CODE_TO_STRING(COAP_501_NOT_IMPLEMENTED);
    CODE_TO_STRING(COAP_503_SERVICE_UNAVAILABLE);
    default:
    	sprintf(buffer, "[%d]", status);
    	return buffer;
    }
}

char * g_device_category[] =
{
		"lwm2m",
		"ep",
		"ocf",
		"modbus",
		"iagent",
		"ilink",
		"self"		// when the resources hosted by the GW bus
};


int check_uri_standard(char *uri)
{
    int i;
	if(*uri == '/') uri++;
	int len = strlen(uri);

	for( i=0;i<COUNT_OF(g_device_category);i++)
	{
		int base_len = strlen(g_device_category[i]);
		if(strncmp(uri, g_device_category[i], base_len) == 0)
		{
			if(uri[base_len] == '/' || uri[base_len] == 0)
			{
				return i;
			}
		}
	}
	return -1;
}



bool parse_iUrl_body2(IURL_BODY raw_uri, int raw_uri_len, iUrl_t * iurl_parsed)
{
    if (NULL == raw_uri) return false;

	char * uri = malloc(raw_uri_len + 1);
	if(uri == NULL)
	{

	}

	memcpy(uri, raw_uri, raw_uri_len);
	uri[raw_uri_len] = 0;

	char * buffer = uri;

	if(*uri == '/') uri++;

	char * standard = uri;
	while(*uri != 0 && *uri != '/') uri ++;

	if(*uri != '/')
		goto end;
	*uri = '\0';
	uri ++;

	char * device = uri;
	while(*uri != 0 && *uri != '/') uri ++;

	if(*uri != '/')
		goto end;
	*uri = '\0';
	uri ++;

	char *res_uri = malloc (strlen(uri)+2);

	sprintf (res_uri, "/%s", uri);
	iurl_parsed->res_uri = res_uri;
	iurl_parsed->device = device;
	iurl_parsed->standard = standard;
	iurl_parsed->buffer = buffer;
	return true;

end:
   free(buffer);
   return false;
}


bool parse_iUrl_body(IURL_BODY raw_uri, iUrl_t * iurl_parsed)
{
	return parse_iUrl_body2(raw_uri, strlen(raw_uri), iurl_parsed);
}

void free_iUrl_body(iUrl_t * body)
{
	if (body->buffer) free(body->buffer);
    if (body->res_uri) free (body->res_uri);
}





//simply check if the content is quoted by {}
bool is_json_format(char * buffer, int len)
{
	int offset = 0;

	if(len == 0 || buffer == NULL)
		return false;

	while(offset < len && buffer[offset] == ' ')
	{
		offset ++;
	}

	if(offset == len)
		return false;

	if(buffer[offset] != '{')
		return false;

	offset = len - 1;
	while(offset > 0 && (buffer[offset] == ' ' || buffer[offset] == 0))
	{
		offset --;
	}
	if(offset == 0)
		return false;

	if(offset > 0 && buffer[offset] == '}')
		return true;

	return false;
}

bool has_string_end(char * buffer, int len)
{
	char * p = buffer + len -1;
	while(len > 0 && *p != 0)
	{
		p--;
		len --;
	}

	return (len > 0);
}

#if 0
uint32_t parse_devided_number(const char * uriString,
                            size_t uriLength,
                            int * headP, char ch_div)
{
    uint32_t result = 0;
    int offset = * headP;
    uint8_t first = 1;

    if(*headP >= uriLength) return -1;

    if (uriString[offset] == ch_div)
    {
        // empty Object Instance ID with resource ID is not allowed
    	offset ++;
    }
    while (offset < uriLength && uriString[offset] != ch_div)
    {
        if ('0' <= uriString[offset] && uriString[offset] <= '9')
        {
          if(first)
            first = 0;
          else
            result *= 10;

          result += uriString[offset] - '0';
        }
        else
        {
            return -1;
        }
        offset ++;

    }

    *headP = offset;
    return result;
}
#endif


bool is_number(char * str)
{
	if(*str == 0)
		return false;

	while(*str != 0)
	{
		if(!isdigit(*str))
			return false;

		str ++;
	}

	return true;
}

JSON_Value * query_string_to_json(const char * query)
{
    if (NULL == query)
        return NULL;
	char * query_str = strdup(query);
	if(NULL == query_str)
	{
		return NULL;
	}

	JSON_Value *json_root = json_value_init_object();
	JSON_Object *  root_obj = json_object(json_root);

	char * p = query_str;
    while(*p == ' ') p++;
	char * key = p;
	char * value = NULL;

	while(*p)
	{
		if(*p == '=')
		{
			*p = '\0';
			p++;
			while(*p == ' ') p++;
			value = p;
		}
		else if(*p == '&')
		{
			*p = '\0';
			if(value == NULL)
				goto end;

			json_object_set_string(root_obj, key, value);
		    p++;
		    while(*p == ' ') p++;
		    value = NULL;
		    key = p;
		}
		else
		{
			p++;
		}
	}
	if (value != NULL && key != NULL)
	    json_object_set_string(root_obj, key, value);
end:
	free(query_str);
	return json_root;
}
