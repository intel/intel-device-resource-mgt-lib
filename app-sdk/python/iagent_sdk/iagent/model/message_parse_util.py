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


import json
from ..model.resource import Resource
from ..model.device import DeviceInfo
from ..model.device import DeviceStatus
from ..model.device import StandardType

import traceback


class MessageParseUtil(object):
    @staticmethod
    def parse_device(data_in_json):
        if data_in_json is None or "" == data_in_json:
            return None
        try:
            device_list = []
            data_in_json = data_in_json[ :-1] if data_in_json[-1] != ']' and  data_in_json[-1] != '}' else data_in_json  # delete strange last char
            json_array = json.loads(data_in_json)
            for device_json_object in json_array:
                device = DeviceInfo()
                device.device_id = device_json_object["di"]
                # device.device_status = DeviceStatus.value_of_type(
                #    device_json_object["status"])
                device.device_status = device_json_object["status"]
                # device.standard_type = StandardType.value_of_type(
                #    device_json_object["st"])
                device.standard_type = device_json_object["st"]
                device.addr = device_json_object[
                    "addr"] if 'addr' in device_json_object else None
                device.groups = device_json_object[
                    "groups"] if 'groups' in device_json_object else []
                device.attrs = device_json_object[
                    "attrs"] if 'attrs' in device_json_object else {}
                device.device_type = device_json_object["dt"]
                device.sleep_type = device_json_object["set"]

                resource_list = MessageParseUtil.__parse_resource(device_json_object["links"], device)
                device.resources = resource_list
                device_list.append(device)
            return device_list

        except Exception, e:
            traceback.print_exc()
            return None
    
    @staticmethod
    def __parse_resource(json_text, device):
        resource_list = []
        json_array = json_text
        for resource_json_object in json_array:
            href = resource_json_object.get("href")
            attrs = resource_json_object.get("attrs")
            resource_type = resource_json_object.get("rt")
            groups = resource_json_object.get("groups")
            if "inst" in resource_json_object:
                for inst in resource_json_object["inst"]:
                    resource = Resource()
                    resource.href = href + "/" + inst
                    resource.attrs = attrs
                    resource.resource_type = resource_type
                    resource.groups = groups
                    resource.device = device
                    resource_list.append(resource)
            else:
                resource = Resource()
                resource.href = href
                resource.attrs = attrs
                resource.resource_type = resource_type
                resource.groups = groups
                resource.device = device
                resource_list.append(resource)

        return resource_list
