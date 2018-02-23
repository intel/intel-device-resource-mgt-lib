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


# import sys
# sys.path.append("../utilities")
from ams_utils.media_type_format import MediaTypeFormat
from .resource_data_general import ResourceDataGeneral
import json


class ResourceDataOCF(ResourceDataGeneral):
    def get_format(self):
        return MediaTypeFormat.APPLICATION_JSON
    def get_property_value(self, property_name):
        for data in self.items:
            if data.prop_name == property_name:
                return data
        return None

    # def get_raw_payload(self):
    #    pass
    # def is_parsed(self):
    #    pass
    # def resource_data_general(self,raw_payload):
    #    pass
    def __init__(self, rawpayload):
        super(ResourceDataOCF, self).__init__(rawpayload)
        super(ResourceDataOCF, self).set_parsed(True)

    # def set_format(self,format):
    #    pass
    # def set_parsed(self,is_parsed):
    #    pass
    def to_json(self):
        obj = {}
        obj["prm"] = prm
        for data in self.items:
            obj[data.prop_name] = data.sv
        return json.dumps(obj)
