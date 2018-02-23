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
 * plugin_constants.h
 *
 *  Created on: Apr 21, 2017
 *      Author: xin
 */

#ifndef APPS_GW_BROKER_PLUGIN_SDK_INCLUDE_PLUGIN_CONSTANTS_H_
#define APPS_GW_BROKER_PLUGIN_SDK_INCLUDE_PLUGIN_CONSTANTS_H_

#ifdef __cplusplus
extern "C"
{
#endif

typedef enum
{
    IA_TEXT_PLAIN = 0,
    IA_LWM2M_CONTENT_TEXT = 0,
    IA_LWM2M_CONTENT_LINK = 40,
    IA_LWM2M_CONTENT_OPAQUE = 42,
    IA_LWM2M_CONTENT_TLV = 89, //1542,
    IA_LWM2M_CONTENT_JSON = 88, //1543,
    IA_APPLICATION_JSON = 50,
    IA_APPLICATION_CBOR = 60,
    IA_OCF_CBOR = 60
}application_media_type_t;


#define XK_TAG "_tag"

#define XK_MID "_id"
#define XK_URI "_uri"
#define XK_QUERY "_qry"
#define XK_PAYLOAD "_pa"
#define XK_ACTION "_ac"
#define XK_FMT "_fmt"
#define XK_TAG "_tag"
#define XK_DEST "_dest"
#define XK_SRC "_src"
#define XK_OBS "_obs"
#define XK_PUBLISH "_pub"
#define XK_RESP_CODE "_status"
#define XK_TM   "_tm"


/// XK_TAG
#define TAG_REST_REQ "req"
#define TAG_REST_RESP "resp"
#define TAG_EVENT "evt"


/// XK_ACTION
/*
 * agent_core_lib.h
#define ACTION_GET "GET"
#define ACTION_PUT "PUT"
#define ACTION_POST "POST"
#define ACTION_DEL "DEL"
#define ACTION_CREATE "CREATE"
*/

/// XK_DEST, XK_SRC
#define MODULE_AGENT "agent"
#define MODULE_MODBUS "modbus"
#define MODULE_LWM2M "lwm2m"
#define MODULE_DATABASE "database"



#define ACTION_GET "GET"
#define ACTION_PUT "PUT"
#define ACTION_POST "POST"
#define ACTION_DEL "DEL"
#define ACTION_CREATE "CREATE"



#ifdef __cplusplus
}
#endif

#endif /* APPS_GW_BROKER_PLUGIN_SDK_INCLUDE_PLUGIN_CONSTANTS_H_ */
