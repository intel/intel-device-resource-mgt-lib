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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;

import com.intel.idrml.iagent.model.ResourceDataGeneral;
import com.intel.idrml.iagent.utilities.LogUtil;
import com.intel.idrml.iagent.utilities.MediaTypeFormat;
import com.intel.idrml.iagent.utilities.PayloadParser;

class DataCoapServerManager {

    private Map<MessageDataMonitor, OnDataListener> dataHandlers = new ConcurrentHashMap<MessageDataMonitor, OnDataListener>();
    private Map<Integer, PayloadParser> parserMap = new ConcurrentHashMap<Integer, PayloadParser>();

    private static DataCoapServerManager manager = new DataCoapServerManager();
    private MyCoapServer server;

    private DataCoapServerManager() {
        server = MyCoapServer.getInstance();

        registerParser(MediaTypeFormat.APPLICATION_JSON, new OCFDataParser());
        registerParser(MediaTypeFormat.APPLICATION_JSON_LWM2M, new LWM2MDataParser());
        LogUtil.log(LogUtil.LEVEL.INFO, "Builtin parser: "+parserMap.size());
    }

    public int getPort() {
        return server.getPort();
    }

    public static DataCoapServerManager getInstance() {
        if (manager == null)
            manager = new DataCoapServerManager();

        return manager;
    }

    public synchronized void addDataListerner(MessageDataMonitor key, OnDataListener listener) {
        if (key == null || listener == null) {
            return;
        }
        dataHandlers.put(key, listener);
    }

    public synchronized void addResource(String resourceName) {
        if (resourceName == null) {
            return;
        }
        Resource resource = server.getRootResource();
        String[] resourceStr = resourceName.split("/");
        Resource rootResource = resource.getChild(resourceStr[0]);
        if (rootResource == null) {
            rootResource = new ListerenCoapResource(resourceStr[0]);
            server.addResource(rootResource);
        }
        for (int i = 1; i < resourceStr.length; i++) {
            Resource child = rootResource.getChild(resourceStr[i]);
            if (child == null) {
                ListerenCoapResource coapResource = new ListerenCoapResource(resourceStr[i]);
                rootResource.add(coapResource);
                rootResource = coapResource;
            } else {
                rootResource = child;
            }
        }
        LogUtil.log(LogUtil.LEVEL.INFO, "DataCoapServerManager port:" + getPort() + "    path:" + resourceName);
    }

    public synchronized void deleteResource(String resourceName) {
        if (resourceName == null) {
            return;
        }
        Resource rootResource = server.getRootResource();
        String[] resourceStr = resourceName.split("/");
        Resource childResource = rootResource.getChild(resourceStr[0]);
        if (childResource != null) {
            rootResource.delete(childResource);
        }
    }

    public synchronized void removeDataListerner(String registerId) {
        MessageDataMonitor key = getDataHandlersKeyByMonitorID(registerId);
        if (key == null) {
            return;
        }
        dataHandlers.remove(key);
    }

    public synchronized MessageDataMonitor getDataHandlersKey(String path) {
        if (path == null) {
            return null;
        }
        Set<MessageDataMonitor> set = dataHandlers.keySet();
        for (MessageDataMonitor o : set) {
            if (o.localPath.equals(path)) {
                return o;
            }
        }
        return null;
    }

    public synchronized MessageDataMonitor getDataHandlersKeyByMonitorID(String monitorID) {
        if (monitorID == null) {
            return null;
        }
        Set<MessageDataMonitor> set = dataHandlers.keySet();
        for (MessageDataMonitor o : set) {
            if (o.monitorId.equals(monitorID)) {
                return o;
            }
        }
        return null;
    }

    public synchronized void registerParser(int fromat, PayloadParser parser) {
        parserMap.put(fromat, parser);
    }

    public synchronized PayloadParser getParser(int fromat) {
        return parserMap.get(fromat);
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
        public Resource getChild(String name) {
            Resource resource = super.getChild(name);
            if (resource == null) {
                resource = super.getChild("*");
            }
            return resource;
        }

        @Override
        public void handlePOST(CoapExchange exchange) {
            String uri = exchange.getRequestOptions().getUriPathString();
            String purl = uri.substring(0, uri.indexOf("/"));
            MessageDataMonitor key = getDataHandlersKey(purl);
            String requestText = exchange.getRequestText();
            LogUtil.log(LogUtil.LEVEL.INFO, "uri:" + uri + "  resp : " + requestText);
            if (key != null && requestText != null && !"".equals(requestText)) {
                OnDataListener listener = dataHandlers.get(key);
                ResponseCode responseCode = ResponseCode.CONTINUE;
                if (listener != null) {
                    int format = exchange.getRequestOptions().getContentFormat();
                    PayloadParser parser = getParser(format);
                    ResourceDataGeneral data = null;
                    if (parser == null) {
                        data = new ResourceDataGeneral(requestText);
                    } else {
                        data = parser.parse(requestText);
                    }
                    data.setFormat(format);
                    String[] paths = uri.split("/");
                    String deviceId = paths.length > 2 ? paths[2] : "";
                    String resourceUri = paths.length > 3 ? paths[3] : "";
                    switch (format) {
                    case MediaTypeFormat.APPLICATION_JSON:
                        resourceUri += paths.length > 4 ? "/" + paths[4] : ""; // add property name
                        break;
                    case MediaTypeFormat.TEXT_PLAIN:
                        resourceUri += paths.length > 4 ? "/" + paths[4] : "";// add property name
                        resourceUri += paths.length > 5 ? "/" + paths[5] : ""; // add property instance
                        break;
                    }
                    responseCode = listener.onResourceDataChanged(deviceId, resourceUri, data);
                    if(key.process==1)
                    {
                	if(responseCode==ResponseCode.CHANGED)
                	{
                	    exchange.respond(responseCode, data.toJson(), data.getFormat());
                	    return;
                	}
                    }
                }
                exchange.respond(responseCode);
            } else {
                exchange.respond(ResponseCode.NOT_ACCEPTABLE);
            }
        }
    }

}
