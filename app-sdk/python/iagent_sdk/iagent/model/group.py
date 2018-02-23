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


class Group(object):
    def __init__(self, name):
        self.name = name
        self.__attributes = {}

    def equals(self, obj):
        return self.name == obj.get_name()

    def get_attribute(self, key):
        return self._attributes[key]

    def get_device_members(self):
        devices = []
        all_devices = IAgentManager.getInstance().get_all_devices()
        for device in all_devices:
            if self.name in device.get_groups():
                devices.append(device)
        return devices

    def get_name(self):
        return self.name

    def get_resource_members(self):
        resources = []
        all_devices = IAgentManager.getInstance().get_all_devices()
        for device in all_devices:
            resources_device = device.get_resources()
            for resource in resource_device:
                if self.name in resource.get_groups:
                    resources.append(resource)
        return resources

    def hash_code(self):
        return hash(self.name)
