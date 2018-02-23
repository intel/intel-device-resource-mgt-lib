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


import random

from .constants import COAPSERVER_HOST_REMOTE_IAGENT
from .constants import COAPSERVER_PORT_LOCAL
from .constants import COAPSERVER_PORT_REMOTE_AMS



class Utils(object):
    @staticmethod
    def compose_purl(scheme, path, port):
        if port is not None:
            builder = [
                scheme, "://", COAPSERVER_HOST_REMOTE_IAGENT, ":",
                str(port), "/", path
            ]

        return "".join(builder)



    @staticmethod
    def get_random_string():
        length = 10
        base = "abcdefghijklmnopqrstuvwxyz0123456789"
        random.seed()
        sb = []
        for i in range(length):
            number = random.randint(0, len(base) - 1)
            sb.append(base[number])
        return "".join(sb)
    @staticmethod
    def stop_server():
        from .my_coap_server import MyCoapServer
        MyCoapServer().stop_server()
        

