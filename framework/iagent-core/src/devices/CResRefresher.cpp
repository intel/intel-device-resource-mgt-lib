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


#include "CResRefresher.h"
#include "coap_platforms.h"
#include "CClientManager.h"

CResRefresher g_res_refresher;
CResRefresher & ResRefresher()
{
    return g_res_refresher;
}


CResRefresher::CResRefresher()
{
    // TODO Auto-generated constructor stub
}


CResRefresher::~CResRefresher()
{
    // TODO Auto-generated destructor stub
}


//
// insert the resource node in order of value expiry time
//
void CResRefresher::AddResOrder(CRefresherPoint *res)
{
    assert (res->m_parent_refresher == NULL || res->m_parent_refresher == this);

    assert (!res->m_waiting_list);

    res->m_parent_refresher = this;

    if (m_expired_res_list.empty())
    {
        this->m_expired_res_list.push_front(res);
        return;
    }

    time_t expiry = res->GetExpiry();
    std::list<CRefresherPoint*>::iterator it;
    for (it=m_expired_res_list.begin(); it!= m_expired_res_list.end(); it++)
    {
        CRefresherPoint *res2 = ((CRefresherPoint*)(*it));
        if (expiry < res2->GetExpiry())
        {
            m_expired_res_list.insert(it, res);
            return;
        }
    }

    // insert at the end if this is the bigger node
    m_expired_res_list.insert(m_expired_res_list.end(), res);
}


void CResRefresher::RemoveRes(CRefresherPoint *res)
{
    if (res)
    {
        this->m_expired_res_list.remove(res);
        res->m_parent_refresher = NULL;
    }
}


CRefresherPoint *CResRefresher::FindRes(char *url, char *clientId, bool working_list)
{
    std::list<CRefresherPoint*>::iterator it;
    std::list<CRefresherPoint*> *list = &m_expired_res_list;
    if (!working_list) list = &m_waiting_list;
    for (it = list->begin(); it != list->end(); it++)
    {
        CRefresherPoint *res = ((CRefresherPoint*)(*it));
        if (res->m_client_id == clientId &&
                res->m_resource_url == url)
        {
            return res;
        }
    }

    return NULL;
}


tick_time_t CResRefresher::GetNearExpiry()
{
    std::list<CRefresherPoint*>::iterator it;

    if (m_expired_res_list.empty())
    {
        return -1;
    }

    for (it = m_expired_res_list.begin(); it != m_expired_res_list.end(); it++)
    {
        CRefresherPoint *res = ((CRefresherPoint*)(*it));
        //if one refresher point has sent get data request,but has't get response from  device
        if (! res->m_read_in_progerss)
        {
            return res->GetExpiry();
        }
    }

    return -1;
}


/*
 * Check if there is one resource value which is expired.
 *
 * Parameter:
 *  leading_time: Since we need update the resource value before it's expired.
 *                leading time defined the time interval that how long we need
 *                update the value before it's expired.
 *
 * Return:
 *  True if one resource value is expired. False while not.
 */
bool CResRefresher::CheckExpiry(int leading_time)
{
    /**
     * GetNearExpiry return the time that the resource value is expired.
     */
    tick_time_t t = GetNearExpiry();
    if (t == -1)
        return false;
    if (t == 0)
        return true;

    return (bh_get_tick_sec() > (t-leading_time));
}


#define MAX_OBS_MS  60*1000
extern "C" int cb_refresher_request(void *ctx_data, void *data, int len, unsigned char format);
bool CResRefresher::RunRefresh()
{
    tick_time_t now = bh_get_tick_sec();
    std::list<CRefresherPoint*>::iterator it;
    std::list<CRefresherPoint*> expired_points_temp;

    for (it = m_expired_res_list.begin(); it != m_expired_res_list.end(); it++)
    {
        CRefresherPoint *point = ((CRefresherPoint*)(*it));
        if (!point->m_read_in_progerss && now >= point->GetExpiry())
        {

            assert (now > (point->m_last_read_time));

            point->m_last_read_time = now;

            // if never tried obs or too long not receving notify,rerun obs operation
            bool observe = 0;
            uint32_t last_data_notified = point->m_last_notified;
            if(point->m_observe == CRefresherPoint::Obs_None ||
                    (point->m_observe == CRefresherPoint::Obs_Success &&
                    get_elpased_ms(&last_data_notified) > MAX_OBS_MS))
                observe = 1;
            int result = point->ReadRefreshPoint(observe);

            if (observe) point->m_observe = CRefresherPoint::Obs_Inprogress;

            if (-2 == result)
            {
                TraceI(FLAG_DATA_POINT, "passive device can't be refresher initiative.");
            }
            else if (result != 0 )
            {
                ERROR( "Error in read, %d.%2d", (result&0xE0)>>5, result&0x1F);
            }
            else
            {
                point->m_read_in_progerss = true;

            }

            /// note: don't call point->SetExpiry() in this loop
            ///       because the SetExpiry() will remove the point from the m_expired_res_list list
            expired_points_temp.push_back(point);
        }
    }

    ///
    for (it = expired_points_temp.begin(); it != expired_points_temp.end(); it++)
    {
        CRefresherPoint *point = ((CRefresherPoint*)(*it));
        point->SetExpiry();
    }

    return true;
}


void CResRefresher::HandleClientStatus(CClient *client, bool Online)
{
    std::list<CRefresherPoint*>::iterator it, tmp;

    if (Online)
    {
        for (it = m_waiting_list.begin(); it != m_waiting_list.end(); )
        {
            CRefresherPoint *point = ((CRefresherPoint*)(*it));

            if (client->HasId(point->m_client_id.c_str()))
            {
                tmp = it;
                it++;
                m_waiting_list.erase(tmp);

                point->m_val_expiry = 0;
                point->m_waiting_list = false;

                point->m_assocated_resource = client->UrlMatchResource( (char*)point->m_resource_url.data(), &point->m_res_property );

                TraceI(FLAG_DATA_POINT, "device [%s] online, refresh point [%s] moved to working list",
                        client, point->m_resource_url.c_str());

                AddResOrder (point);
            }
            else
            {
                it++;
            }
        }
    }
    else if (!m_expired_res_list.empty())
    {
        for (it = m_expired_res_list.begin(); it != m_expired_res_list.end(); )
        {
            CRefresherPoint *point = ((CRefresherPoint*)(*it));

            if (client->HasId(point->m_client_id.c_str()))
            {
                tmp = it;
                it++;
                m_expired_res_list.erase(tmp);

                point->m_waiting_list = true;
                m_waiting_list.insert(m_waiting_list.end(), point);

                TraceI(FLAG_DATA_POINT, "device [%s] offline, refresh point [%s] moved to wait list",
                        client, point->m_resource_url.c_str());

            }
            else
            {
                it++;
            }
        }
    }
    else
    {
    }
}

int CResRefresher::NewRefresherPointforRT(const char * device, const char * rt,  fresher_para_t * param)
{

	return -1;
}

int CResRefresher::NewRefresherPoint(char *device, char *resource, fresher_para_t *param)
{
    // find existing points on this resource
    device = try_use_local_id(device);

    CRefresherPoint *p = FindRes(resource, device, true);
    if (!p)
    {
        p = FindRes(resource, device, false);
    }

    if (!p)
    {
        p = new CRefresherPoint(device, resource);
        CClient *client = ClientManager().FindClient(device);
        if (client)
        {
            p->m_assocated_resource = client->UrlMatchResource( (char*)p->m_resource_url.data(), &p->m_res_property);

            p->m_waiting_list = false;
            AddResOrder (p);
        }
        else
        {
            p->m_waiting_list = true;
            m_waiting_list.insert(m_waiting_list.end(), p);
        }

        WARNING("Created new refresh point %s in active list: \n\tdevice=%s, res=%s",
                p->m_waiting_list?"not":"", device, resource);
    }
    else
    {
        WARNING( "refresh point already exist %s in active list: \n\tdevice=%s, res=%s",
                p->m_waiting_list?"not":"", device, resource);

    }

    if (p != NULL)
    {
        p->AddMonitor(param);
        return p->m_internal_id;
    }
    else
        LOG_RETURN (-1)
}


bool CResRefresher::DelRefresherPoint(char *device, char *resource, char *purl)
{
    CRefresherPoint *p = FindRes(resource, device, true);
    if (!p)
    {
        p = FindRes(resource, device, false);
    }

    if (!p)
    {
        WARNING("DelRefresherPoint: didn't find resource point");
        return false;
    }

    p->FindMonitor(purl, true);
    TraceI(FLAG_DATA_POINT, "Del Refresher Point: device=%s, res=%s, src=%s", device,resource, purl );

    if (p->m_monitors.empty())
    {
        WARNING("DelRefresherPoint: all source removed, destroy the refresh point");
        RemoveRes(p);
        delete p;
    }
    return true;
}


void CResRefresher::MarkAllFlagged(char *device)
{
    std::list<CRefresherPoint*>::iterator it;
    for (it = m_expired_res_list.begin(); it != m_expired_res_list.end(); it++)
    {
        CRefresherPoint *point = ((CRefresherPoint*)(*it));
        if (device == NULL || point->m_client_id == device)
        {
            point->m_flag = CRefresherPoint::Flagged;
        }
    }
    for (it = m_waiting_list.begin(); it != m_waiting_list.end(); it++)
    {
        CRefresherPoint *point = ((CRefresherPoint*)(*it));
        if (device == NULL || point->m_client_id == device)
        {
            point->m_flag = CRefresherPoint::Flagged;
        }
    }
}


void CResRefresher::RemoveAllFlagged(char *purl)
{
    std::list<CRefresherPoint*>::iterator it, tmp;
    for (it = m_expired_res_list.begin(); it != m_expired_res_list.end(); )
    {
        CRefresherPoint *point = ((CRefresherPoint*)(*it));
        tmp = it;
        it++;

        if (point->m_flag)
        {
            point->m_flag = CRefresherPoint::Clear;
            point->FindMonitor(purl, true);

            if (point->m_monitors.empty())
            {
                m_expired_res_list.erase(tmp);
                TraceI(FLAG_DATA_POINT, "removed flagged refresh point from active list. device=%s, res=[%s]",
                        point->m_client_id.c_str(), point->m_resource_url.c_str());
                delete point;
            }
        }
    }

    for (it = m_waiting_list.begin(); it != m_waiting_list.end(); it++)
    {
        CRefresherPoint *point = ((CRefresherPoint*)(*it));
        tmp = it;
        it++;

        if (point->m_flag)
        {
            point->m_flag = CRefresherPoint::Clear;
            point->FindMonitor(purl, true);

            if (point->m_monitors.empty())
            {
                m_expired_res_list.erase(tmp);
                TraceI(FLAG_DATA_POINT, "removed flagged refresh point from wait list. device=%s, res=[%s]",
                        point->m_client_id.c_str(), point->m_resource_url.c_str());

                delete point;
            }
        }
    }
}

