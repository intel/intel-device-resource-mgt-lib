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
from ams_utils.utils import Utils
from ams_utils.singleton import singleton
from ams_utils.media_type_format import MediaTypeFormat
from iagent.model.message_parse_util import MessageParseUtil
import ams_utils.constants
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
            
class RdMoniorResource(Resource):
    max_num=0
    def __init__(self, name="RdMoniorResource",coap_server=None):
        super(RdMoniorResource, self).__init__(name, coap_server=coap_server)
        self._path = 'rdmonitor'
        if self._coap_server:
            self._coap_server.add_resource(self._path+'/', self)
        self._handler={}
        
    def del_handler(self,m_id):
        for key in self._handler.keys():
            if key.mid==m_id:
                del self._handler[key]
                return key
            
    def add_handler(self, key, listener):
        path=self._path+'/'+str(RdMoniorResource.max_num)
        RdMoniorResource.max_num=RdMoniorResource.max_num+1
        self._coap_server.add_resource(path+'/', self)
        if path[0]=='/':
            path=path[1:]
        self._handler[key]=listener
        self._path = 'rdmonitor'
        return path 
     
    def get_data_handlers_key_byuri(self, uri):
        if uri is None:
            return None
        for k in self._handler.keys():
            if k.local_path == uri:
                return k
        return None
    
    def render_POST(self, request):      
        request_text = str(request.payload)
        uri = request.uri_path
        try:
            devices = MessageParseUtil.parse_device(request_text)
            key=self.get_data_handlers_key_byuri(uri)
            print key
            listener=self._handler.get(key)
            if listener:
                listener(devices)
            else:
                print(listenter is none)
#                LogUtil.log("WARNING: there is no RDEventListener for " + uri)         
        except Exception, e:
            print "error rd monitor message"

class DataRefreshResource(Resource):
    max_num=0
    def __init__(self, name="DataRefreshResource",coap_server=None):
        super(DataRefreshResource, self).__init__(name, coap_server=coap_server)
        self._path = 'dtfresh'
        if self._coap_server:
            self._coap_server.add_resource(self._path+'/', self)
        self._handler={}
    def del_handler(self,m_id):
       for key in self._handler.keys():
            if m_id == key.monitor_id:
                del self._handler[key]
                return key
                break
    def add_handler(self, key, listener):
        path=self._path+'/'+str(DataRefreshResource.max_num)
   
        DataRefreshResource.max_num=DataRefreshResource.max_num+1
        self._coap_server.add_resource(path+'/', self)
        if path[0]=='/':
            path=path[1:]
        key.local_path= path
        self._handler[key]=listener
        self._path = 'dtfresh'
        return path  
    def get_data_handlers_key_byuri(self, uri):
        print "the uri is "+ uri
        if uri is None:
            return None
        for k, v in self._handler.iteritems():
            if k.local_path == uri:
                return k
        return None

    def render_PUT_advanced(self, request, response):
        request_text = str(request.payload)
        uri = request.uri_path
        first_part_uri = uri.split('/')[0]
        second_part_uri = uri.split('/')[1]
        local_uri=first_part_uri+'/'+second_part_uri
        key = self.get_data_handlers_key_byuri(local_uri)
 #       assert (isinstance(response, Response))
        if key is not None and request_text is not None and "" != request_text:
            listener = self._handler.get(key)
            if listener is not None:
                fmt = request.content_type
                from data_utility import DataUtility
                parser =DataUtility().get_parser(fmt)
                data = None
                if parser is None:
                    from ..model.resource_data_general import ResourceDataGeneral
                    data = ResourceDataGeneral(request_text)
                else:
                    data = parser.parse(request_text)
                data.set_format(fmt)  # ResourceDataOCF/LMW2M/GENERAL
                paths = uri.split("/")
                device_id = paths[3] if len(paths) > 2 else ""
                
                resource_uri = ""
                if len(paths[4:]) > 0:
                    resource_uri = "/".join(paths[4:])
                    if not resource_uri.startswith("/"):
                        resource_uri = "/" + resource_uri
                
                """
                resource_uri = paths[3] if len(paths) > 3 else ""
                if fmt == MediaTypeFormat.APPLICATION_JSON:
                    resource_uri += "/" + paths[4] if len(paths) > 4 else ""
                if fmt == MediaTypeFormat.TEXT_PLAIN:
                    resource_uri += ("/" + paths[4]) if len(paths) > 4 else ""
                    resource_uri += ("/" + paths[5]) if len(paths) > 5 else ""
                """
                
                rt = listener(device_id, resource_uri,
                         data)  # listener is a function
                if  key.process:
                    if rt is True:
                        response.code = defines.Codes.CHANGED.number
                    else:
                        response.code = defines.Codes.FORBIDDEN.number
                    response.content_type = fmt
                    response.payload = data.to_json()
                    return self, response
                else:
                    return self, None
                    
        # response changed
        else:
            pass
            # response not acceptable (no handler for that resource)
                
    def render_POST_advanced(self, request, response):
        return self.render_PUT_advanced(request, response)
            
@singleton
class MyCoapServer(Thread):
    def __init__(self):
        Thread.__init__(self)
        self.__port = find_port()
        self.__server = CoAP(("0.0.0.0", int(self.__port    )))
        self.__started = False
        self.__rdmonitor_resource= RdMoniorResource(coap_server=self.__server)
        self.__dtrefresh_resource= DataRefreshResource(coap_server=self.__server)
        self.start_server()
        
    def add_rdevent_listener(self, key ,listener):
        return self.__rdmonitor_resource.add_handler(key, listener)
            
    def del_rdevent_listener(self, m_id):
        self.__rdmonitor_resource.del_handler(m_id)
        
    def add_data_listener(self, key, listener):
        return self.__dtrefresh_resource.add_handler(key, listener)
    
    def del_data_listener(self, m_id):
        return self.__dtrefresh_resource.del_handler(m_id)
        
    def get_port(self):
        return self.__port

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
