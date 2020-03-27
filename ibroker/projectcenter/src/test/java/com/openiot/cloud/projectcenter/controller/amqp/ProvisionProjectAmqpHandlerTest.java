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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class ProvisionProjectAmqpHandlerTest {
  @Autowired
  private ProvisionProjectAmqpHandler provisionProjectAmqpHandler;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private GatewayService gatewayService;
  @Autowired
  private ProjectService projectService;
  @Mock
  private JMSResponseSender fakeJMSResponseSender;

  @Before
  public void setup() throws Exception {
    gatewayService.removeAll();
    projectService.removeAll();
  }

  @Test
  public void testBasic() throws Exception {
    ProjectDTO projectDTO = new ProjectDTO();
    projectDTO.setId("pomegranate");
    projectService.save(projectDTO);

    String[] iAgentIds = new String[] {"grape", "cherry", "apple"};
    for (String iAgentId : iAgentIds) {
      GatewayDTO gatewayDTO = new GatewayDTO();
      gatewayDTO.setIAgentId(iAgentId);
      gatewayDTO.setHwSn(iAgentId);
      gatewayService.save(gatewayDTO);
    }

    // POST
    IConnectRequest request =
        IConnectRequest.create(HttpMethod.POST,
                               UriComponentsBuilder.newInstance()
                                                   .path(ConstDef.MQ_QUEUE_PROV_PROJECT)
                                                   .queryParam(ConstDef.Q_PROJECTID,
                                                               projectDTO.getId())
                                                   .build()
                                                   .toString(),
                               MediaType.APPLICATION_JSON,
                               objectMapper.writeValueAsBytes(new String[] {iAgentIds[0],
                                   iAgentIds[1]}));
    request.setResponseSender(fakeJMSResponseSender);

    CompletableFuture<Boolean> result1 = new CompletableFuture<>();
    doAnswer(invocation -> {
      IConnectResponse response = invocation.getArgument(0);
      assertThat(response.getStatus().is2xxSuccessful()).isTrue();

      GatewayDTO gatewayDTO = gatewayService.findByIAgentId(iAgentIds[0]);
      assertThat(gatewayDTO).hasFieldOrPropertyWithValue("projectId", projectDTO.getId());

      gatewayDTO = gatewayService.findByIAgentId(iAgentIds[2]);
      assertThat(gatewayDTO.getProjectId()).isNullOrEmpty();

      result1.complete(true);
      return null;
    }).when(fakeJMSResponseSender).send(isA(IConnectResponse.class));
    provisionProjectAmqpHandler.onRequest(request);
    assertThat(result1.get(5, TimeUnit.SECONDS)).isTrue();

    // GET di+pi
    request = IConnectRequest.create(HttpMethod.GET,
                                     UriComponentsBuilder.newInstance()
                                                         .path(ConstDef.MQ_QUEUE_PROV_PROJECT)
                                                         .queryParam(ConstDef.Q_PROJECTID,
                                                                     projectDTO.getId())
                                                         .queryParam(ConstDef.Q_DEVID, iAgentIds[0])
                                                         .build()
                                                         .toString(),
                                     MediaType.APPLICATION_JSON,
                                     null);
    request.setResponseSender(fakeJMSResponseSender);

    CompletableFuture<Boolean> result2 = new CompletableFuture<>();
    doAnswer(invocation -> {
      IConnectResponse response = invocation.getArgument(0);
      assertThat(response.getStatus().is2xxSuccessful()).isTrue();

      GatewayDTO gatewayDTO = objectMapper.readValue(response.getPayload(), GatewayDTO[].class)[0];
      assertThat(gatewayDTO).hasFieldOrPropertyWithValue("iAgentId", iAgentIds[0])
                            .hasFieldOrPropertyWithValue("projectId", projectDTO.getId());

      result2.complete(true);
      return null;
    }).when(fakeJMSResponseSender).send(isA(IConnectResponse.class));
    provisionProjectAmqpHandler.onRequest(request);
    assertThat(result2.get(5, TimeUnit.SECONDS)).isTrue();

    // GET di
    request = IConnectRequest.create(HttpMethod.GET,
                                     UriComponentsBuilder.newInstance()
                                                         .path(ConstDef.MQ_QUEUE_PROV_PROJECT)
                                                         .queryParam(ConstDef.Q_DEVID, iAgentIds[2])
                                                         .build()
                                                         .toString(),
                                     MediaType.APPLICATION_JSON,
                                     null);
    request.setResponseSender(fakeJMSResponseSender);

    CompletableFuture<Boolean> result3 = new CompletableFuture<>();
    doAnswer(invocation -> {
      IConnectResponse response = invocation.getArgument(0);
      assertThat(response.getStatus().is2xxSuccessful()).isTrue();

      GatewayDTO gatewayDTO = objectMapper.readValue(response.getPayload(), GatewayDTO[].class)[0];
      assertThat(gatewayDTO).hasFieldOrPropertyWithValue("iAgentId", iAgentIds[2])
                            .hasFieldOrPropertyWithValue("projectId", null);

      result3.complete(true);
      return null;
    }).when(fakeJMSResponseSender).send(isA(IConnectResponse.class));
    provisionProjectAmqpHandler.onRequest(request);
    assertThat(result3.get(5, TimeUnit.SECONDS)).isTrue();

    // GET pi
    request = IConnectRequest.create(HttpMethod.GET,
                                     UriComponentsBuilder.newInstance()
                                                         .path(ConstDef.MQ_QUEUE_PROV_PROJECT)
                                                         .queryParam(ConstDef.Q_PROJECTID,
                                                                     projectDTO.getId())
                                                         .build()
                                                         .toString(),
                                     MediaType.APPLICATION_JSON,
                                     null);
    request.setResponseSender(fakeJMSResponseSender);

    CompletableFuture<Boolean> result4 = new CompletableFuture<>();
    doAnswer(invocation -> {
      IConnectResponse response = invocation.getArgument(0);
      assertThat(response.getStatus().is2xxSuccessful()).isTrue();

      GatewayDTO[] gatewayDTOs = objectMapper.readValue(response.getPayload(), GatewayDTO[].class);
      assertThat(gatewayDTOs).hasSize(2).extracting("iAgentId").containsOnly(iAgentIds[0],
                                                                             iAgentIds[1]);

      result4.complete(true);
      return null;
    }).when(fakeJMSResponseSender).send(isA(IConnectResponse.class));
    provisionProjectAmqpHandler.onRequest(request);
    assertThat(result4.get(5, TimeUnit.SECONDS)).isTrue();

    // DELETE
    request = IConnectRequest.create(HttpMethod.DELETE,
                                     UriComponentsBuilder.newInstance()
                                                         .path(ConstDef.MQ_QUEUE_PROV_PROJECT)
                                                         .build()
                                                         .toString(),
                                     MediaType.APPLICATION_JSON,
                                     objectMapper.writeValueAsBytes(new String[] {iAgentIds[0]}));
    request.setResponseSender(fakeJMSResponseSender);

    CompletableFuture<Boolean> result5 = new CompletableFuture<>();
    doAnswer(invocation -> {
      IConnectResponse response = invocation.getArgument(0);
      assertThat(response.getStatus().is2xxSuccessful()).isTrue();

      GatewayDTO gatewayDTO = gatewayService.findByIAgentId(iAgentIds[0]);
      assertThat(gatewayDTO.getProjectId()).isNullOrEmpty();

      gatewayDTO = gatewayService.findByIAgentId(iAgentIds[1]);
      assertThat(gatewayDTO).hasFieldOrPropertyWithValue("projectId", projectDTO.getId());

      result5.complete(true);
      return null;
    }).when(fakeJMSResponseSender).send(isA(IConnectResponse.class));
    provisionProjectAmqpHandler.onRequest(request);
    assertThat(result5.get(5, TimeUnit.SECONDS)).isTrue();
  }
}
