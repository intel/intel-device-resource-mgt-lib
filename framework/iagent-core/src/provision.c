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

#include "iagent_base.h"
#include "ilink_message.h"
#include "ams_sdk_interface.h"
#include "ams_constants.h"
#include "iniparser.h"


bool get_device_key(unsigned char * key, int * len)
{
    return false;
}

static tick_time_t g_last_prov = 0;
static int g_provision_resend_cnt = 0;

void init_provision()
{
    ilink_message_t provision_request;
    ilink_vheader_t vheader;
    vheader_node_t *node;

    memset (&vheader, 0, sizeof(vheader));
    memset (&provision_request, 0, sizeof(provision_request));
    init_ilink_message(&provision_request, INTEL_IAGENT);

    vheader_init(&vheader, 1, 0);
    vheader_set_value_s(&vheader, K_TAG, TAG_KEY_PROVISION);

    ilink_set_vheader(&provision_request, &vheader);

    ilink_msg_send(&provision_request);

    g_provision_resend_cnt ++;
    g_last_prov = bh_get_tick_sec ();

    vheader_destroy(&vheader);
    reset_ilink_message(&provision_request);

    TraceI(FLAG_DUMP_MESSAGE, "Send provision message to ibroker\n");
}

static char * g_my_agent_id = NULL;
void save_iagent_id(char *id)
{
    char *config_root = get_config_path();
    char path[256];

    strcpy (path, config_root);
    strcat (path, "iagent.ini");

    set_ini_key(path, "i-agent:myid", id);
}

char *load_iagent_id()
{
    char *config_root = get_config_path();
    dictionary  *ini;
    char fname[128];
    int val;

    if (g_my_agent_id) free( g_my_agent_id);
    g_my_agent_id = NULL;

    snprintf (fname, 128, "%s%s", config_root, "iagent.ini");
    ini = iniparser_load(fname);
    if (NULL == ini)
    {
        WARNING("Unable to load log config %s\n", fname);
    }
    else
    {
        const char *value = iniparser_getstring(ini, "i-agent:myid", NULL);
        if (value) g_my_agent_id = strdup (value);
        iniparser_freedict(ini);
    }
    return g_my_agent_id;
}

char * get_self_agent_id()
{
#ifdef BUILTIN_IBROKER
    return g_my_agent_id;
#else
    return g_my_agent_id?g_my_agent_id:"iagent";
#endif
}

bool is_my_agent_id(char * id)
{
    char *aid = get_self_agent_id();
    if (aid)
        return (strcmp (get_self_agent_id(), id) == 0);
    else
        return 1;
}


void on_prov_message(ilink_message_t *message)
{
    if (get_cloud_connection_status() != iCloud_Provisioning)
        return;

    char *tag = get_key_value(message, K_TAG);

    if (tag == NULL) return;
    assert(strcmp (tag, TAG_KEY_PROVISION) == 0);

    int response;
    if (! get_key_value_i(message, K_RESPONSE, &response))
    {
        return;
    }
    if (response == 0)
    {
        return;
    }

    char *id = get_key_value(message, K_AGENT_ID);
    if (id)
    {
        save_iagent_id(id);
        load_iagent_id();
    }

    if (get_self_agent_id())
    {
        g_provision_resend_cnt = 0;

        ERROR("Provision completed. iagent id = %s. start to handshake..", get_self_agent_id());

        ams_set_product_id(get_self_agent_id());
        ams_add(TT_DEVICE, get_self_agent_id(),true);

        set_cloud_connection_status (iCloud_Handshaking);
        init_handshake();
        first_handshake();
    }
}

// return next timeout for checking handshake
// -1: for disconnect the socket
int g_prov_expiry = 15;
int check_provision_timeout()
{
    tick_time_t t = bh_get_tick_sec();
	if(t > (g_last_prov+g_prov_expiry))
	{
		if(g_provision_resend_cnt > 5)
		{
			set_cloud_connection_status (iReady_To_Connect);
			return -1;
		}
		else
		{
			init_provision();
			return g_prov_expiry * 1000;
		}
	}
	else
	{
		return (g_last_prov+g_prov_expiry - t) * 1000;
	}
}
