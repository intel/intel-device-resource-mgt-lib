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

#include <stdio.h>
#include <stdint.h>





#if 0

void cache_json_payload(char *buffer, int len,
        lwm2m_uri_t *uriP,
        uint16_t clientID)
{

    Json::Reader reader;
    Json::Value root;

    std::string content(buffer, len);

    bool parsingSuccessful = reader.parse(content, root );
    if ( !parsingSuccessful )
    {
        // report to the user the failure and their locations in the document.
        WARNING("Cannot parse the data payload from client");
        return;
    }

    //  regard data with "base time" is historical data
    if(root["bt"].isInt())
    {
        return;
    }

    if(!root["e"].isArray())
        return;

    lwm2m_uri_t uri_root = {0};
    if(root["bn"].isString())
    {
        lwm2m_stringToUri(root["bn"].asCString(), root["bn"].asString().length(), & uri_root);
    }

    for(int i=0;i<root["e"].size();i++)
    {
        Json::Value & res_item = root["e"][i];

        // resource data with "time" regarded as historic data
        if(!res_item["t"].isNull())
        {
             continue;
        }

        // if the payload has base name, use base name from payload
        // otherwise try to use the uri from head as base name

        lwm2m_uri_t uri = {0};

        if((uri_root.flag & LWM2M_URI_FLAG_OBJECT_ID) != 0)
            uri = uri_root;
        else if((uriP->flag & LWM2M_URI_FLAG_OBJECT_ID) != 0)
                    uri = *uriP;


        // if uri base is not completed, need for "n" in each item
        if((uri.flag & LWM2M_URI_FLAG_RESOURCE_ID) == 0)
        {
            if(    res_item["n"].isNull())
                continue;

            const char *str_id = res_item["n"].asCString();
            int head = 0;
            int len = res_item["n"].asString().size();
            uint32_t num = parse_devided_number(str_id, len, &head, '/');
            if(num == -1)
                continue;

            // three more '/' should be present if obj id not in the uri
            if(!(uri.flag & LWM2M_URI_FLAG_OBJECT_ID ))
            {
                uri.objectId = num;
                uri.flag |= LWM2M_URI_FLAG_OBJECT_ID;

                num = parse_devided_number(str_id, len, &head, '/');
                if(num == -1)
                    continue;
            }

            // expecting two more '/' if instance id is not available from uri
            if(!(uri.flag & LWM2M_URI_FLAG_INSTANCE_ID ))
            {

                uri.instanceId = num;
                uri.flag |= LWM2M_URI_FLAG_INSTANCE_ID;

                num = parse_devided_number(str_id, len,    &head, '/');
                if(num == -1)
                    continue;
            }

            if(!(uri.flag & LWM2M_URI_FLAG_RESOURCE_ID ))
            {

                uri.resourceId = num;
                uri.flag |= LWM2M_URI_FLAG_RESOURCE_ID;

                //num = parse_devided_number(str_id, len,    &head, '/');
            }
        }

        std::string value;
        if(res_item["sv"].isString())
        {
            value = res_item["sv"].asString();
        }
        else if(res_item["v"].isInt())
        {
            char b[50];
            snprintf(b, sizeof(b), "%d", res_item["v"].asInt());
            value = b;
        }
        else if(res_item["v"].isDouble())
        {
            char b[50];
            snprintf(b, sizeof(b), "%f", res_item["v"].asFloat());
            value = b;
        }
        else if(res_item["v"].isString())
        {
            char b[50];
            if(res_item["v"].asString().find('.') == string::npos)
            {
                value = res_item["v"].asCString();
            }
            else
            {
                value = res_item["v"].asCString();
            }
        }
        else if(!res_item["bv"].isNull())
        {
            value = res_item["bv"].asBool()?"1":"0";
        }
        else
        {
            continue;
        }

        prv_cache_resource_value(clientID, & uri, (uint8_t*) value.data(),value.size());
    }
}
#endif

// return the data format
int prv_cache_value(char *clientID,
                               char *uriP,
                               uint8_t *data,
                               int dataLength)
{
    return 0;
}

