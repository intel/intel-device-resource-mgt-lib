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
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.intel.idrml.iagent.model.ResourceDataGeneral;
import com.intel.idrml.iagent.model.ResourceDataLWM2M;
import com.intel.idrml.iagent.model.ResourcePropertyData;
import com.intel.idrml.iagent.utilities.PayloadParser;

class LWM2MDataParser implements PayloadParser {

    @Override
    public ResourceDataGeneral parse(String text) {
        try {
            JSONObject jsonObject = JSONObject.parseObject(text);
            if (jsonObject == null) {
                return null;
            }
            List<ResourcePropertyData> propertyItems = new ArrayList<ResourcePropertyData>();
            // use bt--base time to update each item
            Integer bt = jsonObject.getInteger("bt");
            JSONArray jsonArray = jsonObject.getJSONArray("e");
            int size = jsonArray.size();
            for (int i = 0; i < size; i++) {
                JSONObject elementJson = jsonArray.getJSONObject(i);
                ResourcePropertyData propertyData = new ResourcePropertyData();
                propertyData.propName = elementJson.getString("n");
                if (elementJson.containsKey("v")) {
                    propertyData.v = elementJson.getFloatValue("v");
                    propertyData.valueType = ResourcePropertyData.ValueType.FLOAT;
                } else if (elementJson.containsKey("bv")) {
                    propertyData.bv = elementJson.getBooleanValue("bv");
                    propertyData.valueType = ResourcePropertyData.ValueType.BOOLEAN;
                } else if (elementJson.containsKey("sv")) {
                    propertyData.sv = elementJson.getString("sv");
                    propertyData.valueType = ResourcePropertyData.ValueType.STRING;
                }
                if (bt != null) {
                    propertyData.t = elementJson.getIntValue("t");
                }
                propertyItems.add(propertyData);
            }

            if (bt != null) {
                for (ResourcePropertyData item : propertyItems) {
                    item.t += bt;
                }
            }
            ResourceDataLWM2M data = new ResourceDataLWM2M(text);
            data.items = propertyItems;
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
