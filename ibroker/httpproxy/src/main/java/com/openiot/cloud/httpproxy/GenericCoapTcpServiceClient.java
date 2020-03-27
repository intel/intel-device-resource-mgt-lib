/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.httpproxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.httpproxy.utils.CoapStatusToHttpStatus;
import com.openiot.cloud.httpproxy.utils.HttpContentTypeToCoapContentType;
import com.openiot.cloud.httpproxy.utils.HttpMethodToCoapMethod;
import org.iotivity.cloud.base.connector.ConnectorPool;
import org.iotivity.cloud.base.device.IRequestChannel;
import org.iotivity.cloud.base.device.IResponseEventHandler;
import org.iotivity.cloud.base.protocols.IRequest;
import org.iotivity.cloud.base.protocols.IResponse;
import org.iotivity.cloud.base.protocols.MessageBuilder;
import org.iotivity.cloud.base.protocols.coap.CoapRequest;
import org.iotivity.cloud.base.protocols.enums.RequestMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class GenericCoapTcpServiceClient {
  private static final Logger logger = LoggerFactory.getLogger(GenericCoapTcpServiceClient.class);

  class CoapReceiveHandler implements IResponseEventHandler {
    private DeferredResult<ResponseEntity<?>> result;

    public CoapReceiveHandler(DeferredResult<ResponseEntity<?>> result) {
      this.result = result;
    }

    @Override
    public void onResponseReceived(IResponse response) {
      try {
        HttpStatus status = response == null ? HttpStatus.NOT_IMPLEMENTED
            : CoapStatusToHttpStatus.transfer(response.getStatus());
        byte[] payload = response == null ? null : response.getPayload();
        boolean ret = result.setResult(ResponseEntity.status(status).body(payload));
        if (!ret) {
          logger.warn("set response result failed");
        }
        logger.debug("HTTP response from coap " + response);
      } catch (Exception e) {
        logger.warn(BaseUtil.getStackTrace(e));
      }
    }
  }

  public void defaultRequestHandle(String modifiedUriPath, RequestEntity<byte[]> request,
                                   DeferredResult<ResponseEntity<?>> result,
                                   Map<String, String> extraParam) {
    logger.debug("GenericCoapTcpServiceClient handle it");

    HttpMethod method = request.getMethod();
    byte[] payload = request.getBody();

    // convert HTTP requests into Coap+tcp requests and send them
    // httpMethod => RequestMethod
    RequestMethod cmethod = HttpMethodToCoapMethod.transfer(method);
    if (cmethod == null) {
      logger.warn(String.format("The method %s is not supported", method.name()));
      return;
    }

    MediaType ct = request.getHeaders().getContentType();
    IRequest coapRequest =
        MessageBuilder.createRequest(cmethod,
                                     BaseUtil.removeTrailingSlash(modifiedUriPath),
                                     request.getUrl().getQuery(),
                                     HttpContentTypeToCoapContentType.transfer(ct),
                                     payload);
    if (extraParam != null && !extraParam.isEmpty()) {
      try {
        String optionUser = new ObjectMapper().writeValueAsString(extraParam);
        ((CoapRequest) coapRequest).setUser(optionUser);
      } catch (JsonProcessingException e) {
        logger.info(BaseUtil.getStackTrace(e));
      }
      logger.info("set " + extraParam + " in coap req option: "
          + ((CoapRequest) coapRequest).getOptionsString());
    }

    IRequestChannel coapClient = ConnectorPool.getConnectionWithMinMatch(modifiedUriPath);
    if (coapClient != null) {
      logger.debug("dispatch to a coap connection based on {} ", modifiedUriPath);
      coapClient.sendRequest(coapRequest, new CoapReceiveHandler(result));
    } else {
      logger.warn("There is no handler for the uri {}", modifiedUriPath);
    }
  }

  public void defaultRequestHandle(RequestEntity<byte[]> request,
                                   CompletableFuture<ResponseEntity<byte[]>> result,
                                   String modifiedUriPath, Map<String, String> extraParam) {
    logger.debug("A COAP Server gona handle it");

    // convert HTTP requests into Coap+tcp requests and send them
    HttpMethod method = request.getMethod();
    RequestMethod coapMethod = HttpMethodToCoapMethod.transfer(method);
    if (coapMethod == null) {
      logger.warn(String.format("The method %s is not supported", method.name()));
      result.complete(ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(null));
      return;
    }

    URI uri = request.getUrl();
    IRequest coapRequest =
        MessageBuilder.createRequest(coapMethod,
                                     modifiedUriPath,
                                     uri.getQuery(),
                                     HttpContentTypeToCoapContentType.transfer(request.getHeaders()
                                                                                      .getContentType()),
                                     request.getBody());

    if (extraParam != null && !extraParam.isEmpty()) {
      try {
        String optionUser = new ObjectMapper().writeValueAsString(extraParam);
        ((CoapRequest) coapRequest).setUser(optionUser);
      } catch (JsonProcessingException e) {
        logger.info(BaseUtil.getStackTrace(e));
      }
      logger.info("set " + extraParam + " in coap req option: "
          + ((CoapRequest) coapRequest).getOptionsString());
    }

    IRequestChannel coapClient = ConnectorPool.getConnectionWithMinMatch(modifiedUriPath);
    if (coapClient != null) {
      logger.debug("dispatch to a coap connection based on " + uri.getPath());
      coapClient.sendRequest(coapRequest, response -> {
        HttpStatus status = response == null ? HttpStatus.NOT_IMPLEMENTED
            : CoapStatusToHttpStatus.transfer(response.getStatus());
        byte[] payload = response == null ? null : response.getPayload();
        result.complete(ResponseEntity.status(status).body(payload));
      });
    } else {
      logger.warn(String.format("There is no handler for the uri %s", uri.getPath()));
      result.complete(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
    }
  }
}
