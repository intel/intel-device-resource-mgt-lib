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
import traceback
import os
import json
from ams_utils.coap_exception import CoapException
from ams_utils.singleton import singleton
from .application_confige_utility import ApplicationConfigeUtility
from .ams_request_action import AmsRequestAction

@singleton
class AmsClient(object):

    def __init__(self, product_name):
        self.__product_name = product_name
        self.__app_config_utility = ApplicationConfigeUtility(
            product_name)

    def add_checkpoint(self, target_type, target_id):
        try:
            self.__app_config_utility.check_point(AmsRequestAction.ADD,
                                                  target_type, target_id)
            return 0
        except CoapException, e:
            exstr = traceback.format_exc()
            print exstr
            return -1

    def delete_checkpoint(self, target_type, target_id):
        try:
            self.__app_config_utility.check_point(AmsRequestAction.DELETE,
                                                  target_type, target_id)
            return 0
        except CoapException, e:
            exstr = traceback.format_exc()
            print exstr
            return -1

    def deregister_config_status(self):
        try:
            self.__app_config_utility.delete_watch_point()
            return 0

        except CoapException, e:
            exstr = traceback.format_exc()
            print exstr
            return -1

    def get_config_file_path(config_file_path_local, target_type, target_id):
        app_path_base = os.getenv("BPK_CONFIG_PATH")
        with open('manifest', 'r') as f:
            data = json.load(f)
        app_name = data['app_name'].str()
        app_type = data['app_type'].str()
        app_name = app_name.replace("\"", "")
        app_type = app_type.replace("\"", "")
        app_path_base = app_path_base if app_path_base.endswith("/") else (
            app_path_base + "/")
        app_path_base += "apps" if app_type.equals("main") else "callback"
        return app_path_base + "/" + app_name + "config" + "/" + target_type + "/" + target_id + "/" + config_file_path_local

    def register_config_status(self, event_handler):
        try:
            return self.__app_config_utility.add_watch_point(event_handler)
        except CoapException, e:
            print traceback.format_exe()
            return None

    def set_product_id(self, product_id):
        try:
            self.__app_config_utility.set_product_id(self.__product_name,
                                                     product_id)
            return 0
        except CoapException, e:
            return -1

    def __set_product_name(self, product_name):
        self.__product_name = product_name
        self.__app_config_utility.set_app_name(product_name)
        
    def stop(self):
        self.__app_config_utility.stop()
