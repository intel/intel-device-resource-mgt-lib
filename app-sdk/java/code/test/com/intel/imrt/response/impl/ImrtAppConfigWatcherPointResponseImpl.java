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


package com.intel.imrt.response.impl;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

import com.alibaba.fastjson.JSONObject;
import com.intel.imrt.model.AppConfigMonitor;
import com.intel.imrt.response.ImrtResponse;
import com.intel.imrt.task.AutoResponder;

public class ImrtAppConfigWatcherPointResponseImpl implements ImrtResponse {

    @Override
    public void get(CoapExchange exchange) {
        // TODO Auto-generated method stub
        exchange.respond(ResponseCode.CHANGED);
    }

    @Override
    public void post(CoapExchange exchange) {
        // TODO Auto-generated method stub
        exchange.respond(ResponseCode.CHANGED);
    }

    @Override
    public void put(CoapExchange exchange) {
        // TODO Auto-generated method stub
        String requestTest = exchange.getRequestText();

        AppConfigMonitor payload = JSONObject.parseObject(requestTest, AppConfigMonitor.class);
        // add CoapResource
        String purl = CoAP.COAP_URI_SCHEME + "://" + payload.ip + ":" + payload.port + "/" + payload.path;
        AutoResponder.getInstance().addEntry(AutoResponder.RESPONDER_TYPE_APPCONFIG, purl);
        exchange.respond(ResponseCode.CHANGED);
    }

    @Override
    public void delete(CoapExchange exchange) {
        // TODO Auto-generated method stub
        exchange.respond(ResponseCode.CHANGED);
    }

}
