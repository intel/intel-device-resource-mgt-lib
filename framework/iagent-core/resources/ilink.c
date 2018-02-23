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
#include "parson.h"
#include "er-coap-engine.h"
#include "iagent_config.h"
#include "iagent_base.h"
#include "logs.h"


void get_ilink_handle(void *request, void *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
    char tmp = 0;
    extern char tbuf_conection_status_change_time[32];

    JSON_Value *payload_val = json_value_init_object();
    JSON_Object *payload_obj = json_object(payload_val);
    JSON_Status status = json_object_set_number(payload_obj, "cloud_status", (int)g_cloud_status);
    json_object_set_string(payload_obj, "cloud", ilink_status_text(g_cloud_status));
    json_object_set_string(payload_obj, "start_time", tbuf_conection_status_change_time);

    char * content  = json_serialize_to_string(payload_val);
    if (!content)
    {
        WARNING("get_ilink: create payload failed\n");
    }
    else
    {
        strcpy(buffer, content);
        json_free_serialized_string(content);
        coap_set_payload(response, buffer, strlen(buffer)+1);
    }

    json_value_free(payload_val);
    *offset = -1;
}

/*---------------------------------------------------------------------------*/
RESOURCE(res_get_ilink, "", get_ilink_handle, NULL,
         NULL, NULL);
/*---------------------------------------------------------------------------*/
