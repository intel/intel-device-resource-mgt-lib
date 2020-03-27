/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.httpproxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.common.model.TokenContent;
import com.openiot.cloud.base.mongo.model.help.UserRole;
import com.openiot.cloud.sdk.service.*;
import static org.assertj.core.api.Assertions.assertThat;
import org.iotivity.cloud.base.connector.ConnectorPool;
import org.iotivity.cloud.base.device.IRequestChannel;
import org.iotivity.cloud.base.device.IResponseEventHandler;
import org.iotivity.cloud.base.protocols.IRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isA;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.async.DeferredResult;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HttpProxySecurityTest {
  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private IConnect iConnect;
  @Autowired
  private HttpProxyServer httpProxyServer;
  @Mock
  private JmsMqClient fakeMQClient;
  @Mock
  private GenericCoapTcpServiceClient coapTcpClient;

  @Before
  public void setup() throws Exception {
    ConnectorPool.addConnection("/meta", new IRequestChannel() {
      @Override
      public void sendRequest(IRequest request, IResponseEventHandler responseEvent) {}
    });

    ConnectorPool.addConnection("/rd", new IRequestChannel() {
      @Override
      public void sendRequest(IRequest request, IResponseEventHandler responseEvent) {}
    });

    ConnectorPool.addConnection("/dp", new IRequestChannel() {
      @Override
      public void sendRequest(IRequest request, IResponseEventHandler responseEvent) {}
    });

    ReflectionTestUtils.setField(httpProxyServer, "coapProxy", coapTcpClient);
    ReflectionTestUtils.setField(iConnect, "mqClient", fakeMQClient);
  }

  @Test
  public void testSecurityBasic() throws Exception {
    doAnswer(invocationOnMock -> {
      Object[] arguments = invocationOnMock.getArguments();
      IConnectRequest request = (IConnectRequest) arguments[0];
      IConnectResponseHandler handler = (IConnectResponseHandler) arguments[1];

      if (request.getUrl().equals("/api/user/validation")) {
        TokenContent tokenContent = new TokenContent();
        tokenContent.setUser("apple");
        tokenContent.setProject("orange");
        tokenContent.setRole(UserRole.USER);

        IConnectResponse response =
            IConnectResponse.createFromRequest(request,
                                               HttpStatus.OK,
                                               MediaType.APPLICATION_JSON,
                                               objectMapper.writeValueAsBytes(tokenContent));
        handler.onResponse(response);
      } else {
        IConnectResponse response =
            IConnectResponse.createFromRequest(request,
                                               HttpStatus.ALREADY_REPORTED,
                                               MediaType.APPLICATION_JSON,
                                               objectMapper.writeValueAsBytes("empty payload"));
        handler.onResponse(response);
      }

      return null;
    }).when(fakeMQClient).send(isA(IConnectRequest.class),
                               isA(IConnectResponseHandler.class),
                               anyInt(),
                               isA(TimeUnit.class));

    doAnswer(invocationOnMock -> {
      Object[] arguments = invocationOnMock.getArguments();
      ResponseEntity<byte[]> response = ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).build();
      DeferredResult<ResponseEntity<?>> completableFuture =
          (DeferredResult<ResponseEntity<?>>) arguments[2];
      completableFuture.setResult(response);
      return null;
    }).when(coapTcpClient).defaultRequestHandle(isA(String.class),
                                                isA(RequestEntity.class),
                                                isA(DeferredResult.class),
                                                isA(Map.class));

    doAnswer(invocationOnMock -> {
      Object[] arguments = invocationOnMock.getArguments();
      ResponseEntity<byte[]> response = ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).build();
      CompletableFuture<ResponseEntity<byte[]>> completableFuture =
          (CompletableFuture<ResponseEntity<byte[]>>) arguments[1];
      completableFuture.complete(response);
      return null;
    }).when(coapTcpClient).defaultRequestHandle(isA(RequestEntity.class),
                                                isA(CompletableFuture.class),
                                                isA(String.class),
                                                isA(Map.class));

    HttpHeaders headersWithFakeToken = new HttpHeaders();
    headersWithFakeToken.setBearerAuth(" FAKE_BUT_GOOD ");

    // need authorization information
    ResponseEntity<String> responseEntity = testRestTemplate.getForEntity("/fc/meta", String.class);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    // need ADMIN
    HttpEntity<String> entityWithToken = new HttpEntity<>("fake payload", headersWithFakeToken);
    responseEntity =
        testRestTemplate.exchange("/fc/meta", HttpMethod.POST, entityWithToken, String.class);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    entityWithToken = new HttpEntity<>(null, headersWithFakeToken);
    responseEntity =
        testRestTemplate.exchange("/fc/meta", HttpMethod.GET, entityWithToken, String.class);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.I_AM_A_TEAPOT);

    // authorized
    responseEntity =
        testRestTemplate.exchange("/fc/api/alarm", HttpMethod.POST, entityWithToken, String.class);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.ALREADY_REPORTED);

    //////////////////////////////////////////////////////////////////////
    // BAD TOKEN
    //////////////////////////////////////////////////////////////////////

    doAnswer(invocationOnMock -> {
      Object[] arguments = invocationOnMock.getArguments();
      IConnectRequest request = (IConnectRequest) arguments[0];
      IConnectResponseHandler handler = (IConnectResponseHandler) arguments[1];

      // BAD TOKEN
      if (request.getUrl().equals("/api/user/validation")) {
        IConnectResponse response = IConnectResponse.createFromRequest(request,
                                                                       HttpStatus.BAD_REQUEST,
                                                                       MediaType.APPLICATION_JSON,
                                                                       null);
        handler.onResponse(response);
      } else {
        IConnectResponse response =
            IConnectResponse.createFromRequest(request,
                                               HttpStatus.I_AM_A_TEAPOT,
                                               MediaType.APPLICATION_JSON,
                                               objectMapper.writeValueAsBytes("empty payload"));
        handler.onResponse(response);
      }

      return null;
    }).when(fakeMQClient).send(isA(IConnectRequest.class),
                               isA(IConnectResponseHandler.class),
                               anyInt(),
                               isA(TimeUnit.class));
    // BAD TOKEN
    HttpHeaders headersWithBadToken = new HttpHeaders();
    headersWithBadToken.setBearerAuth(" BAD_WORSE_WORST ");
    entityWithToken = new HttpEntity<>(null, headersWithBadToken);
    responseEntity =
        testRestTemplate.exchange("/fc/meta", HttpMethod.GET, entityWithToken, String.class);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  public void testSecurityWORole() throws Exception {
    doAnswer(invocationOnMock -> {
      Object[] arguments = invocationOnMock.getArguments();
      IConnectRequest request = (IConnectRequest) arguments[0];
      IConnectResponseHandler handler = (IConnectResponseHandler) arguments[1];

      if (request.getUrl().equals("/api/user/validation")) {
        TokenContent tokenContent = new TokenContent();
        tokenContent.setUser("apple");
        tokenContent.setProject("orange");
        tokenContent.setRole(null);

        IConnectResponse response =
            IConnectResponse.createFromRequest(request,
                                               HttpStatus.OK,
                                               MediaType.APPLICATION_JSON,
                                               objectMapper.writeValueAsBytes(tokenContent));
        handler.onResponse(response);
      } else {
        IConnectResponse response =
            IConnectResponse.createFromRequest(request,
                                               HttpStatus.ALREADY_REPORTED,
                                               MediaType.APPLICATION_JSON,
                                               objectMapper.writeValueAsBytes("empty payload"));
        handler.onResponse(response);
      }

      return null;
    }).when(fakeMQClient).send(isA(IConnectRequest.class),
                               isA(IConnectResponseHandler.class),
                               anyInt(),
                               isA(TimeUnit.class));

    HttpHeaders headersWithBadToken = new HttpHeaders();
    headersWithBadToken.setBearerAuth(" ABCDEFG ");

    HttpEntity<String> entityWithToken = new HttpEntity<>(null, headersWithBadToken);
    ResponseEntity<String> responseEntity =
        testRestTemplate.exchange("/fc/api/alarm", HttpMethod.GET, entityWithToken, String.class);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.ALREADY_REPORTED);
  }
}
