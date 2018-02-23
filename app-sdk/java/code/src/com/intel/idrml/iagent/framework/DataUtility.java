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

import javax.rmi.CORBA.Util;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;

import com.intel.idrml.iagent.model.DataQueryParam;
import com.intel.idrml.iagent.model.Resource;
import com.intel.idrml.iagent.model.ResourceDataGeneral;
import com.intel.idrml.iagent.utilities.LogUtil;
import com.intel.idrml.iagent.utilities.PayloadParser;

import org.eclipse.californium.core.coap.MediaTypeRegistry;

class DataUtility {
    private String uri;

    public DataUtility() {
        this(Constants.COAPSERVER_HOST_REMOTE_IAGENT, Constants.COAPSERVER_PORT_REMOTE_IAGENT);
    }

    public DataUtility(String ip, int port) {
        uri = "coap://" + ip + ":" + port;
    }

    // @return observerPointID
    public String startDataObserver(DataQueryParam query, OnDataListener eventHandler) throws CoapException {
        String tempUri = uri + Constants.URL_REFRESHER;
        MessageDataMonitor payload = new MessageDataMonitor();
        payload.deviceID = query.deviceID;
        payload.resourceURI = query.resouceUri;
        payload.interval = query.interval;
        payload.sequence = query.sequence;
        payload.process = query.process?1:0;
      
        payload.localPath = Utils.getRandomString();
        payload.pushURL = Utils.composePurl(CoAP.COAP_URI_SCHEME, DataCoapServerManager.getInstance().getPort(), payload.localPath);
        LogUtil.log(LogUtil.LEVEL.INFO, "POST : " + tempUri +"   payload: \n"+payload.toJsonString());
        CoapClient client = new CoapClient(tempUri);
        client.setTimeout(Constants.TIMEOUT_GET);
        DataCoapServerManager.getInstance().addResource(payload.localPath+Constants.URL_REFRESHER_EXT);
        CoapResponse response = client.post(payload.toJsonString(), MediaTypeRegistry.APPLICATION_JSON);
        if (response == null) {
            DataCoapServerManager.getInstance().deleteResource(payload.localPath);
            throw new CoapException("No response received.");
        }
        if (ResponseCode.isSuccess(response.getCode())) {
            String monitorId = response.getResponseText();
            LogUtil.log(LogUtil.LEVEL.INFO, "POST : " + tempUri +"  resp : " + monitorId);
            payload.monitorId = monitorId;
            DataCoapServerManager.getInstance().addDataListerner(payload, eventHandler);
            return monitorId;
        } else {
            DataCoapServerManager.getInstance().deleteResource(payload.localPath);
            LogUtil.log(LogUtil.LEVEL.INFO, "resp with error code: " + response.getCode());
            throw new CoapException(response);
        }
    }

    public boolean stopDataObserver(String observerPointID) throws CoapException {
        MessageDataMonitor payload = DataCoapServerManager.getInstance().getDataHandlersKeyByMonitorID(observerPointID);
        if (payload == null) {
            return true;
        }
        String tempUri = uri + Constants.URL_REFRESHER + "?id=" + observerPointID;
        CoapClient client = new CoapClient(tempUri);
        client.setTimeout(Constants.TIMEOUT_GET);
        LogUtil.log(LogUtil.LEVEL.INFO, "DELETE : " + tempUri);
        CoapResponse response = client.delete();
        if (response == null) {
            throw new CoapException("No response received.");
        }
        if (ResponseCode.isSuccess(response.getCode())) {
            DataCoapServerManager.getInstance().removeDataListerner(payload.monitorId);
            return true;
        } else {
            throw new CoapException(response);
        }
    }

    //one time query
    public ResourceDataGeneral DoResourceGET(Resource resource) throws CoapException {
        return DoResourceGET(Constants.getIAgentServerUri()+resource.getAbsoluteUri());
    }

    //one time query
    public ResourceDataGeneral DoResourceGET(String resourceUrl) throws CoapException {
        CoapClient client = new CoapClient(resourceUrl);
        client.setTimeout(Constants.TIMEOUT_GET);
        LogUtil.log(LogUtil.LEVEL.INFO, "GET : " + resourceUrl);
        CoapResponse response = client.get();
        if (response == null) {
            throw new CoapException("No response received.");
        }
        if (ResponseCode.isSuccess(response.getCode())) {
            int format = response.getOptions().getContentFormat();
            PayloadParser parser = DataCoapServerManager.getInstance().getParser(format);
            String requestText = response.getResponseText();
            LogUtil.log(LogUtil.LEVEL.INFO, "Response : " + requestText);
            ResourceDataGeneral data = null;
            if (parser == null) {
                data = new ResourceDataGeneral(requestText);
            } else {
                data = parser.parse(requestText);
            }
            return data;
        } else {
            throw new CoapException(response);
        }
    }

    //one time query
    public String DoResourcePost(String resourceUrl, int format, String payload) throws CoapException {
    	if(resourceUrl==null) return "error: resource URL is null";
        CoapClient client = new CoapClient(resourceUrl);
        client.setTimeout(Constants.TIMEOUT_GET);
        LogUtil.log(LogUtil.LEVEL.INFO, "POST : " + resourceUrl);
        CoapResponse response = client.post(payload, format);
        if (response == null) {
            throw new CoapException("No response received.");
        }
        if (ResponseCode.isSuccess(response.getCode())) {
            String requestText = response.getResponseText();
            return requestText;
        } else {
            throw new CoapException(response);
        }
    }

    //one time write
    public boolean DoResourcePUT(Resource resource, int format, String payload) throws CoapException {
    	if(resource==null) return false;
        String url = Constants.getIAgentServerUri()+resource.getAbsoluteUri();
        CoapClient client = new CoapClient(url);
        client.setTimeout(Constants.TIMEOUT_GET);
        LogUtil.log(LogUtil.LEVEL.INFO, "PUT : " + url);
        CoapResponse response = client.put(payload, format);
        if (response == null) {
            throw new CoapException("No response received.");
        }
        if (ResponseCode.isSuccess(response.getCode())) {
            String requestText = response.getResponseText();
            LogUtil.log(LogUtil.LEVEL.INFO, "Response : " + requestText);
            return true;
        } else {
            throw new CoapException(response);
        }
    }

    public boolean DoResourcePUT(String resourceUrl, int format, String payload) throws CoapException {
        CoapClient client = new CoapClient(Constants.getIAgentServerUri()+resourceUrl);
        client.setTimeout(Constants.TIMEOUT_GET);
        LogUtil.log(LogUtil.LEVEL.INFO, "PUT : " + resourceUrl);
        CoapResponse response = client.put(payload, format);
        if (response == null) {
            throw new CoapException("No response received.");
        }
        if (ResponseCode.isSuccess(response.getCode())) {
            return true;
        } else {
            throw new CoapException(response);
        }
    }

    public void registerPayloadParser(int formatType, PayloadParser parser) {
        DataCoapServerManager.getInstance().registerParser(formatType, parser);
    }

}
