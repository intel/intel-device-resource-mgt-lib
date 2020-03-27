/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker.proxy.rd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.ilink.ILinkMessage;
import com.openiot.cloud.base.ilink.LeadingByte;
import com.openiot.cloud.base.ilink.MessageType;
import com.openiot.cloud.base.mongo.model.help.ShortSession;
import com.openiot.cloud.ibroker.base.device.IAgent;
import com.openiot.cloud.ibroker.base.device.IAgentCache;
import com.openiot.cloud.ibroker.base.protocols.ilink.ILinkCoapOverTcpMessageHandler;
import com.openiot.cloud.ibroker.utils.DeviceLite;
import com.openiot.cloud.ibroker.utils.ResHierLite;
import org.iotivity.cloud.base.connector.ConnectorPool;
import org.iotivity.cloud.base.device.Device;
import org.iotivity.cloud.base.device.IRequestChannel;
import org.iotivity.cloud.base.device.IResponseEventHandler;
import org.iotivity.cloud.base.exception.ClientException;
import org.iotivity.cloud.base.protocols.IRequest;
import org.iotivity.cloud.base.protocols.IResponse;
import org.iotivity.cloud.base.protocols.Message;
import org.iotivity.cloud.base.protocols.MessageBuilder;
import org.iotivity.cloud.base.protocols.coap.CoapRequest;
import org.iotivity.cloud.base.protocols.coap.CoapResponse;
import org.iotivity.cloud.base.protocols.enums.ContentFormat;
import org.iotivity.cloud.base.protocols.enums.RequestMethod;
import org.iotivity.cloud.base.protocols.enums.ResponseStatus;
import org.iotivity.cloud.base.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

@Component
public class RDProxy extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(RDProxy.class);

  class RDReceiveHandler implements IResponseEventHandler {
    private final Device srcDevice;
    private final RequestMethod method;
    private final String uriPath;

    public RDReceiveHandler(Device srcDevice, RequestMethod method, String uriPath) {
      this.srcDevice = srcDevice;
      this.method = method;
      this.uriPath = uriPath;
    }

    @Override
    public void onResponseReceived(IResponse response) throws ClientException {
      CoapResponse coapResponse = (CoapResponse) response;
      logger.debug("onResponseReceived {}", response);

      switch (this.method) {
        case POST:
          if (response.getStatus() != ResponseStatus.CHANGED) {
            break;
          }

          if (response.getPayloadSize() == 0) {
            break;
          }

          if (this.uriPath.compareTo("/rd") != 0) {
            break;
          }

          /**
           * a JSON payload of "POST /rd" looks like:
           * [{"di":"modbus_tcp2","s":"58b92217cdd84132815d0854"}]
           */
          try {
            ObjectMapper objMapper = new ObjectMapper();
            ShortSession[] ssList =
                objMapper.readValue(response.getPayload(), ShortSession[].class);
            ((IAgent) srcDevice).cacheConnectedDevice(ssList);

            StringBuilder sb = new StringBuilder();
            for (ShortSession ss : ssList) {
              sb.append(ss + ",");
            }
            logger.debug("add such ep(s) :" + sb.toString());
          } catch (IOException e) {
            e.printStackTrace();
          }
          break;
        default:
          break;
      }

      /** after "POST /rd [JSON PAYLOAD]", there will be response back */
      byte[] token = coapResponse.getToken();
      ILinkMessage ilinkResponse = new ILinkMessage(LeadingByte.RESPONSE.valueOf(),
                                                    (byte) MessageType.COAP_OVER_TCP.valueOf());
      ILinkCoapOverTcpMessageHandler.restoreMessageIDAndToken(token, coapResponse, ilinkResponse);
      ILinkCoapOverTcpMessageHandler.encodeCoapMessageAsPayload(coapResponse, ilinkResponse);
      ilinkResponse.setAgentId(srcDevice.getDeviceId());
      ((IAgent) srcDevice).sendMessage(ilinkResponse);
    }
  }

  @Autowired
  private IAgentCache dc;

  public RDProxy() {
    super(Arrays.asList(ConstDef.RD_URI_SEGMENT));
  }

  /** request is a CoapRequest here */
  @Override
  public void onDefaultRequestReceived(Device srcDevice, IRequest request) {
    /**
     * "POST /rd [JSON PAYLOAD]" to report their resources
     *
     * <p>"DELETE /rd/session?id=xxx" or "DELETE /rd/session?di=xxx" to report offline events
     *
     * <p>"DELETE /rd/device?di=xxx" to report delete events
     */
    // can not tell which device is offline based on a socket status since it only represents the
    // status of a iagent
    if (request.getMethod() == RequestMethod.DELETE
        && request.getUriPath().compareTo("/rd/session") == 0) {
      String query = request.getUriQuery();
      if (query != null && query.length() > 0) {
        // di=xxx -> ["di", "xxx"]
        String[] temp = query.split("=");
        if (temp.length == 2 && temp[0].compareTo("di") == 0) {
          String di = temp[1];
          logger.debug(String.format("remove di=%s", di));
          if (dc.containsKey(di)) {
            dc.removeAgent(di, false);
          } else {
            ((IAgent) srcDevice).removeConnectedDevice(di);
          }
        }
      }
    }

    logger.debug("send request to {} via COAP+TCP", ConstDef.RD_URI);
    IRequestChannel requestChannel = ConnectorPool.getConnection(ConstDef.RD_URI);
    if (requestChannel != null) {
      requestChannel.sendRequest(request,
                                 new RDReceiveHandler(srcDevice,
                                                      request.getMethod(),
                                                      request.getUriPath()));
    } else {
      IResponse response =
          MessageBuilder.createResponse(request, ResponseStatus.SERVICE_UNAVAILABLE);
      srcDevice.sendResponse(response);
    }
  }

  public IRequest reportDeviceDisconnected(IAgent notReachedDevice) {
    if (notReachedDevice == null)
      return null;

    String uriPath = "/rd/session";
    String agentId = notReachedDevice.getAgentId();

    // will remove all connected devcies when removing a gateway
    ShortSession[] removedAgent = new ShortSession[] {new ShortSession(agentId, null)};
    ObjectMapper objMapper = new ObjectMapper();
    try {
      IRequest message = MessageBuilder.createRequest(RequestMethod.DELETE,
                                                      uriPath,
                                                      null,
                                                      ContentFormat.APPLICATION_JSON,
                                                      objMapper.writeValueAsBytes(removedAgent));
      return message;
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return null;
    }
  }

  public void syncDeviceConnectedStatus(String iagentId, String deviceId) {
    IRequest request = MessageBuilder.createRequest(RequestMethod.GET,
                                                    "/rd/device",
                                                    "id=" + deviceId,
                                                    ContentFormat.APPLICATION_JSON,
                                                    null);
    IRequestChannel requestChannel = ConnectorPool.getConnection(ConstDef.RD_URI);
    if (Objects.nonNull(requestChannel)) {
      requestChannel.sendRequest(request, response -> {
        if (!ResponseStatus.CONTENT.equals(response.getStatus())) {
          logger.info("can not get content about device {} from rd", deviceId);
          return;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
          DeviceLite[] device = objectMapper.readValue(response.getPayload(), DeviceLite[].class);
          if (device.length == 0 || device[0].isC()) {
            logger.info("{} is connected, same status with the ibroker record", deviceId);
            return;
          }

          // only send sync message if with different connected status
          ResHierLite syncStatus = new ResHierLite();
          syncStatus.setAid(iagentId);
          syncStatus.setDi(deviceId);
          syncStatus.setDt(device[0].getDt());
          syncStatus.setSt(device[0].getSt());
          IRequest syncRequest =
              MessageBuilder.createRequest(RequestMethod.POST,
                                           "/rd",
                                           null,
                                           ContentFormat.APPLICATION_JSON,
                                           objectMapper.writeValueAsBytes(new ResHierLite[] {
                                               syncStatus}));
          logger.info("set {} as online", deviceId);
          requestChannel.sendRequest(syncRequest, null);
        } catch (IOException e) {
          logger.warn("deserialization failure", e);
        }

      });
    }
  }
}
