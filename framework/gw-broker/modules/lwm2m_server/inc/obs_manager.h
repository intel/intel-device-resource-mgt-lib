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

#ifndef APPS_GW_BROKER_MODULES_LWM2M_SERVER_INC_OBS_MANAGER_H_
#define APPS_GW_BROKER_MODULES_LWM2M_SERVER_INC_OBS_MANAGER_H_
#include <time.h>

#include "message.h"
#include "liblwm2m.h"
#include "azure_c_shared_utility/doublylinkedlist.h"

typedef enum {
	S_Not_Observed = 0,
	S_Observed,
	S_Observing
} observe_status_t;

typedef struct obs_user_data
{
	struct obs_user_data * next;
	uint16_t client_id;
	char * client_uuid;
	char * uri;
	time_t last_report;
	uint8_t keep_on_offline;	// don't delete this node when client goes offline
	uint8_t first_reported;		// used to track the response of observe request
	uint8_t sleep_mode;			// client device is sleep mode
	char * request_id;		// the bus message id
	char * requester;
	char * publish_point;
	uint16_t watch_time;		// if no report over this threshold, resend the obs request to client
	observe_status_t status;
}obs_user_data_t;

lwm2m_context_t * get_lwm2m_context();
lwm2m_client_t *getClientbyID(lwm2m_context_t * contextP, int id);

void check_bus_message();
bool wakeup_lwm2m_thread();
void remove_obs_user_data(obs_user_data_t * node);
void release_obs_user_data(obs_user_data_t *obs_ctx);

void obs_notify_callback(uint16_t clientID,
                                lwm2m_uri_t * uriP,
                                int count,
                                lwm2m_media_type_t format,
                                uint8_t * data,
                                int dataLength,
                                void * userData);

obs_user_data_t * find_obs_user_data(const char * ep, const char * url);

obs_user_data_t * new_obs_user_data(const char * ep, const char * url);

#endif /* APPS_GW_BROKER_MODULES_LWM2M_SERVER_INC_OBS_MANAGER_H_ */
