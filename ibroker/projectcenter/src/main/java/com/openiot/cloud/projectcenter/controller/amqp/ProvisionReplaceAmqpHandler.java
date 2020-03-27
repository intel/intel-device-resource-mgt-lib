/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.projectcenter.controller.ErrorMessage;
import com.openiot.cloud.projectcenter.service.GatewayService;
import com.openiot.cloud.sdk.service.IConnectRequest;
import com.openiot.cloud.sdk.service.IConnectResponse;
import com.openiot.cloud.sdk.service.IConnectServiceHandler;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class ProvisionReplaceAmqpHandler implements IConnectServiceHandler {
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private GatewayService gatewayService;

  @Override
  public void onRequest(IConnectRequest request) {
    Objects.requireNonNull(request);

    try {
      // only support POST
      if (Objects.equals(HttpMethod.POST, request.getAction())) {
        updateReplace(request);
      } else {
        IConnectResponse.createFromRequest(request,
                                           HttpStatus.METHOD_NOT_ALLOWED,
                                           MediaType.APPLICATION_JSON,
                                           objectMapper.writeValueAsBytes(new ErrorMessage("only support POST")))
                        .send();
      }
    } catch (IOException e) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.INTERNAL_SERVER_ERROR,
                                         MediaType.APPLICATION_JSON,
                                         new JSONObject().append("error",
                                                                 "failed to serialize/deserialize with JSON")
                                                         .toString()
                                                         .getBytes())
                      .send();
    }
  }

  void updateReplace(IConnectRequest request) throws IOException {
    log.debug("to update replace flag {}", request);

    Map<String, String> queryParams = UriComponentsBuilder.fromUriString(request.getUrl())
                                                          .build()
                                                          .getQueryParams()
                                                          .toSingleValueMap();
    if (queryParams.isEmpty() || !queryParams.containsKey(ConstDef.Q_IAGENTID)) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need the param \"aid\"")))
                      .send();
      return;
    }

    if (request.getPayload() == null || request.getPayload().length == 0) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need a not empty payload")))
                      .send();
      return;
    }

    String newSerialNumber = objectMapper.readValue(request.getPayload(), String.class);
    if (gatewayService.replaceGateway(queryParams.get(ConstDef.Q_IAGENTID), newSerialNumber)) {
      IConnectResponse.createFromRequest(request, HttpStatus.OK, MediaType.APPLICATION_JSON, null)
                      .send();
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.NOT_ACCEPTABLE,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("replace failed")))
                      .send();
    }
    return;
  }
}
