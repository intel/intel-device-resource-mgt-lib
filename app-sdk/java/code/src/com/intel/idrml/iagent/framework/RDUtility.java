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

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;

import com.intel.idrml.iagent.model.DeviceInfo;
import com.intel.idrml.iagent.model.RDQueryParam;
import com.intel.idrml.iagent.utilities.LogUtil;

import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;

class RDUtility {
    private String uri;

    public RDUtility() {
        this(Constants.COAPSERVER_HOST_REMOTE_IAGENT, Constants.COAPSERVER_PORT_REMOTE_IAGENT);
    }

    public RDUtility(String ip, int port) {
        uri = "coap://" + ip + ":" + port;
    }

    // synch method
    public List<DeviceInfo> queryRD() throws CoapException {
        return queryRD(null, null);
    }

    // synch method
    public List<DeviceInfo> queryRD(RDQueryParam query, String result) throws CoapException {
        String tempUri = uri + Constants.URL_RD;
        if (query != null) {
            tempUri += query.toQueryString();
        }
        CoapClient client = new CoapClient(tempUri);
        client.setTimeout(Constants.TIMEOUT_GET);
        LogUtil.log(LogUtil.LEVEL.INFO, "GET : "+tempUri);
        CoapResponse response = client.get();
        return processResponse(response);
    }

    // there shall be response from RD, purl will be created internally
    public String createMonitor(RDQueryParam query, final OnRDEventListener eventHandler) throws CoapException {
        String tempUri = uri + Constants.URL_RD_MONITOR;
        String localPaht = Utils.getRandomString();
        String purl = Utils.composePurl(CoAP.COAP_URI_SCHEME, RDCoapServerManager.getInstance().getPort(), localPaht);
        RDMonitorPayload payload = new RDMonitorPayload(query.deviceID, query.deviceType, query.resouceType, query.withRts, query.standardType, query.groupIds, null, purl, localPaht);
        CoapClient client = new CoapClient(tempUri);
        client.setTimeout(Constants.TIMEOUT_GET);
        RDCoapServerManager.getInstance().addResouce(localPaht);
        LogUtil.log(LogUtil.LEVEL.INFO, "POST : "+tempUri+"   payload: \n"+payload.toJson());
        CoapResponse response = client.post(payload.toJson(), MediaTypeRegistry.APPLICATION_JSON);
        if (response == null) {
            RDCoapServerManager.getInstance().deleteResouce(localPaht);
            throw new CoapException("No response received.");
        }
        if (ResponseCode.isSuccess(response.getCode())) {
            String monitorId = response.getResponseText();
            LogUtil.log(LogUtil.LEVEL.INFO, "POST ["+tempUri+"] resp : " + monitorId );
            payload.mid = monitorId;
            RDCoapServerManager.getInstance().addRDEventListener(payload, eventHandler);
            return monitorId;
        } else {
            RDCoapServerManager.getInstance().deleteResouce(localPaht);
            LogUtil.log(LogUtil.LEVEL.INFO, "resp with error code: " + response.getCode());
            throw new CoapException(response);
        }
    }

    public String modifyMonitor(String moniterID, RDQueryParam query, final OnRDEventListener eventHandler) throws CoapException {
        if (moniterID == null || moniterID.length() == 0) {
            throw new CoapException("moniterID cannot be empty or null.");
        }
        RDMonitorPayload rdMonitor = RDCoapServerManager.getInstance().getRDHandlersKey(moniterID);
        if (rdMonitor == null) {
            throw new CoapException("This moniterID:" + moniterID + " does not exist.");
        }
        String tempUri = uri + Constants.URL_RD_MONITOR;

        RDMonitorPayload payload = new RDMonitorPayload(query.deviceID, query.deviceType, query.resouceType, query.withRts, query.standardType, query.groupIds, moniterID, rdMonitor.purl, rdMonitor.localPath);
        CoapClient client = new CoapClient(tempUri);
        client.setTimeout(Constants.TIMEOUT_GET);
        LogUtil.log(LogUtil.LEVEL.INFO, "POST : "+tempUri);
        CoapResponse response = client.post(payload.toJson(), MediaTypeRegistry.APPLICATION_JSON);
        if (response == null) {
            throw new CoapException("No response received.");
        }
        if (ResponseCode.isSuccess(response.getCode())) {
            String monitorId = response.getResponseText();
            LogUtil.log(LogUtil.LEVEL.INFO, "resp : " + monitorId);
            return monitorId;
        } else {
            throw new CoapException(response);
        }
    }

    public boolean removeMonitor(String moniterID) throws CoapException {
        if (moniterID == null || moniterID.length() == 0) {
            return false;
        }
        String tempUri = uri + Constants.URL_RD_MONITOR + "?id=" + moniterID;
        RDMonitorPayload payload = RDCoapServerManager.getInstance().getRDHandlersKey(moniterID);
        if (payload == null) {
            return true;
        }
        CoapClient client = new CoapClient(tempUri);
        client.setTimeout(Constants.TIMEOUT_GET);
        Request delReq = Request.newDelete();
        delReq.setPayload(payload.toJson());
        LogUtil.log(LogUtil.LEVEL.INFO, "ADANCED : "+tempUri);
        CoapResponse response = client.advanced(delReq);
        if (response == null) {
            throw new CoapException("No response received.");
        }
        if (ResponseCode.isSuccess(response.getCode())) {
            RDCoapServerManager.getInstance().removeRDEventListener(moniterID);
            return true;
        } else {
            throw new CoapException(response);
        }
    }

    private List<DeviceInfo> processResponse(CoapResponse response) throws CoapException {
        if (response == null) {
            throw new CoapException("No response received.");
        }
        if (ResponseCode.isSuccess(response.getCode())) {
            String responseText = response.getResponseText();
            LogUtil.log(LogUtil.LEVEL.INFO, "Response : data : "+responseText);
            if (responseText == null || "".equals(responseText)) {
                return new ArrayList<DeviceInfo>();
            } else {
                    List<DeviceInfo> devices = MessageParseUtil.parseDevice(responseText);
                    return devices;
            }
        } else {
            throw new CoapException(response);
        }
    }
}
