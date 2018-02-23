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


#ifndef CRESOURCE_H_
#define CRESOURCE_H_
#include <string>
#include <list>
#include <stdlib.h>

//#include "CClient.h"
#include "CResValue.h"
#include "CResRefresher.h"
#include "parson.h"
//#include "CClient.h"

class CClient;
class CResRefresher;
class CRefresherPoint;

typedef enum
{
    T_ResourceBase = 0,
    T_ResourceObject,
    T_ResourceProperty
} ResourceType;

class CResourceConfig
{
public:
	CResourceConfig();
	virtual ~CResourceConfig();
    // Following two items should be from meta data.
    // Just put it in config now.
    std::string m_operation_support;
    int m_data_type;

    // following items are from the config file for the client
    std::string m_name;
    bool m_obs_config;
    int  m_obs_min_interval;
    CResValue *m_cali_offset;
    CResValue *m_mul_times;
    //CResValue *m_default_value;
    CResValue *m_threshold_high;
    CResValue *m_threshold_low;

    int Calibrate(int fmt, char *payload, int payload_len, char **new_payload);

    void LoadFromJson(JSON_Value *value);
    char * Log(char * buffer, int size);

};


class CResource
{
public:
    CResource(char *);
    virtual ~CResource();

    virtual ResourceType GetType() { return T_ResourceBase;}
    bool IsCalibrate() { return false; }
    std::string &get_url() { return m_url; }

    static CResource *NewResource(ResourceType type, char *url);
    void SetConfig(CResourceConfig *config);

    CClient *m_parent_device;
    CResourceConfig *m_config;

private:
    std::string m_url;
};



#endif /* CRESOURCE_H_ */
