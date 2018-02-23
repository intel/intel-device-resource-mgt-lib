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

import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.intel.idrml.iagent.framework.AppConfigWatchPoint.ConfigNotification;
import com.intel.idrml.iagent.model.ResourceDataOCF;
import com.intel.idrml.iagent.model.ResourcePropertyData;
import com.intel.idrml.iagent.model.ResourcePropertyData.ValueType;
import com.intel.idrml.iagent.utilities.LogUtil;

final class Utils {

    public static int findPort() {
        DatagramSocket s = null;
        try {
            s = new DatagramSocket();
            LogUtil.log(LogUtil.LEVEL.INFO, "findPort() =" + s.getLocalPort());
            return s.getLocalPort();
        } catch (Exception e) {
            return Constants.COAPSERVER_PORT_REMOTE_AMS+1;
        } finally {
            if (s != null) {
                s.close();
            }
        }

    }

    public static String composePurl(String scheme, int port, String path) {
        StringBuilder builder = new StringBuilder()
                .append(scheme).append("://").append(Constants.COAPSERVER_HOST_LOCAL).append(":").append(port).append("/").append(path);
        return builder.toString();
    }

    public static String composePurl(String scheme, String path) {
        StringBuilder builder = new StringBuilder()
                .append(scheme).append("://").append(Constants.COAPSERVER_HOST_LOCAL).append(":").append(Constants.COAPSERVER_PORT_LOCAL).append("/").append(path);
        return builder.toString();
    }

    public static String getRandomString() {
        int length = 10;
        String base = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random(System.currentTimeMillis());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }
    
	public static ConfigNotification parseConfigNotification(String text)
	{
        JSONObject jsonObject = JSONObject.parseObject(text);
        if (jsonObject == null) {
            return null;
        }
        ConfigNotification data = new ConfigNotification();
        if (jsonObject.containsKey("software")) data.product = jsonObject.getString("software");
        if (jsonObject.containsKey("target_type")) data.target_type = jsonObject.getString("target_type");
        if (jsonObject.containsKey("target_id")) data.target_id = jsonObject.getString("target_id");
        if (jsonObject.containsKey("config_path")) data.config_path = jsonObject.getString("config_path");

		return data;
	}
    
}
