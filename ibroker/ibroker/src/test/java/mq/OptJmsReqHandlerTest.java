/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package mq;

import com.openiot.cloud.base.ilink.ILinkMessage;
import com.openiot.cloud.ibroker.IBrokerMain;
import com.openiot.cloud.ibroker.base.device.IAgent;
import com.openiot.cloud.ibroker.base.device.IAgentCache;
import com.openiot.cloud.ibroker.mq.OptJmsReqHandler;
import com.openiot.cloud.sdk.service.IConnectRequest;
import com.openiot.cloud.sdk.service.IConnectResponse;
import com.openiot.cloud.sdk.service.JMSResponseSender;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.function.Function;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {IBrokerMain.class})
public class OptJmsReqHandlerTest {
  @MockBean
  private IAgentCache fakeAgentCache;

  @Autowired
  private OptJmsReqHandler optJmsReqHandler;

  @Test
  public void testOptWithOnlineIAgent() throws Exception {
    String iagentId = "exist";

    IAgent fakeIAgent = mock(IAgent.class);
    doAnswer(invocation -> {
      return iagentId;
    }).when(fakeIAgent).getAgentId();

    doAnswer(invocation -> {
      return true;
    }).when(fakeIAgent).getSessionFlag();

    doAnswer(invocation -> {
      return fakeIAgent;
    }).when(fakeAgentCache).getAgent(isA(String.class));

    doAnswer(invocation -> {
      Object[] arguments = invocation.getArguments();
      ILinkMessage request = (ILinkMessage) arguments[0];

      System.out.println("ILinkMessage " + request);
      assertThat(request.isRequest()).isTrue();
      assertThat(request.getAgentId()).isEqualTo(iagentId);
      return null;
    }).when(fakeIAgent).sendMessage(isA(ILinkMessage.class), isA(Function.class));

    IConnectRequest request =
        IConnectRequest.create(HttpMethod.GET, "/opt/iagent/" + iagentId + "/reset", null, null);
    optJmsReqHandler.onRequest(request);
    verify(fakeIAgent, times(1)).sendMessage(isA(ILinkMessage.class), isA(Function.class));
  }

  @Test
  public void testOptWithOfflineIAgent() throws Exception {
    String iagentId = "not_exist";

    // a standard offline device
    JMSResponseSender fakeJmsResponseSender = mock(JMSResponseSender.class);
    doAnswer(invocation -> {
      Object[] arguments = invocation.getArguments();
      IConnectResponse response = (IConnectResponse) arguments[0];
      assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
      return null;
    }).when(fakeJmsResponseSender).send(isA(IConnectResponse.class));

    doAnswer(invocation -> {
      return null;
    }).when(fakeAgentCache).getAgent(isA(String.class));

    IConnectRequest request =
        IConnectRequest.create(HttpMethod.GET, "/opt/iagent/" + iagentId + "/reset", null, null);
    request.setResponseSender(fakeJmsResponseSender);

    optJmsReqHandler.onRequest(request);
    verify(fakeJmsResponseSender, times(1)).send(isA(IConnectResponse.class));

    // a disconnected device
    IAgent fakeIAgent = mock(IAgent.class);
    doAnswer(invocation -> {
      return false;
    }).when(fakeIAgent).getSessionFlag();

    doAnswer(invocation -> {
      return fakeIAgent;
    }).when(fakeAgentCache).getAgent(isA(String.class));

    request =
        IConnectRequest.create(HttpMethod.GET, "/opt/iagent/" + iagentId + "/reset", null, null);
    request.setResponseSender(fakeJmsResponseSender);

    optJmsReqHandler.onRequest(request);
    verify(fakeJmsResponseSender, times(2)).send(isA(IConnectResponse.class));
  }
}
