/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker.base.protocols.ilink;

import com.openiot.cloud.base.ilink.*;
import com.openiot.cloud.ibroker.mq.OptJmsReqHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import static org.assertj.core.api.Assertions.assertThat;
import org.iotivity.cloud.base.protocols.MessageBuilder;
import org.iotivity.cloud.base.protocols.coap.CoapDecoder;
import org.iotivity.cloud.base.protocols.coap.CoapEncoder;
import org.iotivity.cloud.base.protocols.coap.CoapMessage;
import org.iotivity.cloud.base.protocols.enums.ContentFormat;
import org.iotivity.cloud.base.protocols.enums.RequestMethod;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ILinkCOAPOverTCPMessageTest {
  private static final Logger logger = LoggerFactory.getLogger(ILinkCOAPOverTCPMessageTest.class);

  private void dumpByteBuf(ByteBuf bb) {
    byte[] slice = new byte[bb.readableBytes()];
    bb.getBytes(0, slice);
    StringBuilder sb = new StringBuilder();
    for (byte b : slice) {
      sb.append(String.format("0x%x ", b));
    }
    System.out.println(sb.toString());
  }

  @Test
  public void testCase1() {
    String payload =
        "it is a long payload with some 1234 and {}*&^% and it is not a COAP_OVER_TCP payload";
    ILinkMessage request =
        new ILinkMessage(LeadingByte.REQUEST.valueOf(),
                         (byte) MessageType.COAP_OVER_TCP.valueOf()).setIlinkMessageId("request".getBytes());
    request.setPayload(payload.getBytes());

    assertThat(request.getIlinkMessageId()).hasSize(4).isEqualTo("uest".getBytes());
    assertThat(request.getPayloadSize()).isEqualTo(payload.length());
    assertThat(request.getPayload()).isEqualTo(payload.getBytes());
  }

  @Test
  public void testCase2() throws Exception {
    CoapEncoder ce = new CoapEncoder();
    CoapDecoder cd = new CoapDecoder();

    CoapMessage cm = (CoapMessage) MessageBuilder.createRequest(RequestMethod.GET,
                                                                "/ibroker/test",
                                                                null,
                                                                ContentFormat.APPLICATION_CBOR,
                                                                null);

    ByteBuf bb = Unpooled.buffer();
    assertThat(bb).isNotNull();

    ReflectionTestUtils.invokeMethod(ce, "encode", null, cm, bb);
    assertThat(bb.readableBytes()).isGreaterThan(0);
    assertThat(bb.array()).isNotNull();

    List<Object> out = new ArrayList<>();
    ReflectionTestUtils.invokeMethod(cd, "decode", null, bb, out);
    assertThat(out).isNotNull();
    assertThat(out.size()).isGreaterThan(0);

    CoapMessage cmReceive = (CoapMessage) (out.get(0));
    assertThat(cmReceive.getUriPath().length()).isGreaterThan(0);
    assertThat(cmReceive.getUriPath()).isEqualTo("/ibroker/test");
  }

  @Test
  public void testCase3() throws Exception {
    System.out.println("testCase3");

    CoapMessage cm = (CoapMessage) MessageBuilder.createRequest(RequestMethod.GET,
                                                                "/ibroker/test",
                                                                null,
                                                                ContentFormat.APPLICATION_CBOR,
                                                                null);

    ByteBuf bb = Unpooled.buffer();
    CoapEncoder ce = new CoapEncoder();
    ReflectionTestUtils.invokeMethod(ce, "encode", null, cm, bb);
    assertThat(bb.readableBytes()).isGreaterThan(0);

    byte[] cmBytes = new byte[bb.readableBytes()];
    bb.getBytes(bb.readerIndex(), cmBytes);

    dumpByteBuf(bb);

    ILinkMessage message =
        new ILinkMessage(LeadingByte.REQUEST.valueOf(), (byte) MessageType.COAP_OVER_TCP.valueOf());
    message.setIlinkMessageId("8339".getBytes()).setPayload(cmBytes);
    System.out.println("testCase3 " + message);

    ByteBuf bbILink = Unpooled.buffer();
    assertThat(bbILink).isNotNull();
    ILinkEncoder ie = new ILinkEncoder();
    ReflectionTestUtils.invokeMethod(ie, "encode", null, message, bbILink);

    dumpByteBuf(bbILink);

    assertThat(bbILink.readableBytes()).isGreaterThan(0);
    assertThat(bbILink.getByte(0)).isEqualTo(LeadingByte.REQUEST.valueOf());

    assertThat(bbILink.isReadable()).isTrue();

    List<Object> out = new LinkedList<>();
    ILinkDecoder id = new ILinkDecoder();
    ReflectionTestUtils.invokeMethod(id, "decode", null, bbILink, out);
    assertThat(out.size()).isEqualTo(1);
    ILinkMessage messageD = (ILinkMessage) out.get(0);

    assertThat(messageD).isNotNull();
    assertThat(MessageType.fromValue(messageD.getMessageType())).isEqualTo(MessageType.COAP_OVER_TCP);
    assertThat(messageD.getPayloadSize()).isEqualTo((message.getPayloadSize()));
    System.out.println("testCase3 done");
  }

  @Test
  public void testCase4() throws Exception {
    CoapEncoder ce = new CoapEncoder();
    CoapMessage cm = (CoapMessage) MessageBuilder.createRequest(RequestMethod.GET,
                                                                "/ibroker/test?query=something",
                                                                null);
    ByteBuf bbCoap = Unpooled.buffer();
    ReflectionTestUtils.invokeMethod(ce, "encode", null, cm, bbCoap);
    byte[] cmBytes = new byte[bbCoap.readableBytes()];
    bbCoap.getBytes(bbCoap.readerIndex(), cmBytes);

    ILinkEncoder ie = new ILinkEncoder();
    ILinkMessage request = new ILinkMessage(LeadingByte.RESPONSE.valueOf(),
                                            (byte) MessageType.COAP_OVER_TCP.valueOf());
    request.setIlinkMessageId("8339".getBytes()).setPayload(cmBytes);;
    System.out.println("testCase4 " + request.toString());
    assertThat(request.getPayloadSize()).isGreaterThan(0);

    ByteBuf bbILink = Unpooled.buffer();
    ReflectionTestUtils.invokeMethod(ie, "encode", null, request, bbILink);

    dumpByteBuf(bbILink);

    ILinkDecoder id = new ILinkDecoder();
    List<Object> out = new ArrayList<>();
    ReflectionTestUtils.invokeMethod(id, "decode", null, bbILink, out);
    assertThat(out.size()).isEqualTo(1);

    ILinkMessage requestD = (ILinkMessage) out.get(0);
    System.out.println("testCase4 " + requestD.toString());
    assertThat(ILinkCoapOverTcpMessageHandler.decodeAsCoapMessage(requestD.getPayload())
                                             .getUriPath()).isNotEmpty()
                                                           .isEqualTo("/ibroker/test?query=something");
  }

  @Test
  public void testCase5() throws Exception {
    ILinkMessage request =
        new ILinkMessage(LeadingByte.REQUEST.valueOf(),
                         (byte) MessageType.COAP_OVER_TCP.valueOf()).setAgentId("agentID-1")
                                                                    .setTag("tag-1")
                                                                    .setIlinkMessageId("8339".getBytes());
    System.out.println("testCase5 " + request.toString());

    ILinkEncoder ie = new ILinkEncoder();
    ByteBuf bb = Unpooled.buffer();
    ReflectionTestUtils.invokeMethod(ie, "encode", null, request, bb);

    dumpByteBuf(bb);

    ILinkDecoder id = new ILinkDecoder();
    List<Object> out = new ArrayList<>();
    ReflectionTestUtils.invokeMethod(id, "decode", null, bb, out);

    ILinkMessage requestD = (ILinkMessage) out.get(0);
    System.out.println("testCase5 2 " + requestD.toString());
    assertThat(requestD.getAgentId()).isNotEmpty().isEqualTo("agentID-1");
    assertThat(requestD.getTag()).isNotEmpty().isEqualTo("tag-1");
  }

  @Test
  public void testCase6() {
    OptJmsReqHandler ojl = new OptJmsReqHandler();
    String uri = "http://localhost:8080/opt/iagent/agent_id/resource";
    String out = null;
    out = ReflectionTestUtils.invokeMethod(ojl, "getEndpointId", uri);
    assertThat(out).isNotEmpty().isEqualTo("agent_id");
  }

  @Test
  public void testCase7() {
    byte[] mid = new byte[] {0, 0, 25, 9};
    byte[] token = new byte[] {9, 25, 0, 0, 0, 0, 0, 0};
    byte[] newToken = ILinkCoapOverTcpMessageHandler.frameToken(mid, token);

    System.out.println(Arrays.toString(newToken));
    assertThat(newToken).hasSize(12);
  }
}
