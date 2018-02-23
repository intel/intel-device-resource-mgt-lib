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

class Resource(object):
    def equals(self, obj):
        return self.href == obj.href
    def get_absolute_uri(self):
        if self.device is not None:
		if self.href.startswith("/"):
			return self.device.get_absolute_uri() + self.href
		else:
			return self.device.get_absolute_uri() + "/" + self.href
	else:
		return self.href
    def get_attrs(self):
        return self.attrs
    def get_current_value(self, property_name):
        return None
    def get_groups(self):
        return self.groups
    def get_href(self):
        return self.href
    def get_owner_device(self):
        return self.device
    def get_owner_groups(self):
        return self.groups
    def get_properties_config(self, property_key):
        return self.__property_config[property_key]
    def get_resource_type(self):
        return self.resource_type
    def hash_code(self):
        return hash(self.href)
    def __init__(self, resource=None):
        self.href = None
	self.resource_type = []
	self.attrs = None
	self.device = None
	self.groups = []
	self.__property_config = {}
	self.__property_data_cache = {}
	if resource is not None:
		self.href = resource.href
		self.resource_type = resource.resource_type
		self.attrs = resource.attrs
		self.device = resource.device
		self.__property_config = resource.__property_config
		self.__property_data_cache = resource.__property_data_cache
		self.groups = resource.groups
	
