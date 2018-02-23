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

#include <sys/time.h>
#include <errno.h>
#include "iagent_base.h"
#include "ilink_message.h"
#include "ams_sdk_interface.h"
#include "ams_constants.h"

typedef enum
{
	S_Unit = 0,
	S_First_Shake_Sent,
	S_Second_Shake_Sent,
	S_Failed,
	S_Success
} handshake_status_e;

typedef struct
{
	handshake_status_e status;
	tick_time_t last_handshake;
	int resend_cnt;
	char * reason;
} handshake_ctx_t;

handshake_ctx_t g_handshake_ctx;

extern void report_rd_to_cloud();
extern void init_resource_directory();
void post_handshake_action()
{
    // init_resource_directory() was called already in init_agent()
    // we called it again here for the first time provision situation.
    init_resource_directory();

    enable_rd_report_to_cloud();
	report_rd_to_cloud();

    char *iagent_id = load_iagent_id();
    if (iagent_id)
    {
        WARNING("set the product id [%s] and device id to ams", get_self_agent_id());
        ams_set_product_id(get_self_agent_id());
        ams_add(TT_DEVICE, iagent_id,true);

        // for download the device.cfg for the iagent itself
        ams_add(TT_DEVICE_ON_GW, get_self_agent_id(),false);
    }
}


static bool sync_cloud_time(int tm_sec, int tm_mic)
{
	struct timeval now;
	int rc;

	now.tv_sec=tm_sec;
	now.tv_usec=tm_mic;

    rc=settimeofday(&now, NULL);
    if(rc==0) {
    	TraceI(FLAG_CLOUD_CONNECT,"[HANDSHAKE]:sycn cloud time successful with sec=%d mic=%d.\n", tm_sec, tm_mic);
    	return true;
    }
    else {
    	WARNING("sync cloud time failed, errno = %d\n",errno);
        return false;
    }

}

bool handshake_done()
{
    return (g_handshake_ctx.status == S_Success);
}

void init_handshake()
{
    memset (&g_handshake_ctx, 0, sizeof(g_handshake_ctx));
    g_handshake_ctx.reason = "";
}

void first_handshake()
{
	ilink_message_t first_contact;
	ilink_vheader_t vheader;
	vheader_node_t * node;

	memset(&vheader, 0, sizeof(vheader));
	memset(&first_contact, 0, sizeof(first_contact));
	init_ilink_message(&first_contact, INTEL_IAGENT);
	ilink_set_req_resp(&first_contact, 1, bh_gen_id(get_outgoing_requests_ctx()));

	if((node = vheader_find_node(&vheader, (char *)K_TAG, (bool)1, (bool)0)) == NULL)
	{
		LOG_GOTO("handshake with gateway must take tag<K_TAG> in message", end)
	}
	vheader_set_node_str(node,TAG_HANDSHAKE1, 0);

	if((node = vheader_find_node(&vheader, (char *)K_AGENT_ID, (bool)1, (bool)0)) == NULL)
	{
		LOG_GOTO("handshake with gateway must take tag<K_AGENT_ID> in message", end)
	}
	char *self_id = get_self_agent_id();
	vheader_set_node_str(node, self_id, 1);

	ilink_set_vheader(&first_contact, &vheader);

	ilink_msg_send(&first_contact);
    TraceI(FLAG_DUMP_MESSAGE, "Send first handshake message to ibroker\n");
	g_handshake_ctx.resend_cnt ++;
	g_handshake_ctx.status = S_First_Shake_Sent;
	g_handshake_ctx.last_handshake = bh_get_tick_sec();

end:
	reset_ilink_message(&first_contact);
	vheader_destroy(&vheader);
}

void second_handshake(ilink_message_t * message)
{
    ilink_message_t second_contact;
    ilink_vheader_t vheader;
    vheader_node_t * node;

    memset(&vheader, 0, sizeof(vheader));
    init_ilink_message(&second_contact, INTEL_IAGENT);
    ilink_set_req_resp(&second_contact, 1, bh_gen_id(get_outgoing_requests_ctx()));

    if((node = vheader_find_node(&vheader, (char *)K_TAG, (bool)1, (bool)0)) == NULL)
    {
        LOG_GOTO("handshake with gateway must take tag<K_TAG> in message", end)
    }
    vheader_set_node_str(node,TAG_HANDSHAKE2, 0);

    if((node = vheader_find_node(&vheader, (char *)K_AGENT_ID, (bool)1, (bool)0)) == NULL)
    {
        LOG_GOTO("handshake with gateway must take tag<K_AGENT_ID> in message", end)
    }
    char *self_id = get_self_agent_id();
    vheader_set_node_str(node, self_id, 1);

    ilink_set_vheader(&second_contact, &vheader);

    ilink_msg_send(&second_contact);
    TraceI(FLAG_DUMP_MESSAGE, "Send second handshake message to ibroker\n");
    g_handshake_ctx.resend_cnt ++;
	g_handshake_ctx.status = S_Second_Shake_Sent;
	g_handshake_ctx.last_handshake = bh_get_tick_sec();



end:
    vheader_destroy(&vheader);
    reset_ilink_message(&second_contact);
}

#define REQUIRE_2ND_HANDSHAKE 100

void on_handshake_message(ilink_message_t * message)
{
	char * tag = get_key_value(message, K_TAG);

	if(tag == NULL)
	{
	    WARNING("NO tag in the on_handshake_message");
	    return;
	}

	if(strcmp(tag, TAG_HANDSHAKE1) == 0)
	{
		if(g_handshake_ctx.status != S_First_Shake_Sent)
		{
		    WARNING("handshake1 recieved, but I am not in the status. status=%d", g_handshake_ctx.status);
			return;
		}

		int response;
		if(! get_key_value_i(message, K_RESPONSE, &response))
		{
		    WARNING("NO K_RESPONSE in the on_handshake_message");
			g_handshake_ctx.reason = "no response field";
			g_handshake_ctx.status = S_Failed;
			return;
		}
		if(response == 0)
		{
		    WARNING("on_handshake_message: handhake is rejected");
			g_handshake_ctx.reason = "server reject";
			g_handshake_ctx.status = S_Failed;
			return;
		}

		if(response == REQUIRE_2ND_HANDSHAKE)
		{
		    WARNING("on_handshake_message: require 2nd handshake..started");
			second_handshake(message);
		}
		else
		{
			g_handshake_ctx.status = S_Success;
			set_cloud_connection_status (iReady_For_Work);
			g_handshake_ctx.reason = "success";

			WARNING("on_handshake_message: success");

			post_handshake_action();
		}
	}
	else if(strcmp(tag, TAG_HANDSHAKE2) == 0)
	{
		if(g_handshake_ctx.status != S_Second_Shake_Sent)
			return;
		g_handshake_ctx.status = S_Success;
		g_handshake_ctx.last_handshake = bh_get_tick_sec();
		set_cloud_connection_status (iReady_For_Work);
		g_handshake_ctx.reason = "success";
		int tm_sec,tm_mic;

		if(get_key_value_i(message, K_CLOUD_TM_SEC, &tm_sec)
				  &&get_key_value_i(message, K_CLOUD_TM_MIC, &tm_mic))
			sync_cloud_time(tm_sec, tm_mic);
		else
			TraceI(FLAG_DUMP_MESSAGE,"[HANDSHAKE]:sycn cloud failed, error time parameter.\n");

		post_handshake_action();
		TraceI(FLAG_DUMP_MESSAGE, "twice handshake success\n");
	}
}

// return next timeout for checking handshake
// -1: for disconnect the socket
int g_handshake_expiry = 15;
int check_handshake_timeout()
{
    tick_time_t t = bh_get_tick_sec();
	if(t > (g_handshake_ctx.last_handshake+g_handshake_expiry))
	{
		if(g_handshake_ctx.resend_cnt > 5)
		{
			set_cloud_connection_status (iReady_To_Connect);
			init_handshake();
			return -1;
		}
		else
		{
			first_handshake();
			return g_handshake_expiry * 1000;
		}
	}
	else
	{
		return (g_handshake_ctx.last_handshake+g_handshake_expiry - t) * 1000;
	}
}
