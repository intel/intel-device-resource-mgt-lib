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


from ams_utils.media_type_format import MediaTypeFormat

class ResourceDataGeneral(object):
    def get_format(self):
        return self.__format

    def get_raw_payload(self):
        return self.__raw_payload
    
    def set_raw_payload(self, payload):
        self.__raw_payload = payload

    def is_parsed(self):
        return self.is_parsed

    def __init__(self, raw_payload):
        self.__raw_payload = raw_payload
        self.is_parsed = False
        self.resource_id = None
        self.__format = MediaTypeFormat.TEXT_PLAIN

    def set_format(self, format):
        self.__format = format

    def set_parsed(self, is_parsed):
        self.is_parsed = is_parsed

    def to_json(self):
        return self.__raw_payload
