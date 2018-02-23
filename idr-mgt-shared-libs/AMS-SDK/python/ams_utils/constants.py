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


PORT_RD_LOCAL = 0
PORT_DATA_SERVICE_LOCAL = 1
TIMEOUT_GET = 40
COAPSERVER_PORT_REMOTE_IAGENT = 5683
COAPSERVER_PORT_REMOTE_AMS = 2233
COAPSERVER_HOST_REMOTE_IAGENT = "127.0.0.1"
COAPSERVER_HOST_REMOTE_AMS = "127.0.0.1"
COAPSERVER_PORT_LOCAL = 1111  #find_port()
COAPSERVER_HOST_LOCAL = "127.0.0.1"

URL_REFRESHER = "/refresher"
URL_RD = "/rd"
URL_IBROKER="/ibroker"
URL_RD_MONITOR = "/rd/monitor"
URL_AMS_SET_PRODUCTID = "/ams/product_id"
URL_APPCONFIG_CHECKPOINT = "/ams/config_checkpoint"
URL_APPCONFIG_WATCHER = "/config_watcher"
# URL_REFRESHER_EXT = "/*/*/*/*/*/*/*/*"


def get_iagent_server_uri():
    return "coap://" + COAPSERVER_HOST_REMOTE_IAGENT + ":" + str(
        COAPSERVER_PORT_REMOTE_IAGENT)
