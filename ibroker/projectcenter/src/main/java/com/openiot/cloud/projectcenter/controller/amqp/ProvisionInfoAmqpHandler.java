/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.projectcenter.controller.ErrorMessage;
import com.openiot.cloud.projectcenter.service.GatewayService;
import com.openiot.cloud.projectcenter.service.dto.GatewayDTO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;



@Component
@Slf4j
public class ProvisionInfoAmqpHandler implements IConnectServiceHandler {
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private GatewayService gatewayService;

  @Override
  public void onRequest(IConnectRequest request) {
    Objects.requireNonNull(request);

    try {
      // only support GET
      if (Objects.equals(HttpMethod.GET, request.getAction())) {
        queryInformation(request);
      } else {
        IConnectResponse.createFromRequest(request,
                                           HttpStatus.METHOD_NOT_ALLOWED,
                                           MediaType.APPLICATION_JSON,
                                           objectMapper.writeValueAsBytes(new ErrorMessage("only support GET")))
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

  void queryInformation(IConnectRequest request) throws IOException {
    Map<String, String> queryParams = UriComponentsBuilder.fromUriString(request.getUrl())
                                                          .build()
                                                          .getQueryParams()
                                                          .toSingleValueMap();

    List<GatewayDTO> list = new ArrayList<>();

    if (queryParams.containsKey("aid")) {
      list.add(gatewayService.findByIAgentId(queryParams.get("aid")));
    } else if (queryParams.containsKey("sn")) {
      list.add(gatewayService.findBySerialNumber(queryParams.get("sn"), false));
    } else {
      list = gatewayService.findAll();
    }

    IConnectResponse.createFromRequest(request,
                                       HttpStatus.OK,
                                       MediaType.APPLICATION_JSON,
                                       objectMapper.writeValueAsBytes(list))
                    .send();
  }
}
