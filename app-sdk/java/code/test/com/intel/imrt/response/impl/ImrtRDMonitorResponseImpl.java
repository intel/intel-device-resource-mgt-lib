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

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

import com.alibaba.fastjson.JSONObject;
import com.intel.imrt.iagent.utilities.LogUtil;
import com.intel.imrt.model.RDMonitorPayload;
import com.intel.imrt.response.ImrtResponse;
import com.intel.imrt.task.AutoResponder;

public class ImrtRDMonitorResponseImpl implements ImrtResponse {

    @Override
    public void get(CoapExchange exchange) {
        // TODO Auto-generated method stub
        exchange.respond(ResponseCode.CHANGED);
    }

    @Override
    public void put(CoapExchange exchange) {
        // TODO Auto-generated method stub
        exchange.respond(ResponseCode.CHANGED);
    }

    @Override
    public void post(CoapExchange exchange) {
        // TODO Auto-generated method stub
        String requestText = exchange.getRequestText();
        LogUtil.log("resp: "+requestText);

        RDMonitorPayload payload = JSONObject.parseObject(requestText, RDMonitorPayload.class);
        // add CoapResource
        String purl = payload.getPurl();
        AutoResponder.getInstance().addEntry(AutoResponder.RESPONDER_TYPE_RD_MONITOR, purl);
        String monitorId = "id" + System.currentTimeMillis();
        exchange.respond(ResponseCode.CHANGED, monitorId);
    }

    @Override
    public void delete(CoapExchange exchange) {
        // TODO Auto-generated method stub
        exchange.respond(ResponseCode.CHANGED);
    }

}
