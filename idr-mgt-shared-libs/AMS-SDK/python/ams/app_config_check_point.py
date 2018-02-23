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


class AppConfigCheckPoint(object):
    def __init__(self, action, product, target_type, target_id):
        self.__action = action
        self.__product_name = product
        self.__target_type = target_type
        self.__target_id = target_id

    def to_json(self):
        sb = []
        sb.append("{")
        sb.append("\"action\":\"" + self.__action.lower() + "\",")
        sb.append("\"product\":\"" + self.__product_name + "\",")
        sb.append("\"target_type\":\"" + self.__target_type + "\",")
        sb.append("\"target_id\":\"" + self.__target_id + "\"")
        sb.append("}")
        return "".join(sb)
