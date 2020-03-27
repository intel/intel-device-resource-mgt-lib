/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker.utils;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.help.MessageIdMaker;
import com.openiot.cloud.base.ilink.ILinkMessage;
import com.openiot.cloud.base.ilink.LeadingByte;
import static com.openiot.cloud.base.ilink.MessageType.COAP_OVER_TCP;
import static com.openiot.cloud.base.ilink.MessageType.INTEL_IAGENT;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.iotivity.cloud.base.protocols.coap.CoapEncoder;
import org.iotivity.cloud.base.protocols.coap.CoapMessage;
import org.springframework.util.ReflectionUtils;
import java.lang.reflect.Method;

public class ILinkMessageBuilder {

  // handshake is a plain message which doesn't have messageId
  public static ILinkMessage createHandShake1Request(String gatewayId) {
    return new ILinkMessage(LeadingByte.PLAIN.valueOf(),
                            (byte) INTEL_IAGENT.valueOf()).setAgentId(gatewayId)
                                                          .setTag(ConstDef.FH_V_HAN1);
  }

  static ILinkMessage createHandShake1Response() {
    return null;
  }

  // handshake is a plain message which doesn't have messageId
  public static ILinkMessage createHandShake2Request(String gatewayId, byte[] payload) {
    return new ILinkMessage(LeadingByte.PLAIN.valueOf(),
                            (byte) INTEL_IAGENT.valueOf()).setAgentId(gatewayId)
                                                          .setTag(ConstDef.FH_V_HAN2)
                                                          .setPayload(payload);
  }

  static ILinkMessage createHandShake2Response() {
    return null;
  }

  static ILinkMessage createPingRequest() {
    return null;
  }

  static ILinkMessage createPingResponse() {
    return null;
  }

  public static ILinkMessage createCOAPRequest(String deviceId, int messageId,
                                               CoapMessage payload) {
    Method encodeMethod = ReflectionUtils.findMethod(CoapEncoder.class,
                                                     "encode",
                                                     ChannelHandlerContext.class,
                                                     CoapMessage.class,
                                                     ByteBuf.class);
    ReflectionUtils.makeAccessible(encodeMethod);

    ByteBuf out = Unpooled.buffer();
    CoapEncoder encoder = new CoapEncoder();

    // call encoder
    ReflectionUtils.invokeMethod(encodeMethod, encoder, null, payload, out);

    // copy binary content
    byte[] coapMessageBinary = new byte[out.readableBytes()];
    out.getBytes(out.readerIndex(), coapMessageBinary);
    out.release();

    return new ILinkMessage(LeadingByte.REQUEST.valueOf(),
                            (byte) COAP_OVER_TCP.valueOf()).setAgentId(deviceId)
                                                           .setIlinkMessageId(MessageIdMaker.IntegerToBytes(messageId))
                                                           .setPayload(coapMessageBinary);
  }

  public static ILinkMessage createCOAPResponse(ILinkMessage request) {
    return null;
  }

  public static ILinkMessage createResponse(ILinkMessage request, int responseCode) {
    ILinkMessage response =
        new ILinkMessage(LeadingByte.RESPONSE.valueOf(), request.getMessageType());
    response.setAgentId(request.getAgentId());
    response.setIlinkMessageId(request.getIlinkMessageId());
    response.setResponseCode(responseCode);
    return response;
  }

  public static CoapMessage decodeAsCOAPMessage(byte[] data) {
    return null;
  }

  public static byte[] encodeAsCOAPMessage(CoapMessage coapMessage) {
    return null;
  }
}
