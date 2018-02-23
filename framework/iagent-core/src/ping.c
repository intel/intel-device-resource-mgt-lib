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

//iagent
#include "iagent_base.h"
#include "ilink_message.h"

static tick_time_t g_last_ping = 0;
static int g_ping_resend_cnt = 0;

void init_ping()
{
    ilink_message_t ping_request;
    ilink_vheader_t vheader;
    vheader_node_t *node;

    memset (&vheader, 0, sizeof(vheader));
    memset (&ping_request, 0, sizeof(ping_request));
    init_ilink_message(&ping_request, INTEL_IAGENT);
    ilink_set_req_resp(&ping_request, 1, bh_gen_id(get_outgoing_requests_ctx()));

    vheader_init(&vheader, 1, 0);
    vheader_set_value_s(&vheader, K_TAG, TAG_PING);

    ilink_set_vheader(&ping_request, &vheader);

    ilink_msg_send(&ping_request);

    g_ping_resend_cnt ++;
    g_last_ping = bh_get_tick_sec ();

    vheader_destroy(&vheader);
    reset_ilink_message(&ping_request);
    TraceI(FLAG_PING_MESSAGE, "send ping message to ibroker  ping resend=%d \n", g_ping_resend_cnt);

}

// return next timeout for checking handshake
// -1: for disconnect the socket
int g_ping_expiry_sec = 30;
int check_ping_timeout()
{
    tick_time_t t = bh_get_tick_sec();

	assert (get_cloud_connection_status() == iReady_For_Work);

	if(t >= (g_last_ping+g_ping_expiry_sec))
	{
		if(g_ping_resend_cnt >= 3)
		{
			ERROR ("ping ibroker timeout. resetting the ilink..");
			prepare_reconnect_cloud ();
			g_ping_resend_cnt =0;
			g_last_ping =0 ;
			return -1;
		}
		else
		{
			init_ping();
			return g_ping_expiry_sec* 1000;
		}
	}
	else
	{
		return (g_last_ping + g_ping_expiry_sec - t) * 1000;
	}
}


void on_ping_message(ilink_message_t *message)
{
    TraceI(FLAG_PING_MESSAGE, "recieved ilink ping message\n");
	g_ping_resend_cnt = 0;
}
