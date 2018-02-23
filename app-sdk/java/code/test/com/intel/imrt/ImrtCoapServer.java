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
 
 
package com.intel.imrt;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.californium.core.CaliforniumLogger;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.intel.imrt.iagent.utilities.LogUtil;
import com.intel.imrt.response.ImrtResponse;
import com.intel.imrt.response.impl.ImrtAppConfigCheckPointResponseImpl;
import com.intel.imrt.response.impl.ImrtAppConfigWatcherPointResponseImpl;
import com.intel.imrt.response.impl.ImrtBaseResponseImpl;
import com.intel.imrt.response.impl.ImrtDataResponseImpl;
import com.intel.imrt.response.impl.ImrtRDMonitorResponseImpl;
import com.intel.imrt.response.impl.ImrtRDResponseImpl;
import com.intel.imrt.response.impl.ImrtResouceResponseImpl;
import com.intel.imrt.task.AutoResponder;
import com.intel.imrt.util.FileUtil;

public class ImrtCoapServer extends CoapServer {

    private static NetworkConfig config;
    private static final int COAP_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_PORT);

    /**
     * Add individual endpoints listening on default CoAP port on all IPv4 addresses of all network interfaces.
     */
    private void addEndpoints() {
        for (InetAddress addr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
            // only binds to IPv4 addresses and localhost
            if (addr instanceof Inet4Address || addr.isLoopbackAddress()) {
                InetSocketAddress bindToAddress = new InetSocketAddress(addr, COAP_PORT);
                addEndpoint(new CoapEndpoint(bindToAddress));
            }
        }
    }

    /*
     * Constructor for a new Hello-World server. Here, the resources
     * of the server are initialized.
     */
    public ImrtCoapServer(String testCaseName) {

        FileUtil.setTestCaseName(testCaseName);
        ImrtCoapResource rdMonitorResouce = new ImrtCoapResource("monitor", new ImrtRDMonitorResponseImpl());
        ImrtCoapResource rdResouce = new ImrtCoapResource("rd", new ImrtRDResponseImpl());
        // RD/MONITOR
        rdResouce.add(rdMonitorResouce);
        // RD server
        add(rdResouce);
        // REFRESHER
        add(new ImrtCoapResource("refresher", new ImrtDataResponseImpl()));

        ImrtCoapResource appConfigRootResouce = new ImrtCoapResource("ams", null);
        ImrtCoapResource appConfigCheckPointResouce = new ImrtCoapResource("config_checkpoint", new ImrtAppConfigCheckPointResponseImpl());
        ImrtCoapResource appConfigWatcherPointResouce = new ImrtCoapResource("config_watcher", new ImrtAppConfigWatcherPointResponseImpl());
        ImrtCoapResource appConfigWatcherProductIdResouce = new ImrtCoapResource("product_id", null);
        appConfigRootResouce.add(appConfigCheckPointResouce);
        appConfigRootResouce.add(appConfigWatcherProductIdResouce);
        add(appConfigRootResouce);
        add(appConfigWatcherPointResouce);

        addDeiveResouce(FileUtil.FILENAME_DEVICES_DATA);
        int msgSize = 1024 *100;
        config = NetworkConfig.createStandardWithoutFile()
                .setInt(NetworkConfig.Keys.MAX_MESSAGE_SIZE, msgSize)
                .setInt(NetworkConfig.Keys.UDP_CONNECTOR_DATAGRAM_SIZE, msgSize)
                .setInt(NetworkConfig.Keys.PREFERRED_BLOCK_SIZE, msgSize);

        addEndpoints();

        AutoResponder.getInstance().readResource(FileUtil.FILENAME_JSON_DATA_);
        AutoResponder.getInstance().start();
    }

    private void addDeiveResouce(String fileName) {
        String jsonText = FileUtil.readFile(fileName);
        JSONArray jsonArray = JSON.parseArray(jsonText);
        int size = jsonArray.size();
        CoapResource devResource = new ImrtCoapResource("dev", null);
        add(devResource);
        for (int i = 0; i < size; i++) {
            JSONObject device = jsonArray.getJSONObject(i);
            String deivceId = device.getString("di");
            //String deivceAddr = device.getString("addr");
            //String uri = deivceAddr + "/" + deivceId;
            CoapResource deviceResource = new ImrtCoapResource(deivceId, null);
            devResource.add(deviceResource);
            JSONArray linkArray = device.getJSONArray("links");
            int linkSize = linkArray.size();
            for (int j = 0; j < linkSize; j++) {
                String href = linkArray.getJSONObject(j).getString("href");
                if (href == null || href.length() == 0) {
                    continue;
                } else if (href.startsWith("/")) {
                    href = href.substring(1);
                }
                String[] h = href.split("/");
                Resource subResource = deviceResource.getChild(h[0]);
                if (subResource == null) {
                    subResource = new ImrtCoapResource(h[0], new ImrtResouceResponseImpl());
                    deviceResource.add(subResource);
                }
                for (int k = 1; k < h.length; k++) {
                    Resource child = subResource.getChild(h[k]);
                    if (child == null) {
                        ImrtCoapResource coapResource = new ImrtCoapResource(h[k], new ImrtResouceResponseImpl());
                        subResource.add(coapResource);
                        subResource = coapResource;
                    } else {
                        subResource = child;
                    }
                }
                // add instance resource
                if (linkArray.getJSONObject(j).containsKey("inst")) {
                    List<String> instances = JSON.parseObject(linkArray.getJSONObject(j).getString("inst"), new TypeReference<ArrayList<String>>() {
                    });
                    for (String s : instances) {
                        if (subResource.getChild(s) == null) {
                            subResource.add(new ImrtCoapResource(s, new ImrtResouceResponseImpl()));
                        }
                    }
                }
            }
        }
    }

    /*
     * Definition of the Hello-World Resource
     */
    class ImrtCoapResource extends CoapResource {

        private ImrtResponse imrtResponse;

        public ImrtCoapResource(String name, ImrtResponse imrtResponse) {
            super(name);
            this.imrtResponse = imrtResponse;
            if (this.imrtResponse == null) {
                this.imrtResponse = new ImrtBaseResponseImpl();
            }
            getAttributes().setTitle("Imrt-" + name);
        }

        @Override
        public Resource getChild(String name) {
            Resource resource = super.getChild(name);
            if(resource==null){
                resource = super.getChild("*");
            }
            return resource;
        }



        @Override
        public void handleGET(CoapExchange exchange) {
            imrtResponse.get(exchange);
        }

        @Override
        public void handlePOST(CoapExchange exchange) {
            imrtResponse.post(exchange);
        }

        @Override
        public void handleDELETE(CoapExchange exchange) {
            imrtResponse.delete(exchange);
        }

        @Override
        public void handlePUT(CoapExchange exchange) {
            imrtResponse.put(exchange);
        }

        @Override
        public void handleRequest(Exchange exchange) {
            // TODO Auto-generated method stub
            String s = org.eclipse.californium.core.Utils.prettyPrint(exchange.getRequest());
            LogUtil.log(s);
            super.handleRequest(exchange);
        }
    }
}
