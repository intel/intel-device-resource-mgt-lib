/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker.proxy.dp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.ilink.ILinkMessage;
import com.openiot.cloud.base.ilink.LeadingByte;
import com.openiot.cloud.base.ilink.MessageType;
import com.openiot.cloud.base.mq.MessageQueue;
import com.openiot.cloud.ibroker.base.device.IAgent;
import com.openiot.cloud.ibroker.base.protocols.ilink.ILinkCoapOverTcpMessageHandler;
import org.iotivity.cloud.base.connector.ConnectorPool;
import org.iotivity.cloud.base.device.Device;
import org.iotivity.cloud.base.protocols.IRequest;
import org.iotivity.cloud.base.protocols.coap.CoapMessage;
import org.iotivity.cloud.base.protocols.coap.CoapResponse;
import org.iotivity.cloud.base.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Scope("prototype")
public class GenericCoapTCPServiceClient extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(GenericCoapTCPServiceClient.class);

  private String connectionKey;

  @Value("${openiot.performance.echoServer:false}")
  private boolean echoServer;

  @Value("${openiot.performance.blackHole:false}")
  private boolean blackHole;

  @Autowired
  MessageQueue<IRequest> messageQueue;

  public GenericCoapTCPServiceClient(List<String> uriSegment, String connectionKey) {
    super(uriSegment);
    this.connectionKey = connectionKey;
  }

  @Autowired
  public GenericCoapTCPServiceClient(String uriPath, String connectionKey) {
    this(unpackToSegments(uriPath), connectionKey);
  }

  private void defaultRequestHandler(Device srcDevice, IRequest request) {
    ConnectorPool.getConnectionWithMinMatch(connectionKey).sendRequest(request, (response -> {
      CoapResponse coapResponse = (CoapResponse) response;
      logger.debug("onResponseReceived " + coapResponse);

      byte[] token = coapResponse.getToken();
      ILinkMessage iLinkResponse = new ILinkMessage(LeadingByte.RESPONSE.valueOf(),
                                                    (byte) MessageType.COAP_OVER_TCP.valueOf());
      ILinkCoapOverTcpMessageHandler.restoreMessageIDAndToken(token, coapResponse, iLinkResponse);
      ILinkCoapOverTcpMessageHandler.encodeCoapMessageAsPayload(coapResponse, iLinkResponse);
      iLinkResponse.setAgentId(srcDevice.getDeviceId());
      ((IAgent) srcDevice).sendMessage(iLinkResponse);
      return;
    }));
  }

  @Override
  public void onDefaultRequestReceived(Device srcDevice, IRequest request) {
    logger.debug("onDefaultRequestReceived of {}", connectionKey);
    Objects.requireNonNull(ConnectorPool.getConnectionWithMinMatch(connectionKey));

    // if a request is not to /dp
    // or it is not marked with a COAP_MESSAGE_ETAG
    // we thought it is not going to need any response handler
    if (!request.getUriPath().startsWith(ConstDef.DATA_URI)
        && !((CoapMessage) request).getOption(4)
                                   .contains(ILinkCoapOverTcpMessageHandler.COAP_MESSAGE_ETAG)) {
      logger.info("send it via a request-response way");
      defaultRequestHandler(srcDevice, request);
    } else {
      logger.info("send it via a message only way");
      // // only requests to /dp will be queued and send without any expected response handler
      messageQueue.add(request, r -> {
        logger.debug("[MessageQueue] defaultRequestHandler from MessageQueue @ "
            + Thread.currentThread().getName());
        ConnectorPool.getConnectionWithMinMatch(connectionKey).sendRequest(request, null);
      });
    }
  }

  private static List<String> unpackToSegments(String uriPath) {
    String[] uriSegments = uriPath.split("/");
    List<String> uriSegmentsList = new ArrayList<>();
    for (String segment : uriSegments) {
      if (segment == null || segment.length() == 0) {
        continue;
      }

      uriSegmentsList.add(segment);
    }
    return uriSegmentsList;
  }
}
