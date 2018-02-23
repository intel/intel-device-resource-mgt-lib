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


#ifndef APPS_IAGENT_CORE_SRC_DEVICES_CRESOBJECT_H_
#define APPS_IAGENT_CORE_SRC_DEVICES_CRESOBJECT_H_

#include "CResource.h"
#include "parson.h"
//#include "rd.h"
#include "CResProperty.h"

class CResourceObjectConfig {
public:
    virtual ~CResourceObjectConfig();
    CResourceObjectConfig();
    unsigned long m_global_id;
    char *m_res_cfg;  //JSON_string of user attributes, it is transparent to iAgent
    bool m_obs_config;

    char **m_grps;                // the groups that the device is in
    int m_grp_num;
    int m_default_value_life;            // default data life in seconds

    void Cleanup();
    void LoadObjFromJson(JSON_Value *value);
};

class CPropertySeperateUrl {
public:
	CPropertySeperateUrl();
	~CPropertySeperateUrl();
	void SetUrls(JSON_Object * urls);
	char * GetUrl(int action);

private:
	char * m_default_url;
	char * m_get_url;
	char * m_put_url;
	char * m_post_url;
};

class CResource;
class CResourceConfig;

class CResObject: public CResource {
public:
    CResObject(char *, char *);
    virtual ~CResObject();

    virtual ResourceType GetType() { return T_ResourceObject;};

    std::string m_rt;
    char *m_object_type;

    std::list<CResProperty*> m_property_list;
    CResourceObjectConfig *m_res_object_config;
    void CleanObjectCnfg();

    CResourceConfig *GetPropertyConfig(char *);
    void AddPropertyConfig(char *, CResourceConfig *);
    void ClearPropertyConfig();
    virtual int CalibrateProperty(const char *property, int fmt, char *payload, int payload_len, char **new_payload);

    void LoadObjectMeta();

    void CleanProperties();
    void AddResProperty(CResProperty *res);
    int Calibrate(int fmt, char *payload, int payload_len, char **new_payload);

    void SetPropertySeparateUrls(JSON_Value * urls);
    char * GetPropertySeparateUrl(char * property_name, int action);
    void RestPropertySeparateUrls();
    int GetSeparateUrlProperties();

private:
    std::map <std::string, void *> m_map_properties_config;

    std::map <std::string, void *> m_map_property_url;
};

#endif /*APPS_IAGENT_CORE_SRC_DEVICES_CRESOBJECT_H_ */
