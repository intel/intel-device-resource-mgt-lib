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




#include "coap_platforms.h"
#include "er-coap.h"
#include "agent_core_lib.h"
#include "iagent_base.h"
#include "parson.h"


int handler_version_get (void * request_coap, void * response_coap, char ** out_payload, int * payload_len)
{
    char tmp = 0;
    *out_payload = NULL;
    *payload_len = 0;
    coap_packet_t * response = (coap_packet_t* ) response_coap;

    sprintf(response->payload, "%d", IAGENT_VERSION);
    response->payload_len = strlen(response->payload);
    coap_set_header_content_format(response, TEXT_PLAIN);

    return CONTENT_2_05;
}



int handler_reset_put (void * request_coap, void * response_coap, char ** out_payload, int * payload_len)
{
    char tmp = 0;
    *out_payload = NULL;
    *payload_len = 0;
    coap_packet_t * response = (coap_packet_t* ) response_coap;

    ERROR("Received coap request to reset the system!!!");
    system ("reboot");

    coap_set_header_content_format(response, TEXT_PLAIN);

    return CONTENT_2_05;
}


coap_resource_handler_t resource_version = {NULL, "version", handler_version_get, NULL, NULL, NULL};
coap_resource_handler_t resource_reset = {NULL, "reset", NULL, handler_reset_put, handler_reset_put, NULL};
