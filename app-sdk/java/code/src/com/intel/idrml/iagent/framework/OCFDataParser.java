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
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.intel.idrml.iagent.model.ResourceDataGeneral;
import com.intel.idrml.iagent.model.ResourceDataOCF;
import com.intel.idrml.iagent.model.ResourcePropertyData;
import com.intel.idrml.iagent.model.ResourcePropertyData.ValueType;
import com.intel.idrml.iagent.utilities.PayloadParser;

class OCFDataParser implements PayloadParser {

    @Override
    public ResourceDataGeneral parse(String text) {
        try {
            JSONObject jsonObject = JSONObject.parseObject(text);
            if (jsonObject == null) {
                return null;
            }
            ResourceDataOCF data = new ResourceDataOCF(text);
            String fixedKeyPrm = "prm";
            Set<String> keySet = jsonObject.keySet();
            List<ResourcePropertyData> items = new ArrayList<ResourcePropertyData>();
          // OCF + JSON
            if (jsonObject.containsKey(fixedKeyPrm)) {
                String prmJson = jsonObject.getString(fixedKeyPrm);
                Map<String, String> prm = JSON.parseObject(prmJson, new TypeReference<Map<String, String>>() {
                });
                data.prm = prm;
            } else {// MODBUS + JSON
                data.prm = new HashMap<String,String>();
            }
            for (String keyStr : keySet) {
                if (!fixedKeyPrm.equals(keyStr)) {
                    ResourcePropertyData propertyData = new ResourcePropertyData();
                    propertyData.propName = keyStr;
                    propertyData.sv = jsonObject.getString(keyStr);
                    propertyData.valueType = ValueType.STRING;
                    items.add(propertyData);
                }
            }
            data.items = items;
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
