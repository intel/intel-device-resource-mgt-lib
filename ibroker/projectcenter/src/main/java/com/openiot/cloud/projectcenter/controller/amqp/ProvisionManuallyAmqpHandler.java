/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.projectcenter.controller.ErrorMessage;
import com.openiot.cloud.projectcenter.service.GatewayService;
import com.openiot.cloud.projectcenter.service.dto.GatewayDTO;
import com.openiot.cloud.projectcenter.utils.RandomKeyGen;
import com.openiot.cloud.sdk.service.IConnectRequest;
import com.openiot.cloud.sdk.service.IConnectResponse;
import com.openiot.cloud.sdk.service.IConnectServiceHandler;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Base64;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class ProvisionManuallyAmqpHandler implements IConnectServiceHandler {

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private GatewayService gatewayService;


  @Override
  public void onRequest(IConnectRequest request) {
    Objects.requireNonNull(request);

    String path = UriComponentsBuilder.fromUriString(request.getUrl()).build().getPath();
    log.debug("a incoming request {} ", request);
    try {
      if (path.startsWith("/prov/manually")) {
        provisionManually(request);
      } else {
        IConnectResponse.createFromRequest(request,
                                           HttpStatus.NOT_IMPLEMENTED,
                                           MediaType.APPLICATION_JSON,
                                           objectMapper.writeValueAsBytes(new ErrorMessage("not support "
                                               + path)))
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


  public void provisionManually(IConnectRequest request) throws IOException {
    Map<String, String> queryParams = UriComponentsBuilder.fromUriString(request.getUrl())
                                                          .build()
                                                          .getQueryParams()
                                                          .toSingleValueMap();
    // request payload can not be null

    if (!queryParams.isEmpty() && !queryParams.containsValue("") && queryParams.containsKey("sn")
        && queryParams.containsKey("pi")) {

      // the gateway has existed
      Boolean isExisted = false;
      GatewayDTO gateway = gatewayService.findBySerialNumber(queryParams.get("sn"), false);
      if (gateway != null) {
        isExisted = true;
      } else {

        // but still may be a new serial number of an existing gateway
        gateway = gatewayService.findBySerialNumber(queryParams.get("sn"), true);
        if (gateway != null) {
          isExisted = true;
        }
      }

      if (isExisted) {
        IConnectResponse.createFromRequest(request,
                                           HttpStatus.BAD_REQUEST,
                                           MediaType.APPLICATION_JSON,
                                           objectMapper.writeValueAsBytes(new ErrorMessage(String.format("%s has existed",
                                                                                                         queryParams.get("sn")))))
                        .send();
        return;
      }

      // generate a new prov-key
      byte[] provKey = RandomKeyGen.generate();
      gateway = new GatewayDTO();
      gateway.setProvKey(new String(Base64.encode(provKey)));
      gateway.setProvTime(Instant.now(Clock.systemUTC()).toEpochMilli());
      gateway.setHwSn(queryParams.get("sn"));
      gateway.setProjectId(queryParams.get("pi"));
      // use the serial number as iAgentId
      gateway.setIAgentId(queryParams.get("sn"));
      // set flag for this kind of devices
      gateway.setManual(true);

      Objects.requireNonNull(gateway);
      Objects.requireNonNull(gateway.getIAgentId());
      Objects.requireNonNull(gateway.getProvKey());
      Objects.requireNonNull(gateway.getHwSn());
      gatewayService.save(gateway);
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.OK,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(String.format("provision success")))
                      .send();

    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need \"sn\" and \"pi\"")))
                      .send();
      return;
    }

  }
}
