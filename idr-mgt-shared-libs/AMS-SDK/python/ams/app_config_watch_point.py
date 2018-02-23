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

# -*- coding: utf-8 -*-
from .ams_request_action import AmsRequestAction

class AppConfigWatchPoint(object):
    def __init__(self, action, product, addr, port, path):
        self.action = action
        self.product = product
        self.ip = addr
        self.port = port
        self.path = path

    def to_json(self):
        sb = []
        sb.append("{")
        sb.append("\"action\":\"" + self.action.lower() + "\",")
        sb.append("\"product\":\"" + self.product + "\",")
        sb.append("\"ip\":\"" + self.ip + "\",")
        sb.append("\"path\":\"" + self.path + "\",")
        sb.append("\"port\":" + str(self.port) + "")
        sb.append("}")
        return ''.join(sb)
    