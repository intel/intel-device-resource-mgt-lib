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

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.Resource;

class MyCoapServer {
    private CoapServer server;
    private  int port;
    private static MyCoapServer instance = new MyCoapServer(Utils.findPort());
    private boolean started = false;

    public MyCoapServer(int port) {
        this.port = port;
        server = new CoapServer();
    }

    public static MyCoapServer getInstance() {
        if (instance == null)
            instance = new MyCoapServer(Utils.findPort());

        return instance;
    }
    
    protected int getPort(){
        return port;
    }
    public synchronized void startServer() {
        if (started) {
            return;
        }
        NetworkConfig networkConfig = NetworkConfig.getStandard();
        networkConfig.setInt(NetworkConfig.Keys.MAX_RESOURCE_BODY_SIZE, 204800);
        server.addEndpoint(new CoapEndpoint(port, networkConfig));
        server.start();
        started = true;
    }

    public synchronized void stopServer() {
        if (!started) {
            return;
        }
        server.stop();
        started = false;
    }

    public void addResource(Resource listerenCoapResource) {
        server.add(listerenCoapResource);
    }
    
    protected void removeResource(Resource resource){
       server.remove(resource);
    }

    protected Resource getRootResource() {
        return server.getRoot();
    }

}
