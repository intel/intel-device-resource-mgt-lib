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
#include "er-coap.h"
#include "coap_ext.h"

extern void cfg_change_cb(const char *product_name, const char *target_type, const char *target_id, const char *cfg_file_name);
int handler_ams_cfg_put (void * request_coap, void * response_coap, char ** out_payload, int * payload_len)
{

    coap_packet_t * coap_message = (coap_packet_t* ) request_coap;
    coap_packet_t * response = (coap_packet_t* ) response_coap;
    char content[COAP_MAX_PACKET_SIZE];

    *out_payload = NULL;

    if (coap_message->payload_len >= sizeof(content))
    {
        return INTERNAL_SERVER_ERROR_5_00;
    }


    memcpy(content,coap_message->payload,coap_message->payload_len);
    content[coap_message->payload_len] = 0;

    JSON_Value *root_value = json_parse_string(content);

    if(root_value)
    {
        JSON_Object * object = json_object(root_value);
        const char *type = json_object_get_string(object, "target_type");
        const char *id = json_object_get_string(object, "target_id");
        const char *path = json_object_get_string(object, "config_path");
        const char *software = json_object_get_string(object, "software");

        if(type && id && path && software)
        {
            cfg_change_cb(software, type, id, path);
        }
        else
        {
            LOG_MSG("ams/cfg request payload miss some items");
        }
        json_value_free(root_value);
        return CHANGED_2_04;
    }
    else
    {

        WARNING("AMS SDK: failed to parse the config notification. \r\n payload: %s\n", content);
        return BAD_REQUEST_4_00;
    }
}

coap_resource_handler_t ams_cfg_rd = {NULL, "ams/config", NULL, handler_ams_cfg_put, handler_ams_cfg_put, NULL};

