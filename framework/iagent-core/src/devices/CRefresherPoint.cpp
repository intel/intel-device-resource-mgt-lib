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


#include "iagent_base.h"

extern "C" {
#include "connection.h"
}

#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <limits.h>
#include <sys/time.h>

#include "agent_core_lib.h"
#include "rd.h"
#include "url-parser-c/url_parser.h"
#include "CRefresherPoint.h"
#include "message.h"
#include "broker_rest_convert.h"
#include "CClientManager.h"

#include "er-coap.h"
#include "coap_request.h"

#include "CResRefresher.h"
#include "CResource.h"
#include <map>


unsigned int CRefresherPoint::g_current_max_id =1;    // to do should consider the max_is is over the limit of "INT value"
static std::map<unsigned int, void *> g_refresher_point_map;
static unsigned int g_refresher_monitor_id = 0;

CRefresherPara::CRefresherPara(char * publish_point):
		m_sequence(0),
		m_processing(false),
		m_min_duration(30),
		m_publish_point(publish_point),
		m_error_cnt(0),
		m_skip_cnt(0)
{
	m_id = g_refresher_monitor_id++;
}

CRefresherPoint *CRefresherPoint::FindPoint(unsigned int id)
{
    std::map<unsigned int, void *>::iterator it;
    it = g_refresher_point_map.find(id);
    if(it == g_refresher_point_map.end())
        return NULL;
    else
        return (CRefresherPoint *) it->second;
}


CRefresherPoint::CRefresherPoint(char *device, char *resource):
m_flag(Clear),
m_val_expiry(0),
m_parent_refresher(NULL),
m_read_in_progerss(false),
m_last_read_time(0),
m_read_fails(0),
m_last_notified(0),
m_observe(Obs_None),
m_value_lifetime(30),
m_waiting_list(true),
m_client_id(device),
m_resource_url(resource),
m_assocated_resource(NULL),
m_res_property()
{
    m_internal_id=CRefresherPoint::g_current_max_id;
    g_refresher_point_map[m_internal_id] = this;
    CRefresherPoint::g_current_max_id++;
    if(g_current_max_id == 0) g_current_max_id =1;

}


CRefresherPoint::~CRefresherPoint()
{
    // TODO Auto-generated destructor stub
    if (m_parent_refresher)
    {
        m_parent_refresher->RemoveRes(this);
    }

    g_refresher_point_map.erase(m_internal_id);

    Cleanup();
}


void CRefresherPoint::Cleanup()
{
    // todo: cancel any observation on the target resource

}


std::string CRefresherPoint::GetName()
{
    std::string name;
    name =  "[" +m_client_id + "] " + m_resource_url;

    return name;
}


// when the expiry is set for a resource, we must reorder the resource node
// in the expiry link
void CRefresherPoint::SetExpiry(int timeout)
{
    if (timeout == -1)
    {
        timeout = m_value_lifetime;

        // After continous read failures, we enlarge the read interval to reduce system impact
        if (m_read_fails > 5)
        {
            timeout += (m_value_lifetime * MIN(10, m_read_fails-5));
        }
    }
    m_val_expiry = bh_get_tick_sec() + timeout;
    TraceI(FLAG_DATA_POINT, "SetExpiry: m_value_time=%d, timeout=%d, m_val_expiry=%d, m_client_id=%s, m_resource_url=%s, m_res_property=%s",
            m_value_lifetime, timeout, m_val_expiry, m_client_id.c_str(), m_resource_url.c_str(), m_res_property.c_str());

    assert (!m_waiting_list);

    if (m_parent_refresher && !m_waiting_list)
    {
        m_parent_refresher->RemoveRes(this);
        ResRefresher().AddResOrder(this);
    }
}


CRefresherPara *CRefresherPoint::FindMonitor(char *purl, bool remove_if_found)
{
    std::list<void *>::iterator it;
    CRefresherPara *param = NULL;
    for (it = m_monitors.begin(); it != m_monitors.end(); it++)
	{
    	CRefresherPara *monitor = ((CRefresherPara*)(*it));
    	if(monitor->m_publish_point == purl)
    	{
    	    if (remove_if_found)
    	    {
    	    	m_monitors.erase(it);
    	        MergeParameters();
    	        delete monitor;
    	        param = NULL;
    	    }
    	    else
    	    {
    	    	param = monitor;
    	    }

    		break;
    	}
	}

    return param;
}


CRefresherPara *CRefresherPoint::AddMonitor(fresher_para_t *p)
{
    CRefresherPara *param = FindMonitor(p->publish_point, true);
    if (param!= NULL)
        delete param;

    param = new CRefresherPara(p->publish_point);
    param->m_min_duration = p->min_duration;
    param->m_processing = p->process?true:false;
    param->m_sequence = p->sequence;

    std::list<void *>::iterator it;
    for (it = m_monitors.begin(); it != m_monitors.end(); it++)
	{
    	CRefresherPara *monitor = ((CRefresherPara*)(*it));
    	if(monitor->m_sequence < param->m_sequence)
    	{
    		m_monitors.insert(it, param);
    		break;
    	}
	}

    if(it == m_monitors.end())
    {
       m_monitors.insert(m_monitors.end(), param);
    }

    //m_map_monitors[name] = param;
    TraceI(FLAG_DATA_POINT, "[Refresher]:AddMonitor, intv:%d, purl:%s, list_size:%d",
            param->m_min_duration, param->m_publish_point.c_str(), m_monitors.size());

    if (p->min_duration>0)
       MergeParameters();

    // help to identify all point that are no longer observed in the device config file
    m_flag = Clear;

    return param;
}


// get the smallest duration
void CRefresherPoint::MergeParameters()
{
    std::list<void *>::iterator it;
    int min_duration = INT_MAX;
    for (it = m_monitors.begin(); it != m_monitors.end(); it++)
	{
    	CRefresherPara *param = ((CRefresherPara*)(*it));

        if (param->m_min_duration < min_duration)
            min_duration = param->m_min_duration;
    }
    m_value_lifetime = min_duration;
}

typedef struct
{
	unsigned int refresh_point_id;
	unsigned int monitor_id;
	int payload_fmt;
	int payload_len;
	char * payload;
	char * query;
	char * url;
} data_processing_ctx_t;


typedef struct
{
    unsigned int refresh_point_id;
    unsigned int monitor_id;
} failed_monitor_ctx_t;

extern "C" int data_processing_response_handler_for_failed_monitor (void * ctx, void * data, int len, unsigned char format)
{
    failed_monitor_ctx_t * processing_ctx  = (failed_monitor_ctx_t *) ctx;
    CRefresherPoint *point = NULL;
    std::list<void *>::iterator it;

    point = CRefresherPoint::FindPoint(processing_ctx->refresh_point_id);
    if(point == NULL)
    {
        goto end;
    }


    for (it= point->m_monitors.begin(); it != point->m_monitors.end(); ++it)
    {
        CRefresherPara *param = ((CRefresherPara*)(*it));
        if(param->m_id == processing_ctx->monitor_id)
        {
            if(data != NULL)
            {
                param->m_error_cnt = 0;
                param->m_skip_cnt = 0;
            }
            else
            {

                param->m_error_cnt ++;
                TraceI(FLAG_DATA_POINT,"Data processing: timeout for failed monitor [%d], seq=%d, fails=%d",
                             param->m_id, param->m_sequence, param->m_error_cnt);

                if(param->m_error_cnt > 10)
                {
                    ERROR("Data processing: removed failed monitor [%d], seq=%d, fails=%d ",
                            param->m_id, param->m_sequence, param->m_error_cnt);

                    point->m_monitors.erase(it);

                    // todo: remote the point if it is last monitor
                }
            }

            break;
        }
    }

end:
        TraceI(FLAG_DATA_POINT,"Data processing: freed the failed monitor context [%p]. monitor=%d, refresher id=%d",
                processing_ctx, processing_ctx->monitor_id, processing_ctx->refresh_point_id);

        free(ctx);
    return 0;
}


/*
 * 1. data == NULL: timeout, add error cnt, and skip it from processing flow when errors hit a number
 *    after that, retry once every N data.
 * 2. if return code is 200, use the response payload as modified data and continue the next monitor processing
 * 3. if return code is 201, stop this working flow
 * 4. if return code is CONTINUE_2_31 = 95:  continue processing
 */
extern "C" int data_processing_response_handler (void * ctx, void * data, int len, unsigned char format)
{

	data_processing_ctx_t * processing_ctx  = (data_processing_ctx_t *) ctx;
	CRefresherPoint *point = NULL;
	std::list<void *>::iterator it;
	CRefresherPara * next_process_point = NULL;
	bool looking_next_process = false;
	char purl[512];

	point = CRefresherPoint::FindPoint(processing_ctx->refresh_point_id);
	if(point == NULL)
	{
		goto end;
	}

	if(data == NULL)
	{
		//point->
	}


	else if(format == T_Coap_Parsed)
	{
		coap_packet_t * message = (coap_packet_t*) data;
		if(message->code == FORBIDDEN_4_03)
		{
			goto end;
		}
		else if((CONTENT_2_05 == message->code || message->code == CHANGED_2_04) && message->payload_len > 0)
		{
			free(processing_ctx->payload);
			processing_ctx->payload = (char*) malloc(message->payload_len);
			memcpy(processing_ctx->payload, message->payload, message->payload_len);
			processing_ctx->payload_len = message->payload_len;
		}
		else if(message->code == CONTINUE_2_31 || message->code == VALID_2_03)
		{


		}
	}
	else if (format == T_Broker_Message_Handle)
	{

	}
	else
	{
		LOG_GOTO("unknow format", end);
	}

    for (it= point->m_monitors.begin(); it != point->m_monitors.end(); ++it)
    {
        CRefresherPara *param = ((CRefresherPara*)(*it));
        if(looking_next_process)
        {
        	if(param->m_processing)
        	{
        	    //
        	    // If the monitor didn't respond the data report previously,
        	    // we will slow down the report frequency and eventually remove it.
        	    //
        	    // We always use a separate callback for the failed monitor,
        	    // so it will not block the processing flow
        		if(param->m_error_cnt > 0)
        		{
        			if(param->m_skip_cnt < param->m_error_cnt)
        			{
        				param->m_skip_cnt ++;

        			}
        			else
        			{
        				TraceI(FLAG_DATA_POINT,"Data processing: post once after %d skips. monitor=%d, seq=%d [%s]",
        				        param->m_skip_cnt, param->m_id, param->m_sequence, processing_ctx->url);

                        param->m_skip_cnt = 0;
                        failed_monitor_ctx_t * fail_ctx  = (failed_monitor_ctx_t *) malloc(sizeof(failed_monitor_ctx_t));
                        memset(fail_ctx, 0, sizeof(*fail_ctx));
                        fail_ctx->refresh_point_id = processing_ctx->refresh_point_id;
                        fail_ctx->monitor_id = processing_ctx->monitor_id;
                        SendRequest((IURL)purl,
                                processing_ctx->payload_fmt,
                                processing_ctx->payload, processing_ctx->payload_len,
                                false, COAP_POST,
                                processing_ctx->query, data_processing_response_handler_for_failed_monitor, ctx);
        			}

        			continue;
        		}

        		next_process_point = ((CRefresherPara*)(*it));

   				TraceI(FLAG_DATA_POINT,"Data processing: move to next monitor. monitor=%d, seq=%d [%s]",
    				    				param->m_id, param->m_sequence, processing_ctx->url);

        		break;
        	}

        	snprintf(purl, sizeof(purl), "%s%s",param->m_publish_point.c_str(), processing_ctx->url);

        	// post message to the monitor if it don't want processing

    		TraceI(FLAG_DATA_POINT,"Data processing: post for notification. monitor=%d, seq=%d [%s]",
    				param->m_id, param->m_sequence, purl);

        	SendRequest((IURL)purl,
        			processing_ctx->payload_fmt,
        			processing_ctx->payload, processing_ctx->payload_len,
					false, COAP_POST, processing_ctx->query, NULL, NULL);
        }
        else if(param->m_id == processing_ctx->monitor_id)
        {
        	TraceI(FLAG_DATA_POINT,"Data processing: found the monitor for response. monitor=%d, seq=%d [%s]",
        	    				    				param->m_id, param->m_sequence, processing_ctx->url);

        	looking_next_process = true;
        	if(data == NULL)
        	{
        		param->m_error_cnt ++;
        	}
        	else
        	{
        		param->m_error_cnt = 0;
        		param->m_skip_cnt = 0;
        	}
        }
    }

    if(next_process_point)
    {
    	processing_ctx->monitor_id = next_process_point->m_id;
    	snprintf(purl, sizeof(purl), "%s%s",next_process_point->m_publish_point.c_str(), processing_ctx->url);

    	// if this is the last monitor, don't wait for the result.
    	if(++it != point->m_monitors.end())
    	{
    		TraceI(FLAG_DATA_POINT,"Data processing: post for processing flow. monitor=%d, seq=%d [%s]",
    				processing_ctx->monitor_id, next_process_point->m_sequence, purl);

        	SendRequest((IURL)purl,
        			processing_ctx->payload_fmt,
    				processing_ctx->payload, processing_ctx->payload_len,
    				false, COAP_POST,
    				processing_ctx->query, data_processing_response_handler, ctx);
        	return 0;

    	}
    	else
    	{
        	SendRequest((IURL)purl,
        			processing_ctx->payload_fmt,
        			processing_ctx->payload, processing_ctx->payload_len,
					false, COAP_POST, processing_ctx->query, NULL, NULL);

        	TraceI(FLAG_DATA_POINT,"Data processing: post for last monitor [%s]", purl);

    	}
    }

end:
	TraceI(FLAG_DATA_POINT,"Data processing: freed the flow context [%p]. monitor=%d, refresher id=%d [%s]",
			processing_ctx, processing_ctx->monitor_id, processing_ctx->refresh_point_id, processing_ctx->url);
	if(processing_ctx->payload) free(processing_ctx->payload);
	if(processing_ctx->query) free(processing_ctx->query);
	if(processing_ctx->url) free(processing_ctx->url);
	free(ctx);

	return 0;
}

void CRefresherPoint::OnRefresherData(int fmt, char *payload, int payload_len)
{
    std::list<void *>::iterator it;

    if (0 == payload_len)
    {
        WARNING("OnRefresherData: payload was NULL!");
        return;
    }

    uint32_t elapsed_ms = get_elpased_ms(&m_last_notified);

    TraceI(FLAG_DATA_POINT, "data notified on %s, %u ms since last notification", GetName().c_str(), elapsed_ms);

    // calibrate the values
    char *new_payload = NULL;
    int len = 0;

    if (this->m_assocated_resource)
    {
        if (m_assocated_resource->GetType() == T_ResourceBase && m_assocated_resource->m_config)
        {
            len = m_assocated_resource->m_config->Calibrate(fmt, payload, payload_len, &new_payload);
        }
        else if (m_assocated_resource->GetType() == T_ResourceObject)
        {
#ifdef ENABLE_CALIBRATE
            len = ((CResObject*)m_assocated_resource)->CalibrateProperty(m_res_property.data(),
                    fmt, payload, payload_len, &new_payload );
#endif
        }
        if (len)
        {
            payload_len = len;
            payload = new_payload;
        }
    }

    // post the data to all the publish points
    for (it=m_monitors.begin(); it!=m_monitors.end(); ++it)
    {
        CRefresherPara *param = ((CRefresherPara*)(*it));


        CClient *client = ClientManager().FindClient((char*)m_client_id.c_str());
        char *st = "dev";
        const char *di = (char*)m_client_id.c_str();

        char purl[512];
        char *cpurl = (char*)param->m_publish_point.c_str();
        if(client)
        {
            st = (char*) client->m_standard.c_str();

            // ensure global id is used for the publishing to the cloud
            // it is trick by hard coding
            if(strncmp(cpurl, "/ibroker", 8) == 0)
                di = (char*)client->m_epname.c_str();
            else
                di = client->GetOutName();
        }

        char *ri = (char*)m_resource_url.c_str();

        snprintf (purl, sizeof(purl), "%s/%s/%s%s", cpurl, st, di, ri);
        TraceI(FLAG_DATA_POINT, "publishing data for monitor - %s", purl);

        if(param->m_processing)
        {

        	// if this pointer was failed, we skip it from the processing flow.
        	// It will be restored at any time when the monitor is responded.
        	if(param->m_error_cnt==0)
        	{
                data_processing_ctx_t * ctx  = (data_processing_ctx_t *) malloc(sizeof(data_processing_ctx_t));
                memset(ctx, 0, sizeof(*ctx));
                ctx->payload = (char*)malloc(payload_len);
                memcpy(ctx->payload, payload, payload_len);
                ctx->payload_len = payload_len;
                // only save the device and resource part
                ctx->url = strdup(purl + strlen(cpurl));
                ctx->refresh_point_id = m_internal_id;
                ctx->monitor_id = param->m_id;
                TraceI(FLAG_DATA_POINT,"Data processing: allocated the flow context [%p]. monitor=%d, refresher id=%d [%s]",
                        ctx, ctx->monitor_id, ctx->refresh_point_id, ctx->url);

                SendRequest((IURL)purl, fmt, payload, payload_len, false, COAP_POST, NULL,
                        data_processing_response_handler,
                        ctx);
        	    break;
        	}
        	else
        	{
        	    failed_monitor_ctx_t * ctx  = (failed_monitor_ctx_t *) malloc(sizeof(failed_monitor_ctx_t));
                memset(ctx, 0, sizeof(*ctx));
                ctx->refresh_point_id = m_internal_id;
                ctx->monitor_id = param->m_id;
                SendRequest((IURL)purl, fmt, payload, payload_len, false, COAP_POST, NULL,
                        data_processing_response_handler_for_failed_monitor,
                        ctx);

        	}
        }
        else
        {
            SendRequest((IURL)purl, fmt, payload, payload_len, false, COAP_PUT, NULL, NULL, NULL);
        }
    }

    if (new_payload) free(new_payload);
}


extern "C" int cb_refresher_request(void *ctx_data, void *data, int len, unsigned char format);
extern "C" int post_cb_rd_thread(callback_t *cb);
int CRefresherPoint::ReadRefreshPoint(bool observe)
{

    // note: the refresher point allow the client id is empty, in such case we don't force to
    //       find the client object
    CClient *client = NULL;
    if(!m_client_id.empty())
    {
        client = ClientManager().FindClient(m_client_id.c_str());
        if (client == NULL)
            LOG_RETURN(-1)
        if( client->m_is_passive_device == true)
            return -2;
        if(client->m_standard == "ilink" || client->m_standard == "agent")
            LOG_RETURN(-1)
    }


    // note: don't use refresherpoint as context, it may be destroyed during the transaction
    context_refresher_request_t *ctx = (context_refresher_request_t*) trans_malloc_ctx(sizeof(context_refresher_request_t));
    memset (ctx, 0, sizeof(context_refresher_request_t));

    ctx->refresher_id = m_internal_id;
    ctx->observing = observe;
    ctx->request_time_ms = get_platform_time();

    char *separate_url = NULL;
    char *query = NULL;
    if (!m_res_property.empty() && m_assocated_resource &&
            m_assocated_resource->GetType() == T_ResourceObject)
    {
        CResObject *res_obj = (CResObject*)m_assocated_resource;
        separate_url = res_obj->GetPropertySeparateUrl((char*)m_res_property.c_str(), COAP_GET);
    }

    if (client && client->m_standard == "ep")
    {
        char sendbuf[COAP_MAX_PACKET_SIZE];
        coap_packet_t message[1];

        coap_init_message((void *)message, COAP_TYPE_CON, (uint8_t)COAP_GET,
                (uint16_t)bh_gen_id(get_outgoing_requests_ctx()));
        coap_set_header_uri_path(message, m_resource_url.c_str());
        if (observe) coap_set_header_observe (message, 1);
        int len = coap_serialize_message((void *)message, (uint8_t *)sendbuf);

        //
        bh_wait_response_async(get_outgoing_requests_ctx(),
                message->mid,
                (void*)cb_refresher_request,
                ctx,
                GetValueLifetime()*1000,
                NULL);

        // todo: need to setup the connection
        connection_send(client->GetConnection(), (uint8_t *)sendbuf, (size_t)len);
    }
    else
    {
        MESSAGE_CONFIG msgConfig;
        char url[256] = {0};
        // separate_url implicate the client id is present
        if (NULL == separate_url)
        {
            if (client == NULL)
                snprintf(url, sizeof(url), "%s%s", m_resource_url[0]=='/'?"":"/", m_resource_url.c_str());
            else
                snprintf(url, sizeof(url), "/%s/%s%s", client->m_standard.c_str(), client->GetOutName(), m_resource_url.c_str());

        }
        else
        {
            strncpy (url, separate_url, sizeof(url)-1);
            query = strchr (url, '?');
            if (NULL != query)
            {
                *query = '\0';
                query ++;
            }
        }

        unsigned long mid = bh_gen_id(get_outgoing_requests_ctx());

        TraceI(FLAG_DATA_POINT, "[Refresher]:sending data request for url= %s, query=%s, mid=%d, obs=%d",
                m_resource_url.c_str(), query?query:"", mid, observe);

        if (!setup_bus_restful_message(&msgConfig, (char *)TAG_REST_REQ, -1, url, query, COAP_GET, NULL, 0))
        {
            WARNING("failed to setup gw bus message for refresher request");
            return -1;
        }


        char buffer[50];
        sprintf (buffer, "%ld", mid);
        set_bus_message_property(&msgConfig, XK_MID, buffer);

        if (observe)
        {
            set_bus_message_property(&msgConfig, XK_OBS, "1");
        }


        bh_wait_response_async(get_outgoing_requests_ctx(),
                mid,
                (void*)cb_refresher_request,
                ctx,
                GetValueLifetime()*1000,
                (void *)post_cb_rd_thread);

        publish_message_cfg_on_broker(&msgConfig);
    }


    return 0;
}

