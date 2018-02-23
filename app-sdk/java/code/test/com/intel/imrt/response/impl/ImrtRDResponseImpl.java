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

import com.intel.imrt.response.ImrtResponse;
import com.intel.imrt.util.FileUtil;

public class ImrtRDResponseImpl implements ImrtResponse {

    @Override
    public void get(CoapExchange exchange) {
        // TODO Auto-generated method stub
        String dt = exchange.getQueryParameter("dt");
        String rt = exchange.getQueryParameter("rt");
        String group = exchange.getQueryParameter("group");
        String groups = exchange.getQueryParameter("groups");
        String status = exchange.getQueryParameter("status");
        String di = exchange.getQueryParameter("di");
        String fileName = "rd_get_devices.txt";
         /* if (di != null && di.length() > 0) {
            fileName = "rd_get_di_" + di + ".txt";
        }*/
        String jsonPlyload = FileUtil.readFile(fileName);
        exchange.respond(ResponseCode.CHANGED, jsonPlyload);
    }

    @Override
    public void post(CoapExchange exchange) {
        // TODO Auto-generated method stub
        String jsonPlyload = FileUtil.readFile("rd_post_devices.txt");
        exchange.respond(ResponseCode.CHANGED, jsonPlyload);
    }

    @Override
    public void put(CoapExchange exchange) {
        // TODO Auto-generated method stub
        exchange.respond(ResponseCode.CHANGED);
    }

    @Override
    public void delete(CoapExchange exchange) {
        // TODO Auto-generated method stub
        exchange.respond(ResponseCode.CHANGED);
    }

}
