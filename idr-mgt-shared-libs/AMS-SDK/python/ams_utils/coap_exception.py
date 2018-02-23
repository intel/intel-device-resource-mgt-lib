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
from coapthon.messages.response import Response


class CoapException(Exception):
    serial_version_uid = long(1)

    def __init__(self, e):
        if isinstance(e, basestring):
            super(CoapException, self).__init__(e)
        elif isinstance(e, Response):
            super(CoapException, self).__init__(e.pretty_print())
