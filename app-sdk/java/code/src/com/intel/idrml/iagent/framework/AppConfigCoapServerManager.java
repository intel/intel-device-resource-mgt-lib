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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;

import com.intel.idrml.iagent.utilities.LogUtil;

class AppConfigCoapServerManager {

    private List<OnConfigurationListener> configurationListenerList = Collections.synchronizedList(new ArrayList<OnConfigurationListener>());

    private static AppConfigCoapServerManager manager = new AppConfigCoapServerManager();
    private MyCoapServer server;

    private AppConfigCoapServerManager() {
        server = new MyCoapServer(Utils.findPort());
        server.startServer();
    }
    
    public int getPort(){
        return server.getPort();
    }
    
    public void stopServer(){
        server.stopServer();
    }

    public static AppConfigCoapServerManager getInstance() {
        if (manager == null)
            manager = new AppConfigCoapServerManager();

        return manager;
    }

    public synchronized void addConfigurationListerner(AppConfigWatchPoint key, OnConfigurationListener listener) {
        if (key == null || listener == null) {
            return;
        }
        LogUtil.log(LogUtil.LEVEL.INFO, "AppConfigCoapServerManager port:" + getPort() + "    path:" + key.path);
        configurationListenerList.add(listener);
    }

    public synchronized void removeConfiguationListerner() {
        configurationListenerList.clear();
    }

    public synchronized void addResouce(String resourceName) {
        server.addResource(new ListerenCoapResource(resourceName));
    }

    public synchronized void deleteResouce(String resourceName) {
        Resource rootResource = server.getRootResource();
        Resource child = rootResource.getChild(resourceName);
        if (child != null) {
            rootResource.delete(child);
        }
    }
    
    class ListerenCoapResource extends CoapResource {

        private ListerenCoapResource() {
            super("");
        }

        public ListerenCoapResource(String name) {
            super(name);
            getAttributes().setTitle(name);
        }

        @Override
        public void handlePOST(CoapExchange exchange) {
            try {
                String requestText = exchange.getRequestText();
                LogUtil.log(LogUtil.LEVEL.INFO, "resp:" + requestText);
                AppConfigWatchPoint.ConfigNotification notification = Utils.parseConfigNotification(requestText);
                int size = configurationListenerList.size();
                for (int i = 0; i < size; i++) {
                    configurationListenerList.get(i).onConfigChanged(notification.config_path, notification.target_type, notification.target_id);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            exchange.respond(ResponseCode.CHANGED);
        }
    }
}
