/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.httpproxy.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.common.model.TokenContent;
import com.openiot.cloud.sdk.service.IConnectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class TokenClient {
  private static final Logger logger = LoggerFactory.getLogger(TokenClient.class);

  @Cacheable(value = "token", key = "T(java.util.Objects).hash(#token)")
  public TokenContent getTokenContent(String token) throws IOException {
    logger.info("get token content from authcenter for {}", token);
    Map<String, String> payload = Collections.singletonMap("token", token);
    CompletableFuture<TokenContent> afterValidation = new CompletableFuture<>();
    IConnectRequest.create(HttpMethod.POST,
                           "/api/user/validation",
                           MediaType.APPLICATION_JSON,
                           new ObjectMapper().writeValueAsBytes(payload))
                   .send(iConnectResponse -> {
                     TokenContent tokenContent = null;
                     if (!iConnectResponse.getStatus().isError()) {
                       try {
                         tokenContent = new ObjectMapper().readValue(iConnectResponse.getPayload(),
                                                                     TokenContent.class);
                       } catch (IOException e) {
                         logger.error("meet an IOException when deserialize the JSON");
                       }
                     }

                     afterValidation.complete(tokenContent);
                   }, 5, TimeUnit.SECONDS);

    return afterValidation.join();
  }
}
