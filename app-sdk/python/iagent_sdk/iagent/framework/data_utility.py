# -*- coding: utf-8 -*-

# Copyright (C) 2017 Intel Corporation.  All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


import coapthon

from ..model.resource import Resource
from ..model.resource_data_general import ResourceDataGeneral
from .message_data_monitor import MessageDataMonitor
from .ocfdata_parser import OCFDataParser
from .lwmmdata_parser import LWM2MDataParser
from ams_utils.utils import Utils
from ams_utils.media_type_format import MediaTypeFormat
from ams_utils.log_util import LogUtil
from ams_utils.coap_exception import CoapException
from .my_coap_server import MyCoapServer
from ams_utils.singleton import singleton
from ams_utils.constants import *
from coapthon.client.helperclient import HelperClient
from coapthon import defines

@singleton
class DataUtility(object):
    
    def __init__(self,
                 ip=COAPSERVER_HOST_REMOTE_IAGENT,
                 port=COAPSERVER_PORT_REMOTE_IAGENT):
        self.__ip = ip
        self.__port = port
        self.__uri = "coap://" + ip + ":" + str(port)
        self.__parser_map={}

        self.register_parser(MediaTypeFormat.APPLICATION_JSON_OCF, OCFDataParser())
        self.register_parser(MediaTypeFormat.APPLICATION_JSON_LWM2M,
                             LWM2MDataParser())

    def register_parser(self, fromat, parser):
        self.__parser_map[fromat] = parser


    def get_parser(self, fromat):
        return self.__parser_map.get(fromat)
    
    def do_resource_get(self, resource):
        if isinstance(resource, Resource):
            return self.do_resource_get(resource.get_absolute_uri())
        elif isinstance(resource, basestring):
            client = HelperClient(server=(self.__ip, self.__port))
            LogUtil.log("GET : " + resource)
            response = client.get(resource, None, TIMEOUT_GET)
            client.close()
            if response is None:
                raise CoapException("No response received.")
            elif response.code == defines.Codes.CREATED.number or response.code == defines.Codes.DELETED.number or response.code == defines.Codes.VALID.number or response.code == defines.Codes.CHANGED.number or response.code == defines.Codes.CONTENT.number or response.code == defines.Codes.CONTINUE.number:
                format=None
                parser=None
                for option in response.options:
                    if option.number == defines.OptionRegistry.CONTENT_TYPE.number:
                        format = option.value
                        break
                if format:
                    parser =self.get_parser(format)
                request_text = str(response.payload)
                data = None
                if parser is None:
                    data = ResourceDataGeneral(request_text)
                else:
                    data = parser.parse(request_text)
                return data
            else:
                raise CoapException(response)

    def do_resource_post(self, resource_url, format, payload):
        if resource_url is None:
            return "error: resource URL is null"
        client = HelperClient(server=(self.__ip, self.__port))
        LogUtil.log("POST : " + resource_url)
        response = client.post(resource_url, payload, None,
                               TIMEOUT_GET)  # format?
        client.close()
        if reponse is None:
            raise CoapException("No response received.")
        if response.code == defines.Codes.CREATED.number or response.code == defines.Codes.DELETED.number or response.code == defines.Codes.VALID.number or response.code == defines.Codes.CHANGED.number or response.code == defines.Codes.CONTENT.number or response.code == defines.Codes.CONTINUE.number:
            request_text = str(response.payload)
            return request_text
        else:
            raise CoapException(response)

    def do_resource_put(self, resource, format, payload):
        url = get_iagent_server_uri()
        if isinstance(resource, basestring):
            path = resource
            LogUtil.log("PUT : " + url)
        elif isinstance(resource, Resource):
            url += resource.get_absolute_uri()
            path = resource.get_absolute_uri()
            LogUtil.log("PUT : " + url)
        else:
            pass
        client = HelperClient(server=(self.__ip, self.__port))
        print "property payload =" + payload
        data = (format, payload)
        response = client.put(path, data, None, TIMEOUT_GET)
        client.close()
        if response is None:
            raise CoapException("No response received.")
        elif response.code == defines.Codes.CREATED.number or response.code == defines.Codes.DELETED.number or response.code == defines.Codes.VALID.number or response.code == defines.Codes.CHANGED.number or response.code == defines.Codes.CONTENT.number or response.code == defines.Codes.CONTINUE.number:

            # if isinstance(resource, Resource):
            request_text = str(response.payload)
            print "response code =" + str(response.code)
            return True
        else:
            raise CoapException(response)

    def register_payload_parser(self, format_type, parser):
        DataCoapServerManager.get_instance().register_parser(
            format_type, parser)

    def start_data_observer(self, query, event_handler):
        temp_uri = self.__uri + URL_REFRESHER
        coapServer = MyCoapServer()
        payload = MessageDataMonitor()
        payload.device_id = query.device_id
        payload.resource_uri = query.resource_uri
        payload.interval = query.interval
        payload.local_path = coapServer.add_data_listener(payload, event_handler)
        payload.push_url = Utils.compose_purl(
            "coap", payload.local_path,
            str(coapServer.get_port()))
        payload.requester_name = query.requester
        payload.sequence = query.sequence 
        payload.process = query.process
        LogUtil.log("POST : " + temp_uri + "payload: \n" + 
                    payload.to_json_string())
        client = HelperClient(server=(self.__ip, self.__port))
        response = client.post(
            URL_REFRESHER,
            payload.to_json_string(), None, TIMEOUT_GET
        )  # content type = MediaTypeRegistry.APPLICATION_JSON
        client.close()

        if response is None:
            #delete later
            raise CoapException("No response received.")
        elif response.code == defines.Codes.CREATED.number or response.code == defines.Codes.DELETED.number or response.code == defines.Codes.VALID.number or response.code == defines.Codes.CHANGED.number or response.code == defines.Codes.CONTENT.number or response.code == defines.Codes.CONTINUE.number:
            monitor_id = str(response.payload)
            LogUtil.log("POST : " + temp_uri + "  resp : " + monitor_id)
            payload.monitor_id = monitor_id
            return monitor_id

    def stop_data_observer(self, observer_point_id):
        coapServer = MyCoapServer()
        payload = coapServer.del_data_listener(observer_point_id)
        if payload is None:
            print "payload is none!!!!!"
            return True
        temp_uri = self.__uri + URL_REFRESHER + "?id=" + observer_point_id
        client = HelperClient(server=(self.__ip, self.__port))
        print("DELETE : " + temp_uri)
        response = client.delete(
            URL_REFRESHER + "?id=" + observer_point_id, None,
            TIMEOUT_GET)
        client.close()
        if response is None:
            raise CoapException("No response received.")
        elif response.code == defines.Codes.CREATED.number or response.code == defines.Codes.DELETED.number or response.code == defines.Codes.VALID.number or response.code == defines.Codes.CHANGED.number or response.code == defines.Codes.CONTENT.number or response.code == defines.Codes.CONTINUE.number:
            print "remove data listener now!"
            return True
        else:
            print "response code=" + str(response.code)
            raise CoapException(response)
