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

import java.util.Map;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import com.alibaba.fastjson.JSONObject;
import com.intel.idrml.iagent.utilities.LogUtil;

class ApplicationConfigeUtility {
    private static ApplicationConfigeUtility instance;
    private Map<String, OnConfigurationListener> configHandlers;
    private static String appName;
    private String ip;
    private int port;
    private String tartgetID;
    private String uri;

    public ApplicationConfigeUtility() {
        this("iMRT");
    }

    public ApplicationConfigeUtility(String AppName) {
        appName = AppName;
        ip = Constants.COAPSERVER_HOST_REMOTE_AMS;
        port = Constants.COAPSERVER_PORT_REMOTE_AMS;
        uri = "coap://" + ip + ":" + port;
    }

    public void setAMSAddr(String ip, int port) {
        this.ip = ip;
        this.port = port;
        uri = "coap://" + ip + ":" + port;
    }

    //target_id, optional
    public void setTargetId(String deviceId) {
        this.tartgetID = deviceId;
    }

    //@return listener ID
    public String addConfigurationMonitor(String targetType, String targetId, OnConfigurationListener eventHandler) throws CoapException {
        boolean add = checkPoint(AmsRequestAction.ADD, targetType, targetId);
        if (add) {
            return addWatchPoint(eventHandler);
        } else {
            return null;
        }
    }

    public boolean removeConfigurationMonitor(String targetType, String targetId) throws CoapException {
        boolean remove = checkPoint(AmsRequestAction.DELETE, targetType, targetId);
        if (remove) {
            return deleteWatchPoint();
        } else {
            return false;
        }
    }

    public boolean checkPoint(AmsRequestAction action, String targetType, String targetId) throws CoapException {
        String tempUri = uri + Constants.URL_APPCONFIG_CHECKPOINT;
        AppConfigCheckPoint payload = new AppConfigCheckPoint(action, appName, targetType, targetId);
        CoapClient client = new CoapClient(tempUri);
        client.setTimeout(Constants.TIMEOUT_GET);
        LogUtil.log(LogUtil.LEVEL.INFO, "PUT : " + tempUri+"  payload: \n"+payload.toJson());
        CoapResponse response = client.put(payload.toJson(), MediaTypeRegistry.APPLICATION_JSON);
        if (response == null) {
            throw new CoapException("No response received.");
        }
        if (ResponseCode.isSuccess(response.getCode())) {
            return true;
        } else {
            throw new CoapException(response);
        }
    }

    public boolean deleteWatchPoint() throws CoapException {
        String tempUri = uri + Constants.URL_APPCONFIG_WATCHER;
        String tempPath = Utils.getRandomString();
        AppConfigWatchPoint payload = new AppConfigWatchPoint(AmsRequestAction.DELETE, appName, Constants.COAPSERVER_HOST_LOCAL, AppConfigCoapServerManager.getInstance().getPort(), tempPath);
        CoapClient client = new CoapClient(tempUri);
        client.setTimeout(Constants.TIMEOUT_GET);
        LogUtil.log(LogUtil.LEVEL.INFO, "PUT : " + tempUri);
        CoapResponse response = client.put(payload.toJson(), MediaTypeRegistry.APPLICATION_JSON);
        if (response == null) {
            throw new CoapException("No response received.");
        }
        if (ResponseCode.isSuccess(response.getCode())) {
            AppConfigCoapServerManager.getInstance().removeConfiguationListerner();
            return true;
        } else {
            throw new CoapException(response);
        }
    }

    public String addWatchPoint(OnConfigurationListener eventHandler) throws CoapException {
        if (eventHandler == null) {
            throw new CoapException("OnConfigurationListener cannot be empty or null.");
        }
        String tempUri = uri + Constants.URL_APPCONFIG_WATCHER;
        String tempPath = Utils.getRandomString();
        AppConfigWatchPoint payload = new AppConfigWatchPoint(AmsRequestAction.ADD, appName, Constants.COAPSERVER_HOST_LOCAL, AppConfigCoapServerManager.getInstance().getPort(), tempPath);
        CoapClient client = new CoapClient(tempUri);
        client.setTimeout(Constants.TIMEOUT_GET);

        LogUtil.log(LogUtil.LEVEL.INFO, "PUT : " + tempUri+"   payload: \n"+payload.toJson());
        AppConfigCoapServerManager.getInstance().addResouce(tempPath);
        CoapResponse response = client.put(payload.toJson(), MediaTypeRegistry.APPLICATION_JSON);
        if (response == null) {
            AppConfigCoapServerManager.getInstance().deleteResouce(tempPath);
            throw new CoapException("No response received.");
        }
        if (ResponseCode.isSuccess(response.getCode())) {
            AppConfigCoapServerManager.getInstance().addConfigurationListerner(payload, eventHandler);
            LogUtil.log(LogUtil.LEVEL.INFO, "PUT resp payload: "+response.getResponseText());
            return response.getResponseText();
        } else {
            AppConfigCoapServerManager.getInstance().deleteResouce(tempPath);
            throw new CoapException(response);
        }
    }

    public static ApplicationConfigeUtility getInstance(String productName) {
        if (instance == null) {
            instance = new ApplicationConfigeUtility(productName);
        }
        appName = productName;
        return instance;
    }

    public boolean setProductID(String productName, String productId) throws CoapException {
        String path =uri + Constants.URL_AMS_SET_PRODUCTID;
        CoapClient client = new CoapClient(path);
        client.setTimeout(Constants.TIMEOUT_GET);

        JSONObject payloadJson = new JSONObject();
        payloadJson.put("product", productName);
        payloadJson.put("id", productId);
        LogUtil.log(LogUtil.LEVEL.INFO, "PUT : " + path);
        CoapResponse response = client.put(payloadJson.toJSONString(), MediaTypeRegistry.APPLICATION_JSON);
        if (ResponseCode.isSuccess(response.getCode())) {
            return true;
        } else {
            throw new CoapException(response);
        }
    }

	public void setAppName(String productName) {
		appName = productName;
	}
}
