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

#include "iagent_base.h"
#include "rd.h"
#include "url-parser-c/url_parser.h"
#include "CRefresherPoint.h"
#include "message.h"
#include "broker_rest_convert.h"
#include "CClientManager.h"

#include "coap_ext.h"

#include "CResRefresher.h"
#include "CResource.h"
#include <map>
#include "ams_constants.h"


// source: the identify of the user who add the refresher point
//
extern "C" int AddResourceRefresher(char *device, char *resource, fresher_para_t *param)
{
    return ResRefresher().NewRefresherPoint(device, resource, param);
}


extern "C" bool RemoveResourceRefresher(char *device, char *resource, char *source)
{
    return ResRefresher().DelRefresherPoint(device, resource, source);
}


// url: /[lwm2m|ep/ocf]/[device id]/[resource...]
extern "C" void OnDataPoint(char *device,  char *uri, int fmt, char *payload, int payload_len)
{
    CRefresherPoint *point = ResRefresher().FindRes(uri, device, true);
    if (point == NULL)
    {
        TraceI(FLAG_DATA_POINT, "OnDataPoint but no observe: fmt=%d, device=%s,uri=%s, payload_len=%d",
                    fmt, device, uri,payload_len);
        return;
    }
    TraceI(FLAG_DATA_POINT, "OnDataPoint: fmt=%d, device=%s, uri=%s, payload_len=%d",
                fmt, device, uri, payload_len);
    point->OnRefresherData(fmt, payload, payload_len);
    point->SetExpiry();
}

typedef struct
{
	bh_async_callback response_handler;
	void * response_user_data;
} transaction_handle_for_coap_t;

static int coap_response_callback(void *data, void *response)
{
	transaction_handle_for_coap_t * ctx = (transaction_handle_for_coap_t*) data;

	ctx->response_handler(ctx->response_user_data, response, 0, T_Coap_Parsed);

	free(data);
	return 0;
}
/*
 *
1) gwbus://[lwm2m|ocf|ep|modbus|dev]/[:device id]/{:resource href]
���С�gwbus://������ʡ�ԣ�/[lwm2m|ocf|ep|modbus|dev]/[:device id]/{:resource
href]Ҳ��ʾ��ͬ�����塣�����?) 3)��ͬ??

2) gwbus:///ibroker/...
ֱ�ӷ����ƶˣ�����ǰ��url�е�ibroker/ǰ׺����??

3��gwbus://....
local service on the gw bus??use broadcast to send

4) gwbus://module/[:module id]/...
����ǰ��Ҫ��urlת������module id��ȡ����������bus message��dest�ֶΡ�ʣ�µ�url������ΪĿ��url.

5) coap://127.0.0.1:7777/...

6) obs://[token id]/....  (��ʱ��ʵ??
*/
unsigned long SendRequest(IURL target, int fmt, char *payload, int payload_len, bool require_response,
		uint8_t msgcode, char *query,
		bh_async_callback response_handler, void * response_user_data)
{
    TraceI(FLAG_DATA_POINT, "SendRequest target=%s\n", target);
    unsigned long msg_id = ULONG_MAX;
    iUrl_t iurl_body = {0};

    int error;
    url_parser_url_t parsed_url = {0};

    if (NULL == strstr(target, "://"))
    {
        parsed_url.protocol = strdup(ADDR_BUS);
        while (*target != 0 && *target == '/') target++;
        if (*target == 0)
        {
            WARNING("SEND REQUEST: TARGET IS EMPTY");
            goto end;
        }
        parsed_url.path = strdup(target);
    }
    else
    {
        error = parse_url(target, true, &parsed_url);
        if (error != 0 || parsed_url.path == NULL)
        {
            WARNING("SEND REQUEST: Invalid URL \"%s\".\n", target);
            goto end;
        }
    }

    if (0 == strcmp (parsed_url.protocol, ADDR_BUS))
    {
        MESSAGE_CONFIG msgConfig = {0};
        char uri_buffer[200] = {0};
        char *bus_module_name = NULL;
        char *standard = NULL;
        char *uri = NULL;

        if (0 == check_url_start(parsed_url.path, strlen(parsed_url.path), (char *)"ibroker"))
        {
            if (!parse_iUrl_body(parsed_url.path, &iurl_body))
                LOG_GOTO("unable to parse the url path" ,end)
            // the clients can set protocol section in the url to "dev" for unknown type devices.
            // we will convert the url to known protocol for those devices we know.
            standard = iurl_body.standard;
            uri = parsed_url.path;

            // for /module/[:module name]/.. url, we will set the module name into the XKDEST tag in the bus message
            if (0 == strcmp (iurl_body.standard, "module"))
            {
                 uri = iurl_body.res_uri;
                 bus_module_name = iurl_body.device;
            }
            else if (0 == strcmp (iurl_body.standard, "dev"))
            {
                CClient *client = ClientManager().FindClient(iurl_body.device);
                if(client)
                {
                    if(!client->m_standard.empty())
                    {
                        standard = (char* )client->m_standard.c_str();
                        snprintf (uri_buffer, sizeof(uri_buffer), "/%s/%s%s", client->m_standard.c_str(), iurl_body.device, iurl_body.res_uri);
                        uri = &uri_buffer[0];
                    }
                }
            }
        }
        else
        {
            standard = (char*)"ibroker";
        }

        // send to coap endpoint
        if (0 == strcmp (standard, "ep") ||
            0 == strcmp (standard, "coap"))// Bugzilla-2431(1)
        {
            coap_packet_t request[1];
            msg_id = bh_gen_id(get_outgoing_requests_ctx());

            if (require_response || response_handler)
                coap_init_message((void *)request, COAP_TYPE_CON, msgcode, (uint16_t)msg_id);
            else
                coap_init_message((void *)request, COAP_TYPE_NON, msgcode, (uint16_t)msg_id);
            coap_set_header_uri_path(request, iurl_body.res_uri);
            coap_set_header_content_format(request, fmt);
            coap_set_payload(request, payload, payload_len);

            connection_t *conn = get_endpoint_conn((const char *) iurl_body.device);
            if(conn == NULL)
            {
                WARNING ("SEND REQUEST: connection to [%s] is NULL. ", iurl_body.device);
                goto end;
            }
            uip_ipaddr_t addr;
            memset(&addr, 0, sizeof(addr));
            addr.addr_type = A_Sock_Addr;
            memcpy(&addr.sock_addr, &conn->addr, conn->addrLen);
            addr.addr_len = conn->addrLen;
            //
            transaction_handle_for_coap_t * user_data = NULL;
            if(response_handler)
            {
            	user_data = (transaction_handle_for_coap_t* ) malloc(sizeof(transaction_handle_for_coap_t));
            	memset(user_data, 0, sizeof(*user_data));
            	user_data->response_handler = response_handler;
            	user_data->response_user_data = response_user_data;
            	response_user_data = NULL;

            	coap_nonblocking_request(g_endpoint_coap_ctx, &addr, request, coap_response_callback, user_data);
            }
            else
            {
            	coap_nonblocking_request(g_endpoint_coap_ctx, &addr, request, NULL, NULL);
            }
        }
#ifdef BUILTIN_IBROKER
        // send to icloud
        else if (0 == strcmp (standard, "ibroker")) // Bugzilla-2431(2)
        {
            if (g_cloud_status != iReady_For_Work)
            {
                int offset = check_url_start(parsed_url.path, strlen(parsed_url.path), (char *)"ibroker/dp/");
                if (offset)
                {
                    char new_uri[1024];
                    snprintf(new_uri, 1024, "/dp-cache/%s", parsed_url.path+offset);

                    if(!setup_bus_restful_message(&msgConfig, (char*)TAG_REST_REQ, fmt, new_uri, NULL, msgcode, payload, payload_len)) goto end;

                    msg_id = bh_gen_id(get_outgoing_requests_ctx());
                    char c_mid[32];
                    memset(c_mid, 0, 32);
                    snprintf (c_mid, 32, "%ld", msg_id);

                    set_bus_message_property(&msgConfig, XK_MID, c_mid);

                    struct timeval tv;
                    gettimeofday(&tv, NULL);
                    char c_time[64];
                    memset(c_time, 0, 64);
                    snprintf (c_time, 64, "%ld", tv.tv_sec * 1000 + tv.tv_usec / 1000);
                    set_bus_message_property(&msgConfig, XK_TM, c_time);

                    publish_message_cfg_on_broker(&msgConfig);
                }
                goto end;
            }
            // forward the request to the cloud and strip the "ibroker/"
            ilink_message_t contact;
            ilink_vheader_t vheader;
            vheader_node_t *node;
            int len;

            init_ilink_message(&contact, COAP_OVER_TCP);
            ilink_set_req_resp(&contact, 1, bh_gen_id(get_outgoing_requests_ctx()));

            memset(&vheader, 0, sizeof(vheader));
            if((node = vheader_find_node(&vheader, (char *)K_AGENT_ID, (bool)1, (bool)0)) == NULL)
            {
                WARNING ("SEND REQUEST: vheader_find_node failed");
                goto end;
            }
            char *self_id = get_self_agent_id();
            vheader_set_node_str(node, self_id, 1);

            ilink_set_vheader(&contact, &vheader);

            coap_packet_t request[1];
            char *response_buf = NULL;
            if (require_response)
                coap_init_message(request, COAP_TYPE_CON, msgcode, contact.msgID);
            else
                coap_init_message(request, COAP_TYPE_NON, msgcode, contact.msgID);
            int offset = check_url_start(parsed_url.path, strlen(parsed_url.path), (char *)"/ibroker");
            if (offset)
                coap_set_header_uri_path(request, parsed_url.path+offset);
            else
                coap_set_header_uri_path(request, parsed_url.path);
            coap_set_header_content_format(request, fmt);
            if (query)
                coap_set_header_uri_query(request, query);
            coap_set_payload_tcp(request, payload, payload_len);
            coap_set_token(request, (const uint8_t *) &contact.msgID, sizeof(contact.msgID));
            len = coap_serialize_message_tcp(request, (uint8_t **)&response_buf);

            ilink_set_payload(&contact, response_buf, len);
            ilink_msg_send(&contact);
            reset_ilink_message(&contact);
            vheader_destroy(&vheader);

            free(response_buf);
        }
#endif
        else // by default send it to the gateway bus. Bugzilla-2431(3)
        {
            char *tag = NULL;
            if (require_response)
                tag = (char *)TAG_REST_REQ;
            else
                tag = (char *)TAG_EVENT;

            msg_id = bh_gen_id(get_outgoing_requests_ctx());
            char c_mid[32];
            sprintf (c_mid, "%ld", msg_id);

            if(!setup_bus_restful_message(&msgConfig, tag, fmt, uri, NULL, msgcode, payload, payload_len)) goto end;

            if (bus_module_name)// Bugzilla-2431(4)
            {
                set_bus_message_property(&msgConfig, XK_DEST, bus_module_name);
            }

            set_bus_message_property(&msgConfig, XK_MID, c_mid);

            if(response_handler)
            {
                bh_wait_response_async(get_outgoing_requests_ctx(),
                		msg_id,
						(void *)response_handler,
						response_user_data,
                        2*1000,
                        NULL);
            }
            response_user_data = NULL;
			publish_message_cfg_on_broker(&msgConfig);
        }
    }
    // send to coap device
    else if (0 == strcmp (parsed_url.protocol, ADDR_COAP)) // Bugzilla-2431(5)
    {
        coap_packet_t request[1];
        msg_id = bh_gen_id(get_outgoing_requests_ctx());
        if (require_response || response_handler)
            coap_init_message((void *)request, COAP_TYPE_CON, msgcode, (uint16_t)msg_id);
        else
            coap_init_message((void *)request, COAP_TYPE_NON, msgcode, (uint16_t)msg_id);

        // token must be set for the java SDK
        coap_set_token(request, (const uint8_t*) &msg_id, sizeof(msg_id));
        coap_set_header_content_format(request, fmt);
        coap_set_payload_tcp(request, payload, payload_len);
        coap_set_header_uri_path(request,parsed_url.path);

        if (parsed_url.host_exists && parsed_url.port)
        {
            uip_ipaddr_t addr;
            memset(&addr, 0, sizeof(addr));
            addr.addr_type = A_Sock_Addr;
            addr.sock_addr.sin_family = AF_INET;
            addr.sock_addr.sin_addr.s_addr = inet_addr(parsed_url.host_ip);
            addr.sock_addr.sin_port = htons(parsed_url.port);
            addr.addr_len = sizeof(addr);

            transaction_handle_for_coap_t * user_data = NULL;
            if(response_handler)
            {
            	user_data = (transaction_handle_for_coap_t*) malloc(sizeof(transaction_handle_for_coap_t));
            	memset(user_data, 0, sizeof(*user_data));
            	user_data->response_handler = response_handler;
            	user_data->response_user_data = response_user_data;
            	response_user_data = NULL;

            	coap_nonblocking_request(g_endpoint_coap_ctx, &addr, request, coap_response_callback, user_data);
            }
            else
            {
            	coap_nonblocking_request(g_endpoint_coap_ctx, &addr, request, NULL, NULL);
            }
            //coap_nonblocking_request(g_endpoint_coap_ctx, &addr, resquest, coap_response_callback, response_user_data);
        }
        else
        {
            msg_id = ULONG_MAX;
            LOG_GOTO("coap device not available", end)
        }
    }
    else if (0 == strcmp (parsed_url.protocol, ADDR_OBS)) // Bugzilla-2431(6)
    {
        goto end;
    }

end:

    // ensure the response_user_data is freed if it not posted to the transaction
    if(response_user_data && response_handler)
    {
        response_handler(response_user_data, NULL, 0, T_Empty);
    }

    free_parsed_url(&parsed_url);
    free_iUrl_body(&iurl_body);
    return msg_id;
}




extern "C" int cb_refresher_request(void *ctx_data, void *data, int len, unsigned char format)
{
    int content_format, payload_len;
    char *payload;
    CRefresherPoint *point = NULL;
    char mid[100]={0};
    uint32_t ms_used;
    context_refresher_request_t *ctx = (context_refresher_request_t*) ctx_data;

    point = CRefresherPoint::FindPoint(ctx->refresher_id);
    if(point == NULL)
    {
        ERROR("Refresh response returned for [%d], but can't find refresher point ",  ctx->refresher_id);
        goto end;
    }

    ms_used = get_elpased_ms(&ctx->request_time_ms);

    point->m_read_in_progerss = 0;

    if(data == NULL)
    {
        TraceI(FLAG_DATA_POINT, "Refresher request timeout in %d ms,  url=[%s], len=%d",
                ms_used, point->m_resource_url.c_str(), len);
        if(ctx->observing) point->m_observe = CRefresherPoint::Obs_Failed;
        point->m_read_fails ++;
        goto end;
    }
    else if(T_Coap_Raw  == format)
    {
        coap_packet_t coap_message[1];
        int coap_error_code = coap_parse_message(coap_message, (uint8_t *)data, len);

        if(coap_error_code != NO_ERROR) {
            WARNING("%s, parse coap error (%d)", __FUNCTION__, coap_error_code);
            goto end;
        }
        content_format = coap_message->content_format;
        payload_len = coap_message->payload_len;
        payload = (char *)coap_message->payload;
        sprintf(mid, "%d", coap_message->mid);
    }
    else if(T_Broker_Message_Handle ==  format)
    {
        // todo: implement it
        CONSTMAP_HANDLE properties = Message_GetProperties((MESSAGE_HANDLE) data); /*by contract this is never NULL*/
        const char *format = ConstMap_GetValue(properties, XK_FMT);
        const char *msg_id = ConstMap_GetValue(properties, XK_MID);
        mid[0] = 0;
        if(msg_id)
        {
            strncpy(mid, msg_id, sizeof(mid));
        }

        if(format !=NULL)
            content_format= atoi(format);
        else
            content_format = IA_TEXT_PLAIN; //default
        ConstMap_Destroy(properties);

        const CONSTBUFFER *content = Message_GetContent((MESSAGE_HANDLE)data);
        payload_len = content->size;
        payload = (char *)content->buffer;
    }
    else
    {
        WARNING("Refresh response returned for device [%s], res [%s] , but format [%d] not supported  ",
                point->m_client_id.c_str(), point->m_resource_url.c_str(), format);
        assert (1);
    }

    if(payload_len == 0)
    {
        point->m_read_fails ++;
        TraceI(FLAG_DATA_POINT, "Refresher response (mid=%s) returned in %d ms. No payload, url=[%s], fails=%d",
                 mid, ms_used, point->m_resource_url.c_str(), point->m_read_fails);
        goto end;
    }

    TraceI(FLAG_DATA_POINT, "Refresher response (mid=%s) returned in %d ms.url=%s, lenth=%d",
             mid, ms_used, point->m_resource_url.c_str(), len);

    point->m_read_fails = 0;

    point->OnRefresherData(content_format, payload, payload_len);

    if(ctx->observing)
    {
        point->m_observe = CRefresherPoint::Obs_Success;
        WARNING("Refresh point for device [%s], res [%s] is in observing mode ",
                point->m_client_id.c_str(), point->m_resource_url.c_str());
    }

end:
    if (T_Broker_Message_Handle == format && data)
    {
        Message_Destroy((MESSAGE_HANDLE)data);
    }

    trans_free_ctx(ctx_data);

    return 0;
}



/// there are three message sources:
/// 1. from gateway message bus
/// 2. from coap client
/// 3. from ilink
/// 4. from the response feedback which can be in gw bus, coap or ilink


extern "C" dlist_entry_ctx_t g_internal_queue;
typedef void (*handler)(void *msg);
void agent_post_message_handler(void *handler, void *message)
{
    dlist_post(&g_internal_queue, T_Message_Handler, message, handler);
    wakeup_ports_thread();
}

// post the callback to ports handler thread for avoiding thread confliction
extern "C" int post_cb_rd_thread(callback_t *cb)
{
    if(cb->format == T_Broker_Message_Handle)
        cb->data = Message_Clone((MESSAGE_HANDLE)cb->data);
    else if(cb->format != T_Empty)
    {
        WARNING("Fomrat %d is Not broker message handle in the post_cb_rd_thread", cb->format);
    }
    dlist_post(&g_internal_queue, T_Callback, cb, NULL);
    wakeup_ports_thread();

    return 0;
}

extern "C" void check_internal_message()
{
    while(1)
    {
        dlist_node_t *msg =  dlist_get(&g_internal_queue);
        if(msg == NULL)
            return;
        if(msg->type == T_Bus_Message)
        {
            //
        }

        free(msg);
    }
}

extern "C" void ResRefresherExpiry(uint32_t *next_expiry_ms)
{
    uint32_t timeout;

    ResRefresher().RunRefresh();

    tick_time_t expiry =  ResRefresher().GetNearExpiry();

    if (expiry == -1)
        return;

    timeout = (expiry - bh_get_tick_sec()) * 1000;

    if(timeout < *next_expiry_ms )
        *next_expiry_ms = timeout;
}


#if 0
int ReadResource(char *device, char *res, void *cb, bool observe)
{
    CClient *client = ClientManager().FindClient(device);
    if(client == NULL || client->m_standard == "ilink" ||
            client->m_standard == "agent")
        return -1;

    context_refresher_request_t *ctx = (context_refresher_request_t*)
        trans_malloc_ctx(sizeof(context_refresher_request_t));
    memset(ctx, 0, sizeof(context_refresher_request_t));

    ctx->device = strdup(device);
    ctx->res = strdup(res);
    ctx->observing = observe;

    if(client->m_standard == "ep")
    {
        char sendbuf[COAP_MAX_PACKET_SIZE];
        coap_packet_t message[1];

        coap_init_message((void *)message, COAP_TYPE_CON, (uint8_t)COAP_GET,
                (uint16_t)bh_gen_id(get_outgoing_requests_ctx()));
        coap_set_header_uri_path(message, res);
        if(observe) coap_set_header_observe (message, 1);
        int len = coap_serialize_message((void *)message, (uint8_t *)sendbuf);

        //
        bh_wait_response_async(get_outgoing_requests_ctx(),
                message->mid,
                cb,
                ctx,
                10*1000,
                NULL);

        // todo: need to setup the connection
        connection_send(client->GetConnection(), (uint8_t *)sendbuf, (size_t)len);
    }
    else
    {
        MESSAGE_CONFIG msgConfig;
        char url[128];
        sprintf(url, "/%s/%s%s", client->m_standard.data(), client->m_epname.data(), res);
        if(!setup_bus_restful_message(&msgConfig, (char *)TAG_REST_REQ, -1, url, NULL, COAP_GET, NULL, 0))
            return -1;

        unsigned long mid = bh_gen_id(get_outgoing_requests_ctx());
        char buffer[50];
        sprintf(buffer, "%ld", mid);
        set_bus_message_property(&msgConfig, XK_MID, buffer);

        bh_wait_response_async(get_outgoing_requests_ctx(),
                mid,
                cb,
                ctx,
                10*1000,
                (void *)post_cb_rd_thread);

        publish_message_cfg_on_broker(&msgConfig);
    }


    return 0;
}
#endif

static int handle_data_observing_json(JSON_Value *json, char *purl_prefix)
{
    fresher_para_t param;
    int id=-1, interval;
    const char *device, *res, *purl, * rt;
    int sequence = -1;
    char buffer[256];

    memset(&param, 0, sizeof(param));

     if (json == NULL) LOG_RETURN(-1)

    JSON_Object *obj = json_value_get_object(json);
    if( obj== NULL) LOG_GOTO("observing payload was NULL", end);

    device = json_object_get_string(obj, "di");
    purl = json_object_get_string(obj, "purl");
    if(purl == NULL ||  *purl == 0)  LOG_GOTO("No publish url info", end);
    if(device == NULL || *device == 0)  LOG_GOTO("no device info", end);


    rt = json_object_get_string(obj, "rt");
    res = json_object_get_string(obj, "ri");

    if(res != NULL && *res != 0 && rt != NULL)
    {
    	TraceI(FLAG_DATA_POINT, "creating refresher point, rt is [%s] in the request, but res is also set to [%s]. rt is discarded",
    			rt, res);
    	rt = NULL;
    }

    interval = (int)get_json_number_safe(obj, "interval");

    if (interval <= 0)
        interval = 10;   //default interval is ten seconds

	if(purl_prefix && purl_prefix[0])
	{
		if(strstr (purl, "://") == NULL)
		{
			if(purl[0] == '/') purl ++;
			snprintf(buffer, sizeof(buffer), "%s%s%s", purl_prefix, purl[0]=='/'?"":"/",purl);
			purl=buffer;
		}
	}

	param.sequence = (int)get_json_number_safe(obj, "sequence");
	if(param.sequence < 0 ) param.sequence = 0;
	param.process = (char*) json_object_get_string(obj, "process");
	param.min_duration = interval;
	param.publish_point = (char*) purl;

    TraceI(FLAG_DATA_POINT, "handle_data_observing payload. device=%s, res=%s, interval=%d, purl=%s, seq=%d, process=%s",
        device, res, interval, purl, param.sequence, param.process?param.process:"");

	if(res == NULL || * res == 0)
	{
		 id =  ResRefresher().NewRefresherPointforRT(device, rt, &param);
	}
	else
	{
		id = AddResourceRefresher((char *)device, (char *)res, &param);
	}

end:
    return id;
}


extern "C" int handle_data_observing(const char *playload, char *purl_prefix)
{
    JSON_Value *json = json_parse_string( playload);
    int ret = handle_data_observing_json(json, purl_prefix);
    if(json)json_value_free(json);

    return ret;

}

extern "C" void load_configured_data_monitors()
{
    char *content = NULL;
    char ini_path[MAX_PATH_LEN] = {0};
    char *path = get_product_config_pathname((char*)"data_monitors.cfg", (char*)TT_DEVICE, (char*)NULL, ini_path);

    load_file_to_memory(path, &content);

    if(content == NULL)
    	return;

    TraceI(FLAG_DEVICE_REG, "loading data_monitors.cfg for static data monitors");
    JSON_Array *monitors;
    JSON_Value *root_value = json_parse_string(content);
    free(content);

    if (root_value == NULL)
    {
        LOG_MSG("data_monitors.cfg is not valid json format");
        return;
    }

    if (json_value_get_type(root_value) != JSONArray)
    {
    	WARNING("data_monitors.cfg is json array format");
    	goto end;
    }

    monitors = json_value_get_array(root_value);
	for (int i = 0; i < (int)json_array_get_count(monitors); i++)
	{
		JSON_Value  * one_monitor = json_array_get_value(monitors, i);
		handle_data_observing_json(one_monitor, NULL);
	}

end:
    json_value_free(root_value);

}
