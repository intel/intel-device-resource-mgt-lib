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


#include "CResObject.h"
#include "er-coap-constants.h"
#include "CClient.h"

CResObject::CResObject(char *url, char *type)
:CResource(url),
 m_rt(type),
 m_res_object_config(NULL)
{

}


/// note: the destructor of base class is auto called.
CResObject::~CResObject()
{
    CleanProperties();
    ClearPropertyConfig();

    if(m_res_object_config)
        delete m_res_object_config;

    RestPropertySeparateUrls();
}


void CResObject::CleanProperties()
{
    // todo:
}


void CResObject::AddResProperty(CResProperty *res)
{
    this->m_property_list.push_front(res);
    res->m_parent_object = this;
    res->m_parent_device = this->m_parent_device;
}


void CResObject::LoadObjectMeta()
{

}


void CResObject::CleanObjectCnfg()
{
    if(m_res_object_config)
        m_res_object_config->Cleanup();
}


CResourceConfig *CResObject::GetPropertyConfig(char *property_name)
{
    std::map<std::string,void *>::iterator it;

    it = m_map_properties_config.find(property_name);
    if (it == m_map_properties_config.end())
    {
        TraceV(FLAG_CLOUD_CONFIG, "property <%s> didn't have configuration", property_name);
        return NULL;
    }

    CResourceConfig *res = (CResourceConfig*) m_map_properties_config[property_name];
    return res;
}


void CResObject::AddPropertyConfig(char *url, CResourceConfig *config)
{
    m_map_properties_config[url] = config;
}


void CResObject::ClearPropertyConfig()
{
    std::map<std::string,void *>::iterator it;

    for (it = m_map_properties_config.begin(); it != m_map_properties_config.end(); it++)
    {
        CResourceConfig *config = (CResourceConfig *) it->second;
        delete config;
    }

    m_map_properties_config.clear();
}


// do the whole object calibrating
int CResObject::Calibrate(
        int fmt, char *payload,
        int payload_len, char **new_payload)
{
#if TODO
    if(fmt == LWM2M_CONTENT_JSON)
    {

        Json::Reader reader;
        Json::Value root;
        std::string content((char*)old_value, old_value_len);
        bool is_calibrated = false;

        bool parsingSuccessful = reader.parse(content, root );
        if ( !parsingSuccessful )
        {
            // report to the user the failure and their locations in the document.
            WARNING("Cannot parse the data payload from client");
            return false;
        }

        if(!root["e"].isArray())
            LOG_RETURN (false);

        lwm2m_uri_t uri_root = {0};
        if(root["bn"].isString())
        {
            lwm2m_stringToUri(root["bn"].asCString(), root["bn"].asString().length(), & uri_root);
        }

        for(int i=0;i<root["e"].size();i++)
        {
            Json::Value & res_item = root["e"][i];


            // if the payload has base name, use base name from payload
            // otherwise try to use the uri from head as base name

            lwm2m_uri_t uri = {0};

            if((uri_root.flag & LWM2M_URI_FLAG_OBJECT_ID) != 0)
                uri = uri_root;
            else if((uriP->flag & LWM2M_URI_FLAG_OBJECT_ID) != 0)
                uri = *uriP;


            // if uri base is not completed, need for "n" in each item
            if((uri.flag & LWM2M_URI_FLAG_RESOURCE_ID) == 0)
            {
                if(    res_item["n"].isNull())
                    continue;

                const char *str_id = res_item["n"].asCString();
                int head = 0;
                int len = res_item["n"].asString().size();
                uint32_t num = parse_devided_number(str_id, len, &head, '/');
                if(num == -1)
                    continue;

                // three more '/' should be present if obj id not in the uri
                if(!(uri.flag & LWM2M_URI_FLAG_OBJECT_ID ))
                {
                    uri.objectId = num;
                    uri.flag |= LWM2M_URI_FLAG_OBJECT_ID;

                    num = parse_devided_number(str_id, len, &head, '/');
                    if(num == -1)
                        continue;
                }

                // expecting two more '/' if instance id is not available from uri
                if(!(uri.flag & LWM2M_URI_FLAG_INSTANCE_ID ))
                {

                    uri.instanceId = num;
                    uri.flag |= LWM2M_URI_FLAG_INSTANCE_ID;

                    num = parse_devided_number(str_id, len,    &head, '/');
                    if(num == -1)
                        continue;
                }

                if(!(uri.flag & LWM2M_URI_FLAG_RESOURCE_ID ))
                {

                    uri.resourceId = num;
                    uri.flag |= LWM2M_URI_FLAG_RESOURCE_ID;

                    //num = parse_devided_number(str_id, len,    &head, '/');
                }
            }

            if((uri.flag & LWM2M_URI_FLAG_RESOURCE_ID) == 0)
                continue;

            CResource *res = client->FindRes(&uri);
            if ( res && res->IsCalibrate())
            {
                if(res_item["v"].isInt())
                {
                    res_item["v"] = (res_item["v"].asInt() *res->m_mul_times.i_times + res->m_add_offset.i_offset);
                    is_calibrated = true;
                }
                else if(res_item["v"].isDouble())
                {
                    res_item["v"] = (res_item["v"].asFloat() *res->m_mul_times.f_times + res->m_add_offset.f_offset);
                    is_calibrated = true;
                }
            }
        }

        if (!is_calibrated)
            LOG_RETURN (false);

        Json::FastWriter writer;
        writer.omitEndingLineFeed();
        new_value = writer.write(root);
        return true;
    }
#endif
    return 0;
}


//property: if it is blank or NULL, the payload is for the whole res object.
int CResObject::CalibrateProperty(const char *property, int fmt, char *payload, int payload_len, char **new_payload)
{
    // if it is not a property, then do the whole object calibrating
    if(property == NULL || *property == 0)
    {
        return Calibrate(fmt, payload, payload_len, new_payload);
    }

    CResourceConfig *config = GetPropertyConfig((char *)property);
    if(config == NULL)
    {
        TraceD(FLAG_DUMP_MESSAGE,"current property<%s>'s configuration was NULL", property);
        return 0;
    }

    return config->Calibrate(fmt, payload, payload_len, new_payload);
}


void CResObject::SetPropertySeparateUrls(JSON_Value *urls)
{

    int num = 0;
    RestPropertySeparateUrls();

    JSON_Array  *property_urls =  json_array (urls);
    if (property_urls == NULL)
        LOG_RETURN();

    num = (int)json_array_get_count(property_urls);
    for (int i = 0; i < num; i++)
    {
        JSON_Object *item = json_array_get_object (property_urls, i);
        if (item == NULL) continue;

        char *name = (char*) json_object_get_string(item, (const char *)"name");
        if(name == NULL) continue;

        CPropertySeperateUrl *seperate_url = new CPropertySeperateUrl();
        seperate_url->SetUrls(item);

        m_map_property_url[name] = seperate_url;
    }

    if (num && m_parent_device)
        m_parent_device->m_has_separate_url = true;
}


char *CResObject::GetPropertySeparateUrl(char *property_name, int action)
{
    std::map<std::string, void *>::iterator it;

    it = m_map_property_url.find(property_name);
    if (it != m_map_property_url.end())
    {
        CPropertySeperateUrl *property_url = (CPropertySeperateUrl *) it->second;
        return property_url->GetUrl(action);
    }

    return NULL;
}


void CResObject::RestPropertySeparateUrls()
{
    // todo: need to check the parent device no longer has any sepearate url
    //if(m_parent_device)
    //    m_parent_device->m_has_separate_url = false;


    std::map<std::string,void *>::iterator it;

    for (it = m_map_property_url.begin(); it != m_map_property_url.end(); it++)
    {
        CPropertySeperateUrl *url = (CPropertySeperateUrl *) it->second;
        delete url;
    }

    m_map_property_url.clear();
}

int CResObject::GetSeparateUrlProperties()
{
    return m_map_property_url.size();
}


/*
 *           Class CResourceObjectConfig
 */


CResourceObjectConfig::CResourceObjectConfig()
{
    m_grp_num = 0;
    m_res_cfg = NULL;
    m_grps = NULL;
    m_obs_config = false;
    m_default_value_life = 30;
}

CResourceObjectConfig::~CResourceObjectConfig()
{
    Cleanup();
}

void CResourceObjectConfig::Cleanup()
{
    if (m_res_cfg)
        json_free_serialized_string (m_res_cfg);
    m_res_cfg = NULL;

    if (m_grp_num != 0)
    {
        for (int i = 0; i < m_grp_num; i++)
            free (m_grps[i]);
        free (m_grps);

        m_grp_num = 0;
        m_grps = NULL;
    }
}

void CResourceObjectConfig::LoadObjFromJson(JSON_Value *value)
{
    JSON_Object *res_obj = json_value_get_object(value);

    m_global_id = get_json_number_safe(res_obj, "ins");
    JSON_Value *attr = json_object_get_value(res_obj, "attr");
    if (attr)
        m_res_cfg = json_serialize_to_string(attr);

    JSON_Value *j_cfg = json_object_get_value(res_obj, "cfg");
    if (j_cfg)
    {
        JSON_Value *j_obs = json_object_get_value(json_object(j_cfg), "obs");
        if(json_value_get_type(j_obs) == JSONBoolean)
            m_obs_config = json_boolean(j_obs);
        else if (json_value_get_type(j_obs) == JSONString)
        {
            const char * sz_obs = json_string(j_obs);
            if(sz_obs && strcmp(sz_obs, "true") == 0)
                m_obs_config = true;
            else
                m_obs_config = false;
        }
        else
        {
            WARNING("Invalid obs type %d in JSON", json_value_get_type(j_obs));
        }
    }

    JSON_Array *grp_arr = json_object_get_array(res_obj, "grp");
    if (NULL != grp_arr)
    {
        m_grp_num = (int)json_array_get_count(grp_arr);
        if (0 != m_grp_num)
        {
            m_grps = (char **)malloc (sizeof(char **) *m_grp_num);
            if (NULL != m_grps)
            {
                for (int i = 0; i < m_grp_num; i++)
                {
                    const char *grps = json_array_get_string(grp_arr, i);
                    m_grps[i] = strdup (grps);
                }
            }
            else
                m_grp_num = 0;
        }
    }
    m_default_value_life = get_json_number_safe(res_obj, "dl");
}


/*
 *
 *       Class CPropertySeperateUrl
 *
 */
CPropertySeperateUrl::CPropertySeperateUrl()
:m_default_url(NULL),
 m_get_url(NULL),
 m_put_url(NULL),
 m_post_url(NULL)
{

}


CPropertySeperateUrl::~CPropertySeperateUrl()
{
    if (m_default_url) free (m_default_url);
    if (m_get_url) free (m_get_url);
    if (m_put_url) free (m_put_url);
    if (m_post_url) free (m_post_url);
}

void CPropertySeperateUrl::SetUrls(JSON_Object *urls)
{
    if (urls == NULL) LOG_RETURN();

    char *url = (char *)json_object_get_string(urls, "default");
    if (url) m_default_url = strdup(url);

    url = (char *)json_object_get_string(urls, "get");
    if (url) m_get_url = strdup(url);

    url = (char *)json_object_get_string(urls, "put");
    if (url) m_put_url = strdup(url);

    url = (char *)json_object_get_string(urls, "post");
    if (url) m_post_url = strdup(url);
}


char *CPropertySeperateUrl::GetUrl(int action)
{
    char *url = m_default_url;
    if (COAP_GET == action && m_get_url)
        url = m_get_url;
    else if (COAP_PUT == action && m_put_url)
        url = m_put_url;
    else if (COAP_POST == action && m_post_url)
        url = m_post_url;

    return url;
}
