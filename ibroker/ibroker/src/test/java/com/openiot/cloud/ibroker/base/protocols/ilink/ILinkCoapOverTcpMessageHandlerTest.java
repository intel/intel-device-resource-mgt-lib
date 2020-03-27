/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker.base.protocols.ilink;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.ilink.ILinkMessage;
import com.openiot.cloud.base.ilink.LeadingByte;
import com.openiot.cloud.base.ilink.MessageType;
import com.openiot.cloud.ibroker.base.device.IAgent;
import lombok.extern.slf4j.Slf4j;
import static org.assertj.core.api.Assertions.assertThat;
import org.iotivity.cloud.base.connector.CoapClient;
import org.iotivity.cloud.base.connector.ConnectorPool;
import org.iotivity.cloud.base.device.Device;
import org.iotivity.cloud.base.device.IResponseEventHandler;
import org.iotivity.cloud.base.protocols.IRequest;
import org.iotivity.cloud.base.protocols.MessageBuilder;
import org.iotivity.cloud.base.protocols.coap.CoapMessage;
import org.iotivity.cloud.base.protocols.enums.RequestMethod;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import org.mockito.Mockito;
import static org.mockito.Mockito.doAnswer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ILinkCoapOverTcpMessageHandlerTest {
  @Autowired
  private ILinkCoapOverTcpMessageHandler iLinkCoapOverTcpMessageHandler;

  // they are not bean, so has to mock it manually
  private CoapClient fakeRequestChannel;
  private IAgent fakeIAgent;

  {
    fakeRequestChannel = Mockito.mock(CoapClient.class);
    fakeIAgent = Mockito.mock(IAgent.class);

    doAnswer(invocation -> {
      return true;
    }).when(fakeIAgent).getSessionFlag();

    doAnswer(invocation -> {
      return "i_am_a_fake_iagent";
    }).when(fakeIAgent).getDeviceId();
  }

  @Before
  public void setup() throws Exception {
    assertThat(fakeIAgent).isNotNull();
    assertThat(fakeRequestChannel).isNotNull();
    assertThat(iLinkCoapOverTcpMessageHandler).isNotNull();
    log.debug("fakeRequestChannel {}", fakeRequestChannel);
    ConnectorPool.addConnection("/dp", fakeRequestChannel);
  }

  @Test
  public void testMessagesWithoutResponse() throws Exception {
    final IRequest coapRequest =
        MessageBuilder.createRequest(RequestMethod.POST, ConstDef.DATA_URI, null);

    // send a ILINK_PLAIN message to /dp
    CompletableFuture<Boolean> result1 = new CompletableFuture<>();
    doAnswer(invocation -> {
      IRequest request = invocation.getArgument(0);
      IResponseEventHandler handler = invocation.getArgument(1);
      assertThat(handler).isNull();
      assertThat(request.getUriPath()).isEqualTo(ConstDef.DATA_URI);
      result1.complete(true);
      return null;
    }).when(fakeRequestChannel).sendRequest(isA(IRequest.class), any());
    ILinkMessage iLinkMessage =
        new ILinkMessage(LeadingByte.PLAIN.valueOf(), (byte) MessageType.COAP_OVER_TCP.valueOf());
    iLinkMessage =
        ILinkCoapOverTcpMessageHandler.encodeCoapMessageAsPayload((CoapMessage) coapRequest,
                                                                  iLinkMessage);
    iLinkCoapOverTcpMessageHandler.onMessage(fakeIAgent, iLinkMessage);
    assertThat(result1.get(5, TimeUnit.SECONDS)).isTrue();

    // send a ILINK_REQUEST message to /dp
    CompletableFuture<Boolean> result2 = new CompletableFuture<>();
    doAnswer(invocation -> {
      IRequest request = invocation.getArgument(0);
      IResponseEventHandler handler = invocation.getArgument(1);
      assertThat(handler).isNull();
      assertThat(request.getUriPath()).isEqualTo(ConstDef.DATA_URI);
      result2.complete(true);
      return null;
    }).when(fakeRequestChannel).sendRequest(isA(IRequest.class), any());
    iLinkMessage =
        new ILinkMessage(LeadingByte.REQUEST.valueOf(), (byte) MessageType.COAP_OVER_TCP.valueOf());
    iLinkMessage =
        ILinkCoapOverTcpMessageHandler.encodeCoapMessageAsPayload((CoapMessage) coapRequest,
                                                                  iLinkMessage);
    iLinkCoapOverTcpMessageHandler.onMessage(fakeIAgent, iLinkMessage);
    assertThat(result2.get(5, TimeUnit.SECONDS)).isTrue();
  }
}
