/*
 * Copyright (C) 2017 Intel Corporation.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.intel.idrml.iagent.framework;

class Constants {
    //TODO need to define constants here
    public static final int PORT_RD_LOCAL = 0;
    public static final int PORT_DATA_SERVICE_LOCAL = 1;
    public static final int TIMEOUT_GET = 5000;
    public static final int COAPSERVER_PORT_REMOTE_IAGENT = 5683;
    public static final int COAPSERVER_PORT_REMOTE_AMS = 2233;
    public static final String COAPSERVER_HOST_REMOTE_IAGENT = "127.0.0.1";
//    public static final String COAPSERVER_HOST_REMOTE_IAGENT = "10.238.151.109";
    public static final String COAPSERVER_HOST_REMOTE_AMS = "127.0.0.1";
    public static int COAPSERVER_PORT_LOCAL = Utils.findPort();
    public static final String COAPSERVER_HOST_LOCAL = "127.0.0.1";
//    public static final String COAPSERVER_HOST_LOCAL = "10.238.151.103";

    public static final String URL_REFRESHER = "/refresher";
    public static final String URL_RD = "/rd";
    public static final String URL_RD_MONITOR = "/rd/monitor";
    public static final String URL_AMS_SET_PRODUCTID = "/ams/product_id";
    public static final String URL_APPCONFIG_CHECKPOINT = "/ams/config_checkpoint";
    public static final String URL_APPCONFIG_WATCHER = "/config_watcher";
    public static final String URL_REFRESHER_EXT = "/*/*/*/*/*/*/*/*"; // iAgent send data refresher to coap://127.0.0.1:port/purl/dt/device_id/resource_id/property
	public static String getIAgentServerUri() {
		return "coap://"+COAPSERVER_HOST_REMOTE_IAGENT+":"+COAPSERVER_PORT_REMOTE_IAGENT;
	}

}
