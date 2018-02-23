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
 * rd.cpp
 *
 *  Created on: Dec 4, 2016
 *      Author: xwang98
 *  RD Requirements:
 *  1. Static configured endpoints
 *     a) On startup or new config event, load the static endpoint configuration
 *        setup the RD items for configured endpoints
 *     b) maintain static ep status {service, oos}
 *  2. Handle bus request/event on the /rd entry,
 *     a) add client node for device registration
 *     b) remove client node for device de-registeration
 *     c) add ext resource node for ext service registration
 *
 *  3. Handle query from bus, ilink and local clients
 *
 *  4. optional, handle obs on /rd from clients for particular query parameters
 *

The RD message format:
ONLINE:
[
    {
    "di": "0685B960-736F-46F7-BEC0-9E6CBD61ADC1",
    "st" : "lwm2m" | "ocf" | "ep" | "iagent" | "modbus",
    "addr": "coap://127.0.0.1:2345"        // optional, only when the source is "static configuration"
    "dt": "light",                        // optional
    "ttl": 30,                            // optional
    "set": "u",                           // optional, u: sleep
    "links": [
        {
        "href": "/oic/d",
        "rt":["oic.d.light", "oic.wd.d"],
        “url_p" [
            {                                // optional, only when use a patterned url for a resource
                "name":"prop name",
                "default": "/ab/cd/cd",
                "get": "/mb/a/b/c/d?aa=1",                // optional
                "put": "/xxx/xxxx/",                    // optional
                "post":""                                // optional
                }
            ]
        },
        {
        "href": "/oic/p",
        "rt": ["oic.wk.p"],
        },
        {
        "href": "/switch/1",
        "rt": ["oic.r.switch.binary"],
        },
        {
        "href": "/brightness",
        "rt": ["oic.r.light.brightness"],
        },
        {
        "href": "/3000",
        "inst": [ 10, 11, 12 ],            // optional
        "rt": ["oma.lwm2m.3000"],
        }
    ]
    }
]


OFFLINE:
DELETE /rd/session? [id=123445] or [di=12345]

Heartbeat to cloud (update):
POST /rd/session?[id=123445] or [di=12345]
Payload: blank

 */

#include "rd.h"
#include "parson.h"
#include "CClient.h"
#include "CClientManager.h"
#include "ilink_message.h"
#include "CResObject.h"
#include "er-coap-constants.h"
#include "ams_constants.h"

std::map <unsigned long, JSON_Value *> g_rd_monitor_map;
unsigned long g_rd_monitor_id = 0;
#define CLOUD_RD_MONITOR_ID 0xFFFFFF00

CClient * g_iagent_client = NULL;

void iagent_register_AMS()
{
    if(g_iagent_client) g_iagent_client->RegisterAMS(true);
}

void enable_rd_report_to_cloud()
{
    JSON_Value *json_root = json_value_init_object();
    JSON_Object * root_obj = json_object(json_root);

    json_object_set_string(root_obj, "purl","/ibroker/rd");
    g_rd_monitor_map[CLOUD_RD_MONITOR_ID] = json_root;

}

void init_resource_directory()
{
    if(g_iagent_client) return;

    if(NULL == load_iagent_id())
        return;

    CClient *g_iagent_client = new CClient(get_self_agent_id());

    g_iagent_client->m_standard = "iagent";
    g_iagent_client->m_device_type = "intel.iagent";

    ClientManager().AddClient(g_iagent_client);

    g_iagent_client->Initialize();
    iagent_register_AMS();


    WARNING("Rd initialized");
}


// when a device get online or offline, scan all the monitors.
// and send out rd message to publish urls for matched monitors
void device_rd_monitor_scan(CClient *client)
{
    JSON_Value *payload_val = NULL;

    char *rd_payload = NULL;

    std::map <unsigned long, JSON_Value *>::iterator it;
    for (it=g_rd_monitor_map.begin(); it!=g_rd_monitor_map.end(); ++it)
    {
        JSON_Value *condition = it->second;

        // don't handle RD DELETE for the cloud report since it use different format
        if(!client->m_online && it->first == CLOUD_RD_MONITOR_ID)
            continue;

        if(client->QueryRD(condition))
        {
            // generate the RD payload for matched monitor point once

            // there is a problem yet:
            // if we previously send RD to a monitor point,  now after RD change,
            // it may no longer match the point, but we should send to the point for the device offline
            if(!rd_payload)
            {
                JSON_Value *root_value = json_value_init_array();
                JSON_Array *devices_arr = json_value_get_array(root_value);

                // no configuraion information in the RD report to cloud
                payload_val = client->GenerateRDReport((it->first != CLOUD_RD_MONITOR_ID));
                if(payload_val)
                {
                    json_array_append_value(devices_arr, payload_val);
                    rd_payload = json_serialize_to_string(root_value);
                }
                json_value_free(root_value);
            }

            if(rd_payload)
            {
                const char *purl = json_object_get_string(json_object(condition), "purl");
                unsigned long msgID = SendRequest((char *)purl, IA_APPLICATION_JSON, rd_payload, strlen(rd_payload),
                		true, COAP_POST, NULL, NULL, NULL);
                TraceI(FLAG_CLOUD_MSG, "Send RD report for <%s> to [%s]. monitor id=%d, msgID=%d\n",
                        client->m_epname.c_str(), purl?purl:"", it->first, msgID);

            }
        }
    }


    if (rd_payload) json_free_serialized_string(rd_payload);
}


static bool load_local_device_id(const char *alias, char * local_di, unsigned int len)
{
    char * iagent_id = get_self_agent_id();

    if(iagent_id)
    {
        snprintf(local_di, len, "%s_%s", iagent_id, alias);
        return true;
    }

    else
    {
        snprintf(local_di, len, "%s", alias);
        return true;
    }
}

char * check_local_device_id(char * device_id)
{
    char * iagent_id = get_self_agent_id();

    if(iagent_id)
    {
        char * p = strrchr(device_id, '_');
        if(p && p != device_id)
        {
            int len = (int) (p - device_id);
            if(strncmp(device_id, iagent_id, len) ==0)
                return p+1;
        }
    }
    else
        return device_id;

    return NULL;
}

char * try_use_local_id(char * device_id)
{
    char * local_id = check_local_device_id(device_id);
    if(local_id)
        return local_id;
    else
        return device_id;
}


// handle plugin report its own resource. It is found by the payload has no "di"
// no rt is allowed.
static CClient * handle_plugin_resource_post(JSON_Object *one_device, char *source)
{
    bool resource_change = false;

    if(get_self_agent_id() == NULL) return NULL;

    CClient *client = ClientManager().FindClient(get_self_agent_id());
    if(client == NULL) return NULL;

    JSON_Array *links = json_object_get_array(one_device, "links");
    for (int j = 0; j < (int)json_array_get_count(links); j++)
    {
        JSON_Object *one_link = json_array_get_object(links, j);

        const char *href = json_object_get_string(one_link, "href");
        if (href == NULL) continue;

        JSON_Value *j_rt = json_object_get_value(one_link, "rt");

        const char *rt = "";

        if (j_rt == NULL)
        {
            WARNING("No resource type info in the plugin resource post");
        }
        else if  (json_value_get_type(j_rt) == JSONArray)
        {
            rt = json_array_get_string(json_array(j_rt), 0);
            // todo: mult-type
        }
        else if (json_value_get_type(j_rt) == JSONString)
        {
            rt = json_object_get_string(one_link, "rt");
        }

        std::string::size_type res_num = client->GetResNum();

        CResource *res = client->HandleRdPost((char*)href, (char*)rt, -1);
        client->AddRes(res);

        // if this resource is new allocated, to publish full RD report for all monitors
      if (res_num == client->GetResNum())
          resource_change = true;
    }

    if(resource_change)
        device_rd_monitor_scan(client);

    return client;
}

// handle the rd post from single device
CClient *handle_rd_from_one_device(JSON_Object *one_device, char *source)
{
    const char *di = json_object_get_string(one_device, "di");
    std::list<CResource*> resource_list;
    char local_di[100] = {0};
    const char *alias = NULL;
    alias = json_object_get_string(one_device, "alias");

    if (di == NULL)
    {
    	if(alias != NULL)
    	{
    		if(load_local_device_id(alias, local_di, sizeof(local_di)))
    		{
    			WARNING("Loaded local device id [%s] from alias [%s]", local_di, alias);
    			di = local_di;
    		}
    	}

    	if(di == NULL)
    	{
    	    if(source && strncmp(source, ADDR_BUS "://", 8) == 0)
    	    {
    	        return handle_plugin_resource_post(one_device, source);
    	    }
			LOG_MSG("rd payload: di was NULL");
			return NULL;
    	}
    }

    const char *st = json_object_get_string(one_device, "st");
    if (st && source == NULL && strcmp(st, "ep") == 0)
    {
        const char *addr = json_object_get_string(one_device, "addr");
        // static configured endpoint must have a ip address
        if (addr == NULL)
        {
            LOG_MSG("static configured endpoint must have a ip address");
            return NULL;
        }
        source = (char *)addr;
    }

    const char *dt = json_object_get_string(one_device, "dt");
    int ttl = json_object_get_boolean(one_device, "ttl");
    const char *set = json_object_get_string(one_device, "set");
    bool resource_change = false;
    if (ClientManager().FindClient(di) != NULL)
        resource_change = true;

    CClient *client = ClientManager().HandleRdPost(di, st, dt, ttl, set);

    // later we can find device by device alias name
    if(alias != NULL)
        ClientManager().SetDeviceAlias(alias, client);

    // the connection will be set later
    if (source && strcmp(source, "connection") != 0)
        client->SetAddress (source);

    JSON_Array *links = json_object_get_array(one_device, "links");
    for (int j = 0; j < (int)json_array_get_count(links); j++)
    {
        JSON_Object *one_link = json_array_get_object(links, j);

        const char *href = json_object_get_string(one_link, "href");
        if (href == NULL) continue;

        JSON_Value *j_rt = json_object_get_value(one_link, "rt");
        if (j_rt == NULL) continue;
        const char *rt = NULL;

        if  (json_value_get_type(j_rt) == JSONArray)
        {
            rt = json_array_get_string(json_array(j_rt), 0);
            // todo: mult-type
        }
        else if (json_value_get_type(j_rt) == JSONString)
        {
            rt = json_object_get_string(one_link, "rt");
        }
        if (rt == NULL) continue;

        std::string::size_type res_num = client->GetResNum();

        JSON_Array *instances = json_object_get_array(one_link, "inst");
        if (instances != NULL )
        {
            for (int k = 0; k < (int)json_array_get_count(instances); k++)
            {
                int instance = json_array_get_number(instances, k);
                CResource *res = client->HandleRdPost((char*)href, (char*)rt, instance);
                resource_list.push_front(res);
            }
        }
        else
        {
            CResource *res = client->HandleRdPost((char*)href, (char*)rt, -1);

            // handle for properties has separate url for access
            if (res->GetType() == T_ResourceObject)
            {
                JSON_Value  *url_separate = json_object_get_value(one_link, "url_p");
                if (url_separate) ((CResObject*)res)->SetPropertySeparateUrls(url_separate);
            }
            resource_list.push_front(res);
        }

          // if this resource is new allocated, to publish full RD report for all monitors
        if (res_num == client->GetResNum())
            resource_change = true;
    }

    // if there is any resource to remove (not in current report), publish full RD report
    if (client->GetResNum() != 0)
        resource_change = true;

    //
    client->RenewResources(resource_list);

    // USER DEFINE DATA
    JSON_Value  * user_data = json_object_get_value(one_device, "user_data");
    if(user_data == NULL)
    {
        ;
    }
    else if(json_type(user_data) == JSONString)
    {
        TraceI(FLAG_CLOUD_MSG, "rd contain string type user data");
        client->SetUserData(Is_string, (char *)json_string(user_data) );
    }
    else if(json_type(user_data) == JSONObject)
    {

        TraceI(FLAG_CLOUD_MSG, "rd contain JSON type user data");
        client->SetUserData(Is_json,user_data);
    }

    client->Initialize();

    if(resource_change)
        device_rd_monitor_scan(client);

    // enable the refresher points under this client
    ResRefresher().HandleClientStatus(client, true);
    return client;

}


bool handle_rd_on_connection(const char *payload, uint16_t payload_len, void  *connection)
{
    bool allocated = false;
    char *content = get_string((char *)payload, payload_len, &allocated);

    TraceI(FLAG_CLOUD_MSG, "handle rd: payload=%s, playload_len=%d, content=%s",
                payload, payload_len, content);
    JSON_Value *root_value = json_parse_string(content);
    if (allocated) free(content);

    if (root_value == NULL)
    {
        WARNING("<%s> rd payload is not valid!", __FUNCTION__);
        return false;
    }

    TraceI(FLAG_CLOUD_MSG, "rootType=%d", json_value_get_type(root_value));
    if (json_value_get_type(root_value) == JSONObject)
    {
        CClient *client = handle_rd_from_one_device(json_object(root_value), (char *)"connection");

        if(client)
        {
            client->SetConnection((connection_t*)connection);
            json_value_free(root_value);
            return true;
        }
    }

    json_value_free(root_value);
    WARNING("handle rd on connection failed!");
    return false;
}


// source: "bus//:[module-id]", "udp//:[ip addr]:[port]", "Static"
extern "C" bool handle_rd_post(const char *payload, char *source)
{
    // parse the payload
    bool result = true;

    JSON_Array *devices;
    JSON_Object *one_device;
    JSON_Value *root_value = json_parse_string(payload);
    if (root_value == NULL)
    {
        LOG_MSG("post rd msg payload was NULL");
        return false;
    }

    if(json_value_get_type(root_value) == JSONObject)  //one device
    {
        one_device =json_value_get_object(root_value);
        result = handle_rd_from_one_device(one_device, source);
    }
    else if (json_value_get_type(root_value) == JSONArray)
    {
        devices = json_value_get_array(root_value);
        for (int i = 0; i < (int)json_array_get_count(devices); i++)
        {
            one_device = json_array_get_object(devices, i);
            CClient *client = handle_rd_from_one_device(one_device, source);
            if(client == NULL) result = false;
        }
    }
    else
        result = false;

    json_value_free(root_value);
    return result;
}

/******************************************
 *
 * dt/rt/group/groups/status
    [
        {
            "di": "0685B960-736F-46F7-BEC0-9E6CBD61ADC1",
            "s" : "on" | "off"
            "st" : "lwm2m" | "ocf" | "rest" | "publish",
            "addr": "coap://127.0.0.1:2345"        // optional, only when
            "groups": ["", "", ""],
            "dt": "light",                        // optional
            "set": "u",                            // optional, u: sleep
            "links": [
                {
                    "href": "/switch/1",
                    "rt": ["oic.r.switch.binary"],
                    "groups": ["", "", ""],
                },
                {
                    "href": "/brightness",
                    "rt": ["oic.r.light.brightness"],
                    "groups": ["", "", ""],
                },
                {
                    "href": "/3000",
                    "inst": [ 10, 11, 12 ],            // optional
                    "rt": ["oma.lwm2m.3000"],
                    "groups": ["", "", ""],
                }
            ]
        }
    ]
 */


/*
 * Query conditions:
 * 1) rt=rt1, return
 * 2) dt
 * 3) withrts=[rt1, rt2, rt3]
 * 4) di=
 */

extern "C" void handle_rd_get(const char *q_value, char **res_payload, bool with_cfg)
{
    JSON_Value *jroot = NULL;
    if (q_value)
    {
        jroot = query_string_to_json(q_value);
        if (jroot == NULL)
        {
            LOG_MSG("get rd msg query was NULL");
            return;
        }

        JSON_Object *root_obj =  json_object(jroot);
        const char  * withrts = json_object_get_string (root_obj, "with_rts");
        if (withrts)
        {
            // can't use parson json parser because the elements have no ""

            JSON_Value *j_withrts = (JSON_Value *)parse_string_array_to_Json((char *)withrts, ',');
            if(j_withrts)
            {
                json_object_set_value(root_obj, "with_rts",j_withrts);
            }
            else
            {
                json_object_remove(root_obj, "with_rts");
            }
        }
        const char  * groups = json_object_get_string (root_obj, "groups");
        if (groups)
        {
            // can't use parson json parser because the elements have no ""

            JSON_Value *j_groups = (JSON_Value *)parse_string_array_to_Json((char *)groups, ',');
            if(j_groups)
            {
                json_object_set_value(root_obj, "groups",j_groups);
            }
            else
            {
                json_object_remove(root_obj, "groups");
            }
        }
    }

    JSON_Value *response_val = json_value_init_array();
    JSON_Array *response_arr = json_value_get_array(response_val);
    JSON_Value *payload_val = NULL;

    std::list<CClient*> &clist = ClientManager().FindAllClient();
    std::list<CClient*>::iterator it;

    for (it=clist.begin(); it!= clist.end(); it++)
    {
        CClient *client = ((CClient*)(*it));
        if (client->QueryRD(jroot))
        {
            payload_val = client->GenerateRDReport(with_cfg);
            if (payload_val)
                json_array_append_value (response_arr, payload_val);
        }
    }

    *res_payload = json_serialize_to_string(response_val);
    json_value_free(response_val);
    if (jroot)
        json_value_free(jroot);
}


static void report_rd_delete_to_cloud(CClient *client)
{
    char query[100];

    if (g_cloud_status != iReady_For_Work)
    {
        return;
    }

    snprintf(query, sizeof(query), "di=%s", client->m_epname.c_str());
    SendRequest((char *) "/ibroker/rd/session", IA_APPLICATION_JSON,
            NULL, 0, false, COAP_DELETE, query, NULL, NULL);
    WARNING("Sent RD <%s> offline to ibroker.\n", client->m_epname.c_str());
}


static bool one_device_delete(char *di)
{
    if(di == NULL)
        return false;


        CClient *client = ClientManager().FindClient(di);
        if(client)
        {
            client->m_online = false;

            // disable the refresher points under this client
            ResRefresher().HandleClientStatus(client, false);


            // the rd delete message to cloud has different format with publish points
            report_rd_delete_to_cloud(client);

            device_rd_monitor_scan(client);
            ClientManager().FreeClient(client);

            WARNING("RD: device %s is removed", di);
            return true;
        }

    return false;

}

extern "C" bool handle_rd_delete(const char *query)
{
    bool result = true;
    JSON_Value *query_value = query_string_to_json(query);
    const char *di = json_object_get_string(json_value_get_object(query_value), "di");

    if(di == NULL)
    {
        LOG_MSG("delete rd di was NULL");
        return false;
    }
    result = one_device_delete((char *)di);

    return result;
}


extern "C" void do_rd_monitor_scan(unsigned long monitor_id)
{
    if (g_rd_monitor_map.find(monitor_id) == g_rd_monitor_map.end())
        LOG_RETURN()

    JSON_Value *j_monitor = g_rd_monitor_map[monitor_id];
    std::list<CClient*> &clist = ClientManager().FindAllClient();
    std::list<CClient*>::iterator it;
    const char *purl = json_object_get_string(json_object(j_monitor), "purl");

    for (it=clist.begin(); it!= clist.end(); it++)
    {
        CClient *client = ((CClient*)(*it));
        if(client->QueryRD(j_monitor))
        {
            JSON_Value *device_val = client->GenerateRDReport((CLOUD_RD_MONITOR_ID != monitor_id));
            if(device_val)
            {
                JSON_Value *root_value = json_value_init_array();
                JSON_Array *devices_arr = json_value_get_array(root_value);
                json_array_append_value(devices_arr, device_val);
                char *rd_payload = json_serialize_to_string(root_value);
                if(rd_payload)
                {
                    SendRequest((char *)purl, IA_APPLICATION_JSON, rd_payload,
                            strlen(rd_payload)+1, false, COAP_POST, NULL, NULL, NULL);
                    WARNING("Post RD to %s, monitor id is %d, device  is <%s>", purl, monitor_id, client->m_epname.c_str());
                    json_free_serialized_string(rd_payload);
                }
                json_value_free(root_value);
            }
        }

    }
}



extern "C" int handle_rd_monitor_put(const char *payload, char *purl_prefix)
{
    int result = -1;
    JSON_Value *json = json_parse_string( payload);
    if (json == NULL) return false;
    JSON_Object *obj = json_value_get_object(json);

    const char *purl = json_object_get_string(obj, "purl");
    if (purl == NULL)
    {
        WARNING("No publish url in RD monitor request");
        goto end;
    }

    if(purl_prefix && purl_prefix[0])
    {
        if(strstr (purl, "://") == NULL)
        {
            char buffer[256];
            if(purl[0] == '/') purl ++;
            snprintf(buffer, sizeof(buffer), "%s%s", purl_prefix, purl);
            TraceI(FLAG_DUMP_MESSAGE, "RD monitor: purl is %s\n", buffer);
            json_object_set_string(obj, "purl", buffer);
        }
    }


    // presence of mid means to modify existing monitor point
    JSON_Value *value;
    if (checkjson(obj, (char*)"mid", &value, JSONNumber))
    {
        unsigned long monitor_id = get_json_number_safe(obj, "mid");
        if(g_rd_monitor_map.find(monitor_id) != g_rd_monitor_map.end())
        {
            JSON_Value *v = g_rd_monitor_map[monitor_id];
            json_value_free(v);
            g_rd_monitor_map[monitor_id] = json;

            WARNING("RD monitor point %d updated. PURL=%s", monitor_id, purl);

            return monitor_id;
        }
        else
        {
            LOG_GOTO("can't find monitor id when handle monitor put", end);
        }
    }
    else
    {
        g_rd_monitor_map[++g_rd_monitor_id] = json;

        WARNING("RD monitor point %d created. PURL=%s", g_rd_monitor_id, purl);

        return g_rd_monitor_id;
    }

end:
    json_value_free(json);
    return result;
}

extern "C" void report_rd_to_cloud()
{
    do_rd_monitor_scan(CLOUD_RD_MONITOR_ID);
}


extern "C" void load_configured_endpoints()
{
    char *content = NULL;
    char ini_path[MAX_PATH_LEN] = {0};
    char *path = get_product_config_pathname((char*)"endpoints.cfg", (char*)TT_DEVICE, (char*)NULL, ini_path);

    load_file_to_memory(path, &content);

    if(content)
    {
        TraceI(FLAG_DEVICE_REG, "loading endpoints.cfg for static devices");
        handle_rd_post(content, NULL);
        free(content);
    }
}


void *find_device(const char *epname)
{
    return ClientManager().FindClient(epname);
}


bool is_passive_device(void *device)
{
    return ((CClient*)device)->m_is_passive_device;
}


connection_t *get_endpoint_conn(const char *epname)
{
    CClient *client = ClientManager().FindClient(epname);
    if(client)
    {
        return client->GetConnection();
    }

    return NULL;
}


extern "C" char *get_endpoint_type(const char *epname)
{
    CClient *client = ClientManager().FindClient(epname);
    if (client)
        return (char*) client->m_standard.c_str();
    else
        return NULL;
}


void check_url_generic_dev(coap_packet_t *coap_message, char *url_buffer, int buffer_len)
{
    char device_id[100] = {0};

    int idx = 0;
    char new_url[512];
    char *p = new_url+4;        // now p is the start of device id section

    // the caller must call this function only for url start with "dev/"
    assert(strncmp(url_buffer, "dev/", 4) == 0);

    // ensure no buffer overflow
    if (strlen (url_buffer) >= (sizeof (new_url)-1))
    {
        WARNING("check_url_generic_dev: url to long");
        return;
    }

    strcpy(new_url, url_buffer);

    while (*p != '\0' && idx < 99)
    {
        if (*p == '/')
            break;
        device_id[idx] = *p;
        p++;
        idx++;
    }

    if (idx == 99)
    {
        WARNING("the device id is longer than 100 in coap url");
        return;
    }

    CClient *client = ClientManager().FindClient(device_id);
    if (!client)
        return;

    strcpy (url_buffer, client->m_standard.c_str());

    if(!client->m_alias_name.empty())
    {
        strcat(url_buffer, "/");
        strcat(url_buffer, client->m_alias_name.c_str());
        if (buffer_len < (strlen (url_buffer) + strlen (p)))
            return;
        strcat (url_buffer, p);
    }
    else
    {

        if (buffer_len < (strlen (url_buffer) + strlen (new_url+3)))
            return;
        strcat (url_buffer, new_url+3);
    }

    TraceI(FLAG_DEVICE_REG, "URL changed to %s", url_buffer);


    coap_set_header_uri_path(coap_message, url_buffer);
}


extern "C" char *get_alias_device_id(const char *alias, char * url_buffer, int url_len)
{
    CClient *client = ClientManager().FindAliasDevice(alias);
    if (client)
    {
        if(url_buffer && url_len)
        {
            snprintf(url_buffer,url_len,  "%s/%s", client->m_standard.c_str(), client->m_epname.c_str());
        }
        return (char*) client->m_epname.c_str();
    }
    else
        return NULL;
}

extern "C" char *get_alias_url_from_di(const char *di, char * url_buffer, int url_len)
{
    CClient *client = ClientManager().FindClient(di);
    if (client && (!client->m_alias_name.empty()))
    {
        if(url_buffer && url_len)
        {
            snprintf(url_buffer,url_len,  "%s/%s", client->m_standard.c_str(), client->m_alias_name.c_str());
        }
        return (char*) client->m_alias_name.c_str();
    }
    else
        return NULL;
}


extern "C" bool check_seperate_url(char *device_id, char *url, int action, char **new_url, char **new_params)
{
    CClient *client = ClientManager().FindClient(device_id);
    if (client == NULL) return false;

    if (!client->m_has_separate_url) return false;

    std::string property_name;
    CResource *res = client->UrlMatchResource(url, & property_name);
    if (res == NULL) return false;

    if (res->GetType() == T_ResourceObject)
    {
            CResObject *res_obj = (CResObject*)res;
            char *separate_url = res_obj->GetPropertySeparateUrl((char*)property_name.c_str(), action);

            if (separate_url)
            {
                *new_url = strdup(separate_url);
                char *param = strchr(*new_url, '?');
                if (param)
                {
                    *new_params = strdup(param+1);
                    *param = 0;
                }

                return true;
            }
    }

    return false;
}




#if 0
static bool load_local_device_id(const char *alias, char * local_di, unsigned int len)
{
    char local_cfg_path[512];

    JSON_Value * root = NULL;
    JSON_Array *id_arr = NULL;
    JSON_Value * j_device_id;
    snprintf(local_cfg_path, sizeof(local_cfg_path), "%sep_local_device_id.cfg", get_config_path());

    if ((root = json_parse_file(local_cfg_path)) == NULL)
    {
        goto add;
    }

    /// handle the bus configuration
    id_arr =  json_object_get_array(json_value_get_object(root), "id");
    if(id_arr == NULL)
    {
        goto add;
    }

    for (unsigned i = 0; i < json_array_get_count(id_arr); i++)
    {
        JSON_Object*  jobj_id = json_array_get_object(id_arr, i);
        const char *uuid = json_object_get_string(jobj_id,"uuid" );
        const char *alias_read = json_object_get_string(jobj_id, "alias");

        if ( alias_read == NULL)
        {
            WARNING("ep_local_device_id.cfg: no alias!");
            continue;
        }

        if(strcmp(alias_read, alias) == 0)
        {
            if(uuid)
            {
                if(len <= strlen(uuid))
                {
                    LOG_ME();
                    return false;
                }
                strcpy(local_di, uuid);

                return true;
            }

            snprintf(local_di, len, "%lu-%s", time(NULL), alias);
            json_object_set_string(jobj_id, "uuid", local_di);
            goto save;
        }
    }



add:
    snprintf(local_di, len, "%lu-%s", time(NULL), alias);


    if(root == NULL)
        root = json_value_init_object();

    if(id_arr == NULL)
    {
        JSON_Value * v = json_value_init_array();
        json_object_set_value(json_object(root), "id",v );
        id_arr = json_array(v);
    }

    j_device_id = json_value_init_object();
    json_object_set_string(json_object(j_device_id), "alias", alias);
    json_object_set_string(json_object(j_device_id), "uuid", local_di);

    json_array_append_value(id_arr, j_device_id);

    WARNING("created a new local stored ep device id [%s] for [%s]", local_di, alias );

save:
    json_serialize_to_file(root, local_cfg_path);
    json_value_free(root);

    return true;
}

#endif
