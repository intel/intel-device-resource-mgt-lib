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



from coapthon import defines
from coapthon.server.coap import CoAP
from coapthon.resources.resource import Resource
from coapthon import defines
from .utils import Utils
from .singleton import singleton
from .media_type_format import MediaTypeFormat
import constants
import socket
from threading import Thread
import json
def find_port():
    s = None
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.bind(('', 0))
        addr, port = s.getsockname()
        return port
    except Exception as e:
        return COAPSERVER_PORT_REMOTE_AMS + 1
    finally:
        if s != None:
            s.close()

class ConfigNotification(object):
    def __init__(self):
        self.product = None
        self.target_type = None
        self.target_id = None
        self.config_path = None

    @staticmethod
    def parse_config_notification(text):
        json_object = json.loads(text)
        if json_object is None:
            return None
        data = ConfigNotification()
        if json_object.has_key("software"):
            data.product = json_object["software"]
        if json_object.has_key("target_type"):
            data.target_type = json_object["target_type"]
        if json_object.has_key("target_id"):
            data.target_id = json_object["target_id"]
        if json_object.has_key("config_path"):
            data.config_path = json_object["config_path"]
        return data
              
class AmsCoapResource(Resource):
        
    def __init__(self, name="AmsCoapResource",coap_server=None):
        super(AmsCoapResource, self).__init__(name, coap_server, visible=True, observable=True, allow_children=True)
        self.path = 'amsconf'
        if self._coap_server:
            self._coap_server.add_resource(self.path+'/', self)         
        self.__listener_list=[]
        
    def render_PUT(self, request):
        try:
            request_text = request.payload
            notification = ConfigNotification.parse_config_notification(request_text)
            for fc in self.__listener_list:
                fc(notification.config_path,notification.target_type, notification.target_id)
        except Exception, e:
            print e

    def render_POST(self, request):
        self.render_PUT(request) 

    def add_handler(self,listener):
        self.__listener_list.append(listener)
        
    def del_handler(self,listener):
        self.__listener_list.remove(listener) 
 
            
@singleton
class MyCoapServer(Thread):
    def __init__(self):
        Thread.__init__(self)
        self.__port = find_port()
        self.__server = CoAP(("0.0.0.0", int(self.__port    )))
        self.__started = False
        self.__amsconf_resource= AmsCoapResource(coap_server=self.__server)
        self.start_server()

    def add_configuration_listerner(self, listener):
        self.__amsconf_resource.add_handler(listener)
        
    def del_configuration_listerner(self,listener):
        self.__amsconf_resource.del_handler(listener)
                
    def get_port(self):
        return self.__port

    def get_amsconf_path(self):
        return self.__amsconf_resource.path 

    def start_server(self):
        if self.__started:
            return
        self.start()
        self.__stared = True

    def run(self):
        try:
            self.__server.listen(10)       
        except:
            self.__server.close()
            print " server close by error"

    def stop_server(self):
        self.__server.close()
        self.__stared = False
