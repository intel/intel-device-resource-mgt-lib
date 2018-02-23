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



const char * ilink_status_text(int status)
{
    if(status >= iReady_For_Work)
        return "working";
    else if(status == iCloud_Handshaking)
        return "handshaking";
    else if(status == iCloud_Provisioning)
        return "provisioning";
    else if(status == iCloud_Provisioning)
        return "provisioning";
    else if(status < iSocket_Connected)
        return "connecting";
    else
        return "connected";
}

int handler_ilink_get (void * request_coap, void * response_coap, char ** out_payload, int * payload_len)
{
    char tmp = 0;
    *out_payload = NULL;
    *payload_len = 0;
    extern char tbuf_conection_status_change_time[32];

    coap_packet_t * response = (coap_packet_t* ) response_coap;

    JSON_Value *payload_val = json_value_init_object();
    JSON_Object *payload_obj = json_object(payload_val);
    JSON_Status status = json_object_set_number(payload_obj, "cloud_status", (int)g_cloud_status);
    json_object_set_string(payload_obj, "cloud", ilink_status_text(g_cloud_status));
    json_object_set_string(payload_obj, "start-time", tbuf_conection_status_change_time);
    json_object_set_string(payload_obj, "iagent-id", get_self_agent_id()?get_self_agent_id():"No ID");

    char * content  = json_serialize_to_string(payload_val);
    if (!content)
    {
        WARNING("get_ilink: create payload failed\n");
        return INTERNAL_SERVER_ERROR_5_00;
    }
    else
    {
        strcpy(response->payload, content);
        response->payload_len = strlen(content);
        json_free_serialized_string(content);
    }

    json_value_free(payload_val);

    return CONTENT_2_05;
}


coap_resource_handler_t resource_ilink = {NULL, "ilink", handler_ilink_get, NULL, NULL, NULL};
