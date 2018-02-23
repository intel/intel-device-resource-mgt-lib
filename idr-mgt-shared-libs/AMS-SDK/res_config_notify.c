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
 * res_config_notify.c
 *
 *  Created on: Sep 21, 2017
 *      Author: xin
 */
#include <sys/time.h>
#include <sys/prctl.h>
#include "ams_sdk_interface.h"
#include "ams_sdk_internal.h"
#include "coap_platforms.h"
#include "er-coap.h"
#include "er-coap.h"
#include "coap_ext.h"

int sdk_handler_ams_cfg_put (void * request_coap, void * response_coap, char ** out_payload, int * payload_len)
{

    coap_packet_t * coap_message = (coap_packet_t* ) request_coap;
    coap_packet_t * response = (coap_packet_t* ) response_coap;
    char content[COAP_MAX_PACKET_SIZE];

    *out_payload = NULL;

    if (coap_message->payload_len >= sizeof(content))
    {
        return INTERNAL_SERVER_ERROR_5_00;
    }

    if(g_ams_context->p_callback == NULL)
    {
        return INTERNAL_SERVER_ERROR_5_00;
    }


    memcpy(content,coap_message->payload,coap_message->payload_len);
    content[coap_message->payload_len] = 0;

    cJSON * j_root = cJSON_Parse((const char *) content);
    if(j_root)
    {
        int ret = CHANGED_2_04;
       if(cJSON_HasObjectItem(j_root, "target_type") &&
           cJSON_HasObjectItem(j_root, "target_id") &&
           cJSON_HasObjectItem(j_root, "config_path") &&
           cJSON_HasObjectItem(j_root, "software"))
       {
           g_ams_context->p_callback(cJSON_GetObjectItem(j_root, "software")->valuestring,
               cJSON_GetObjectItem(j_root, "target_type")->valuestring,
               cJSON_GetObjectItem(j_root, "target_id")->valuestring,
               cJSON_GetObjectItem(j_root, "config_path")->valuestring);
       }
       else
       {
           printf("AMS SDK: config notification miss some keys. \r\n payload: %s\n", content);
           ret = BAD_REQUEST_4_00;
       }

       cJSON_Delete(j_root);

        return ret;
    }
    else
    {

        printf("AMS SDK: failed to parse the config notification. \r\n payload: %s\n", content);
        return BAD_REQUEST_4_00;
    }
}

coap_resource_handler_t ams_sdk_cfg_rd = {NULL, "ams/config", NULL, sdk_handler_ams_cfg_put, sdk_handler_ams_cfg_put, NULL};
