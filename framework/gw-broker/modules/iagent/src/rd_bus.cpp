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
#ifdef _CRTDBG_MAP_ALLOC
#include <crtdbg.h>
#endif
#include <parson.h>

#include "module.h"
#include "azure_c_shared_utility/threadapi.h"
#include "azure_c_shared_utility/lock.h"

#include "broker_rest_convert.h"
#include "iagent.h"
#include "iagent_base.h"
#include "agent_core_lib.h"
#include "rest-engine.h"

#include "rd.h"
#include "message.h"


/*
 *  NOTE: All functions in this file are executed in the ports_handler thread
 */


extern "C" void handle_bus_rd_event(MESSAGE_HANDLE messageHandle)
{
	CONSTMAP_HANDLE properties = Message_GetProperties(messageHandle); /*by contract this is never NULL*/
	CONSTBUFFER * content = (CONSTBUFFER *)Message_GetContent(messageHandle);
	const char* src = ConstMap_GetValue(properties, XK_SRC);
    const char* mid = ConstMap_GetValue(properties, XK_MID);
    char source[200];
    char* payload;
	bool result = false;

	bool alloc = get_payload( content, &payload);
	snprintf(source, sizeof(source), ADDR_BUS "://%s", src?src:"unknown");
	if(payload !=NULL) result |= handle_rd_post(payload, source);
	if(alloc) free(payload);

	//response
    MESSAGE_CONFIG msgConfig = {0};
    int reponse_code;
    if (result == false) reponse_code = NOT_IMPLEMENTED_5_01;
    else reponse_code = CONTENT_2_05;

    if(!setup_bus_restful_message(&msgConfig, (char *)TAG_REST_RESP, -1, NULL, NULL, reponse_code, NULL, 0)) goto end;
    set_bus_message_property(&msgConfig, XK_MID, mid);
    set_bus_message_property(&msgConfig, XK_DEST, src);

    publish_message_cfg_on_broker(&msgConfig);

end:
    ConstMap_Destroy(properties);
    Message_Destroy(messageHandle);
}


extern "C" void handle_bus_rd_get(MESSAGE_HANDLE messageHandle)
{

    CONSTMAP_HANDLE properties = Message_GetProperties(messageHandle); /*by contract this is never NULL*/
    const char* query = ConstMap_GetValue(properties, XK_QUERY);
    const char* mid = ConstMap_GetValue(properties, XK_MID);
    const char* src = ConstMap_GetValue(properties, XK_SRC);
    char *response_payload=NULL;

    handle_rd_get(query, &response_payload, true);

//response
    MESSAGE_CONFIG msgConfig = {0};
    int reponse_code;
    if (response_payload == NULL) reponse_code = NOT_IMPLEMENTED_5_01;
    else reponse_code = CONTENT_2_05;

    if(!setup_bus_restful_message(&msgConfig, (char *)TAG_REST_RESP, IA_APPLICATION_JSON, NULL, NULL, reponse_code, (void *)response_payload, strlen(response_payload))) goto end;
    set_bus_message_property(&msgConfig, XK_MID, mid);
    set_bus_message_property(&msgConfig, XK_DEST, src);

    publish_message_cfg_on_broker(&msgConfig);

end:

	ConstMap_Destroy(properties);
	Message_Destroy(messageHandle);

    if (response_payload)
        free (response_payload);
}



extern "C" void handle_bus_rd_delete(MESSAGE_HANDLE messageHandle)
{
    CONSTMAP_HANDLE properties = Message_GetProperties(messageHandle);
	CONSTBUFFER * content = (CONSTBUFFER *)Message_GetContent(messageHandle);
    const char* mid = ConstMap_GetValue(properties, XK_MID);
    const char* src = ConstMap_GetValue(properties, XK_SRC);
    const char* query = ConstMap_GetValue(properties, XK_QUERY);
	bool result = false;

	if(query !=NULL) result |= handle_rd_delete(query);

//response
    MESSAGE_CONFIG msgConfig = {0};
    int reponse_code = result? DELETED_2_02:NOT_FOUND_4_04;

    if(!setup_bus_restful_message(&msgConfig, (char *)TAG_REST_RESP, IA_APPLICATION_JSON, NULL, NULL, reponse_code, NULL, 0)) goto end;
    set_bus_message_property(&msgConfig, XK_MID, mid);
    set_bus_message_property(&msgConfig, XK_DEST, src);

    publish_message_cfg_on_broker(&msgConfig);

end:
	ConstMap_Destroy(properties);
	Message_Destroy(messageHandle);

}




extern "C" void handle_bus_rd_monitor_post(MESSAGE_HANDLE messageHandle)
{
	CONSTMAP_HANDLE properties = Message_GetProperties(messageHandle); /*by contract this is never NULL*/
	CONSTBUFFER * content = (CONSTBUFFER *)Message_GetContent(messageHandle);
    const char* mid = ConstMap_GetValue(properties, XK_MID);
    const char* src = ConstMap_GetValue(properties, XK_SRC);
	char *payload = NULL;
	bool alloc = get_payload(content, &payload);

    char buffer[256] = {0};
    if(src) sprintf(buffer,"gwbus://module/%s/", src);
	
	int id = handle_rd_monitor_put(payload, buffer);

    //response
    MESSAGE_CONFIG msgConfig = {0};
    int reponse_code;
    int payload_len = 0;
    if (id == -1)
    {
    	reponse_code = BAD_REQUEST_4_00;
    }
    else
    {

        sprintf(buffer, "%d", id);
        payload_len = strlen(buffer)+1;
        reponse_code = CHANGED_2_04;
    }


    if(!setup_bus_restful_message(&msgConfig,
    		(char *)TAG_REST_RESP, payload_len == 0 ? -1 : IA_TEXT_PLAIN,
    		NULL, NULL, reponse_code, payload_len==0?NULL:buffer, payload_len))
    	return;

    set_bus_message_property(&msgConfig, XK_MID, mid);
    set_bus_message_property(&msgConfig, XK_DEST, src);

    publish_message_cfg_on_broker(&msgConfig);

    if(id != -1)
    	do_rd_monitor_scan(id);


	if (alloc) free(payload);
	ConstMap_Destroy(properties);
	Message_Destroy(messageHandle);


}

extern "C" void handle_bus_calibration(MESSAGE_HANDLE messageHandle)
{
	CONSTMAP_HANDLE properties = Message_GetProperties(messageHandle); /*by contract this is never NULL*/
	CONSTBUFFER * content = (CONSTBUFFER *)Message_GetContent(messageHandle);
	const char* src = ConstMap_GetValue(properties, XK_SRC);
	const char* sfmt = ConstMap_GetValue(properties, XK_FMT);
	const char* action = ConstMap_GetValue(properties, XK_ACTION);
	const char* mid = ConstMap_GetValue(properties, XK_MID);
	const char* uri = ConstMap_GetValue(properties, XK_URI);
    MESSAGE_HANDLE BrokerMessage = NULL;
    MESSAGE_CONFIG msgConfig = {0};
    int response_code;

	if(strcmp(action, ACTION_POST) == 0)
	{
		char * new_payload = NULL;

		int len = calibrate((char *)uri, 1/*sfmt*/, (char *)content->buffer, (int)content->size, &new_payload);
		if(len == 0)
		{
			response_code = NOT_IMPLEMENTED_5_01;

		}
		else
		{
			response_code = CONTENT_2_05;
		}

	    if(!setup_bus_restful_message(&msgConfig,
	    		(char *)TAG_REST_RESP, -1,
	    		NULL, NULL, response_code, len==0?NULL:new_payload, len))
	    	return;
	    set_bus_message_property(&msgConfig, XK_MID, mid);
	    set_bus_message_property(&msgConfig, XK_DEST, src);

	    publish_message_cfg_on_broker(&msgConfig);

		if (new_payload!=NULL) free(new_payload);
	}
    ConstMap_Destroy(properties);
	Message_Destroy(messageHandle);

}


extern "C" void handle_bus_ibroker(MESSAGE_HANDLE messageHandle, char * url, char * src_module, char *tm)
{
	coap_packet_t coap_message[1];
	if(!convert_bus_msg_to_coap(messageHandle, coap_message))
	{
	    WARNING ("Can not conver bus msg to coap\n");
        return;
	}

    TraceI(FLAG_CLOUD_MSG, "\t ibroker to bus url=%s\n", url);
	if(url) coap_set_header_uri_path(coap_message, url);
	send_bus_request_to_ibroker(coap_message,  src_module, tm);
}

extern "C" void handle_bus_endpoint(MESSAGE_HANDLE messageHandle, char * epname, char * url, uint32_t mid, char * src_module)
{
	coap_packet_t coap_message[1];
	if(!convert_bus_msg_to_coap(messageHandle, coap_message))
    {
        WARNING ("Can not conver bus msg to coap\n");
        return;
    }

	if(url)
		coap_set_header_uri_path(coap_message, url);

	// send the request to the endpoint
	send_bus_request_to_ep(epname, coap_message, mid, src_module);
}

/*
Rrefresher Content  PAYLOAD:
{
"device":"",
"res": "",
"interval": 30,
"requester": "",
"purl": ""		// publish url
}
*/

extern "C" void handle_refresher_data(MESSAGE_HANDLE messageHandle)
{
	CONSTMAP_HANDLE properties = Message_GetProperties(messageHandle); /*by contract this is never NULL*/
	CONSTBUFFER * content = (CONSTBUFFER *)Message_GetContent(messageHandle);
	const char *action = ConstMap_GetValue(properties, XK_ACTION);
	const char* src = ConstMap_GetValue(properties, XK_SRC);
	const char* mid = ConstMap_GetValue(properties, XK_MID);
	char *playload=NULL;
	bool alloc =get_payload(content, &playload);
	int id=-1;
	char buffer[256] = {0};
	int payload_len;
    MESSAGE_CONFIG msgConfig = {0};
    iUrl_t i_url = {0};

    //create new refresher point
	if(strcmp(action, ACTION_PUT) == 0)
	{
		 if(src) sprintf(buffer,"gwbus://module/%s/", src);
	     id= handle_data_observing (playload, buffer);
	     int reponse_code;
	     int payload_len = 0;
	     if (id == -1)
	     {
	     	reponse_code = BAD_REQUEST_4_00;
	     }
	     else
	     {

	         sprintf(buffer, "%d", id);
	         payload_len = strlen(buffer)+1;
	         reponse_code = CHANGED_2_04;
	     }


	     if(!setup_bus_restful_message(&msgConfig,
	     		(char *)TAG_REST_RESP, payload_len == 0 ? -1 : IA_TEXT_PLAIN,
	     		NULL, NULL, reponse_code, payload_len==0?NULL:buffer, payload_len))

	     	goto end;

	     set_bus_message_property(&msgConfig, XK_MID, mid);
	     set_bus_message_property(&msgConfig, XK_DEST, src);
	     publish_message_cfg_on_broker(&msgConfig);
	}

	//handle data report
	else if(strcmp(action, ACTION_POST) == 0)
	{

		const char* uri = ConstMap_GetValue(properties, XK_URI);
		const char* fmt_str = ConstMap_GetValue(properties, XK_FMT);
		if ((uri ==NULL)||(fmt_str==NULL)) goto end;
	    int fmt = atoi(fmt_str);
		if(!parse_iUrl_body((char*)uri, &i_url))
	        LOG_GOTO("parse_iUrl_body failed", end)

		OnDataPoint((char *)i_url.device, i_url.res_uri, fmt,(char*)content->buffer,content->size);
	}
	else if (strcmp(action, ACTION_DEL))
	{
        if(src) sprintf(buffer,"gwbus://module/%s/", src);
        bool ret = 0; //handle_data_observing_cancel();
        int reponse_code;
        int payload_len = 0;
        if (ret)
        {
            reponse_code = CHANGED_2_04;
        }
        else
        {
            reponse_code = BAD_REQUEST_4_00;
        }

        set_bus_message_property(&msgConfig, XK_MID, mid);
        set_bus_message_property(&msgConfig, XK_DEST, src);
        publish_message_cfg_on_broker(&msgConfig);
	}

end:
    free_iUrl_body(&i_url);
	if(alloc) free(playload);
	ConstMap_Destroy(properties);
	Message_Destroy(messageHandle);

}


extern "C" void handle_data_point(MESSAGE_HANDLE messageHandle)
{
	CONSTMAP_HANDLE properties = Message_GetProperties(messageHandle); /*by contract this is never NULL*/
	CONSTBUFFER * content = (CONSTBUFFER *)Message_GetContent(messageHandle);

	const char* uri = ConstMap_GetValue(properties, XK_URI);
	const char* fmt_str = ConstMap_GetValue(properties, XK_FMT);
	iUrl_t i_url = {0};
	int fmt;
	if ((uri ==NULL)||(fmt_str==NULL)) goto end;
	fmt = atoi(fmt_str);



	if(strncmp(uri, "/refresher/", 11) == 0)
		uri += 11;
	else if(strncmp(uri, "/dp/", 4) == 0)
		uri += 4;
	else
		goto end;

	if(!parse_iUrl_body((char*)uri, &i_url))
		LOG_GOTO("parse_iUrl_body failed", end)

	OnDataPoint((char *)i_url.device, i_url.res_uri, fmt,(char*)content->buffer,content->size);

end:
	free_iUrl_body(&i_url);
	ConstMap_Destroy(properties);
	Message_Destroy(messageHandle);

}

extern "C" void handle_bus_default(MESSAGE_HANDLE messageHandle)
{
    MESSAGE_HANDLE brokerMessage = NULL;
	CONSTMAP_HANDLE properties = Message_GetProperties(messageHandle); /*by contract this is never NULL*/

	//const CONSTBUFFER * content = Message_GetContent(messageHandle);
	coap_packet_t coap_message[1];
	char  response_buf[COAP_MAX_PACKET_SIZE];
	int len=0;
    coap_status_t coap_error_code = NO_ERROR;
    coap_packet_t response[1];


	const char* mid = ConstMap_GetValue(properties, XK_MID);
	const char* src = ConstMap_GetValue(properties, XK_SRC);

	if(!convert_bus_msg_to_coap(messageHandle, coap_message))
		LOG_GOTO("convert_bus_msg_to_coap failed", end)

	if(serve_request(coap_message, response_buf, &len))
	{
	    coap_error_code = coap_parse_message(response,
	    		(uint8_t* )response_buf, len);

	    if (coap_error_code != NO_ERROR)
	    {
	    	return;
	    }

	}
	brokerMessage = coap_to_bus_msg(response, atoi(mid), src);
	if(brokerMessage!=NULL)
	{
	      publish_message_on_broker(brokerMessage);
	      Message_Destroy(brokerMessage);
	}

end:
	ConstMap_Destroy(properties);

}
