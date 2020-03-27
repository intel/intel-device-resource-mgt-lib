/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.amqp;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.projectcenter.service.GatewayService;
import com.openiot.cloud.projectcenter.service.dto.GatewayDTO;
import com.openiot.cloud.projectcenter.utils.AES128CBC;
import com.openiot.cloud.projectcenter.utils.MD5;
import com.openiot.cloud.projectcenter.utils.RandomKeyGen;
import com.openiot.cloud.sdk.service.IConnectRequest;
import com.openiot.cloud.sdk.service.IConnectResponse;
import com.openiot.cloud.sdk.service.JMSResponseSender;
import lombok.extern.slf4j.Slf4j;
import static org.assertj.core.api.Assertions.assertThat;
import org.bouncycastle.util.encoders.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.isA;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ProvisionAuthAmqpHandlerTest {
  @Autowired
  private ProvisionAuthAmqpHandler provisionAuthAmqpHandler;
  @Autowired
  private GatewayService gatewayService;
  @Mock
  private JMSResponseSender fakeResponseSender;

  @Before
  public void setup() throws Exception {
    gatewayService.removeAll();
  }

  @Test
  public void testBasic() throws Exception {
    final String serialNumber = "test_obd";
    final String iAgentId = serialNumber;
    final String provKey = "test_obd";
    final String random1 = "test_obd";
    final String random2 = "whatever_it_is";

    // TODO:
    byte[] payload = new byte[32];

    final Method m = RandomKeyGen.class.getDeclaredMethod("getStrMD5Hash", String.class);
    m.setAccessible(true);

    final byte[] encryptKey = (byte[]) m.invoke(null, provKey);
    final byte[] encryptIV = MD5.getMd5Hash(serialNumber.getBytes());

    byte[] encryptContent016 = MD5.getMd5Hash(random1.getBytes());
    System.arraycopy(encryptContent016, 0, payload, 0, 16);

    byte[] encryptContent1632 = MD5.getMd5Hash(random2.getBytes());
    System.arraycopy(encryptContent1632, 0, payload, 16, 16);

    payload = AES128CBC.encrypt(payload, encryptKey, encryptIV);

    // prepare a gateway document
    GatewayDTO gatewayDTO = new GatewayDTO();
    gatewayDTO.setProvKey(new String(Base64.encode(encryptKey)));
    gatewayDTO.setHwSn(serialNumber);
    gatewayDTO.setIAgentId(iAgentId);
    gatewayService.save(gatewayDTO);
    log.debug("gateway is " + gatewayDTO);

    //
    CompletableFuture<Boolean> result1 = new CompletableFuture<>();
    doAnswer(invocation -> {
      IConnectResponse response = invocation.getArgument(0);
      result1.complete(response.getStatus().is2xxSuccessful());
      return null;
    }).when(fakeResponseSender).send(isA(IConnectResponse.class));

    IConnectRequest request = IConnectRequest.create(HttpMethod.POST,
                                                     String.format("%s?random=%s&aid=%s",
                                                                   ConstDef.MQ_QUEUE_PROV_AUTH,
                                                                   random1,
                                                                   iAgentId),
                                                     MediaType.TEXT_PLAIN,
                                                     payload);
    request.setResponseSender(fakeResponseSender);
    provisionAuthAmqpHandler.onRequest(request);
    assertThat(result1.get(5, TimeUnit.SECONDS)).isTrue();
  }

}
