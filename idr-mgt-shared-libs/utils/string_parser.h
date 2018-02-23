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


#ifndef string_parser_H_
#define string_parser_H_

#include "agent_core_lib.h"
#include "parson.h"


#ifdef __cplusplus
extern "C" {
#endif




/************************************************************************/
/*                                                                      */
/*                           string_parser.cpp                          */
/*                                                                      */
/************************************************************************/
char ** parse_string_array(char * str, char delimiter);
void free_str_list(char ** str_list);
void * parse_string_array_to_Json(char * str, char delimiter);





/************************************************************************/
/*                                                                      */
/*                           url_util.c                              */
/*                                                                      */
/************************************************************************/



// /[lwm2m|ocf|ep|modbus]/[:device id]/{:resource href]
#define IURL_BODY		char*

// target:
// 1) bus://module/[module id]/...
// 2) bus://...
// 3) coap://127.0.0.1:7777/...
// 4) agent://[ibroker]/....
// 5) obs://[token id]/....
// 6) ep://ep
#define ADDR_BUS "gwbus"
#define ADDR_COAP "coap"
#define ADDR_OBS "obs"

#define IURL			char*

typedef struct  _iUri
{
	char * standard;
	char * device;
	char * res_uri;
	char * buffer;
}iUrl_t;

bool parse_iUrl_body(IURL_BODY url_body, iUrl_t *);
bool parse_iUrl_body2(IURL_BODY raw_uri, int raw_uri_len, iUrl_t * iurl_parsed);
void free_iUrl_body(iUrl_t *);
int check_uri_standard(char *uri);
bool is_number(char * str);

JSON_Value * query_string_to_json(const char * query);

extern int parse_devided_number(const char * uriString,
                            size_t uriLength,
                            int * headP, char ch_div);

const char* coap_status_to_string(int status);



#ifdef __cplusplus
}
#endif

#endif /* APPS_IAGENT_CORE_LIB_AGENT_CORE_LIB_H_ */
