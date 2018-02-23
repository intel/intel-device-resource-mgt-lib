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

package com.intel.imrt.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;

import com.alibaba.fastjson.JSONObject;
import com.intel.imrt.iagent.utilities.LogUtil;
import com.intel.imrt.iagent.utilities.MediaTypeFormat;
import com.intel.imrt.util.FileUtil;

public class AutoResponder {

    private List<AutoResponderEntry> listResource = Collections.synchronizedList(new ArrayList<AutoResponderEntry>());
    private Map<String, String> dataMap = new ConcurrentHashMap<String, String>();
    public static final String RESPONDER_TYPE_RD_MONITOR = "RD_MONITOR";
    public static final String RESPONDER_TYPE_DATA_MONITOR = "DATA_MONITOR";
    public static final String RESPONDER_TYPE_APPCONFIG = "APPCONFIG";

    private static AutoResponder instance = new AutoResponder();

    public static AutoResponder getInstance() {
        return instance;
    }

    private int index = 0;

    public void start() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (listResource.isEmpty()) {
                    return;
                }
                try {
                    if (index >= listResource.size()) {
                        index = 0;
                    }
                    post(listResource.get(index));
                } catch (Exception e) {
                    e.printStackTrace();
                    listResource.remove(index);
                    //index++;
                }
            }
        };
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleWithFixedDelay(runnable, 2, 2, TimeUnit.SECONDS);
    }

    public void addEntry(String type, String purl) {
        AutoResponderEntry r = new AutoResponderEntry(type, purl);
        listResource.add(r);
        LogUtil.log("add entry [ uri : " + purl + " type:" + type + "]");
    }

    public void readResource(String fileName) {
        ArrayList<AutoResponderEntry> data = new ArrayList<AutoResponderEntry>();
        try {
            String jsonStr = FileUtil.readFile(fileName);
            List<AutoResponderEntry> _list = JSONObject.parseArray(jsonStr, AutoResponderEntry.class);
            data.addAll(_list);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (AutoResponderEntry e : data) {
            dataMap.put(e.type + e.purl, e.filename);
            LogUtil.log("add resource data :[uri : " + e.purl + " type:" + e.type + " filename:" + e.filename + "]");
        }
    }

    private void post(AutoResponderEntry entry) throws Exception {
        String payload = getPayload(entry);
        if (RESPONDER_TYPE_RD_MONITOR.equals(entry.type)) {
            sendData(payload, entry, CoAP.Code.POST, CoAP.Type.NON);
        } else {
            sendData(payload, entry, CoAP.Code.POST, CoAP.Type.ACK);
        }
    }

    private String getPayload(AutoResponderEntry entry) {
        String fileName = dataMap.get(entry.type + entry.purl);
        if (fileName == null) {
            fileName = dataMap.get(entry.type);
        }
        String jsonPlyload = "";
        if (fileName == null) {
            if (RESPONDER_TYPE_RD_MONITOR.equals(entry.type)) {
                jsonPlyload = FileUtil.readFile("rd_monitor_post_devices.txt");
            } else if (RESPONDER_TYPE_DATA_MONITOR.equalsIgnoreCase(entry.type)) {
                //jsonPlyload = FileUtil.readFile("data_monitor_post_lwm2m.txt");
                jsonPlyload = FileUtil.readFile("data_monitor_post_ocf.txt");
            } else if (RESPONDER_TYPE_APPCONFIG.equalsIgnoreCase(entry.type)) {
                jsonPlyload = FileUtil.readFile("appconfig_post_watch_point.txt");
            }
        } else {
            jsonPlyload = FileUtil.readFile(fileName);
        }
        return jsonPlyload;
    }

    private void sendData(String data, AutoResponderEntry entry, CoAP.Code code, CoAP.Type msgType) throws Exception {
        CoapClient client = new CoapClient(entry.purl);
        Request request = new Request(code, msgType);
        request.setPayload(data);
        request.getOptions().setContentFormat(MediaTypeFormat.APPLICATION_JSON);
        CoapResponse response = client.advanced(request);
        LogUtil.log("Start sending data to URI : " + entry.purl);
        if (CoAP.Type.NON.equals(msgType)) {
            listResource.remove(index);
            LogUtil.log("Send data to " + entry.purl + " successfully");
        } else {
            if (response == null) {
                throw new Exception("No response received.");
            }
            if (ResponseCode.isSuccess(response.getCode())) {
                listResource.remove(index);
                LogUtil.log("Send data to " + entry.purl + " successfully");
            } else {
                String s = org.eclipse.californium.core.Utils.prettyPrint(response);
                LogUtil.log("Send unsuccessful:" + s);
                throw new Exception("Error post uri:" + entry.purl + " code:" + response.getCode() + " payload: \n" + data);
            }
        }
    }
}
