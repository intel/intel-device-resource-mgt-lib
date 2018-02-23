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

import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.intel.idrml.iagent.model.ResourcePropertyData.ValueType;
import com.intel.idrml.iagent.utilities.MediaTypeFormat;

public class ResourceDataLWM2M extends ResourceDataGeneral {

    public List<ResourcePropertyData> items;

    public ResourceDataLWM2M(String rawPayload) {
        super(rawPayload);
        setParsed(true);
    }

    @Override
    public int getFormat() {
        return MediaTypeFormat.APPLICATION_JSON_LWM2M;
    }

    @Override
    public String toJson() {
        // TODO Auto-generated method stub
        if (items == null || items.isEmpty()) {
            return "{\"e\":[]}";
        }
        JSONObject object = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        int size = items.size();
        for (int i = 0; i < size; i++) {
            ResourcePropertyData data = items.get(i);
            JSONObject o = new JSONObject();
            o.put("n", data.propName);
            if (ValueType.BOOLEAN.equals(data.valueType)) {
                o.put("bv", data.bv);
            } else if (ValueType.FLOAT.equals(data.valueType)) {
                o.put("v", data.v);
            } else if (ValueType.STRING.equals(data.valueType)) {
                o.put("sv", data.sv);
            }
            jsonArray.add(o);
        }
        object.put("e", jsonArray);
        return object.toJSONString();
    }

}
