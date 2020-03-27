/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.projectcenter.service.GatewayService;
import com.openiot.cloud.projectcenter.service.dto.GatewayDTO;
import com.openiot.cloud.sdk.service.IConnectRequest;
import com.openiot.cloud.sdk.service.IConnectResponse;
import com.openiot.cloud.sdk.service.JMSResponseSender;
import lombok.extern.slf4j.Slf4j;
import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class ProvisionResetAmqpHandlerTest {
  @Autowired
  private ProvisionResetAmqpHandler provisionResetAmqpHandler;
  @Autowired
  private GatewayService gatewayService;
  @Autowired
  private ObjectMapper objectMapper;
  @Mock
  private JMSResponseSender fakeJMSResponseSender;

  @Before
  public void setup() throws Exception {
    gatewayService.removeAll();
  }

  @Test
  public void testBasic() throws Exception {
    String[] iAgentIds = new String[] {"grape", "cherry", "apple"};
    for (String iAgentId : iAgentIds) {
      GatewayDTO gatewayDTO = new GatewayDTO();
      gatewayDTO.setIAgentId(iAgentId);
      gatewayService.save(gatewayDTO);
    }

    // GET failed
    IConnectRequest request =
        IConnectRequest.create(HttpMethod.GET,
                               UriComponentsBuilder.newInstance()
                                                   .path(ConstDef.MQ_QUEUE_PROV_RESET)
                                                   .queryParam("reset", "false")
                                                   .build()
                                                   .toString(),
                               MediaType.APPLICATION_JSON,
                               objectMapper.writeValueAsBytes(iAgentIds));
    request.setResponseSender(fakeJMSResponseSender);

    CompletableFuture<Boolean> result1 = new CompletableFuture<>();
    doAnswer(invocation -> {
      IConnectResponse response = invocation.getArgument(0);
      assertThat(response.getStatus().is4xxClientError()).isTrue();
      result1.complete(true);
      return null;
    }).when(fakeJMSResponseSender).send(isA(IConnectResponse.class));
    provisionResetAmqpHandler.onRequest(request);
    assertThat(result1.get(5, TimeUnit.SECONDS)).isTrue();

    // POST ok
    request =
        IConnectRequest.create(HttpMethod.POST,
                               UriComponentsBuilder.newInstance()
                                                   .path(ConstDef.MQ_QUEUE_PROV_RESET)
                                                   .queryParam("reset", "true")
                                                   .build()
                                                   .toString(),
                               MediaType.APPLICATION_JSON,
                               objectMapper.writeValueAsBytes(Arrays.copyOfRange(iAgentIds, 0, 2)));
    request.setResponseSender(fakeJMSResponseSender);

    CompletableFuture<Boolean> result2 = new CompletableFuture<>();
    doAnswer(invocation -> {
      IConnectResponse response = invocation.getArgument(0);
      assertThat(response.getStatus().is2xxSuccessful()).isTrue();

      GatewayDTO gatewayDTO = gatewayService.findByIAgentId(iAgentIds[0]);
      assertThat(gatewayDTO.isReset()).isTrue();

      gatewayDTO = gatewayService.findByIAgentId(iAgentIds[2]);
      assertThat(gatewayDTO.isReset()).isFalse();
      result2.complete(true);
      return null;
    }).when(fakeJMSResponseSender).send(isA(IConnectResponse.class));
    provisionResetAmqpHandler.onRequest(request);
    assertThat(result2.get(5, TimeUnit.SECONDS)).isTrue();
  }
}
