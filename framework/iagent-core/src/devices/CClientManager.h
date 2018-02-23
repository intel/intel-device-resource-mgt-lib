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


#ifndef CCLIENTMANAGER_H_
#define CCLIENTMANAGER_H_

#include <list>

#include "CClient.h"

using namespace std;

class CClient;

#include "CResource.h"
class CResource;

class ClientQueryParameter
{

};

/**

 */
class CClientManager {

public:
	CClientManager();
	virtual ~CClientManager();

	bool Lock(int locktype);
	void Unlock();

// The functions that only called by Lite-server main thread.
public:
	CClient *HandleRdPost(const char * di, const char * st, const char * dt, int ttl, const char * settings);
	void AddClient(CClient *);
	void FreeClient(CClient *);
	void FreeAll();
	std::list<CClient*> &FindAllClient();
	CClient *FindClient(unsigned int id);
	CClient *FindClient(const char * epname);
	CClient * FindClientByConnection(void * connection);

	// Get the clients, and save it in clients in json format
	void QueryClients(ClientQueryParameter & filter, string &result);
	void QueryRes(ClientQueryParameter & query, string &result);

	// This function return one Locked CClient* object based on the epname.
	// The CClient* object (include it's resource) returned by this function
	// will not be destroyed by other thread until call the UnlockClient explicitly.
	CClient *GetLockedClient(const char * epname);

	/**
	 * After call this function, the the reference number of this CClient object
	 * will be decreased. If the reference number is zero, the CClient object
	 * will also be destroyed.
	 *
	 * Don't use the CClient* anymore after this function.
	 *
	 */
	void UnlockClient(CClient *);

	// std::map <std::string, void *> m_map_client_config;

	void SetDeviceAlias(const char * alias, CClient *);
	void UnsetDeviceAlias(const char * alias);
	CClient * FindAliasDevice(const char * alias);

private:
	enum
	{
		R_LOCK = 0,
		W_LOCK = 1
	};

	std::list<CClient*> m_list;
	pthread_rwlock_t m_client_list_rwlock;

	std::map <std::string, void *> m_map_alias;
};

CClientManager & ClientManager();

#endif /* CCLIENTMANAGER_H_ */
