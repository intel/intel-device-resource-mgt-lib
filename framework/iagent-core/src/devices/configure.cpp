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
 * configure.cpp
 *
 *  Created on: Dec 6, 2016
 *      Author: xwang98
 *
 *
 The configure format:
[
    {
    "di": "0685B960-736F-46F7-BEC0-9E6CBD61ADC1",
    "rn":  34                        // reversion number of configuration, optional
    "attr" : {Json value},          // optional
    "grp": [123,124],                // the groups that the device is in, optional
    "cfg": {},                      // device level config that iagent used, optional
    "dl": 30,                        // default data life in seconds, optional
    "links": [
        {
        "href": "/switch/1",
        "attr" : {Json value},        // user configuration, optional
        "cfg": {"obs": true | false}, // configs used by iAgent, optional
        "grp": [223, 224],            // in what groups, optional
        "dl" 20,                     // data life in seconds, optional
        "props": [
            {"n":"prop name","o": 0|1, "omin": 30, "ci": -12: "cd": 2.001,"th":100,"tl":0 },
        ],
        },
        {
        "href": "/oic/p",
        },
    ]
    }
]

 *
 */

//iagent
#include "CClient.h"
#include "CResRefresher.h"
#include "CClientManager.h"
extern void device_rd_monitor_scan(CClient *client);

void reload_client_config(char *epname)
{
    CClient *client = ClientManager().FindClient((epname));

    if(client == NULL)
        LOG_RETURN ();

    // set a mark flag for all refresher points under this client
    ResRefresher().MarkAllFlagged((char *)client->m_epname.data());

    client->CleanupConfig();

    // the flags marked will be cleaned up if it is configured as observed
    // during load the configuration file
    client->Initialize();

    // remove refresh points that are no longer observed in the configration
    ResRefresher().RemoveAllFlagged((char*)URI_IBROKER_DP);

    device_rd_monitor_scan(client);

}
