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
 
 
package com.intel.idrml.iagent.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class DeviceInfo {
    public String deviceId;
    public StandardType standardType;
    public String deviceType;
    public Map<String, String> attrs; // json format {"k1":"v1","k2":"v2"...} configured in cloud
    public String sleepType; // sleep type
    public int ttl; //heart rate between device and gateway
    public String addr;
    public List<Resource> resources; // TODO Resources, but it is defined as links in interface
    public List<String> groups;
    public DeviceStatus deviceStatus; // status, it is defined as "s" in interface

    public enum StandardType {
        lwm2m {@Override public String toString() {return "lwm2m";}},
        ocf {@Override public String toString() {return "ocf";}},
        rest {@Override public String toString() {return "rest";}},
        publish {@Override public String toString() {return "publish";}},
        modbus {@Override public String toString() {return "modbus";}},
        unknow {@Override public String toString() {return "unknow";}};
        public static StandardType valueOfType(String value) {
            if(value == null || value.trim().length() == 0){
                return unknow;
            }
            StandardType[] array = values();
            for (StandardType st : array) {
                if (value.toLowerCase().equals(st.toString())) {
                    return st;
                }
            }
            return unknow;
        }
    }
    public enum DeviceStatus {
        on { @Override public String toString() { return "on"; } },  
        off  { @Override public String toString() { return "off"; } },  
        unknow  { @Override public String toString() { return "unknow"; } };
        public static DeviceStatus valueOfType(String value) {
            if(value == null || value.trim().length() == 0){
                return unknow;
            }
            DeviceStatus[] array = values();
            for (DeviceStatus ds : array) {
                if (value.toLowerCase().equals(ds.toString())) {
                    return ds;
                }
            }
            return unknow;
        }
    }

    public DeviceInfo() {
        
    }

    public String getDeviceId() {
        return deviceId;
    }

    public StandardType getStandardType() {
        return standardType;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public Map<String, String> getAttrs() {
        return attrs;
    }

    public String getSleepType() {
        return sleepType;
    }

    public int getTtl() {
        return ttl;
    }

    public String getAddr() {
        return addr;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public DeviceStatus getDeviceStatus() {
        return deviceStatus;
    }

    public List<String> getGroups() {
        return groups;
    }

    public List<Resource> getResources(String resourceType) {
        List<Resource> results = new ArrayList<Resource>();
        for (Resource resource : resources) {
            if (resource.getResourceType().equals(resourceType)) {
                results.add(resource);
            }
        }
        return results;
    }

    public Resource getResource(String uri) {
        for (Resource resource : resources) {
            if (resource.getHref().equals(uri)) {
                return resource;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return deviceId.equals(((DeviceInfo) obj).getDeviceId());
    }

    @Override
    public int hashCode() {
        return deviceId.hashCode();
    }

    public String toJson() {
        JSONObject deviceJSONObeject = new JSONObject();
        deviceJSONObeject.put("di", deviceId);
        deviceJSONObeject.put("st", standardType);
        deviceJSONObeject.put("addr", addr);
        deviceJSONObeject.put("dt", deviceType);
        deviceJSONObeject.put("ttl", ttl);
        deviceJSONObeject.put("set", sleepType);
        deviceJSONObeject.put("status", deviceStatus);
        deviceJSONObeject.put("groups", groups);
        deviceJSONObeject.put("attrs", attrs);
        JSONArray resourceJSONArray = new JSONArray();
        if (resources != null && !resources.isEmpty()) {
            for (Resource res : resources) {
                JSONObject obj = new JSONObject();
                obj.put("href", res.href);
                obj.put("rt", res.resourceType);
                obj.put("groups", res.groups);
                obj.put("attrs", res.attrs);
                resourceJSONArray.add(obj);
            }
        }
        deviceJSONObeject.put("links", resourceJSONArray);
        return deviceJSONObeject.toJSONString();
    }

    public String getAbsoluteUri() {
        return "/dev" + "/" + deviceId;
    }
}
