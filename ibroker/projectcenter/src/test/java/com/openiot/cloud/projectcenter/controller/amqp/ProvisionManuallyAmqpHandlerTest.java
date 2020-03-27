/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.projectcenter.service.GatewayService;
import com.openiot.cloud.projectcenter.service.ProjectService;
import com.openiot.cloud.projectcenter.service.dto.GatewayDTO;
import com.openiot.cloud.projectcenter.service.dto.ProjectDTO;
import com.openiot.cloud.projectcenter.utils.RandomKeyGen;
import com.openiot.cloud.sdk.service.IConnectRequest;
import com.openiot.cloud.sdk.service.IConnectResponse;
import com.openiot.cloud.sdk.service.JMSResponseSender;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class ProvisionManuallyAmqpHandlerTest {

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private GatewayService gatewayService;

  @Autowired
  private ProjectService projectService;


  @Mock
  private JMSResponseSender fakeJMSResponseSender;

  @Autowired
  private ProvisionManuallyAmqpHandler provisionManuallyAmqpHandler;

  @Before
  public void setup() throws Exception {
    gatewayService.removeAll();
    projectService.removeAll();
  }

  @Test
  public void testBasic() throws Exception {

    byte[] provKey = RandomKeyGen.generate();
    String SN = "cherry";

    ProjectDTO projectDTO = new ProjectDTO();
    projectDTO.setId("pomegranate");
    projectService.save(projectDTO);

    GatewayDTO gw = new GatewayDTO();
    gw.setHwSn(SN);
    gw.setIAgentId(SN);
    gw.setManual(false);
    gw.setProjectId(projectDTO.getId());
    gw.setProvKey(new String(Base64.encode(provKey)));
    gatewayService.save(gw);

    // POST a existed device and return failed
    IConnectRequest request =
        IConnectRequest.create(HttpMethod.POST,
                               UriComponentsBuilder.newInstance()
                                                   .path("/prov/manually")
                                                   .queryParam(ConstDef.Q_PROJECTID,
                                                               projectDTO.getId())
                                                   .queryParam("sn", SN)
                                                   .build()
                                                   .toString(),
                               MediaType.APPLICATION_JSON,
                               null);
    request.setResponseSender(fakeJMSResponseSender);

    CompletableFuture<Boolean> result1 = new CompletableFuture<>();
    doAnswer(invocation -> {
      IConnectResponse response = invocation.getArgument(0);
      assertThat(response.getStatus().is4xxClientError()).isTrue();
      result1.complete(true);
      return null;
    }).when(fakeJMSResponseSender).send(isA(IConnectResponse.class));
    provisionManuallyAmqpHandler.onRequest(request);
    assertThat(result1.get(5, TimeUnit.SECONDS)).isTrue();

    // POST without params and return failed
    request = IConnectRequest.create(HttpMethod.POST,
                                     UriComponentsBuilder.newInstance()
                                                         .path("/prov/manually")
                                                         .build()
                                                         .toString(),
                                     MediaType.APPLICATION_JSON,
                                     null);
    request.setResponseSender(fakeJMSResponseSender);

    doAnswer(invocation -> {
      IConnectResponse response = invocation.getArgument(0);
      assertThat(response.getStatus().is4xxClientError()).isTrue();

      result1.complete(true);
      return null;
    }).when(fakeJMSResponseSender).send(isA(IConnectResponse.class));
    provisionManuallyAmqpHandler.onRequest(request);
    assertThat(result1.get(5, TimeUnit.SECONDS)).isTrue();

    // POST without "sn" param and return faild
    request = IConnectRequest.create(HttpMethod.POST,
                                     UriComponentsBuilder.newInstance()
                                                         .path("/prov/manually")
                                                         .queryParam(ConstDef.Q_PROJECTID,
                                                                     projectDTO.getId())
                                                         .queryParam("sn", "")
                                                         .build()
                                                         .toString(),
                                     MediaType.APPLICATION_JSON,
                                     null);
    request.setResponseSender(fakeJMSResponseSender);

    doAnswer(invocation -> {
      IConnectResponse response = invocation.getArgument(0);
      assertThat(response.getStatus().is4xxClientError()).isTrue();

      result1.complete(true);
      return null;
    }).when(fakeJMSResponseSender).send(isA(IConnectResponse.class));
    provisionManuallyAmqpHandler.onRequest(request);
    assertThat(result1.get(5, TimeUnit.SECONDS)).isTrue();

    // POST device and return ok
    request = IConnectRequest.create(HttpMethod.POST,
                                     UriComponentsBuilder.newInstance()
                                                         .path("/prov/manually")
                                                         .queryParam(ConstDef.Q_PROJECTID,
                                                                     projectDTO.getId())
                                                         .queryParam("sn", "banana")
                                                         .build()
                                                         .toString(),
                                     MediaType.APPLICATION_JSON,
                                     null);
    request.setResponseSender(fakeJMSResponseSender);

    doAnswer(invocation -> {
      IConnectResponse response = invocation.getArgument(0);
      assertThat(response.getStatus().is2xxSuccessful()).isTrue();
      result1.complete(true);
      return null;
    }).when(fakeJMSResponseSender).send(isA(IConnectResponse.class));
    provisionManuallyAmqpHandler.onRequest(request);
    assertThat(result1.get(5, TimeUnit.SECONDS)).isTrue();
  }
}
