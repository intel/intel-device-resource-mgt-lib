/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.httpproxy;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.common.model.TokenContent;
import org.iotivity.cloud.base.connector.ConnectorPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public class HttpProxyServer {
  private static final Logger logger = LoggerFactory.getLogger(HttpProxyServer.class);
  @Autowired
  private GenericCoapTcpServiceClient coapProxy;
  @Autowired
  private GenericMqClient jmsProxy;
  private static final String contextPath = "/fc";

  @GetMapping("/")
  @ResponseStatus(HttpStatus.OK)
  public String rootRequestHandler() {
    return "welcome to openiot platform";
  }

  // @RequestMapping("/**")
  public ResponseEntity<byte[]> defaultRequestHandlerSync(RequestEntity<byte[]> request) {
    CompletableFuture<ResponseEntity<byte[]>> responseFuture = new CompletableFuture<>();

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Map<String, String> extraParam = new HashMap<>();
    if (auth != null) {
      TokenContent tokenContent = (TokenContent) auth.getDetails();
      extraParam.put(ConstDef.MSG_KEY_USR, tokenContent.getUser());
      extraParam.put(ConstDef.MSG_KEY_PRJ, tokenContent.getProject());
      if (tokenContent.getRole() != null) {
        extraParam.put(ConstDef.MSG_KEY_ROLE, tokenContent.getRole().getValue());
      }
      logger.info("add extra parameters into request: " + extraParam);
    } else {
      logger.warn("No auth info in request!");
    }

    URI uri = request.getUrl();
    String uriPath = uri.getPath();
    uriPath = BaseUtil.removeTrailingSlash(uriPath);

    if (uriPath.startsWith(contextPath)) {
      uriPath = uriPath.replaceFirst(contextPath, "");
    }
    logger.info("looking for a connection by {}", uriPath);

    if (ConnectorPool.getConnectionWithMinMatch(uriPath) != null) {
      logger.info("to a COAP client");
      coapProxy.defaultRequestHandle(request, responseFuture, uriPath, extraParam);
    } else {
      logger.info("to a MQ client");
      jmsProxy.defaultRequestHandle(request, responseFuture, uriPath, extraParam);
    }

    return responseFuture.join();
  }

  @RequestMapping(value = "/**", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public DeferredResult<ResponseEntity<?>>
      defaultRequestHandle(RequestEntity<byte[]> request, @RequestHeader HttpHeaders headers) {
    logger.info("an incoming request to {} ", request.getUrl());

    DeferredResult<ResponseEntity<?>> result = new DeferredResult<>();
    try {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      Map<String, String> extraParam = new HashMap<>();
      if (auth != null) {
        TokenContent tokenContent = (TokenContent) auth.getDetails();
        extraParam.put(ConstDef.MSG_KEY_USR, tokenContent.getUser());
        extraParam.put(ConstDef.MSG_KEY_PRJ, tokenContent.getProject());
        if (tokenContent.getRole() != null) {
          extraParam.put(ConstDef.MSG_KEY_ROLE, tokenContent.getRole().getValue());
        }
        logger.info("add extra parameters into request: " + extraParam);
      } else {
        logger.warn("No auth info in request!");
      }

      URI uri = request.getUrl();
      String uriPath = uri.getPath();
      uriPath = BaseUtil.removeTrailingSlash(uriPath);

      if (uriPath.startsWith(contextPath)) {
        uriPath = uriPath.replaceFirst(contextPath, "");
      }
      logger.info("looking for a connection by {}", uriPath);

      if (ConnectorPool.getConnectionWithMinMatch(uriPath) != null) {
        logger.info("to a COAP client");
        coapProxy.defaultRequestHandle(uriPath, request, result, extraParam);
      } else {
        logger.info("to a MQ client");
        jmsProxy.defaultRequestHandle(uriPath, request, result, extraParam);
      }
    } catch (Exception e) {
      logger.warn("defaultRequestHandler meets an exception while dealing with a request", e);
    }

    return result;
  }
}
