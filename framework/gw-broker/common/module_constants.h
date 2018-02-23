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


#ifndef APPS_GW_BROKER_COMMON_MODULE_CONSTANTS_H_
#define APPS_GW_BROKER_COMMON_MODULE_CONSTANTS_H_


#define MAX_PATH_LEN 512

typedef enum
{
    iReady_To_Connect = 0,
    iError_In_Connection = 1,

    // socket is connected
    iSocket_Connected = 98,
    iCloud_Provisioning ,
    iCloud_Handshaking,
    iReady_For_Work
} cloud_status_e;


enum
{
	Log0_Common = 0,
	Log1_IAgent = 1,
	Log2_Modbus = 2,
	Log3_DB = 3,
	Log4_LWM2M = 4,
	Log5_Max_Num
};


#endif /* APPS_GW_BROKER_COMMON_MODULE_CONSTANTS_H_ */
