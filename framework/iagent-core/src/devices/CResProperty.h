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


#ifndef APPS_IAGENT_CORE_SRC_DEVICES_CRESPROPERTY_H_
#define APPS_IAGENT_CORE_SRC_DEVICES_CRESPROPERTY_H_

#include "CResource.h"
//#include "CResObject.h"

class CResObject;
class CResource;

class CResProperty: public CResource {
public:
    CResProperty(char *);
    virtual ~CResProperty();

    virtual ResourceType GetType() { return T_ResourceProperty;};

    CResValue m_value;

    std::string m_property_name;

    CResValue & GetResValue(){ return m_value;};

    CResObject *m_parent_object;


};

#endif /* APPS_IAGENT_CORE_SRC_DEVICES_CRESPROPERTY_H_ */
