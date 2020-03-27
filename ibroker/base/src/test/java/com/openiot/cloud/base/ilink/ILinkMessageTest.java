/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.ilink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.openiot.cloud.base.Application;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.help.MessageIdMaker;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class})
public class ILinkMessageTest {
  @Test
  public void testCoder() throws Exception {
    // an empty payload
    ILinkMessage original =
        new ILinkMessage(LeadingByte.REQUEST.valueOf(),
                         (byte) MessageType.INTEL_IAGENT.valueOf()).setIlinkMessageId("1234".getBytes())
                                                                   .setAgentId("dev1");
    ILinkMessage after = decodeEncoded(original);
    assertThat(after).isNotNull();
    assertThat(after.getLeadingByte()).isEqualTo((byte) LeadingByte.REQUEST.valueOf());
    assertThat(after.getMessageType()).isEqualTo((byte) MessageType.INTEL_IAGENT.valueOf());
    assertThat(after.getAgentId()).isEqualTo("dev1");
    assertThat(after.getMessageType()).isEqualTo((byte) MessageType.INTEL_IAGENT.valueOf());

    after = decodeEncoded(original);
    assertThat(after.getIlinkMessageId()).isEqualTo("1234".getBytes());

    // with payload
    original =
        new ILinkMessage(LeadingByte.REQUEST.valueOf(),
                         (byte) MessageType.COAP_OVER_TCP.valueOf()).setIlinkMessageId("1234".getBytes())
                                                                    .setAgentId("dev1")
                                                                    .setTag("tag1")
                                                                    .setPayload("test".getBytes());
    after = decodeEncoded(original);
    assertThat(after.getPayload()).isEqualTo("test".getBytes());
    assertThat(after.getAgentId()).isEqualTo("dev1");
    assertThat(after.getMessageType()).isEqualTo((byte) MessageType.COAP_OVER_TCP.valueOf());

    // w/o ilinkmessageid
    original =
        new ILinkMessage(LeadingByte.REQUEST.valueOf(),
                         (byte) MessageType.INTEL_IAGENT.valueOf()).setPayload("test".getBytes());
    after = decodeEncoded(original);
    assertThat(after.getPayload()).isEqualTo("test".getBytes());
    assertThat(after.getIlinkMessageId()).isEqualTo(new byte[] {0, 0, 0, 0});

    // handshake
    original =
        new ILinkMessage(LeadingByte.PLAIN.valueOf(),
                         (byte) MessageType.INTEL_IAGENT.valueOf()).setAgentId("dev1")
                                                                   .setTag(ConstDef.FH_V_HAN1)
                                                                   .setIlinkMessageId(MessageIdMaker.IntegerToBytes(1));
    after = decodeEncoded(original);
    // plain messages don't have a ilink messge id
    assertThat(after.getIlinkMessageId()).isEqualTo(new byte[] {0, 0, 0, 0});
    assertThat(after.getTag()).isEqualTo(ConstDef.FH_V_HAN1);
    assertThat(after.getMessageType()).isEqualTo((byte) MessageType.INTEL_IAGENT.valueOf());

    // something new
    original =
        new ILinkMessage(LeadingByte.PLAIN.valueOf(), (byte) MessageType.INTEL_IAGENT.valueOf());
    Map<String, Object> flexHeader = new HashMap<>();
    flexHeader.put("k1", "key_1");
    flexHeader.put("k2", 100);
    original.setFlexHeadre(flexHeader);
    after = decodeEncoded(original);

    // plain messages don't have a ilink messge id
    assertThat(after.getIlinkMessageId()).isEqualTo(new byte[] {0, 0, 0, 0});
    assertThat(after.getMessageType()).isEqualTo((byte) MessageType.INTEL_IAGENT.valueOf());
    assertThat(after.getFlexHeaderValue("k1")).isEqualTo("key_1");
    assertThat(after.getFlexHeaderValue("k2")).isEqualTo(100);
  }

  private ILinkMessage decodeEncoded(ILinkMessage source) {
    ILinkEncoder encoder = new ILinkEncoder();
    ByteBuf out = Unpooled.directBuffer();
    out.clear();

    ReflectionTestUtils.invokeMethod(encoder, "encode", null, source, out);

    ILinkDecoder decoder = new ILinkDecoder();
    List<ILinkMessage> messageList = new LinkedList<>();
    ReflectionTestUtils.invokeMethod(decoder, "decode", null, out, messageList);

    assertThat(messageList).asList().isNotEmpty();

    out.release();
    return messageList.get(0);
  }

  @Test
  public void testCBOR() throws Exception {
    CBORFactory factory = new CBORFactory();
    ObjectMapper mapper = new ObjectMapper(factory);
    Map<String, Object> map;

    Resource cborFile = new ClassPathResource("cbor.data");
    map = mapper.readValue(cborFile.getInputStream(), Map.class);
    System.out.println("map cbor.data = " + map);
    assertThat(map.get("key1")).isEqualTo("hello");
    assertThat(map.get("key2")).isEqualTo(100);

    byte[] fileContent = new byte[(int) cborFile.contentLength()];
    cborFile.getInputStream().read(fileContent);
    StringBuilder builder = new StringBuilder();
    for (byte b : fileContent) {
      builder.append(String.format("0x%h ", b));
    }
    System.out.println(builder.toString());
  }
}
