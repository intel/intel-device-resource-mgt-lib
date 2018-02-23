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

from device import *

class RDQueryParam(object):
    def __init__(self):
        self.device_id = None
        self.standard_type = None
        self.device_type = None
        self.group_id = None
        self.group_ids = []
        self.resource_type = None
        self.resource_uri = None
        self.with_rts = []
        self.status = None

    def to_query_string(self):
        bf = []
        if self.device_id is not None:
            bf.append("&di=" + self.device_id)
        if self.standard_type is not None:
            bf.append("&st=" + self.standard_type)
        if self.device_type is not None:
            bf.append("&dt=" + self.device_type)
        if self.group_id is not None:
            bf.append("&group=" + self.group_id)
        if self.resource_type is not None:
            bf.append("&rt=" + self.resource_type)
        if self.status is not None:
            bf.append("&status=" + self.status)
        if self.resource_uri is not None:
            bf.append("&ri=" + self.resource_uri)
        if self.group_ids:
            bf.append("&groups=")
            ids = []
            ids.append("[")
            l = len(self.group_ids)
            for i in range(l):
                if i == (l - 1):
                    ids.append(self.group_ids[i] + "]")
                else:
                    ids.append(self.group_ids[i] + ",")
            bf.append("".join(ids))
        if self.with_rts:
            bf.append("&with_rts=")
            ids = []
            l = len(self.with_rts)
            for i in range(l):
                if i == (l - 1):
                    ids.append(self.with_rts[i] + "]")
                else:
                    ids.append(self.with_rts[i] + ",")
            bf.append("".join(ids))
        bf = ''.join(bf)
        return bf.replace("&", "?", 1) if len(bf) > 0 else bf
