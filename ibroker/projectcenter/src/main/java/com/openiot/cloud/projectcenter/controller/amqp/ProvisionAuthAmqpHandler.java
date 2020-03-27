/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.projectcenter.controller.ErrorMessage;
import com.openiot.cloud.projectcenter.service.GatewayService;
import com.openiot.cloud.projectcenter.service.dto.GatewayDTO;
import com.openiot.cloud.projectcenter.utils.AES128CBC;
import com.openiot.cloud.projectcenter.utils.MD5;
import com.openiot.cloud.projectcenter.utils.RandomKeyGen;
import com.openiot.cloud.sdk.service.IConnectRequest;
import com.openiot.cloud.sdk.service.IConnectResponse;
import com.openiot.cloud.sdk.service.IConnectServiceHandler;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Base64;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class ProvisionAuthAmqpHandler implements IConnectServiceHandler {
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private GatewayService gatewayService;

  @Override
  public void onRequest(IConnectRequest request) {
    Objects.requireNonNull(request);

    try {

      if (Objects.equals(HttpMethod.POST, request.getAction())) {
        doAuth(request);
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

  void doAuth(IConnectRequest request) throws IOException {
    Map<String, String> queryParams = UriComponentsBuilder.fromUriString(request.getUrl())
                                                          .build()
                                                          .getQueryParams()
                                                          .toSingleValueMap();
    if (queryParams.isEmpty() || !queryParams.containsKey(ConstDef.Q_IAGENTID)
        || !queryParams.containsKey(ConstDef.Q_RANDOM)) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need the param \"aid\" and \"random\"")))
                      .send();
      return;
    }

    GatewayDTO gateway = gatewayService.findByIAgentId(queryParams.get(ConstDef.Q_IAGENTID));
    if (gateway == null) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.NOT_FOUND,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage(String.format("not found any one with iAgentId %s",
                                                                                                       queryParams.get(ConstDef.Q_IAGENTID)))))
                      .send();
      return;
    }

    // if the gateway is marked to reset, need to provision with 1804 again
    if (gateway.isReset()) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.FORBIDDEN,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage(String.format("%s is under reset, need provision again",
                                                                                                       queryParams.get(ConstDef.Q_IAGENTID)))))
                      .send();
      return;
    }

    if (gateway.getNewHwSn() != null && !gateway.getNewHwSn().isEmpty()) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.FORBIDDEN,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage(String.format("%s is under replacement, need provision first",
                                                                                                       queryParams.get(ConstDef.Q_IAGENTID)))))
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

    byte[] key = Base64.decode(gateway.getProvKey().getBytes());
    byte[] iv = MD5.getMd5Hash(gateway.getHwSn().getBytes());
    byte[] randomCode1Md5 = MD5.getMd5Hash(queryParams.get(ConstDef.Q_RANDOM).getBytes());
    byte[] verifiedPayload = verifyAndCreateResp(request.getPayload(), key, iv, randomCode1Md5);

    if (verifiedPayload == null) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage(String.format("iAgent: %s authentication failed! Might because of the Base64 decoded key %s",
                                                                                                       gateway.getIAgentId(),
                                                                                                       Arrays.toString(key)))))
                      .send();
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.OK,
                                         MediaType.TEXT_PLAIN,
                                         verifiedPayload)
                      .send();
    }
  }

  private byte[] verifyAndCreateResp(byte[] data, byte[] key, byte[] iv, byte[] randomMd5) {
    if (data == null) {
      return null;
    }

    byte[] decrypt = AES128CBC.decrypt(data, key, iv);
    if (decrypt == null || decrypt.length != 32) {
      log.warn("Decryption failed!");
      return null;
    }

    byte[] random1 = new byte[16];
    byte[] random2 = new byte[16];
    System.arraycopy(decrypt, 0, random1, 0, 16);
    System.arraycopy(decrypt, 16, random2, 0, 16);

    // verify random code 1 MD5
    if (!Arrays.equals(random1, randomMd5)) {
      log.info(String.format("[Auth] randomMd5=%s", randomMd5));
      log.warn("[Auth] Verify random code 1 Md5 Failed! " + Arrays.toString(random1) + " != "
          + Arrays.toString(randomMd5));
      return null;
    }

    byte[] respRaw = new byte[32];
    byte[] sessionKey = RandomKeyGen.generate();

    System.arraycopy(random2, 0, respRaw, 0, random2.length);
    System.arraycopy(sessionKey, 0, respRaw, 16, sessionKey.length);

    return AES128CBC.encrypt(respRaw, key, iv);
  }
}
