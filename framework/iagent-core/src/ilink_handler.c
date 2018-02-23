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


//iagent
#include "iagent_base.h"
#include "ilink_message.h"
#include "rd.h"

//external
#ifdef RUN_ON_LINUX
#include "er-coap-constants.h"
#include "er-coap.h"
#include "rest-engine.h"
#endif

sync_ctx_t* g_ilink_ctx = NULL;




sync_ctx_t* get_outgoing_requests_ctx()
{
	return g_ilink_ctx;
}

char * get_key_value(ilink_message_t * msg, char * key)
{
    vheader_node_t  node;
    memset(&node, 0, sizeof(node));

    if(vheader_raw_find_key(&node, key, msg->vheader, msg->vheader_len, 1) == 0)
    {
        if(node.type == T_Str)
        {
            return node.value.s_val;
        }
    }
    return NULL;
}

bool get_key_value_i(ilink_message_t * msg, char * key, int *value)
{
    vheader_node_t  node;
    memset(&node, 0, sizeof(node));

    if(vheader_raw_find_key(&node, key, msg->vheader, msg->vheader_len, 1) == 0)
    {
        if(node.type == T_Long)
        {
            *value = node.value.i_val32;
            return true;
        }
    }
    return false;
}



int send_to_sub_agent(char* agent, recv_context_t* message, int len)
{
    return 0;
}



int handle_type_iagent(ilink_message_t *message)
{

	char * tag = get_key_value(message, K_TAG);

	if(tag == NULL) return 0;

	if(strncmp(tag, "handshake",9) == 0)
	{
		on_handshake_message(message);
	}
	else if(strcmp(tag, TAG_KEY_PROVISION) == 0)
	{
		on_prov_message(message);
	}
	else if(strcmp(tag, TAG_PING) == 0)
	{
		on_ping_message(message);
	}
	return 0;
}



static void handle_cloud_coap_refresher_request(coap_packet_t* coap_message, ilink_message_t *ilink_msg)
{
    coap_packet_t response[1];
    char buffer[256];
    char* packet=NULL;

    strcpy(buffer,"/ibroker/");// gwbus:/

    int  id= handle_data_observing(coap_message->payload,  buffer);
    if (COAP_POST == coap_message->code)
    {
        if(id == -1)
        {
            TraceI(FLAG_CLOUD_MSG, "handle_data_observing: add refresher failed. return id==-1");
            coap_init_message(response, COAP_TYPE_ACK, INTERNAL_SERVER_ERROR_5_00, coap_message->mid);
        }
        else
        {
            coap_init_message(response, COAP_TYPE_ACK, CHANGED_2_04, coap_message->mid);
            sprintf(buffer, "%d", id);
            int payload_len = strlen(buffer)+1;
            coap_set_payload_tcp(response,buffer,payload_len);
            coap_set_header_content_format(response, IA_TEXT_PLAIN);
        }
    }
    else if (COAP_DELETE == coap_message->code)
    {
        if(!id)
        {
            TraceI(FLAG_CLOUD_MSG, "handle_data_observing:delete refresher failed. return id==-1");
            coap_init_message(response, COAP_TYPE_ACK, INTERNAL_SERVER_ERROR_5_00, coap_message->mid);
        }
        else
        {
            coap_init_message(response, COAP_TYPE_ACK, CHANGED_2_04, coap_message->mid);
        }
    }

    if(coap_message->token_len>0)
    {
        coap_set_token(response, coap_message->token, coap_message->token_len);
    }
    else if(ilink_msg->has_mid)
    {
        coap_set_token(response, (const uint8_t*)&ilink_msg->msgID, sizeof(ilink_msg->msgID));
    }

    packet = NULL;
    size_t packet_len = coap_serialize_message_tcp(response, (uint8_t **)&packet);
    if(packet_len)
    {
        ilink_message_t  msg;
        init_ilink_message(&msg, COAP_OVER_TCP);
        msg.has_mid = ilink_msg->has_mid;
        msg.is_req = 0;
        msg.msgID = ilink_msg->msgID;
        ilink_set_payload(&msg, packet, packet_len);
        ilink_msg_send(&msg);
        reset_ilink_message(&msg);
        free(packet);
        TraceI(FLAG_CLOUD_MSG, "handle_cloud_coap_refresher_request response packet_len=%d, response to ibroker!", packet_len);
    }
    else
    {
        TraceI(FLAG_CLOUD_MSG, "handle_cloud_coap_refresher_request response packet_len=0, no response to ibroker!");
    }
}


int handle_type_coap(recv_context_t *message)
{
	ilink_message_t *ilink_msg = &message->ilink_msg;

	// parse the coap payload
    coap_status_t coap_error_code = NO_ERROR;
    coap_packet_t coap_message[1];
    char* response_buf=NULL;
    int len = 0;

    coap_error_code = coap_parse_message_tcp(coap_message, ilink_msg->payload, ilink_msg->payload_len);

	if(coap_error_code != NO_ERROR) {
	    ERROR("parse coap message from ibroker failed. error_code=%d\n", coap_error_code);
        ERROR("leading=%x, msgID=%d, msgType=%d, has_vheader=%d,\n p_len=%d, p=%s\n",
                ilink_msg->leading_byte,
                ilink_msg->msgID,
                ilink_msg->msgType,
                ilink_msg->has_vheader,
                ilink_msg->payload_len,
                ilink_msg->payload);
		return 0;
	}

	// 0xCE is only responded when 0xFA was in the request.
	// Agent only add 0xFA if it is coap request from client
	// forward when it is response to external coap client
	if(ilink_msg->leading_byte == 0xCE )
	{
		bh_feed_response(get_outgoing_requests_ctx(), ilink_msg->msgID,
				coap_message,
				sizeof(char *), T_Coap_Parsed);
		return 0;
	}


	// handle coap response
	uint8_t code = coap_message->code;
	if(ilink_msg->leading_byte == 0xBA &&
			(code < COAP_GET || code >COAP_DELETE))
	{
		if(coap_message->token_len != sizeof(uint32_t))
			return 0;
		uint32_t id = *((uint32_t*)coap_message->token);
		bh_feed_response(get_outgoing_requests_ctx(), id,
				coap_message,
				sizeof(char *), T_Coap_Parsed);
		return 0;
	}

    assert (coap_message->code >= COAP_GET && coap_message->code <= COAP_DELETE);
    const char *url = NULL;
    int url_len;

	char * ep = get_key_value(ilink_msg, K_EP);

    url_len = coap_get_header_uri_path(coap_message, &url);
    char url_str[256];

    if(url == NULL || (url_len+1) >= sizeof(url_str))
    {
    	WARNING("coap from cloud url illegal. url len: %d", url_len);
    	return -2;
    }

    memset(url_str,0,256);
    memcpy(url_str,url,url_len);
    url = &url_str[0];

    TraceI(FLAG_CLOUD_MSG, "coap request from cloud, url is [%s]\n", url_str);


    // if the cloud wants to access the device by its alias name (local id under gateway),
    // it must provide the "/iagent/[iagent id]" as prefix in the url
    char iagent_id[64];
    snprintf(iagent_id, sizeof(iagent_id), "iagent/%s", get_self_agent_id());
    if(strncmp(url, iagent_id, strlen(iagent_id)) == 0)
    {
        url = url + strlen(iagent_id);
        coap_set_header_uri_path(coap_message, url);
        url_len = coap_get_header_uri_path(coap_message, &url);
    }

    // try to replace the "dev" with actually standard, such as "modbus",
    // it mostly help the plugin like modbus to filter the request it cared.
    if(check_url_start(url, url_len, "dev/")) // Bugzilla-2429
    {
        check_url_generic_dev(coap_message, url_str, sizeof(url_str));
        url_len = coap_get_header_uri_path(coap_message, &url);
    }


    if(check_url_start(url, url_len, "refresher"))
    {
    	handle_cloud_coap_refresher_request(coap_message, ilink_msg);
    	return 0;
    }

    else if(check_url_start(url, url_len, "ep/"))
    {
    	char * id_end = strchr(url+3, '/');
    	if(id_end == NULL)
    		return -1;
    	*id_end = 0;
    	coap_set_header_uri_path(coap_message, id_end+1);
    	send_ilink_request_to_ep((char*)(url+3), ilink_msg, coap_message);
    	return 0;
    }
    else if(ep)
	{
    	assert(strncmp(url, "ep/", 3) != 0);

    	// forward if it is request for my endpoint
		send_ilink_request_to_ep(ep, ilink_msg, coap_message);
		return 0;
	}
    else if(check_url_start(url, url_len,  "rd/"))
    {
    //	handle_cloud_coap_rd_request(coap_message);

    }

    // this is built-in service request

    else if(serve_request_from_tcp(coap_message, &response_buf, &len))
	{
		if(len)
		{
            ilink_message_t buildin_res;
            ilink_vheader_t vheader;


            init_ilink_message(&buildin_res, COAP_OVER_TCP);
            if(ilink_msg->has_mid)
            	ilink_set_req_resp(&buildin_res, 0, ilink_msg->msgID);

            ilink_set_payload(&buildin_res, response_buf, len);
            vheader_init(&vheader, 1, 0);
            char *self_id = get_self_agent_id();
            vheader_set_value_s(&vheader, K_AGENT_ID, self_id);

            ilink_set_vheader(&buildin_res, &vheader);

            ilink_msg_send(&buildin_res);

            vheader_destroy(&vheader);
            reset_ilink_message(&buildin_res);

            TraceI(FLAG_CLOUD_MSG, "\t send response back to ibroker\n");
            if(response_buf!=NULL)
            	free(response_buf);
		}
	}
#ifdef RUN_AS_BROKER_MODULE
	else
	{
		// forward to gw-broker for extension service
		publish_cloud_request_to_gw_bus(ilink_msg, coap_message);
		TraceI(FLAG_CLOUD_MSG, "\t forwarded to the gateway bus\n");
	}
#endif
	return 0;
}

int handle_ilink_message(recv_context_t *message)
{
	ilink_message_t *ilink_msg = &message->ilink_msg;
	int len = 0, result = 0;
    char * agent;

	// only when handshake completed, non-iagent message type can be accepted
	if(ilink_msg->msgType != INTEL_IAGENT && !handshake_done())
	{
	    WARNING("recieved message type [%d] from ibroker, but handshake not done yet\n", ilink_msg->msgType);
		return -1;
	}


	// forward to sub agent without depack the package
	if (NULL == ilink_msg->vheader)
	{
	    WARNING ("vheader in ilink message is NULL.mid=%d, msgType=%d, is_req=%d, payloadLen=%d, payload=%s\n",
	            ilink_msg->msgID, ilink_msg->msgType, ilink_msg->is_req, ilink_msg->payload_len, ilink_msg->payload);
	    return -1;
	}

	agent  = get_key_value(ilink_msg, K_AGENT_ID);
	if(agent&& !is_my_agent_id(agent))
	{
	    return send_to_sub_agent(agent, message, len);
	}

	switch(ilink_msg->msgType)
	{
	case COAP_OVER_TCP:
	    result = handle_type_coap(message);
	    break;

	case HTTP_OVER_TCP:
	    result = 0;
	    break;

	case TCP_UDP_TUNNEL:
	    result = 0;
	    break;

	case TCF_TUNNEL:
	    result = 0;
	    break;

	case INTEL_IAGENT:
	    result = handle_type_iagent(ilink_msg);
	    break;

	case EVENT_DATA:
	    result = 0;
	    break;

	case COMMENT_SETTING:
	    result = 0;
	    break;

	default:
	    result = -1;
	    break;
	}

	return result;

}
