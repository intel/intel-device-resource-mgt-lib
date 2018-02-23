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

# from enum import Enum
import json

class StandardType(object):
    lwm2m = 1
    ocf = 2
    rest = 3
    publish = 4
    modbus = 5
    unknow = 6
    ''' 
    @staticmethod
    def value_of_type(value):
        if value is None or len(value.strip()) == 0:
            return StandardType.unknow
        for name, member in StandardType.__members__.items():
            if value.lower() == name:
                return name
        return StandardType.unknow.name
    def to_string():
        return '"'+self.name+'"'	
    '''

class DeviceInfo(object):
    def __init__(self):
        self.device_id = None
        self.standard_type = None
        self.device_type = None
        self.attrs = {}
        self.sleep_type = None
        self.ttl = None
        self.addr = None
        self.resources = []
        self.groups = []
        self.device_status = None

    def equals(self, obj):
        return self.device_id == obj.get_device_id()

    def get_absolute_uri(self):
        return "/dev" + "/" + self.device_id

    def get_addr(self):
        return self.addr

    def get_attrs(self):
        return self.attrs

    def get_device_id(self):
        return self.device_id

    def get_device_status(self):
        return self.device_status

    def get_device_type(self):
        return self.device_type

    def get_groups(self):
        return self.groups

    # def get_resource(self,uri):
    #    return self.resources
    # def get_resources(self):
    #    pass
    def get_resources(self, para=None):
        if para is None:
            return self.resources
        elif isinstance(para, basestring):
            results = []
            for resource in self.resources:
                if resource.get_resource_type == para:
                    results.append(resource)
                elif resource.get_href() == para:
                    return resource
            return results
        else:
            pass

    def get_sleep_type(self):
        return self.sleep_type

    def get_standard_type(self):
        return self.standard_type

    def get_ttl(self):
        return self.ttl

    def hash_code(self):
        return self.device_id.hash_code()

    def to_json(self):
        device_json = {}
        if self.device_id is not None:
            device_json["di"] = self.device_id
        
        if self.standard_type is not None:
            print "st =" + str(self.standard_type)
            device_json["st"] = self.standard_type
        
        if self.addr is not None:
            device_json["addr"] = self.addr
        if self.device_type is not None:
            device_json["dt"] = self.device_type
        if self.ttl is not None:
            device_json["ttl"] = self.ttl
        if self.sleep_type is not None:
            device_json["set"] = self.sleep_type
        if self.device_status is not None:
            device_json["status"] = self.device_status
        if self.groups is not None: 
           device_json["groups"] = self.groups
        if self.attrs is not None:
            device_json["attrs"] = self.attrs
        
        resource_array = []
        if self.resources is not None and len(self.resources) != 0:
            for resource in self.resources:
                 obj = {}
                 if resource.href is not None:
		     obj["href"] = resource.href 
	         if resource.resource_type is not None:
                     obj["rt"] = resource.resource_type 
                 if resource.groups is not None:
                     obj["groups"] = resource.groups
                 if resource.attrs is not None:
                     obj["attrs"] = resource.attrs 
                 resource_array.append(obj)
        device_json["links"] = resource_array
        # print "device_json links=", device_json["links"]
        return json.dumps(device_json)



class DeviceStatus(object):
    on = 1
    off = 2
    unknow = 3
    ''' 
    def to_string():
        return '"'+self.name+'"'

    @staticmethod
    def value_of_type(value):
        if value is None or len(value.strip()) == 0:
            return unknow
        for name, member in DeviceStatus.__members__.items():
            if value.lower() == name:  #name type is str
                return name
        return DeviceSatus.unknow.name
    '''
