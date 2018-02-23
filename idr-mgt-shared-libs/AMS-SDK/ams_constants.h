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
 * ams_constants.h
 *
 *  Created on: Feb 3, 2017
 *      Author: xwang98
 */

#ifndef EXTERNAL_IMRT_SHARED_LIBS_AMS_SDK_AMS_CONSTANTS_H_
#define EXTERNAL_IMRT_SHARED_LIBS_AMS_SDK_AMS_CONSTANTS_H_


///
/// definition for the software products
///
#define SW_IAGENT 	"iagent"
#define SW_IMRT 	"imrt"
#define SW_AMS		"ams"

///
/// definition for the target types
///
//#define TT_GLOBAL 			"global"		// target id: blank for any device
#define TT_DEVICE			"device"		// target id: the id assigned by the software for the device, such as iagent id
#define TT_DEVICE_ON_GW		"device_on_gateway"		// target id: the id of subordinate devices that are connected or managed by this software
#define TT_DEVICE_GROUP		"group"			// target id: the group id
#define TT_DEVICE_TYPE		"device_type"		// target id: the name of device type
#define TT_GROUP_TYPE		"group_type"	// target id: the name of group type
#define TT_RESOURCE_TYPE   	"resource_meta"		// target id: the name of resource type, such as "oic.light"
#define TT_MODBUS_TYPE   	"modbus_type"	// target id: the name of resource type, such as "oic.light"


#endif /* EXTERNAL_IMRT_SHARED_LIBS_AMS_SDK_AMS_CONSTANTS_H_ */
