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

class RDMonitorPayload(object):
    def __init__(self, dt=None, rt=None, di=None, with_rts=[], st=None, groups=[], mid=None, purl=None, local_path=None):
        self.dt = dt
        self.rt = rt
        self.di = di
        self.with_rts = with_rts
        self.st = st
        self.mid = mid
        self.groups = groups
        self.purl = purl
        self.local_path = local_path

    def equals(self, obj):
        if self == obj:
            return True
        if obj is None or getattr(self, '__class__') != getattr(
                obj, '__class__'):
            return False
        other = obj
        if other.mid.__eq__(self.mid):
            return True
        else:
            return False

    def hash_code(self):
        return 0 if self.mid is None else self.mid.__hash__()

    def to_json(self):
        bf = []
        # bf.append("{")
        if self.dt is not None:
        	bf.append("\"dt\":\"" + self.dt + "\"")
        if self.rt is not None:
                bf.append("\"rt\":\"" + self.rt + "\"")
        if self.di is not None:
                bf.append("\"di\":\"" + str(self.di) + "\"")
        if len(self.with_rts) != 0 :
                bf.append("\"with_rts\":" + json.dumps(self.with_rts))
        if self.st is not None:
                bf.append("\"st\":\"" + str(self.st) + "\"")
        if self.mid is not None:
                bf.append("\"mid\":\"" + self.mid + "\"")
        if len(self.groups) != 0:
                bf.append("\"groups\":" + json.dumps(self.groups))
        if self.purl is not None:
                bf.append("\"purl\":\"" + self.purl + "\"")
        # if self.local_path is not None:
                # bf.append("\"local_path\":\"" + self.local_path + "\"")
       
        # bf.append("}")
        return '{' + ','.join(bf) + '}'
