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

from ..model.message_parse_util import MessageParseUtil
from ams_utils.constants import *
from ams_utils.utils import Utils
from ams_utils.coap_exception import CoapException
from ams_utils.log_util import LogUtil
from ams_utils.singleton import singleton

from coapthon.client.helperclient import HelperClient
from coapthon import defines

@singleton
class IBrokerUtility(object):
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
            if response.code == defines.Codes.CREATED.number or \
               response.code == defines.Codes.DELETED.number or \
               response.code == defines.Codes.VALID.number or \
               response.code == defines.Codes.CHANGED.number or \
               response.code == defines.Codes.CONTENT.number or\
               response.code == defines.Codes.CONTINUE.number:

                response_text = str(response.payload)
                if response_text is None or "".__eq__(response_text):
                    return []
                else:
                    return response_text
        except CoapException, e:
            # print "Exception \t {}:{}".format(Exception, e)
            print traceback.format_exc()
            return None

    def ibroker_gut(self, uri=None, result=None):
        try:
            url = URL_IBROKER+uri
            client = HelperClient(server=(self.ip, self.port))
            LogUtil.log("GET : " + self.__uri + uri)
            response = client.get(uri, None, timeout=TIMEOUT_GET)
            client.close()
            return self.__process_response(response)
        except CoapException, e:
            # print "Exception \t {}:{}".format(Exception, e)
            print traceback.format_exc()
            return None

    def ibroker_put(self, uri=None, strContent=None):
        try:
            url = URL_IBROKER+uri
            client = HelperClient(server=(self.ip, self.port))
            payload = (defines.Content_types["application/json"], strContent)
            response = client.put(url, payload, None, timeout=TIMEOUT_GET)
            client.close()
            return self.__process_response(response)
        except CoapException, e:
            # print "Exception \t {}:{}".format(Exception, e)
            print traceback.format_exc()
            return None



