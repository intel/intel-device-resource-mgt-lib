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


#ifndef APPS_IAGENT_CORE_SRC_DEVICES_RD_H_
#define APPS_IAGENT_CORE_SRC_DEVICES_RD_H_

//iagent

#include <stdint.h>
#include "iagent_base.h"


#ifdef __cplusplus
extern "C" {
#endif

//external
#include "connection.h"

typedef struct _fresher_para
{
	int min_duration;
	char * publish_point;
	int sequence;
	char * process;
}fresher_para_t;

#define CONFIG_FILE		    	".config"

#define J_ep                        "ep"
#define J_en                        "en"
#define J_attr                      "attr"
#define J_obj                       "obj"
#define J_id                        "id"
#define J_ins                       "ins"
#define J_res                       "res"
#define J_v                         "v"
#define J_o                         "o"
#define J_p                         "p"
#define J_t                         "t"
#define J_dv                        "dv"
#define J_rn                        "rn"
#define J_m                         "m"
#ifndef LOG_TAG

#endif
#define J_ad						"add"
#define J_mul						"mul"

#define J_st                        "st"    // source type
#define J_si                        "si"    // source id

#define J_sl                        "sl"    // severity level
#define J_et                        "et"    // event type


#define J_tt                        "total"
#define J_us                        "urls"


#define J_cb						"callback"
#define J_ip						"ip"
#define J_port						"port"
#define J_rest_url					"url"
#define J_reg_id					"reg-id"

#define J_status					"status"
#define J_f							"format" // the content format

typedef enum
{
    T_UNDEFINED,
    T_LWM2M,
    T_OCF,
    T_MODBUS,
    T_COAP_SERVER,
    T_HTTP_SERVER
} ENDPOINT_CATEGORY;

ENDPOINT_CATEGORY get_client_ep_type(const char *standard);
void post_msg_rd(void *body, unsigned int len);
void init_resource_directory();
void enable_rd_report_to_cloud();
void iagent_register_AMS();

bool handle_rd_on_connection(const char *payload, uint16_t payload_len, void * connection);
bool handle_rd_post(const char * payload, char * source);
void handle_rd_get(const char *query, char **res_payload, bool with_cfg);
bool handle_rd_delete(const char * di);
int handle_rd_monitor_put(const char *payload, char * purl_prefix);
void do_rd_monitor_scan(unsigned long monitor_id);
void resend_rd_post(char *epname);

void load_configured_endpoints();
connection_t * get_endpoint_conn(const char * epname);
char *get_endpoint_type(const char *epname);
char *get_alias_device_id(const char *alias, char * url_buffer, int url_len);
bool check_seperate_url(char * device_id, char * url, int action, char ** new_url, char ** new_params);
void * find_device(const char * epname);
bool is_passive_device(void * device);
char * get_alias_url_from_di(const char *di, char * url_buffer, int url_len);


unsigned long SendRequest(char * target, int fmt, char * payload, int payload_len, bool require_response, uint8_t cmdcode, char *query
		, bh_async_callback response_handler, void * response_user_data);
void OnDataPoint(char * device, char * uri, int fmt, char * payload, int payload_len);
bool RemoveResourceRefresher(char * device, char * resource, char * purl);
int AddResourceRefresher(char * device, char * resource, fresher_para_t * param);
int ReadResource(char * device, char * res, void * callback, bool observe);

int calibrate(char * target, int fmt, char * payload, int payload_len, char ** new_payload);

void reload_client_config(char * epname);

void check_url_generic_dev(coap_packet_t *coap_message, char *url_buffer, int buffer_len);
char * try_use_local_id(char * device_id);
char * check_local_device_id(char * device_id);


//refresher.cpp
int handle_data_observing(const char *payload, char * purl_prefix);
void agent_post_message_handler(void * handler, void * message);
void load_configured_data_monitors();

//CClientManager.cpp
char * find_client_by_connection(void * connection, char * buffer, int buf_len);

#ifdef __cplusplus
}
#endif
#endif /* APPS_IAGENT_CORE_SRC_DEVICES_RD_H_ */
