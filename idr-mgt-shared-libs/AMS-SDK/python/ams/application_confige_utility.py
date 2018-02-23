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

# -*- coding: utf-8 -*-

from .app_config_watch_point import AppConfigWatchPoint
from .app_config_check_point import AppConfigCheckPoint
from .ams_request_action import AmsRequestAction
from ams_utils.constants import *
from ams_utils.coap_exception import CoapException
from ams_utils.utils import Utils
from ams_utils.log_util import LogUtil
from ams_utils.singleton import singleton
from ams_utils.my_coap_server import MyCoapServer
from coapthon import defines
from coapthon.messages.response import Response
from coapthon.client.helperclient import HelperClient

@singleton
class ApplicationConfigeUtility(object):
    __app_name = None
    def __init__(self, app_name="iMRT"):
        self.__target_id = None
        self.__config_handlers = None
        ApplicationConfigeUtility.__app_name = app_name
        self.__ip = COAPSERVER_HOST_REMOTE_AMS
        self.__port = COAPSERVER_PORT_REMOTE_AMS
        self.__uri = "coap://" + self.__ip + ":" + str(self.__port)
    def _send_Request(self, path, payload): 
        client = HelperClient(server=(self.__ip, self.__port))
        response = client.put(path,payload, None, TIMEOUT_GET)
        client.close()
        if response is None:
            raise CoapException("No response received.")
        elif response.code == defines.Codes.CREATED.number or response.code == defines.Codes.DELETED.number or response.code == defines.Codes.VALID.number or response.code == defines.Codes.CHANGED.number or response.code == defines.Codes.CONTENT.number or response.code == defines.Codes.CONTINUE.number:
            return True
        else:
            raise CoapException(response)

    def add_configuration_monitor(self, target_type, target_id, event_handler):
        check_point(AmsRequestAction.ADD, target_type, target_id)
        add_watch_point(event_handler)

    def add_watch_point(self, event_handler):
        if event_handler is None:
            raise CoapException(
                "OnConfigurationListener cannot be empty or null.")

        temp_path = Utils.get_random_string()
        coap_server=MyCoapServer()
        print self.__app_name
        payload = AppConfigWatchPoint(
            AmsRequestAction.ADD, ApplicationConfigeUtility.__app_name,
            COAPSERVER_HOST_LOCAL,
            coap_server.get_port(), coap_server.get_amsconf_path())
        
        path = URL_APPCONFIG_WATCHER
        self._send_Request(path,  payload.to_json())
        coap_server.add_configuration_listerner(event_handler)


    def check_point(self, action, target_type, target_id):
        temp_uri = self.__uri + URL_APPCONFIG_CHECKPOINT
        payload = AppConfigCheckPoint(action,
                                      ApplicationConfigeUtility.__app_name,
                                      target_type, target_id)
        self._send_Request(URL_APPCONFIG_CHECKPOINT, payload.to_json())

    def delete_watch_point(self, event_handler):
        temp_path = Utils.get_random_string()
        coap_server=MyCoapServer()
        payload = AppConfigWatchPoint(
            AmsRequestAction.DELETE, self.__app_name,
            COAPSERVER_HOST_LOCAL,
            coap_server.get_port(), coap_server.get_amsconf_path())
        
        path = URL_APPCONFIG_WATCHER
        self._send_Request(path,  payload.to_json())
        coap_server.del_configuration_listerner(event_handler)


    def remove_configuration_monitor(self, target_type, target_id):
        check_point(AmsRequestAction.DELETE, target_type, target_id)
        delete_watch_point()

    def set_ams_addr(self, ip, port):
        self.__ip = ip
        self.__port = port
        self.__uri = "coap://" + ip + ":" + port

    def set_app_name(self, product_name):
        ApplicationConfigeUtility.__app_name = product_name

    def set_product_id(self, product_name, product_id):     
        path = constants.URL_AMS_SET_PRODUCTID
        payload_json = {}
        payload_json["product"] = product_name
        payload_json["id"] = product_id
        LogUtil.log("PUT : " + path)     
        self._send_Request(path,  json.dumps(payload_json))


    def set_target_id(self, device_id):
        self.__target_id = device_id
        
    def stop(self):
        MyCoapServer().stop_server()
