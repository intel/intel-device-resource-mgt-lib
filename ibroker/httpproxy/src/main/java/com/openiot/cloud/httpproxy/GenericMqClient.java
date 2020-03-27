/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.httpproxy;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConcurrentRequestMap;
import com.openiot.cloud.base.help.MessageIdMaker;
import com.openiot.cloud.sdk.service.IConnectRequest;
import com.openiot.cloud.sdk.service.IConnectResponse;
import com.openiot.cloud.sdk.service.IConnectResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class GenericMqClient implements IConnectResponseHandler {
  private static final Logger logger = LoggerFactory.getLogger(GenericMqClient.class);

  private ConcurrentRequestMap<Long, DeferredResult<ResponseEntity<?>>> requestContext =
      new ConcurrentRequestMap<>(5120, 5, 30, (k, v) -> {
        if (v != null)
          v.setResult(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
      });

  public void defaultRequestHandle(String modifiedUriPath, RequestEntity<byte[]> request,
                                   DeferredResult<ResponseEntity<?>> result,
                                   Map<String, String> extraParam) {
    IConnectRequest req;
    int messageID = MessageIdMaker.getMessageIdAsInteger();
    String jmsUrl = "jms:" + modifiedUriPath;
    if (request.getUrl().getQuery() != null)
      jmsUrl += "?" + request.getUrl().getQuery();
    // for opt request, path is variable and can NOT be used as message destination
    if (modifiedUriPath.startsWith("/opt")) {
      req = IConnectRequest.create("/opt",
                                   request.getMethod(),
                                   jmsUrl,
                                   request.getHeaders().getContentType(),
                                   request.getBody(),
                                   messageID);
    } else {
      req = IConnectRequest.create(request.getMethod(),
                                   jmsUrl,
                                   request.getHeaders().getContentType(),
                                   request.getBody(),
                                   messageID);
    }

    if (extraParam != null && !extraParam.isEmpty()) {
      for (String key : extraParam.keySet()) {
        req.setTokenInfo(key, extraParam.get(key));
      }
    } else {
      logger.warn("There is no user details info in httpproxy request!");
    }

    synchronized (this) {
      requestContext.put((long) messageID, result);
      req.send(this, 30, TimeUnit.SECONDS);
    }
  }

  @Override
  public void onResponse(IConnectResponse response) {
    try {
      long messageId = Long.parseLong(response.getMessageID());

      DeferredResult<ResponseEntity<?>> result = requestContext.remove(messageId);
      if (result == null) {
        logger.warn("can't find a DeferredResult for " + messageId);
        return;
      } else {
        logger.debug("-->[MESSAGEID] get a DeferredResult for " + messageId);
      }

      byte[] payload = response.getPayload();
      MediaType mt = response.getFormat();
      HttpStatus status = response.getStatus();
      if (payload == null || payload.length == 0) {
        result.setResult(ResponseEntity.status(status).body(null));
      } else {
        if (mt == null) {
          logger.error("MediaType is NULL when payload is not");
          result.setResult(ResponseEntity.status(status).body(payload));
        } else {
          result.setResult(ResponseEntity.status(status).contentType(mt).body(payload));
        }
      }
    } catch (Exception e) {
      logger.warn(BaseUtil.getStackTrace(e));
    }
  }

  public void defaultRequestHandle(RequestEntity<byte[]> request,
                                   CompletableFuture<ResponseEntity<byte[]>> result,
                                   String modifiedUriPath, Map<String, String> extraParam) {
    URI uri = request.getUrl();

    IConnectRequest req;
    int messageID = MessageIdMaker.getMessageIdAsInteger();
    String jmsUrl = "jms:" + modifiedUriPath;
    if (uri.getQuery() != null)
      jmsUrl += "?" + uri.getQuery();
    // for opt request, path is variable and can NOT be used as message destination
    if (modifiedUriPath.startsWith("/opt")) {
      req = IConnectRequest.create("/opt",
                                   request.getMethod(),
                                   jmsUrl,
                                   request.getHeaders().getContentType(),
                                   request.getBody(),
                                   messageID);
    } else {
      req = IConnectRequest.create(request.getMethod(),
                                   jmsUrl,
                                   request.getHeaders().getContentType(),
                                   request.getBody(),
                                   messageID);
    }

    if (extraParam != null && !extraParam.isEmpty()) {
      for (String key : extraParam.keySet()) {
        req.setTokenInfo(key, extraParam.get(key));
      }
    } else {
      logger.warn("There is no user details info in httpproxy request!");
    }

    req.send(response -> {
      byte[] payload = response.getPayload();
      MediaType mt = response.getFormat();
      HttpStatus status = response.getStatus();

      if (payload == null || payload.length == 0) {
        result.complete(ResponseEntity.status(status).build());
      } else {
        if (mt == null) {
          logger.error("MediaType is NULL when payload is not");
          result.complete(ResponseEntity.status(status).body(payload));
        } else {
          result.complete(ResponseEntity.status(status).contentType(mt).body(payload));
        }
      }
    }, 30, TimeUnit.SECONDS);
  }
}
