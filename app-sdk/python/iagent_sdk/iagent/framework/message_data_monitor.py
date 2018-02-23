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


class MessageDataMonitor(object):
    def __init__(self):
        self.device_id = None
        self.resource_uri = None
        self.interval = None
        self.requester_name = None
        self.push_url = None
        self.local_path = None
        self.monitor_id = None
        self.sequence = None
        self.process = None

    def equals(self, obj):
        if self == obj:
            return True

        if obj is None or getattr(self, '__class__') != getattr(
                obj, '__class__'):
            return False

        other = obj
        if other.local_path == self.local_path:
            return True

        return False

    def hash_code(self):
        return 0 if self.local_path is None else self.local_path.hash_code()

    def to_json_string(self):
        bf = []
        bf.append("{")
        bf.append("\"di\":\"" + self.device_id + "\",")
        bf.append("\"ri\":\"" + self.resource_uri + "\",")
        if self.sequence is not None:
            bf.append('"sequence":{},'.format(self.sequence))
        if self.process is not None and self.process is True:
            bf.append('"process":"{}",'.format(self.process))
        bf.append('"interval":{},'.format(self.interval))
        bf.append("\"requester\":\"" + self.requester_name + "\",")
        bf.append("\"purl\":\"" + self.push_url + "\"")

        bf.append("}")
        return "".join(bf)
