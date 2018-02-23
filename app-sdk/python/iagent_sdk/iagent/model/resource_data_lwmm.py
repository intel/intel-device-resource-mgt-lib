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
from .resource_data_general import ResourceDataGeneral
import sys
from  ams_utils.media_type_format import MediaTypeFormat
# from  .resource_property_data import ValueType

class ResourceDataLWM2M(ResourceDataGeneral):
    def get_format(self):
        return MediaTypeFormat.APPLICATION_JSON_LWM2M

    # def get_raw_payload(self):
    #    pass
    # def is_parsed(self):
    #    pass
    # def resource_data_general(self,raw_payload):
    #   pass
    def __init__(self, raw_payload):
        super(ResourceDataLWM2M, self).__init__(raw_payload)
        super(ResourceDataLWM2M, self).set_parsed(True)
        self.items = []

    # def set_format(self,format):
    #    pass
    # def set_parsed(self,is_parsed):
    #    pass
    def to_json(self):
        if len(self.items) == 0:
            return "{\"e\":[]}"
        obj = {}
        json_array = []
        for item in self.items:
            data = item
            o = {}
            o["n"] = data.prop_name
            if isinstance(data.value, bool):
                o["bv"] = data.value
            elif isinstance(data.value, float) or isinstance(data.value, int):
                o["v"] = data.value
            elif isinstance(data.value, basestring):
                o["sv"] = data.value
            else:
                pass
            json_array.append(o)
        obj["e"] = json_array
        return json.dumps(obj)
