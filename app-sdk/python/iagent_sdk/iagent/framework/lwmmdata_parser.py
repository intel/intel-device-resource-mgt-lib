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
from ams_utils.payload_parser import PayloadParser
from ..model.resource_property_data import ResourcePropertyData
from ..model.resource_data_lwmm import ResourceDataLWM2M
import json
import traceback


class LWM2MDataParser(PayloadParser):
    def parse(self, text):
        try:
            json_object = json.loads(text)
            if json_object is None:
                return None
            property_items = []
            bt = json_object.get("bt")
            json_array = json_object.get("e")
            for i in json_array:
                element_json = i
                property_data = ResourcePropertyData()
                property_data.prop_name = element_json["n"]
                if element_json.has_key("v"):
                    property_data.value = element_json["v"]
                    # property_data.value_type = ValueType.FLOAT
                elif element_json.has_key("bv"):
                    property_data.value = element_json["bv"]
                    # property_data.value_type = ValueType.BOOLEAN
                elif element_json.has_key("sv"):
                    property_data.value = element_json["sv"]
                    # property_data.value_type = ValueType.STRING
                if bt is not None:
                    property_data.t += bt
                property_items.append(property_data)
            if bt is not None:
                for item in property_items:
                    item.t += bt
            data = ResourceDataLWM2M(text)
            data.items = property_items
            return data
        except Exception, e:
            print traceback.format_exc()
            return None
