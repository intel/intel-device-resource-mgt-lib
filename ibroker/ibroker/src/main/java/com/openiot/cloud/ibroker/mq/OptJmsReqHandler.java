/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker.mq;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.help.MessageIdMaker;
import com.openiot.cloud.base.ilink.ILinkMessage;
import com.openiot.cloud.base.ilink.LeadingByte;
import com.openiot.cloud.base.ilink.MessageType;
import com.openiot.cloud.ibroker.base.device.IAgent;
import com.openiot.cloud.ibroker.base.device.IAgentCache;
import com.openiot.cloud.ibroker.base.protocols.ilink.ILinkCoapOverTcpMessageHandler;
import com.openiot.cloud.sdk.service.IConnectRequest;
import com.openiot.cloud.sdk.service.IConnectResponse;
import com.openiot.cloud.sdk.service.IConnectServiceHandler;
import org.iotivity.cloud.base.protocols.MessageBuilder;
import org.iotivity.cloud.base.protocols.coap.CoapMessage;
import org.iotivity.cloud.base.protocols.coap.CoapResponse;
import org.iotivity.cloud.base.protocols.enums.ContentFormat;
import org.iotivity.cloud.base.protocols.enums.RequestMethod;
import org.iotivity.cloud.base.protocols.enums.ResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class OptJmsReqHandler implements IConnectServiceHandler {

  @Autowired
  private IAgentCache dc;

  public static final Logger logger = LoggerFactory.getLogger(OptJmsReqHandler.class);

  @Override
  public void onRequest(IConnectRequest request) {
    String messageId = request.getMessageID();

    /**
     * uri path of opt looks like: "http://localhost:8080/opt/modbus/modbus_tcp_sanity_test/boiler/"
     * /opt/DEVTYPE/DI/RESOURCEID/...
     */
    String uriStr = request.getUrl();

    // once we receive a wanted, we should check whether the device is
    // reachable
    String targetEpId = getEndpointId(uriStr);
    // is it mine?
    IAgent ilinkDevice = dc.getAgent(targetEpId);
    if (ilinkDevice == null) {
      logger.info(String.format("reject #%s request to /opt because of impossible agentId",
                                messageId));
      IConnectResponse.createFromRequest(request, HttpStatus.NOT_FOUND, MediaType.TEXT_PLAIN, null)
                      .send();
      return;
    } else if (ilinkDevice.getSessionFlag() != true) {
      logger.warn(String.format("reject #%s request to /opt because of session not established",
                                messageId));
      IConnectResponse.createFromRequest(request, HttpStatus.NOT_FOUND, MediaType.TEXT_PLAIN, null)
                      .send();
      return;
    }

    // need to remove a "/opt" prefix
    String uriPathStrtoCoap = uriStr.split("/opt")[1];
    String uriQuryStrtoCoap =
        (uriPathStrtoCoap.contains("\\?")) ? uriPathStrtoCoap.split("\\?")[1] : null;
    RequestMethod method = getCoapMethodFromHttpMethod(request.getAction());
    CoapMessage cm =
        (CoapMessage) MessageBuilder.createRequest(method, uriPathStrtoCoap, uriQuryStrtoCoap);
    cm.setPayload(request.getPayload());
    cm.setContentFormat(getContentFormat(request.getFormat().toString()));

    logger.debug("form such a COAP message " + cm);

    ILinkMessage reqToDev =
        new ILinkMessage(LeadingByte.REQUEST.valueOf(), (byte) MessageType.COAP_OVER_TCP.valueOf());
    ILinkCoapOverTcpMessageHandler.encodeCoapMessageAsPayload(cm, reqToDev);
    reqToDev.setIlinkMessageId(MessageIdMaker.IntegerToBytes(Integer.valueOf(messageId)));
    reqToDev.setAgentId(ilinkDevice.getAgentId());
    ilinkDevice.sendMessage(reqToDev, (ILinkMessage respFromDev) -> {
      try {
        if (respFromDev.getResponseCode() == ConstDef.FH_V_FAIL) {
          IConnectResponse.createFromRequest(request,
                                             HttpStatus.SERVICE_UNAVAILABLE,
                                             MediaType.APPLICATION_JSON,
                                             "\"error\": \"the iAgent is not available\"".getBytes())
                          .send();
        } else {
          // send response back to message queue
          CoapResponse coapMessage =
              (CoapResponse) ILinkCoapOverTcpMessageHandler.decodeAsCoapMessage(respFromDev.getPayload());
          IConnectResponse.createFromRequest(request,
                                             getHttpStatus(coapMessage.getStatus()),
                                             getMediaType(coapMessage.getContentFormat()),
                                             coapMessage.getPayload())
                          .send();
        }
      } catch (Exception e) {
        logger.error("meet an exception during /opt response handling");
        logger.error(BaseUtil.getStackTrace(e));
        IConnectResponse.createFromRequest(request,
                                           HttpStatus.INTERNAL_SERVER_ERROR,
                                           MediaType.APPLICATION_JSON,
                                           "\"error\": \"meet an excepton \"".getBytes())
                        .send();
      }
      return null;
    });
  }

  private ContentFormat getContentFormat(String format) {
    if (format == null)
      return ContentFormat.APPLICATION_TEXTPLAIN;
    if (format.equals(MediaType.APPLICATION_JSON.toString()))
      return ContentFormat.APPLICATION_JSON;
    if (format.equals(MediaType.APPLICATION_OCTET_STREAM.toString()))
      return ContentFormat.APPLICATION_OCTET_STREAM;
    if (format.equals(MediaType.APPLICATION_XML.toString()))
      return ContentFormat.APPLICATION_XML;
    return ContentFormat.APPLICATION_TEXTPLAIN;
  }

  private String getEndpointId(String uriStr) {
    if (uriStr == null || uriStr.isEmpty()) {
      return null;
    }

    String[] segments = uriStr.split("/");

    int ii = 0;
    while (segments[ii].compareTo("opt") != 0 && ii < segments.length) {
      ii++;
    }

    if (ii >= segments.length || ii + 2 >= segments.length) {
      return null;
    }

    return segments[ii + 2];
  }

  private RequestMethod getCoapMethodFromHttpMethod(HttpMethod method) {
    if (method == null) {
      return null;
    }
    switch (method) {
      case GET:
        return RequestMethod.GET;
      case PUT:
        return RequestMethod.PUT;
      case POST:
        return RequestMethod.POST;
      case DELETE:
        return RequestMethod.DELETE;
      default:
        return null;
    }
  }

  private HttpStatus getHttpStatus(ResponseStatus status) {
    if (status == null)
      return HttpStatus.OK;
    switch (status) {
      case CREATED:
        return HttpStatus.CREATED;
      case DELETED:
        return HttpStatus.OK;
      case VALID:
        return HttpStatus.OK;
      case CHANGED:
        return HttpStatus.OK;
      case CONTENT:
        return HttpStatus.OK;
      case BAD_REQUEST:
        return HttpStatus.BAD_REQUEST;
      case UNAUTHORIZED:
        return HttpStatus.UNAUTHORIZED;
      case BAD_OPTION:
        return HttpStatus.NOT_ACCEPTABLE;
      case FORBIDDEN:
        return HttpStatus.FORBIDDEN;
      case NOT_FOUND:
        return HttpStatus.NOT_FOUND;
      case METHOD_NOT_ALLOWED:
        return HttpStatus.METHOD_NOT_ALLOWED;
      case NOT_ACCEPTABLE:
        return HttpStatus.NOT_ACCEPTABLE;
      case PRECONDITION_FAILED:
        return HttpStatus.PRECONDITION_FAILED;
      case REQUEST_ENTITY_TOO_LARGE:
        return HttpStatus.PAYLOAD_TOO_LARGE;
      case UNSUPPORTED_CONTENT_FORMAT:
        return HttpStatus.UNSUPPORTED_MEDIA_TYPE;
      case INTERNAL_SERVER_ERROR:
        return HttpStatus.INTERNAL_SERVER_ERROR;
      case NOT_IMPLEMENTED:
        return HttpStatus.NOT_IMPLEMENTED;
      case BAD_GATEWAY:
        return HttpStatus.BAD_GATEWAY;
      case SERVICE_UNAVAILABLE:
        return HttpStatus.SERVICE_UNAVAILABLE;
      case GATEWAY_TIMEOUT:
        return HttpStatus.GATEWAY_TIMEOUT;
      case PROXY_NOT_SUPPORTED:
        return HttpStatus.SERVICE_UNAVAILABLE;
      default:
        break;
    }
    return HttpStatus.NOT_ACCEPTABLE;
  }

  private MediaType getMediaType(ContentFormat format) {
    if (format == null)
      return MediaType.TEXT_PLAIN;
    if (format.equals(ContentFormat.APPLICATION_JSON))
      return MediaType.APPLICATION_JSON;
    if (format.equals(ContentFormat.APPLICATION_OCTET_STREAM))
      return MediaType.APPLICATION_OCTET_STREAM;
    if (format.equals(ContentFormat.APPLICATION_XML))
      return MediaType.APPLICATION_XML;
    return MediaType.TEXT_PLAIN;
  }
}
