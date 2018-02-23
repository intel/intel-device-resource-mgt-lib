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
 * calibration.c
 *
 *  Created on: Apr 22, 2015
 *      Author: wangxin
 */
/*

LWM2M Data SPECIFICATION:

Field                 JSON Variable     Mandatory?                 Description
Resource Array         e                 Yes The Resource list as JSON value array per [SENML].
Name                 n                 Yes The Name of the resource is the path of the Resource relative to
                                                            the request URI.
                                                            Example: If the request URI is /{Object}/{Object Instance}, the
                                                            Resource name will be {Resource}/{Resource Instance}
Time                 t                 No     The time of the representation relative to the Base Current Time
                                                            in seconds (a negative integer) for a notification. Required only
                                                            for historical representations.
Base Time            bt                 No                         The base current time which the Time values are relative to as a
                                                            Time data type (See Appendix B)
Float Value         v                 One value field is mandatory     Value as a JSON float if the Resource data type is integer or float.
Boolean Value         bv                     Value as a JSON Boolean if the Resource data type is boolean.
String Value         sv                     Value as a JSON string for all other Resource data types. If the
                                        Resource data type is opaque the string value holds the Base64
                                        encoded representation of the Resource.


For example a request to Device Object of the LWM2M example client (Get /3//) would return the following JSON payload.
This example has a size of 397 bytes.
/3
{“e”:[
{"n":"0","sv":"Open Mobile Alliance"},
{"n":"1","sv":"Lightweight M2M Client"},
{"n":"2","sv":"345000123"},
{"n":"3","sv":"1.0"},
{"n":"6/0","v":"1"},
{"n":"6/1","v":"5"},
{"n":"7/0","v":"3800"},
{"n":"7/1","v":"5000"},
{"n":"8/0","v":"125"},
{"n":"8/1","v":"900"},
{"n":"9","v":"100"},
{"n":"10","v":"15"},
{"n":"11/0","v":"0"},
{"n":"13","v":"1367491215"},
{"n":"14","sv":"+02:00"},
{"n":"15","sv":"U"}]
}

For example a notification about a Resource containing multiple historical representations of a Temperature Resource in the
example could result in the following JSON payload:
{“e”:[
{"n":"1/2","v":"22.4","t":"-5"},
{"n":"1/2","v":"22.9","t":"-30"},
{"n":"1/2","v":"24.1","t":"-50"}],
"bt":"25462634"
}

OIC 1.1 SPECIFICATION:

{
"rt": ["oic.r.temperature"],
"if": ["oic.if.a","oic.if.baseline"],
"temperature": 20,
"units": "C",
"range": [0,100]
}

/the/light/1
{
"rt": ["acme.light"],
"if": ["oic.if.a", "oic.if.baseline"],
"state": 0,
"colortemp": "2700K"
}

Request: GET /a/act/heater?if="oic.if.a"
Response:
{
"prm": {"sensitivity": 5, "units": "C", "range": "0 .. 10"},
"settemp": 10,
"currenttemp" : 7
}
NOTE: “prm” is the Property name for ‘parameters’ Property
 */

//#include "limits.h"
//#include "float.h"
#include "rd.h"

#include "agent_core_lib.h"
#include "CClient.h"
#include "CClientManager.h"
#include "CResObject.h"

static int g_supported_fmt[] =
{
    LWM2M_CONTENT_TEXT,
    LWM2M_CONTENT_JSON
};


bool fmt_calibrate_supported(int fmt)
{
    for(int i=0; i<(int)COUNT_OF(g_supported_fmt); i++)
    {
        if(g_supported_fmt[i] == fmt)
            return true;
    }

    return false;
}


int calibrate(IURL_BODY url_body, int fmt, char * payload, int payload_len, char ** new_payload)
{
    iUrl_t iurl = {0};
    int ret = 0;
    char *res_property;
    CResource *resource;
    CClient *client;

    if(!fmt_calibrate_supported(fmt))
        LOG_RETURN(0)

    // get from the target
    if(!parse_iUrl_body(url_body, &iurl))
        LOG_RETURN(0)

    // only calibrate what we can understand
    if(check_uri_standard(iurl.standard) == -1)
        LOG_GOTO("Unknow standard", end)

    //
    client = ClientManager().FindClient(iurl.device);
    if (!client)
        LOG_GOTO("Can't find client", end)

    resource = client->UrlMatchResource(iurl.res_uri);
    if (!resource)
        LOG_GOTO("Can't find resource", end)

    res_property = url_body + resource->get_url().length();
    if (*res_property == '/') res_property++;

    if (resource->GetType() == T_ResourceBase && resource->m_config)
        ret = resource->m_config->Calibrate(fmt, payload, payload_len, new_payload);
    else if (resource->GetType() == T_ResourceObject)
        ret =  ((CResObject*)resource)->CalibrateProperty(res_property,
                fmt, payload, payload_len, new_payload );

end:
    free_iUrl_body(&iurl);
    return ret;
}
