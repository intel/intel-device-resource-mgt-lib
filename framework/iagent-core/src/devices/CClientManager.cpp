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


#include <stdlib.h>
#include <iostream>
#include <string>
#include "CClientManager.h"
#include "CClient.h"
#include "ams_constants.h"
extern "C" int ams_add(char *target_type, char *target_id,bool overwrite);

CClientManager g_client_manager;
CClientManager & ClientManager()
{
    return g_client_manager;
}


extern "C" char *find_client_by_connection(void *connection, char *buffer, int buf_len)
{
    CClient *client = ClientManager().FindClientByConnection((connection));
    if(client == NULL ) return NULL;

    strncpy(buffer, client->m_epname.c_str(), buf_len-1);

    return buffer;
}

CClientManager::CClientManager()
{
    // TODO Auto-generated constructor stub
    pthread_rwlock_init (&m_client_list_rwlock, NULL); //PTHREAD_MUTEX_INITIALIZER;
}

CClientManager::~CClientManager()
{
    FreeAll();
}


bool CClientManager::Lock(int locktype)
{
    if (locktype==this->R_LOCK)
    {
        pthread_rwlock_rdlock (&m_client_list_rwlock);
    }
    else if (locktype==this->W_LOCK)
    {
        pthread_rwlock_wrlock (&m_client_list_rwlock);
    }
    else
        LOG_RETURN (false);

    return true;
}


void CClientManager::Unlock()
{
    pthread_rwlock_unlock (&m_client_list_rwlock);
}

void CClientManager::AddClient(CClient *client)
{
    Lock (W_LOCK);
    this->m_list.push_front(client);

    client->IncreaseRef();
    Unlock ();
}


// the CClient object is not freed
void CClientManager::FreeClient(CClient *client)
{
    Lock (W_LOCK);


    if(!client->m_alias_name.empty())
    {
        UnsetDeviceAlias(client->m_alias_name.c_str());
    }

    client->m_online = false;
    this->m_list.remove(client);

    client->DecreasRef();
    if (client->ReferrenceCnt() == 0)
    {
        delete client;
    }

    Unlock ();
}


void CClientManager::FreeAll()
{
    // Get the write lock
    Lock (W_LOCK);

    std::list<CClient*>::iterator it;
    int cnt = 0;

    for (it=m_list.begin(); it!= m_list.end(); it++ )
    {
        /**
         * Normally we should check if the client's reference number is 0.
         * But, FreeAll is only called in the CClientManager's destructure function.
         * And we use the singleton pattern. if the freeall function is called, that
         * means the program is exit. Then we don't check it here.
         */
        delete (CClient*)(*it);
        cnt ++;
    }

    m_list.clear();

    Unlock ();

    TraceI(FLAG_DEVICE_REG, "client manager: freed all %d clients",  cnt);

}

std::list<CClient*> &CClientManager::FindAllClient()
{
    return m_list;
}

CClient *CClientManager::FindClient(unsigned int id)
{
    std::list<CClient*>::iterator it;
    for (it=m_list.begin(); it!= m_list.end(); it++)
    {
        CClient *pClient = ((CClient*)(*it));
        if ((unsigned int)pClient->m_id ==  id)
        {
            return pClient;
        }
    }

    return NULL;
}


// find the client by global device id or the local id (alias name)
CClient *CClientManager::FindClient(const char *epname)
{
    std::list<CClient*>::iterator it;
    for (it=m_list.begin(); it!= m_list.end(); it++)
    {
        CClient *pClient = ((CClient*)(*it));
        if ((!pClient->m_epname.empty() && pClient->m_epname ==  epname) ||
            (!pClient->m_alias_name.empty() && pClient->m_alias_name == epname))
        {
            return pClient;
        }
    }

    return NULL;
}

CClient *CClientManager::FindClientByConnection(void *connection)
{
    std::list<CClient*>::iterator it;
    for (it=m_list.begin(); it!= m_list.end(); it++)
    {
        CClient *pClient = ((CClient*)(*it));
        if (pClient->m_connection ==  connection)
        {
            return pClient;
        }
    }

    return NULL;
}

CClient *CClientManager::GetLockedClient(const char *epname)
{
    Lock(R_LOCK);

    std::list<CClient*>::iterator it;
    for (it=m_list.begin(); it!= m_list.end(); it++)
    {
        CClient *pClient = ((CClient*)(*it));
        if (pClient->m_epname ==  epname)
        {
            // If this client is not online, ignore it.
            if (! pClient->m_online)
                break;

            pClient->IncreaseRef();
            Unlock ();
            return pClient;
        }
    }

    Unlock();
    return NULL;
}


void CClientManager::UnlockClient(CClient *client)
{
    Lock (W_LOCK);

    client->DecreasRef();

    // if th referrence count reach zero, it means the
    // node is no longer in the list(mostly device went to offline)
    if (client->ReferrenceCnt() == 0)
    {
        assert (!client->m_online);
        delete client;
    }

    Unlock ();
}


CClient *CClientManager::HandleRdPost(
        const char *di, const char *st, const char *dt,
        int ttl, const char *settings)
{
    bool alloc = false;
    CClient *client = FindClient(di);

    if (client == NULL)
    {
        client = new CClient((char *)di);
        alloc = true;
    }
    client->m_standard = st?st:"";
    client->m_device_type = dt?dt:"";
    client->m_settings = settings?settings:"";
    client->m_ttl = (ttl > 0)? ttl:0;
    if (client->m_settings.find('u') != client->m_settings.npos)
        client->m_is_queue_mode = true;
    else
        client->m_is_queue_mode = false;

    if (client->m_settings.find('p') != client->m_settings.npos)
        client->m_is_passive_device = true;
    else
        client->m_is_passive_device = false;

    if(alloc)
    {
        AddClient(client);
    }

    return client;
}



void CClientManager::SetDeviceAlias(const char * alias, CClient * client)
{
    m_map_alias[alias] = client;
    client->m_alias_name = alias;
}

void CClientManager::UnsetDeviceAlias(const char * alias)
{
    std::map<std::string, void *>::iterator it;
    it = m_map_alias.find(alias);
    if(it == m_map_alias.end())
        return;
    else
    {
        m_map_alias.erase(it);
    }
}

CClient * CClientManager::FindAliasDevice(const char * alias)
{
    std::map<std::string, void *>::iterator it;
    it = m_map_alias.find(alias);
    if(it == m_map_alias.end())
    {
        return NULL;
    }
    else
    {
        return ((CClient *) it->second);
    }

}


void CClientManager::QueryRes(ClientQueryParameter & query, string &result)
{

}


// Get the clients, and save it in clients in json format
void CClientManager::QueryClients(ClientQueryParameter & filter, string &result)
{

}

