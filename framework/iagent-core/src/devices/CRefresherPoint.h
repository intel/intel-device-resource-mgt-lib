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

#ifndef APPS_IAGENT_CORE_SRC_DEVICES_CREFRESHERPOINT_H_
#define APPS_IAGENT_CORE_SRC_DEVICES_CREFRESHERPOINT_H_
#include "CResRefresher.h"
#include "CResource.h"
#include "rd.h"

#include <map>



typedef struct context_refresher_request
{
	unsigned refresher_id;
	bool observing;
	uint32_t request_time_ms;
}context_refresher_request_t;




class CResRefresher;
class CResource;

class CRefresherPara
{
public:
	CRefresherPara(char * publish_point);
	unsigned int m_id;
	unsigned int m_error_cnt;
	unsigned int m_skip_cnt;
	int m_sequence;
	bool m_processing;

	int m_min_duration;
	std::string m_publish_point;
};


class CRefresherPoint {
public:
	CRefresherPoint(char * device, char * resource);
	virtual ~CRefresherPoint();
	enum {
		Clear = 0,
		Flagged = 1
	} m_flag;


	static CRefresherPoint * FindPoint(unsigned int);
    unsigned int m_internal_id;
	int GetValueLifetime() {return m_value_lifetime;};
	void SetValueLifeTime(int lifetime) {m_value_lifetime = lifetime;};
	tick_time_t GetExpiry() {return m_val_expiry;};
    void SetExpiry(int timeout = -1);
    tick_time_t m_val_expiry;
	CResRefresher * m_parent_refresher;

	void Cleanup();

	std::string GetName();


	bool   m_read_in_progerss;
	time_t m_last_read_time;
	int m_read_fails;
	uint32_t m_last_notified;	// record the obs report
	enum
	{
		Obs_None,
		Obs_Inprogress,
		Obs_Success,
		Obs_Failed
	}m_observe;

	int    m_value_lifetime;  //seconds

	bool m_waiting_list;

	std::string m_client_id;
	std::string m_resource_url;

	// the resource object, NOT property even this is refreshing a property
	CResource * m_assocated_resource;
	std::string m_res_property;

	std::list <void *> m_monitors;

	CRefresherPara * FindMonitor(char * name, bool remove_if_found = false);
	CRefresherPara * AddMonitor(fresher_para_t * p);
	void MergeParameters();

	void OnRefresherData(int fmt, char * payload, int payload_len);
	int ReadRefreshPoint(bool observe);

    static unsigned int g_current_max_id;

};

#endif /* APPS_IAGENT_CORE_SRC_DEVICES_CREFRESHERPOINT_H_ */
