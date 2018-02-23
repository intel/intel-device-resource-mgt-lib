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


#ifndef CCLIENT_H_
#define CCLIENT_H_

#include <string>
#include <list>
#include "rd.h"
#include "parson.h"
#include "CResource.h"
#include "CResObject.h"

class CResource;


enum
    {
        Is_None,
        Is_string,
        Is_json
    };
/**
 *
 */

class CClientConfig {
public:
    int m_rn;                        // reversion number of configuration
    char *m_cfg;    // JSON_Value
    unsigned long m_id;                    // global identity of device
    char **m_grps;                // the groups that the device is in
    int m_grp_num;
    int m_dl;
};



inline bool checkjson(JSON_Object *value_obj, char *name, JSON_Value **value, JSON_Value_Type type)
{
    *value = json_object_get_value(value_obj, name);
    if (json_value_get_type(*value) != type)
    {
        ERROR("%s is not string type", name);
        return false;
    }
    return true;
}


class CClient {
public:
    CClient(char *epname);
    virtual ~CClient();

public:
    CResource *FindRes(const char *);
    JSON_Value *GenerateRDReport(bool with_cfg);

    static int new_id;

    void IncreaseRef() { m_ref_num ++; };
    void DecreasRef() { m_ref_num --; };
    int ReferrenceCnt() { return m_ref_num; };

    bool IsCalibrate(void){return m_is_calibrate;};

    // Remove the resources from ResRefresher.
    void AddRes(CResource *);
    void RemoveRes(CResource *);
    void FreeAllRes();
    std::string::size_type GetResNum() { return m_res_list.size(); };
    CResource *UrlMatchResource( char *url, std::string * property_name = NULL);
    bool QueryRD(JSON_Value *);

    bool LoadConfig(JSON_Value **device_cfg);
    bool CheckConfigSanity(JSON_Value *device_cfg);
    bool ApplyDefaultConfig();
    bool Initialize();
    bool ApplyClientConfig(JSON_Value *device_cfg);
    void CleanupConfig();
    void RegisterAMS(bool register);


    CResource *HandleRdPost(char *uri, char *type, int instance = -1);
    void RenewResources(std::list<CResource*> & resources);
    connection_t *GetConnection() { return m_connection;};
    void SetAddress(char *);
    void SetConnection(connection_t *);

    void SetUserData(int type, void * data);

    const char * GetOutName();
    bool  HasId(const char *);

public:
    std::string m_device_type;

    std::string m_epname;

    // alias is unique in a gateway, but not for a system
    std::string m_alias_name;

    // refer to doc "resource_directory.txt" for the usage of standard
    std::string m_standard;
    std::string m_settings;
    int m_ttl;
    bool m_online;
    bool m_is_queue_mode;
    bool m_is_passive_device;
    time_t m_last_active_time; // the last time that we receive the message from the lwm2m client.
    int m_queued_req_num;
    CClientConfig *m_config;

    int m_id;

    bool m_has_separate_url;
    connection_t *m_connection;

private:

    std::string m_addr;
    std::list<CResource*> m_res_list;

    bool m_is_calibrate;

    int m_user_data_type;
    void * m_user_data;


    int m_ref_num;
};


#endif /* CCLIENT_H_ */
