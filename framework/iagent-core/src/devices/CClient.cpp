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
 * CClient.cpp
 *
 *  Created on: Aug 6, 2015
 *      Author: xwang98
 *
 *  Requirements:
 *  There are two situation that client is created:
 *  1. a client device (lwm2m, ocf) registration
 *  2. pre-configured industrial devices (modbus etc) or RESTful servers
 *
 *  In the first case, the resource objects are created from the registration payload,
 *  then the resource properties is created from the resource meta configuration file.
 *
 *  In the second case, the resource objects are retrieved from the device meta configuration
 */
// system
#include <stdlib.h>
#include <fstream>

//iagent
#include "CClient.h"
#include "CResObject.h"
#include "parson.h"
#include "CResProperty.h"
#include "ilink_message.h"
#include "iagent.h"
#include "parson_ext.h"
#include "url-parser-c/url_parser.h"
#include "ams_constants.h"
#include "logs.h"
#include "ams_sdk_interface.h"

extern "C" {
#include "connection.h"
}

int CClient::new_id = 0;
int g_lwm2m_ack_timeout = 2;

//extern bool g_enable_calibration;

CClient::CClient(char *epname):
    m_epname(epname),
    m_standard(""),
    m_settings(""),
    m_ttl(0),
    m_online(true),
    m_is_queue_mode(false),
    m_is_passive_device(false),
    m_last_active_time(0),
    m_queued_req_num(0),
    m_config(NULL),
    m_has_separate_url(false),
    m_connection(NULL),
    m_is_calibrate(false),
    m_user_data_type(Is_None),
    m_user_data(NULL)
{
    // TODO Auto-generated constructor stub
    m_ref_num = 0;
    m_id = new_id++;
}

CClient::~CClient()
{
    CleanupConfig();

    FreeAllRes();

    SetConnection(NULL);

    if(m_user_data)
    {
        SetUserData((int)Is_None, NULL);
        m_user_data = NULL;
    }


}



bool CClient::Initialize()
{
    JSON_Value *jconfig = NULL;
    bool result = false;

    // ensure the ams will download the device.cfg.
    // after the device.cfg is downloaded, the RegisterAMS()
    // will add ams register for groups config files.
    ams_add((char*)TT_DEVICE_ON_GW, (char *)m_epname.c_str(),false);

    /* Load the device config file and apply it*/
    if (!LoadConfig(&jconfig))
        return false;

    if(!CheckConfigSanity(jconfig))
    {
        ERROR("Client %s, id=%d, config file is not correct", m_epname.data(), m_id);
        json_value_free(jconfig);
        return false;
    }

    // apply the device configuration
    result = ApplyClientConfig(jconfig);

    this->RegisterAMS(true);

    TraceI(FLAG_DEVICE_REG, "client %s (%d) initialize finished. total resource: %d", m_epname.data(), m_id, GetResNum());
    json_value_free(jconfig);
    return result;
}

void CClient::AddRes(CResource *res)
{
    this->m_res_list.push_front(res);
    res->m_parent_device = this;

    if (res->GetType() == T_ResourceObject)
    {
        if (((CResObject*)res)->GetSeparateUrlProperties() != 0)
            this->m_has_separate_url = true;
    }
}

void CClient::RemoveRes(CResource *res)
{
    this->m_res_list.remove(res);
    res->m_parent_device=NULL;

}

//
// prefer the alias name, so the plugin can recoginize it
// give the global id if no local alias name
//
const char * CClient::GetOutName()
{
    if(!m_alias_name.empty())
        return m_alias_name.c_str();
    else
        return m_epname.c_str();
}


bool  CClient::HasId(const char * id)
{
    if(!m_alias_name.empty() && m_alias_name == id)
        return true;
    else
        return (m_epname == id);
}

void CClient::SetUserData(int type, void * data)
{
    if(m_user_data)
    {
        if(m_user_data_type == Is_json)
        {
            json_value_free((JSON_Value*)m_user_data);
        }
        else
        {
            free(m_user_data);
        }

        m_user_data = NULL;
    }


    m_user_data_type = type;

    if(m_user_data_type == Is_json)
    {
        m_user_data = json_value_deep_copy((JSON_Value*)data);
    }
    else
    {
        m_user_data = strdup((const char*)data);
    }
}


void CClient::RenewResources(std::list<CResource*> & resources)
{
    // clean up the remaining resource node
    // during the RD post handling, it mean the resources in the list
    // are no longer in the device
    FreeAllRes();

    std::list<CResource*>::iterator it;
    for (it=resources.begin(); it!= resources.end(); it++ )
    {
        CResource *res = (CResource*)(*it);
        AddRes(res);
    }

    resources.clear();
}

void CClient::FreeAllRes()
{

    std::list<CResource*>::iterator it;
    int cnt = 0;

    for (it=m_res_list.begin(); it!= m_res_list.end(); it++ )
    {
        CResource *res = (CResource*)(*it);
        /* remove will impact the list also, it is needn't.
        if(res->m_parent_device)
            res->m_parent_device->RemoveRes(res);
        */
        delete res;
        cnt ++;
    }

    // note: use clear() rather than erase in the loop as it impacts the loop traverse
    m_res_list.clear();

    TraceI(FLAG_DEVICE_REG, "client [%s], freed all %d resources", m_epname.data(), cnt);
}

CResource *CClient::FindRes(const char *url)
{
    std::list<CResource*>::iterator it;
    for (it=m_res_list.begin(); it!= m_res_list.end(); it++)
    {
        CResource *res = ((CResource*)(*it));
        if (res->get_url() == url)
        {
            return res;
        }
    }

    return (NULL);
}


bool CClient::QueryRD(JSON_Value *query)
{
    // no query condition means true condition
    if (query == NULL)
        return true;

    JSON_Object *root_obj =  json_object(query);
    bool match = false;
    const char *dt = json_object_get_string(root_obj, "dt");
    if (dt)
    {
        if (m_device_type.compare(dt) != 0)
            return (false);
    }
    const char *di = json_object_get_string(root_obj, "di");
    if (di)
    {
        if (m_epname.compare(di) != 0 && m_alias_name.compare(di) != 0)
            return (false);
    }
    const char *st = json_object_get_string(root_obj, "st");
    if (st)
    {
        if (m_standard != st)
            return (false);
    }

    const char *rt = json_object_get_string(root_obj, "rt");
    JSON_Array  *with_rts = json_object_get_array (root_obj, "with_rts");
    if (rt || (with_rts && json_array_get_count(with_rts)>0))
    {
        std::list<CResource*>::iterator it;
        for (it=m_res_list.begin(); it!= m_res_list.end(); it++)
        {
            CResource *res = ((CResource*)(*it));
            if(res->GetType() == T_ResourceObject)
            {
                CResObject *obj = (CResObject*)res;
                if(with_rts)
                {
                    for(int i = 0; i < (int)json_array_get_count(with_rts); i++)
                    {
                        const char *rt = json_array_get_string (with_rts,i);
                        if(obj->m_rt == rt)
                        {
                            match = true;
                            break;
                        }
                    }
                }
                else if(obj->m_rt == rt)
                {
                    match = true;
                    break;
                }
            }
        }
        if(!match) return false;
    }

    JSON_Array  *j_groups = json_object_get_array (root_obj, "groups");
    match = false;
    if (j_groups)
    {
        CClientConfig *cfg = m_config;

        for (int n = 0; n < (int)json_array_get_count(j_groups); n++)
        {
            const char *group = json_array_get_string (j_groups,n);

            // check if the device is in the groups
            if (NULL != cfg)
            {
                for(int i=0;i<cfg->m_grp_num;i++)
                {
                    if(strcmp(cfg->m_grps[i], group) == 0)
                    {
                        return true;
                    }
                }
            }

            // check if any resource object is in the groups
            std::list<CResource*>::iterator it;
            for (it=m_res_list.begin(); it!= m_res_list.end(); it++)
            {
                CResource *res = ((CResource*)(*it));
                if(res->GetType() == T_ResourceObject)
                {
                    CResObject *obj = (CResObject*)res;
                    CResourceObjectConfig *res_config = obj->m_res_object_config;
                    if(res_config)
                    {
                        for(int i=0;i<res_config->m_grp_num;i++)
                        {
                            if(strcmp(res_config->m_grps[i], group) == 0)
                            {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    return true;
}

// with_cfg: also mean whether this RD report will be sent to cloud.
//           no config info for the rd to cloud.
JSON_Value *CClient::GenerateRDReport(bool with_cfg)
{
    JSON_Value *res_value = json_value_init_object();
    JSON_Object *res_object = json_value_get_object(res_value);

    json_object_set_string(res_object, "di", m_epname.c_str());

    if(!with_cfg) //only include aid when report to ibroker
    {
        // if "di" is same as alias, the di name was auto generated when the iagent
        // was not provisioned yet. we should report it to the cloud.
        // refer to function: load_local_device_id()
        if(m_epname == m_alias_name)
        {
            return NULL;
        }
        json_object_set_string(res_object, "aid", get_self_agent_id());
    }
    else
    {
        if(!m_alias_name.empty()) json_object_set_string(res_object, "alias", m_alias_name.c_str());
    }

    json_object_set_string(res_object, "status", m_online ?"on":"off");

    json_object_set_string(res_object, "st", m_standard.c_str());

    if (!m_addr.empty())
        json_object_set_string(res_object, "addr", m_addr.c_str());

    if (with_cfg&&m_config)
    {
        json_object_set_value(res_object, "groups", json_value_init_array());
        JSON_Array *group_arr = json_object_get_array(res_object, "groups");
        if (NULL != group_arr)
        {
            for (int i = 0; i < m_config->m_grp_num; i++)
            {
                if (NULL != m_config->m_grps[i])
                    json_array_append_string(group_arr, m_config->m_grps[i]);
            }
        }
        if (NULL != m_config->m_cfg)
            json_object_set_string(res_object, "attrs", m_config->m_cfg);
    }

    if (!m_device_type.empty())
        json_object_set_string(res_object, "dt", m_device_type.c_str());
    else
        json_object_set_string(res_object, "dt", "");

    if (!m_settings.empty())
        json_object_set_string(res_object, "set", m_settings.c_str());
    else
        json_object_set_string(res_object, "set", "");

    if(m_user_data)
    {
        if(m_user_data_type == Is_string)
        {
            json_object_set_string(res_object, "user_data", (const char*) m_user_data);
        }
        else if(m_user_data_type == Is_json)
        {
            json_object_set_value(res_object, "user_data", json_value_deep_copy((JSON_Value*) m_user_data));
        }
    }

    JSON_Value *links_value = json_value_init_array();
    JSON_Array *links_arr = json_value_get_array(links_value);
    //links
    std::list<CResource*>::iterator it;

    for (it = m_res_list.begin(); it!= m_res_list.end(); it++)
    {
        CResource *res = ((CResource*)(*it));
        if (res == NULL || res->GetType() != T_ResourceObject) continue;

        CResObject *res_obj = (CResObject *)res;
        CResourceObjectConfig *config = res_obj->m_res_object_config;
        JSON_Value *link_value = json_value_init_object();
        JSON_Object *link_object = json_value_get_object(link_value);

        json_object_set_string(link_object, "href", res_obj->get_url().c_str());
        json_object_set_value(link_object, "rt", json_value_init_array());
        JSON_Array *rt_arr = json_object_get_array(link_object, "rt");
        json_array_append_string(rt_arr, res_obj->m_rt.c_str());
        if (with_cfg && config)
        {
            if (0 != res_obj->m_res_object_config->m_grp_num)
            {
                json_object_set_value(link_object, "groups", json_value_init_array());
                JSON_Array *grps_arr = json_object_get_array(link_object, "groups");
                for (int i = 0; i < config->m_grp_num; i++)
                {
                    json_array_append_string(grps_arr, config->m_grps[i]);
                }
            }
            if(config->m_res_cfg)
                json_object_set_string(link_object, "attrs", config->m_res_cfg);
        }
        json_array_append_value(links_arr, link_value);
    }

    json_object_set_value(res_object, "links", links_value);

    return res_value;
}


extern "C" void remove_connection_from_list(connection_t *conn);
void CClient::SetConnection(connection_t *connection)
{
    if (m_connection && connection != m_connection)
    {
        remove_connection_from_list(m_connection);
        free (m_connection);
        m_connection = NULL;
    }

    m_connection = connection;
}


void CClient::SetAddress(char *addr)
{
    if (!addr)
        LOG_RETURN();
    m_addr = addr;
    url_parser_url_t parsed_url = {0};
    int error = parse_url(addr, true, &parsed_url);
    if(error != 0)
    {
        free_parsed_url(&parsed_url);
        WARNING("client [%s] set address error: %s", m_epname.data(), addr);
        return;
    }

    TraceI(FLAG_DEVICE_REG, "client [%s] set addr: %s", m_epname.data(), addr);

    if(parsed_url.host_exists && parsed_url.port)
    {
        char port_str[100];
        sprintf (port_str, "%d",parsed_url.port);
        connection_t *connection = connection_create(NULL, g_socket_coap_ep, parsed_url.host_ip, port_str,AF_INET);
        TraceI(FLAG_DEVICE_REG, "client [%s] had socket connection ready", m_epname.data());
        SetConnection(connection);
    }

    free_parsed_url(&parsed_url);
}


//
// remove the existing the node and return it to the caller
// or malloc a new node and return it to caller
// it help to rebuild the resource list
CResource *CClient::HandleRdPost(char *uri, char *type, int instance)
{
    char res_uri[100];
    if (-1 == instance)
        sprintf (res_uri, "%s", uri);
    else
        sprintf (res_uri, "%s/%d", uri, instance);

    CResource *res = FindRes(res_uri);
    if (res == NULL)
    {
        res = (CResObject*) new CResObject(res_uri, type);
    }
    else
    {
        // remove the resource node from the list
        // so when the whole RD post process is completed,
        // the remaining nodes is no longer present and will be deleted
        // then we renew the resource list.
        RemoveRes(res);
    }

    return res;
}


CResource *CClient::UrlMatchResource(char *url, std::string *property_name)
{
    std::list<CResource*>::iterator it;
    int len = strlen (url);

    for (it = m_res_list.begin(); it != m_res_list.end(); it++)
    {
        CResource *res = ((CResource*) (*it));
        int offset = check_url_start(url, len, (char*)res->get_url().c_str());

        if (offset)
        {
            if (property_name && (len) > offset)
                *property_name = (url+offset);

            TraceI(FLAG_DEVICE_REG, "client [%s], matched url [%s], res=[%s], p=[%s]",
                    m_epname.c_str(), url, res->get_url().c_str(),
                    property_name?property_name->c_str():".");
            return res;
        }
    }
    TraceD(FLAG_DEVICE_REG, "client [%s], NOT match url [%s]",
            m_epname.c_str(), url);
    return NULL;
}


/* Load the device config file into m_jconfig*/
bool CClient::LoadConfig(JSON_Value **device_cfg)
{

	char ini_path[MAX_PATH_LEN] = {0};
    const char * path = (const char*)get_product_config_pathname((char*)"device.cfg", (char*)TT_DEVICE_ON_GW, (char *)m_epname.c_str(), ini_path);
    *device_cfg = json_parse_file(path);
    if(NULL == *device_cfg)
    {
        TraceD(FLAG_CLOUD_CONFIG, "configuration file(%s) doesn't exist or corrupted.", path);
        return false;
    }

    return true;
}


inline bool check_json_type(JSON_Object *value_obj, char *name, JSON_Value **value, JSON_Value_Type type, bool check = false)
{
    *value = json_object_get_value(value_obj, name);
    if (*value && json_value_get_type(*value) != type)
    {
        ERROR("checkjson: %s is not %d type.(2:string. 3:number. 4:object. 5:array)", name, type);
        return false;
    }
    else if(!*value && check)
    {
        ERROR("checkjson: miss key <%s>", name);
        return false;
    }
    return true;
}


//
// check the config content is valid
//
bool CClient::CheckConfigSanity(JSON_Value *device_cfg)
{
    if (JSONObject != json_value_get_type(device_cfg))
    {
        ERROR("config is not a object json:%d", json_value_get_type(device_cfg));
        return false;
    }
    JSON_Object *value_obj =  json_value_get_object(device_cfg);
    JSON_Value *value = NULL;
    bool result = true;

    result |= check_json_type(value_obj, (char *)"di", &value, JSONString, true);
    result |= check_json_type(value_obj, (char *)"rn", &value, JSONNumber);
    result |= check_json_type(value_obj, (char *)"cfg", &value, JSONObject);
    result |= check_json_type(value_obj, (char *)"attr", &value, JSONObject);
    result |= check_json_type(value_obj, (char *)"id", &value, JSONNumber);
    result |= check_json_type(value_obj, (char *)"grp", &value, JSONArray);
    result |= check_json_type(value_obj, (char *)"dl", &value, JSONNumber);
    result |= check_json_type(value_obj, (char *)"links", &value, JSONArray, true);

    // todo:
    TraceI(FLAG_DEVICE_REG, "client [%s], configuration sanity check okay", m_epname.c_str());
    //json_value_free(value);
    return result;
}




// refer to configure.cpp for the format definition of configuration file
bool CClient::ApplyClientConfig(JSON_Value *device_cfg)
{
    // load the device configuration part
    JSON_Object *device_obj = json_value_get_object(device_cfg);

    if (m_config == NULL)
    {
        m_config = new CClientConfig();

        m_config->m_rn = (int)get_json_number_safe(device_obj, "rn");
        if (-1 == m_config->m_rn)
            m_config->m_rn = 1;

        JSON_Value *attr = json_object_get_value(device_obj, "attr");
        if (attr)
            m_config->m_cfg = json_serialize_to_string(attr);

        m_config->m_id = (unsigned long)get_json_number_safe(device_obj, "id");

        JSON_Array *grp_arr = json_object_get_array(device_obj, "grp");
        if (NULL != grp_arr)
        {
            m_config->m_grp_num = (int)json_array_get_count(grp_arr);
            if (0 < m_config->m_grp_num)
            {
                m_config->m_grps = (char **)malloc(sizeof(char **) *m_config->m_grp_num);
                if (NULL != m_config->m_grps)
                {
                    for (int j = 0; j < m_config->m_grp_num; j++)
                    {
                        const char *grps = json_array_get_string(grp_arr, j);
                        m_config->m_grps[j] = strdup (grps);
                    }
                }
                else
                    m_config->m_grp_num = 0;
            }
        }
        m_config->m_dl = get_json_number_safe(device_obj, "dl");
    }

    JSON_Array *links_arr = json_object_get_array(device_obj, "links");
    if (NULL != links_arr)
    {
        for (int i=0; i < (int)json_array_get_count(links_arr); i++)
        {
            JSON_Value *link_val = json_array_get_value(links_arr, i);
            JSON_Object *link_obj = json_value_get_object(link_val);
            if (NULL == link_obj)
            {
                WARNING("link not object in the config file. dev=%s",m_epname.c_str());
                continue;
            }

            const char *href = json_object_get_string(link_obj, "href");
            if (NULL == href)
            {
                WARNING("No href in the config file. dev=%s",m_epname.c_str());
                continue;
            }

            CResource *res = FindRes((char *)href);
            if (res == NULL || res->GetType() != T_ResourceObject) continue;

            CResObject *res_obj = (CResObject *)res;
            if (res_obj->m_res_object_config == NULL)
            {
                res_obj->m_res_object_config = new CResourceObjectConfig();
                res_obj->m_res_object_config->LoadObjFromJson(link_val);
            }

            WARNING("loading config for di=%s, %s", m_epname.c_str(), href);

            // handle the configuration for individual properties
            JSON_Array *props_arr = json_object_get_array(link_obj, "props");
            if (NULL != props_arr)
            {
                for (int j = 0; j < (int)json_array_get_count(props_arr); j++)
                {
                    char buffer[200];
                    JSON_Value *prop_val = json_array_get_value(props_arr, j);
                    JSON_Object *prop_obj = json_value_get_object(prop_val);
                    const char *prop_name = json_object_get_string(prop_obj, "n");

                    if(prop_name == NULL)
                    {
                        WARNING("load config for [%s], %s, no property key", m_epname.c_str(), href);
                        continue;
                    }

                    if (res_obj->GetPropertyConfig((char *)prop_name) != NULL)
                        continue;

                    CResourceConfig *config = new CResourceConfig();
                    config->LoadFromJson(prop_val);

                    WARNING("\tproperty=%s, config:\n\t%s", prop_name, config->Log(buffer, sizeof(buffer)));

                    res_obj->AddPropertyConfig((char *)prop_name, config);

                    // enable the observation. the refresher support overwrite previous
                    // obs on the same resource by the same source
                    if (config->m_obs_config)
                    {
                        fresher_para_t param;
                        memset(&param, 0, sizeof(param));
                        param.min_duration = config->m_obs_min_interval;
                        param.publish_point = (char *) URI_IBROKER_DP;

                        // ensure the ibroker is last in the processing flow
                        param.sequence = -1;
                        char resource[512];
                        sprintf (resource, "%s/%s", res_obj->get_url().c_str(), (char *)prop_name);
                        ResRefresher().NewRefresherPoint((char *)m_epname.c_str(), (char *)resource, &param);
                    }
                }
            }
        }
    }
    return 0;
}


void CClient::CleanupConfig()
{
    if (m_config)
    {
        if (m_config->m_cfg)
            free (m_config->m_cfg);
        if (m_config->m_grp_num != 0)
        {
            for (int i = 0; i < m_config->m_grp_num; i++)
                free (m_config->m_grps[i]);
            free (m_config->m_grps);
        }

        delete m_config;
        m_config = NULL;
    }
    std::list<CResource*>::iterator it;
    int cnt = 0;

    for (it=m_res_list.begin(); it!= m_res_list.end(); it++ )
    {
        CResource *res = (CResource*)(*it);
        if (res->m_config)
        {
            delete res->m_config;
            res->m_config = NULL;
        }

        if (res->GetType() == T_ResourceObject)
        {
            CResObject* resObj = (CResObject*) res;
            resObj->ClearPropertyConfig();
            if(resObj->m_res_object_config)
            {
                resObj->m_res_object_config->Cleanup();
                delete resObj->m_res_object_config;
                resObj->m_res_object_config = NULL;
            }
        }

        cnt ++;
    }
}


// register: true - add ams; false - delete ams
void CClient::RegisterAMS(bool register)
{
    ams_add((char*)TT_DEVICE_ON_GW, (char *)m_epname.c_str(),false);

    if (m_config)
    {
        if (m_config->m_grp_num != 0)
        {
            for (int i = 0; i < m_config->m_grp_num; i++)
                ams_add((char*)TT_DEVICE_GROUP, (char *)m_config->m_grps[i],false);
        }
    }
    std::list<CResource*>::iterator it;
    int cnt = 0;

    for (it=m_res_list.begin(); it!= m_res_list.end(); it++ )
    {
        CResource *res = (CResource*)(*it);
        if (res->GetType() == T_ResourceObject)
        {
            CResObject* resObj = (CResObject*) res;
            if(resObj->m_res_object_config)
            {
                for (int i = 0; i < resObj->m_res_object_config->m_grp_num; i++)
                    ams_add((char*)TT_DEVICE_GROUP, (char *)resObj->m_res_object_config->m_grps[i],false);
            }
        }
        cnt ++;
    }
}
