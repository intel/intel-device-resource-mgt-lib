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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;

public class Resource {
    //@SerializedName("href")
    public String href; // uri, defined as href in interface

    public List<String> resourceType;

    //@SerializedName("attrs")
    public String attrs; // configured in cloud

    //@Expose(serialize = false, deserialize = false)
    public DeviceInfo device;

    //@SerializedName("groups")
    public List<String> groups;

    //@Expose(serialize = false, deserialize = false)
    private Map<String, String> propertyConfig;

    //@Expose(serialize = false, deserialize = false)
    private Map<String, String> propertyDataCache; //TODO DataCach shall be defined

    public Resource() {

    }

    public Resource(Resource resource) {
        this.href = resource.href;
        this.resourceType = resource.resourceType;
        this.attrs = resource.attrs;
        this.device = resource.device;
        this.propertyConfig = resource.propertyConfig;
        this.propertyDataCache = resource.propertyDataCache;
        this.groups = resource.groups;
    }

    public String getHref() {
        return href;
    }

    public String getAttrs() {
        return attrs;
    }

    public List<String> getGroups() {
        return groups;
    }

    public String getPropertiesConfig(String propertyKey) {
        return propertyConfig.get(propertyKey);
    }

    //TODO
    public String getCurrentValue(String propertyName) {
        return null;
    }

    public String getAbsoluteUri() {
        if (device != null) {
            return device.getAbsoluteUri() + ((href.startsWith("/"))?(href):("/" + href));
        } else {
            return href;
        }
    }

    public List<String> getResourceType() {
        return resourceType;
    }

    public DeviceInfo getOwnerDevice() {
        return device;
    }

    public List<String> getOwnerGroups() {
        return groups;
    }

    @Override
    public boolean equals(Object obj) {
        return href.equals(((Resource) obj).href);
    }

    @Override
    public int hashCode() {
        return href.hashCode();
    }

}
