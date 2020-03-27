/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker.mq;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.ilink.ILinkMessage;
import com.openiot.cloud.base.ilink.LeadingByte;
import com.openiot.cloud.base.ilink.MessageType;
import com.openiot.cloud.ibroker.base.device.IAgent;
import com.openiot.cloud.ibroker.base.protocols.ilink.ILinkCoapOverTcpMessageHandler;
import com.openiot.cloud.sdk.service.IConnectRequest;
import org.iotivity.cloud.base.device.Device;
import org.iotivity.cloud.base.protocols.IRequest;
import org.iotivity.cloud.base.protocols.MessageBuilder;
import org.iotivity.cloud.base.protocols.coap.CoapRequest;
import org.iotivity.cloud.base.protocols.coap.CoapResponse;
import org.iotivity.cloud.base.protocols.enums.ContentFormat;
import org.iotivity.cloud.base.protocols.enums.RequestMethod;
import org.iotivity.cloud.base.protocols.enums.ResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

@Component
public class DefaultJmsHandler {
  public static final Logger logger = LoggerFactory.getLogger(DefaultJmsHandler.class);

  public void onDefaultRequestReceived(Device srcDevice, IRequest request,
                                       ILinkMessage iLinkMessage) {
    try {
      // coap_tcp -> jms
      CoapRequest cReq = (CoapRequest) request;
      logger.info(String.format("#Request %s -> %s -> %s",
                                iLinkMessage.getMessageId(),
                                cReq.getUriPath(),
                                cReq.getPayload() == null ? "null"
                                    : new String(cReq.getPayload())));

      URI uri = new URI(null, null, cReq.getUriPath(), cReq.getUriQuery(), null);
      HttpMethod method = getHttpMethodFromCoapMethod(cReq.getMethod());
      MediaType format = getMediaType(cReq.getContentFormat());
      IConnectRequest jmsReq =
          IConnectRequest.create(method, uri.toString(), format, cReq.getPayload());

      jmsReq.send((response) -> {
        HttpStatus respStatus = response.getStatus();
        MediaType respFormat = response.getFormat();
        CoapResponse coapResp =
            (CoapResponse) MessageBuilder.createResponse(request,
                                                         getCoapStatus(respStatus),
                                                         getContentFormat(respFormat),
                                                         response.getPayload());

        logger.info("receive JMS response message[status=" + respStatus + "(" + coapResp.getCode()
            + ")]: " + response);

        ILinkMessage ilink = new ILinkMessage(LeadingByte.RESPONSE.valueOf(),
                                              (byte) MessageType.COAP_OVER_TCP.valueOf());
        ILinkCoapOverTcpMessageHandler.restoreMessageIDAndToken(coapResp.getToken(),
                                                                coapResp,
                                                                ilink);
        ILinkCoapOverTcpMessageHandler.encodeCoapMessageAsPayload(coapResp, ilink);
        ilink.setAgentId(((IAgent) srcDevice).getAgentId());
        ((IAgent) srcDevice).sendMessage(ilink);
      }, 10, TimeUnit.SECONDS);
    } catch (URISyntaxException e) {
      e.printStackTrace();
      logger.error(BaseUtil.getStackTrace(e));
    }
  }

  private ContentFormat getContentFormat(MediaType format) {
    if (format == null)
      return ContentFormat.APPLICATION_TEXTPLAIN;
    if (format.equals(MediaType.APPLICATION_JSON))
      return ContentFormat.APPLICATION_JSON;
    if (format.equals(MediaType.APPLICATION_OCTET_STREAM))
      return ContentFormat.APPLICATION_OCTET_STREAM;
    if (format.equals(MediaType.APPLICATION_XML))
      return ContentFormat.APPLICATION_XML;
    return ContentFormat.APPLICATION_TEXTPLAIN;
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

  private HttpMethod getHttpMethodFromCoapMethod(RequestMethod method) {
    if (method == null) {
      return null;
    }

    switch (method) {
      case GET:
        return HttpMethod.GET;
      case PUT:
        return HttpMethod.PUT;
      case POST:
        return HttpMethod.POST;
      case DELETE:
        return HttpMethod.DELETE;
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

  private ResponseStatus getCoapStatus(HttpStatus status) {
    if (status == null)
      return ResponseStatus.CREATED;
    switch (status) {
      case CREATED:
        return ResponseStatus.CREATED;
      case OK:
        return ResponseStatus.CHANGED;
      case BAD_REQUEST:
        return ResponseStatus.BAD_REQUEST;
      case UNAUTHORIZED:
        return ResponseStatus.UNAUTHORIZED;
      case NOT_ACCEPTABLE:
        return ResponseStatus.NOT_ACCEPTABLE;
      case FORBIDDEN:
        return ResponseStatus.FORBIDDEN;
      case NOT_FOUND:
        return ResponseStatus.NOT_FOUND;
      case METHOD_NOT_ALLOWED:
        return ResponseStatus.METHOD_NOT_ALLOWED;
      case PRECONDITION_FAILED:
        return ResponseStatus.PRECONDITION_FAILED;
      case PAYLOAD_TOO_LARGE:
        return ResponseStatus.REQUEST_ENTITY_TOO_LARGE;
      case UNSUPPORTED_MEDIA_TYPE:
        return ResponseStatus.UNSUPPORTED_CONTENT_FORMAT;
      case INTERNAL_SERVER_ERROR:
        return ResponseStatus.INTERNAL_SERVER_ERROR;
      case NOT_IMPLEMENTED:
        return ResponseStatus.NOT_IMPLEMENTED;
      case BAD_GATEWAY:
        return ResponseStatus.BAD_GATEWAY;
      case SERVICE_UNAVAILABLE:
        return ResponseStatus.SERVICE_UNAVAILABLE;
      case GATEWAY_TIMEOUT:
        return ResponseStatus.GATEWAY_TIMEOUT;
      default:
        break;
    }
    return ResponseStatus.NOT_ACCEPTABLE;
  }
}
