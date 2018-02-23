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
import java.util.HashMap;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.intel.idrml.iagent.model.DeviceInfo;
import com.intel.idrml.iagent.model.Resource;
import com.intel.idrml.iagent.model.DeviceInfo.DeviceStatus;
import com.intel.idrml.iagent.model.DeviceInfo.StandardType;

class MessageParseUtil {

    public static List<DeviceInfo> parseDevice(String dataInJson) {
        if (dataInJson == null || "".equals(dataInJson)) {
            return null;
        }
        try {
            List<DeviceInfo> deviceList = new ArrayList<DeviceInfo>();
            JSONArray jsonArray = JSONArray.parseArray(dataInJson);
            int s = jsonArray.size();
            for (int i = 0; i < s; i++) {
                JSONObject deviceJsonObject = jsonArray.getJSONObject(i);
                DeviceInfo device = new DeviceInfo();
                device.deviceId = deviceJsonObject.getString("di");
                device.deviceStatus = DeviceStatus.valueOfType(deviceJsonObject.getString("status"));
                device.standardType = StandardType.valueOfType(deviceJsonObject.getString("st"));
                device.addr = deviceJsonObject.getString("addr");
                device.groups = JSON.parseObject(deviceJsonObject.getString("groups"), new TypeReference<ArrayList<String>>() {
                });
                device.attrs = JSON.parseObject(deviceJsonObject.getString("attrs"), new TypeReference<HashMap<String, String>>() {
                });
                device.deviceType = deviceJsonObject.getString("dt");
                device.sleepType = deviceJsonObject.getString("set");
                List<Resource> resourceList = parseResource(deviceJsonObject.getString("links"),device);
                device.resources = resourceList;
                deviceList.add(device);
            }
            return deviceList;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<Resource> parseResource(String jsonText,DeviceInfo device) {
        List<Resource> resourceList = new ArrayList<Resource>();
        JSONArray jsonArray = JSONArray.parseArray(jsonText);
        int s = jsonArray.size();
        for (int i = 0; i < s; i++) {
            JSONObject resourceJSONObject = jsonArray.getJSONObject(i);
            String href = resourceJSONObject.getString("href");
            String attrs = resourceJSONObject.getString("attrs");
            List<String> resourceType = JSON.parseObject(resourceJSONObject.getString("rt"), new TypeReference<ArrayList<String>>() {
            });
            List<String> groups = JSON.parseObject(resourceJSONObject.getString("groups"), new TypeReference<ArrayList<String>>() {
            });
            if (resourceJSONObject.containsKey("inst")) {
                List<String> instances = JSON.parseObject(resourceJSONObject.getString("inst"), new TypeReference<ArrayList<String>>() {
                });
                for (String inst : instances) {
                    Resource resource = new Resource();
                    resource.href = href + "/" + inst;
                    resource.attrs = attrs;
                    resource.resourceType = resourceType;
                    resource.groups = groups;
                    resource.device = device;
                    resourceList.add(resource);
                }
            } else {
                Resource resource = new Resource();
                resource.href = href;
                resource.attrs = attrs;
                resource.resourceType = resourceType;
                resource.groups = groups;
                resource.device = device;
                resourceList.add(resource);
            }
        }
        return resourceList;
    }

}
