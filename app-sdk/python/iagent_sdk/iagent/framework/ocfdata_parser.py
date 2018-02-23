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
import traceback
import sys
from ..model.resource_data_general import ResourceDataGeneral
from ..model.resource_data_ocf import ResourceDataOCF
from ..model.resource_property_data import ResourcePropertyData
# from ..model.resource_property_data import ValueType
from ams_utils.payload_parser import PayloadParser


class OCFDataParser(PayloadParser):
    def parse(self, text):
        try:
            json_object = json.loads(text)
            if json_object is None:
                return None
            data = ResourceDataOCF(text)
            fixed_key_prm = "prm"
            key_set = json_object.keys()
            items = []
            # OCF + JSON
            if fixed_key_prm in json_object:
                prm_json = json_object[fixed_key_prm]
                prm = prm_json
                data.prm = prm
            # MODBUS + JSON
            else:
                data.prm = []
            for key_str in key_set:
                if fixed_key_prm != key_str:
                    property_data = ResourcePropertyData()
                    property_data.prop_name = key_str
                    # property_data.sv = json_object[key_str]
                    property_data.value = json_object[key_str]
                    # property_data.value_type = ValueType.STRING
                    items.append(property_data)

            data.items = items
            return data

        except Exception, e:
            print traceback.format_exc()
            return None
