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


#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <map>
#include <string>
//#include <fstream.h>
#ifdef _CRTDBG_MAP_ALLOC
#include <crtdbg.h>
#endif

#include "parson.h"
#include "liblwm2m.h"

using namespace std;

map <int, string > g_client_names;

extern "C" void set_client_name(int client_id, char * name)
{
    g_client_names[client_id] = name;
}


extern "C" const char * get_client_name(int client_id)
{
    if(g_client_names.find(client_id) == g_client_names.end())
        return NULL;
    else
        return g_client_names[client_id].c_str();
}

extern "C" void del_client_name(int client_id)
{
    if(g_client_names.find(client_id) != g_client_names.end())
        g_client_names.erase(client_id);
}

/* refer to the msg_format_sample.txt for the message payload format:*/
extern "C" char * generate_register_payload(lwm2m_client_t * client, int * payload_len)
{
    JSON_Value *root_value = json_value_init_array();
    JSON_Array *device_arr = json_value_get_array(root_value);
    int device_num = 0;
    lwm2m_client_t *p_client = client;

    while(p_client != NULL)
    {
    	JSON_Value *device_value = json_value_init_object();
        JSON_Object *device_object = json_value_get_object(device_value);

        json_object_set_string(device_object, "di", client->name);
        json_object_set_string(device_object, "st", "lwm2m");
        json_object_set_number(device_object, "ttl", (double)client->lifetime);
        json_object_set_value(device_object, "links", json_value_init_array());
        JSON_Array *links_arr = json_object_get_array(device_object, "links");

        lwm2m_client_object_t * obj_list = client->objectList;
        while(obj_list != NULL)
        {
            JSON_Value *links_value = json_value_init_object();
            JSON_Object *links_object = json_value_get_object(links_value);
            JSON_Array *inst_arr;

            char value[100];
            sprintf(value, "/%d", obj_list->id);
            json_object_set_string(links_object, "href", value);
            sprintf(value, "oma.lwm2m.%d", obj_list->id);
            JSON_Value *rt_val = json_value_init_array();
            JSON_Array *rt_arr = json_value_get_array(rt_val);
            json_array_append_string(rt_arr, value);
            json_object_set_value(links_object, "rt", rt_val);
            json_object_set_value(links_object, "inst", json_value_init_array());
            inst_arr = json_object_get_array(links_object, "inst");

            lwm2m_list_t * inst = obj_list->instanceList;
            while (inst != NULL)
            {
                json_array_append_number(inst_arr, inst->id);
                inst = inst->next;
            }
            json_array_append_value(links_arr, links_value);
            obj_list = obj_list->next;
        }
        json_array_append_value(device_arr, device_value);
        p_client = p_client->next;
        device_num++;
    }

    char *payload = json_serialize_to_string(root_value);
    json_value_free(root_value);
    *payload_len = strlen(payload) ;

    return payload;
}
