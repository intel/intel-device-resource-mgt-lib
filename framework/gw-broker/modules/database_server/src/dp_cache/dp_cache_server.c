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

#include "dp_cache_server.h"
#include "dp_cache_db.h"

char g_quit_thread = 0;
cloud_status_e g_cloud_status_dp = iReady_To_Connect;
sync_ctx_t* g_broker_ctx = NULL;
extern int g_max_dpcache_send_speed;
extern int g_max_dpcache_row;
extern int g_current_item_in_db;
msg_queue_t *g_dp_cache_queue;

static void sendCacheDataToAgent(DpCacheData *pCacheData)
{

    MESSAGE_CONFIG msgConfig = {0};
	char c_uri[256];
	memset(c_uri,0,256);
    snprintf(c_uri, sizeof(c_uri), "/ibroker/dp%s", pCacheData->uri);
	if(!setup_bus_restful_message(&msgConfig, TAG_REST_REQ, pCacheData->fmt,
			    c_uri, NULL, COAP_PUT, pCacheData->payload, pCacheData-> payload_len))
		return ;
	
	int msg_id = bh_gen_id(g_broker_ctx);
	char c_mid[32];
    memset(c_mid,0,32);
	snprintf (c_mid, 32, "%d", msg_id);
	set_bus_message_property(&msgConfig, XK_MID, c_mid);

	char c_tm[64];
    memset(c_tm,0,64);
    snprintf(c_tm, sizeof(c_tm), "%ld", pCacheData->time);
	set_bus_message_property(&msgConfig, XK_TM, c_tm);
	
	publish_message_cfg_on_broker(&msgConfig);
}

char * my_ini_filename()
{
    static char g_ini_path[MAX_PATH_LEN] = {0};

    if(g_ini_path[0])
        return g_ini_path;

    get_product_config_pathname("gw_broker.ini", TT_DEVICE, NULL,g_ini_path);

    return g_ini_path;
}

static bool load_ini()
{
    dictionary  *ini;

    ini = iniparser_load(my_ini_filename());
    if(NULL != ini)
    {
        g_max_dpcache_send_speed = iniparser_getint(ini, "dp-cache:speed", MAX_SEND_DP_CACHE_ITEMS_NUM);
        g_max_dpcache_row = iniparser_getint(ini, "dp-cache:maxrow", MAX_DP_CACHE_DB_ROW);

        iniparser_freedict(ini);
    }
    else
    {
    	g_max_dpcache_send_speed = MAX_SEND_DP_CACHE_ITEMS_NUM;
    	g_max_dpcache_row = MAX_DP_CACHE_DB_ROW;
    }
    return true;
}

int dp_cache_db_server_init( )
{
	g_broker_ctx = create_sync_ctx();
	g_dp_cache_queue = create_queue();
	return (int)intDpCasheDB();
}



tick_time_t g_last_link_change = 0;
int thread_dp_cache_db_server(void * param)
{
#ifdef RUN_ON_LINUX
    prctl (PR_SET_NAME, "dp cache server");
#endif
    bool config_loaded = false;
    msg_t *msg;
    sleep(2);  //wait iagent start up first
    TraceI(FLAG_DP_CACHE_DB, "DP_CACHE_MOD: start dp cache db server thread");
    unsigned    int sent = 0;
    while (g_quit_thread == 0)
    { 
       if (!config_loaded)
           config_loaded = load_ini();

      int timeout = 10;

          //send the stored data records to agent and delete it form DB
         if(g_cloud_status_dp==iReady_For_Work && g_current_item_in_db>0 && ( g_last_link_change + 60) < bh_get_tick_sec())
         {
                // continue to send out the cached dp if sending was successful and no incoming DP in the queue
	      if(0 != deleteOlderDpCacheData( 1,  sendCacheDataToAgent) && g_dp_cache_queue->cnt ==0)
                 {
                       sent ++;
                       useconds_t usec = g_max_dpcache_send_speed * 1000;
                       // don't send more than 20 messages per seconds to avoid overload
                       if(usec < 50*1000) usec = 50*1000;
                       usleep(usec);
                      continue;
                 }
         }

       // wait for incoming DP for caching if there is no 
       msg = get_msg(g_dp_cache_queue, timeout );
       if (msg)
       {
            DpCacheData* dpData=msg->body;
            storeDpCacheData(dpData);
            free(dpData->payload);
            free(dpData);
            free(msg);

            if(g_current_item_in_db>g_max_dpcache_row)
            	deleteOlderDpCacheData(1, NULL);
       }
    }

    return -1;
}

void operate_broad_msg(int payload_len, const char* payload)
{
    //to pararse the payload to check if new cloud status info in it
    JSON_Value *payload_val = json_parse_string(payload);
    JSON_Object *payload_obj = json_object(payload_val);
    TraceI(FLAG_DP_CACHE_DB,"[dpcache]:cloud status changed. ex status:%d\n", g_cloud_status_dp);
    g_cloud_status_dp = (int)json_object_get_number(payload_obj, "cloud_status");
    g_last_link_change = bh_get_tick_sec();
    TraceI(FLAG_DP_CACHE_DB,"[dpcache]:cloud status changed. current status:%d\n", g_cloud_status_dp);

}

int add_dp_cache_data(const char* uri, int fmt, time_t time, int payload_len, const char* payload )
{
	//to pararse the payload to get the di/ri infomation
    DpCacheData* dp_Data= (DpCacheData*) malloc(sizeof(DpCacheData));
    strncpy(dp_Data->uri, uri,256);
    dp_Data->payload= malloc(payload_len);
    memcpy(dp_Data->payload, payload, payload_len);
    dp_Data->payload_len= payload_len;
    dp_Data->fmt =fmt;
    dp_Data->time =time;
	post_msg(g_dp_cache_queue, dp_Data,0);
	return 0;
}

