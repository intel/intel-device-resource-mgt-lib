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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;

import com.intel.idrml.iagent.model.DeviceInfo;
import com.intel.idrml.iagent.utilities.LogUtil;

class RDCoapServerManager {

    private Map<RDMonitorPayload, OnRDEventListener> rdHandlers = new ConcurrentHashMap<>();

    private static RDCoapServerManager manager = new RDCoapServerManager();
    private MyCoapServer server;

    private RDCoapServerManager() {
        server = MyCoapServer.getInstance();
    }

    public int getPort() {
        return server.getPort();
    }

    public static RDCoapServerManager getInstance() {
        if (manager == null)
            manager = new RDCoapServerManager();

        return manager;
    }

    public synchronized void addRDEventListener(RDMonitorPayload key, OnRDEventListener listener) {
        if (key == null || listener == null) {
            return;
        }
        LogUtil.log(LogUtil.LEVEL.INFO, "RDCoapServerManager port:" + getPort() + "    path:" + key.localPath);
        rdHandlers.put(key, listener);
    }

    public synchronized void removeRDEventListener(String moniterID) {
        RDMonitorPayload key = getRDHandlersKey(moniterID);
        if (key != null) {
            rdHandlers.remove(key);
        }
    }

    public synchronized RDMonitorPayload getRDHandlersKey(String moniterID) {
        if (moniterID == null) {
            return null;
        }
        Set<RDMonitorPayload> set = rdHandlers.keySet();
        for (RDMonitorPayload o : set) {
            if (o.mid.equals(moniterID)) {
                return o;
            }
        }
        return null;
    }

    private synchronized OnRDEventListener getRDEventListener(String uri) {
        if (uri == null) {
            return null;
        }
        Set<RDMonitorPayload> set = rdHandlers.keySet();
        for (RDMonitorPayload o : set) {
            if (o.localPath.equals(uri)) {
                return rdHandlers.get(o);
            }
        }
        return null;
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
            String uri = exchange.getRequestOptions().getUriPathString();
            String requestText = exchange.getRequestText();
            LogUtil.log(LogUtil.LEVEL.INFO, "resp: " + requestText);
            try {
                List<DeviceInfo> devices = MessageParseUtil.parseDevice(requestText);
                IAgentManager.getInstance().refreshDevicesCached(devices);
                OnRDEventListener listener = getRDEventListener(uri);
                if (listener != null) {
                    listener.onDeviceChanged(devices);
                }
                else
                {
                	LogUtil.log(LogUtil.LEVEL.INFO, "WARNING: there is no RDEventListener for "+uri);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

//        @Override
//        public void handleDELETE(CoapExchange exchange) {
//            String uri = exchange.getRequestOptions().getUriPathString();
//            LogUtil.log("notify parameter: " + exchange.getRequestOptions().getUriQueryString());
//            try {
//                OnRDEventListener listener = getRDEventListener(uri);
//                if (listener != null) {
//                    String deviceId = exchange.getQueryParameter("di");
//                    if (deviceId != null && !deviceId.equals("")) {
//                        listener.onDeviceDeleted(deviceId);
//                    }else{
//                        LogUtil.log("notify parameter is null or empty");
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }

    }
}
