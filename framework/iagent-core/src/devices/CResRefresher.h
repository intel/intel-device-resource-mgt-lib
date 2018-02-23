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


#ifndef CRESREFRESHER_H_
#define CRESREFRESHER_H_
#include "CResource.h"
#include <list>

#include "CRefresherPoint.h"
#include "rd.h"

#define MIN_REFRESH_TIME 1  //dont try to read twice in 1 second

class CClient;
class CResource;
class CRefresherPoint;

class CResRefresher {

public:
	CResRefresher();
	virtual ~CResRefresher();

	int  GetResNum() { return m_expired_res_list.size(); }

	CRefresherPoint *FindRes(char *, char * clientId, bool working_list = true);

	tick_time_t GetNearExpiry();
	bool CheckExpiry(int leading_time = 0);

	bool RunRefresh();

    void AddResOrder(CRefresherPoint *);
    void RemoveRes(CRefresherPoint *);

    void HandleClientStatus(CClient * client, bool Online);

    int NewRefresherPoint(char * device, char * ,  fresher_para_t * param);
    int NewRefresherPointforRT(const char * device, const char * ,  fresher_para_t * param);
    bool DelRefresherPoint(char * device, char * resource, char * purl);
	void MarkAllFlagged(char * device);
	void RemoveAllFlagged(char * purl);


private:
    std::list<CRefresherPoint*> m_expired_res_list;

    // wait the client to be online
    std::list<CRefresherPoint*> m_waiting_list;
};

CResRefresher & ResRefresher();

#endif /* CRESREFRESHER_H_ */
