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

import sys
from .my_coap_server import MyCoapServer
from .rdmonitor_payload import RDMonitorPayload
from ..model.message_parse_util import MessageParseUtil
from ams_utils.constants import *
from ams_utils.utils import Utils
from ams_utils.coap_exception import CoapException
from ams_utils.log_util import LogUtil
from .my_coap_server import MyCoapServer

from coapthon.client.helperclient import HelperClient
from coapthon import defines

class RDUtility(object):
    def __init__(self,
                 ip=COAPSERVER_HOST_REMOTE_IAGENT,
                 port=COAPSERVER_PORT_REMOTE_IAGENT):
        self.__uri = "coap://" + ip + ":" + str(port)
        self.ip = ip
        self.port = port

    def __process_response(self, response):
        try:
            if response is None:
                LogUtil.log("No response received.")
                return None
            if response.code == defines.Codes.CREATED.number or response.code == defines.Codes.DELETED.number or response.code == defines.Codes.VALID.number or response.code == defines.Codes.CHANGED.number or response.code == defines.Codes.CONTENT.number or response.code == defines.Codes.CONTINUE.number:

                response_text = str(response.payload)
                if response_text is None or "".__eq__(response_text):
                    return []
                else:
                    devices = MessageParseUtil.parse_device(response_text)
                    return devices
        except CoapException, e:
            # print "Exception \t {}:{}".format(Exception, e)
            print traceback.format_exc()
            return None

    def query_rd(self, query=None, result=None):
        try:
            uri = URL_RD
            if query is not None:
                uri += query.to_query_string()
            client = HelperClient(server=(self.ip, self.port))
            LogUtil.log("GET : " + self.__uri + uri)
            response = client.get(uri, None, timeout=TIMEOUT_GET)
            client.close()
            return self.__process_response(response)
        except CoapException, e:
            # print "Exception \t {}:{}".format(Exception, e)
            print traceback.format_exc()
            return None

    def create_monitor(self, query, event_handler):
        uri = URL_RD_MONITOR
        coapServer = MyCoapServer()
        rdm_payload = RDMonitorPayload(query.device_type, query.resource_type, query.device_id,
                                       query.with_rts, query.standard_type, query.group_ids)
        local_path = coapServer.add_rdevent_listener(rdm_payload,event_handler)
        purl = Utils.compose_purl(
            "coap", local_path, coapServer.get_port())
        rdm_payload.local_path=local_path
        rdm_payload.purl=purl
 
        client = HelperClient(server=(self.ip, self.port))

        payload = (defines.Content_types["application/json"],
                   rdm_payload.to_json())
        response = client.post(
            uri, payload, None, timeout=TIMEOUT_GET)
        client.close()
        if response is None:
            coapServer.del_rdevent_listener(event_handler)
            LogUtil.log("No response received.")
        elif response.code == defines.Codes.CREATED.number or response.code == defines.Codes.DELETED.number or response.code == defines.Codes.VALID.number or response.code == defines.Codes.CHANGED.number or response.code == defines.Codes.CONTENT.number or response.code == defines.Codes.CONTINUE.number:
            monitor_id = str(response.payload)
            print "monitor_id is", monitor_id
            LogUtil.log("POST [" + self.__uri + uri + "] resp : " + monitor_id)
            rdm_payload.mid = monitor_id
            return monitor_id
        else:
            coapServer.del_rdevent_listener(event_handler)
            LogUtil.log("resp with error code: " + response.code())

    def modify_monitor(self, moniter_id, query, event_handler):
        '''
        if moniter_id is None or len(moniter_id) == 0:
            LogUtil.log("moniterID cannot be empty or null.")
            return None
        rdm_payload = RDCoapServerManager.get_instance().get_rdhandlers_key(
            moniter_id)
        if rdm_payload is None:
            LogUtil.log("This moniterID:" + moniter_id + " does not exist.")
            return None
        uri = URL_RD_MONITOR
        payload = RDMonitorPayload(query.device_type, query.resource_type, query.device_id,
                                   query.with_rts, query.standard_type,
                                   query.group_ids, moniter_id,
                                   rdm_payload.purl, rdm_payload.local_path)
        client = HelperClient(server=(self.ip, self.port))
        LogUtil.log("POST : " + self.__uri + uri)
        payload = (defines.Content_types["application/json"], rdm_payload.to_json())
        response = client.post(uri, payload, None, timeout=TIMEOUT_GET)
        client.close()
        if response is None:
            LogUtil.log("No response received.")
            return None
        if response.code == defines.Codes.CREATED.number or response.code == defines.Codes.DELETED.number or response.code == defines.Codes.VALID.number or response.code == defines.Codes.CHANGED.number or response.code == defines.Codes.CONTENT.number or response.code == defines.Codes.CONTINUE.number:

            monitor_id = str(response.payload)
            LogUtil.log("resp : " + monitor_id)
            return monitor_id
        '''
    def remove_monitor(self, moniter_id):
        if moniter_id is None or len(moniter_id) == 0:
            return False
        uri = URL_RD_MONITOR + "?id=" + moniter_id
        coapServer = MyCoapServer()
        payload = coapServer.del_rdevent_listener(moniter_id)
        if payload is None:
            return True
        client = HelperClient(server=(self.ip, self.port))
        print ("DELETE : " + self.__uri + uri)
        response = client.delete(uri, None, timeout=TIMEOUT_GET)
        client.close()
        if response is None:
            print ("No response received.")
            return False
        if response.code == defines.Codes.CREATED.number or response.code == defines.Codes.DELETED.number or response.code == defines.Codes.VALID.number or response.code == defines.Codes.CHANGED.number or response.code == defines.Codes.CONTENT.number or response.code == defines.Codes.CONTINUE.number:
            return True
        else:
            raise CoapException(response)
