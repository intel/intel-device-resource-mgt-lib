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
 * CResource.cpp
 *
 *  Created on: Aug 6, 2015
 *      Author: xwang98
 */

#include <assert.h>
#include <stdlib.h>
#include "CResource.h"
#include "CClient.h"
#include "rd.h"

int g_value_lifetime= 30; //seconds


CResource::CResource(char *url):
m_parent_device(NULL),
m_config(NULL),
m_url(url)
{
}

CResource::~CResource() {
    // TODO Auto-generated destructor stub
    if (m_parent_device)
         ResRefresher().DelRefresherPoint((char *)m_parent_device->m_epname.data(),
                (char *)m_url.data(), (char*)URI_IBROKER_DP);

    if (m_config)delete m_config;
}


CResource *CResource::NewResource(ResourceType type, char *url)
{
    if (type == T_ResourceBase)
        return new CResource(url);
    else if (type == T_ResourceObject)
        return new CResObject(url, NULL);
    else if (type == T_ResourceProperty)
        return new CResProperty(url);
    else
        LOG_RETURN (NULL)
}


void CResource::SetConfig(CResourceConfig *config)
{
    if(m_config) delete m_config;

    m_config = config;
}


/**********************************************************************
 *
 *                     Class CResourceConfig
 *
 *********************************************************************/
CResourceConfig::CResourceConfig():
        m_operation_support(""),
        m_data_type(0),
        m_name(""),
        m_obs_config(false),
        m_obs_min_interval(0),
        m_cali_offset(NULL),
        m_mul_times(NULL),
        m_threshold_high(NULL),
        m_threshold_low(NULL)
{

}


CResourceConfig::~CResourceConfig()
{
    if(m_cali_offset) delete m_cali_offset;
    if(m_mul_times) delete m_mul_times;
    //if(m_default_value) delete m_default_value;
    if(m_threshold_high) delete m_threshold_high;
    if(m_threshold_low) delete m_threshold_low;
}


int CResourceConfig::Calibrate(int fmt, char *payload, int payload_len, char **new_payload)
{
    VALUE_T type = m_cali_offset->GetType();

    if(fmt == LWM2M_CONTENT_TEXT)
    {
        if (T_Int == type)
        {
            int value = atoi(payload);
            value += m_cali_offset->GetInt();
            snprintf (*new_payload, sizeof(int), "%d", value);
        }
        else if (T_Float == type)
        {
            double value = atof(payload);
            value += m_cali_offset->GetFloat();
            snprintf (*new_payload, sizeof(double), "%f", value);
        }
    }
    else if(fmt == LWM2M_CONTENT_JSON)
    {
        JSON_Value *value_val = json_parse_string((const char*)payload);
        if (!value_val) LOG_RETURN(0)
        JSON_Object *value_obj = json_object(value_val);
        if (!value_obj) LOG_RETURN(0)
        JSON_Value *new_value_val = json_value_init_object();
        if (!new_value_val) LOG_RETURN(0)
        JSON_Object *new_value_obj = json_object(new_value_val);

        if (T_Int == type)
        {
            for (int i = 0; i < (int)json_object_get_count(value_obj); i++)
            {
                char new_value[8];
                const char *name = json_object_get_name(value_obj, i);
                int value = atoi (json_object_dotget_string(value_obj, name));

                value += m_cali_offset->GetInt();
                snprintf (new_value, 8, "%d", value);
                json_object_set_string(new_value_obj, name, new_value);
            }
        }
        else if (T_Float == type)
        {
            for (int i = 0; i < (int)json_object_get_count(value_obj); i++)
            {
                char new_value[8];
                const char *name = json_object_get_name(value_obj, i);
                double value = atof (json_object_dotget_string(value_obj, name));

                value += m_cali_offset->GetFloat();
                snprintf (new_value, 8, "%f", value);
                json_object_set_string(new_value_obj, name, new_value);
            }
        }
        *new_payload = json_serialize_to_string(new_value_val);
    }
    return 0;
}



/* device.cfg format:
 *
 *  {
            "href": "/10241/0",
            "props": [
                {
                    "cfg": {
                        "th": "100",
                        "ci": "1",
                        "omin": "10",
                        "tl": "1",
                        "o": "true"
                    },
                    "n": "0"
                },
 */

static bool check_digit(char * str)
{
    while(*str == ' ')str++;

    if(*str == '-' || *str == '+')
        str ++;

    return isdigit(str[0]);
}

void setvalue_int(char *c_value, CResValue *res_value)
{
    if (c_value == NULL)
        return;

    if(c_value[0] == 0)
        return;


    int n = atoi(c_value);

    res_value->SetInt(n);
}


char * CResourceConfig::Log(char * buffer, int size)
{
    char buf[100];
    snprintf(buffer, size, "name=[%s], oper=%s, obs=%d, interval=%d,cali=[%s], mul=[%s]",
            m_name.c_str(), m_operation_support.c_str(), m_obs_config, m_obs_min_interval,
            m_cali_offset?m_cali_offset->dump(buf, sizeof(buf)):"nul",
            m_mul_times?m_mul_times->dump(buf, sizeof(buf)):"nul" );

    return buffer;

}

void CResourceConfig::LoadFromJson(JSON_Value *value)
{

    JSON_Object * obj = json_value_get_object(value);
    const char *name = json_object_get_string(obj, "n");
    if (name)
        m_name =  name;
    else
    {
        ERROR("Resource configruation miss propery name!");
    }

    JSON_Value *cfg = json_object_get_value(obj, "cfg");

    // note: the ibroker 2.0 since 2017/12 has changed the device.cfg format
    //       we do some trick here to support two device.cfg versions
    if(cfg == NULL)
        cfg = value;

    JSON_Object *res_obj = json_value_get_object(cfg);
    if(res_obj == NULL)
    {
        return;
    }

    const char *support = json_object_get_string(res_obj, "p");
    if (support)
        m_operation_support = support;
    else
        m_operation_support =  "r";


    JSON_Value *j_obs = json_object_get_value(res_obj, "o");
    if(json_value_get_type(j_obs) == JSONBoolean)
        m_obs_config = json_boolean(j_obs);
    else if (json_value_get_type(j_obs) == JSONString)
    {
        const char * sz_obs = json_string(j_obs);
        if(sz_obs && strcmp(sz_obs, "true") == 0)
            m_obs_config = true;
    }

    JSON_Value *j_obs_min = json_object_get_value(res_obj, "omin");
    if(json_value_get_type(j_obs_min) == JSONNumber)
        m_obs_min_interval = json_number(j_obs_min);
    else if (json_value_get_type(j_obs_min) == JSONString)
    {
        const char * omin = json_string(j_obs_min);
        if(omin && check_digit((char *)omin))
            m_obs_min_interval = atoi(omin);
    }

    if (true == m_obs_config && -1 == m_obs_min_interval)
        m_obs_min_interval = 30;

    m_data_type = get_json_number_safe(res_obj, "t");

    char *res_value;
    if ((res_value = (char *)json_object_get_string(res_obj, "ci")) != NULL)
    {
        if(check_digit(res_value))
        {
            m_cali_offset = new CResValue();
            setvalue_int(res_value, m_cali_offset);
        }
    }

    else if ((res_value = (char *)json_object_get_string(res_obj, "cd")) != NULL)
    {
        if(check_digit(res_value))
        {
            m_cali_offset->SetFloat(atof(res_value));
        }
    }


    res_value = (char *)json_object_get_string(res_obj, "mul");
    if(res_value && check_digit(res_value))
    {
        m_mul_times = new CResValue();
        m_mul_times->SetFloat(atof(res_value));
    }

    res_value = (char *)json_object_get_string(res_obj, "th");
    if(res_value && check_digit(res_value))
    {
        m_threshold_high = new CResValue();
        setvalue_int(res_value, m_threshold_high);
    }

    res_value = (char *)json_object_get_string(res_obj, "tl");
    if(res_value && check_digit(res_value))
    {
        m_threshold_low = new CResValue();
        setvalue_int(res_value, m_threshold_low);
    }
}
