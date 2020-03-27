/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker.base.protocols.ilink;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.MessageIdMaker;
import com.openiot.cloud.base.ilink.ILinkMessage;
import com.openiot.cloud.base.ilink.LeadingByte;
import com.openiot.cloud.ibroker.base.device.IAgent;
import com.openiot.cloud.ibroker.mq.DefaultJmsHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.iotivity.cloud.base.connector.ConnectorPool;
import org.iotivity.cloud.base.device.Device;
import org.iotivity.cloud.base.protocols.coap.CoapDecoder;
import org.iotivity.cloud.base.protocols.coap.CoapEncoder;
import org.iotivity.cloud.base.protocols.coap.CoapMessage;
import org.iotivity.cloud.base.protocols.enums.Observe;
import org.iotivity.cloud.base.resource.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class ILinkCoapOverTcpMessageHandler {
  private static final Logger logger =
      LoggerFactory.getLogger(ILinkCoapOverTcpMessageHandler.class);

  @Autowired
  private ResourceManager resourcesForCoapOverTcp;
  @Autowired
  private DefaultJmsHandler jmsHandler;
  @Autowired
  public final static byte[] COAP_MESSAGE_ETAG = new byte[] {0x70, 0x07, 0x70, 0x07};

  /** ILinkMessage.payload -> CoapMessage ILinkMessage.ilinkMessageId -> CoapMessage.token */
  public void onMessage(Device srcDevice, ILinkMessage request) {
    if (((IAgent) srcDevice).getSessionFlag() != true) {
      logger.warn("iAgent-iBroker session has not been created yet!");
      // no response
      return;
    }

    if (request.getPayload() == null || request.getPayloadSize() == 0) {
      logger.error(String.format("%s is an invalid COAP_OVER_TCP ILINK message", this));
      // no response
      return;
    }

    if (srcDevice.getDeviceId() == null) {
      logger.warn(String.format("device %s @ channel %s doesn't have the device id",
                                srcDevice,
                                ((IAgent) srcDevice).getRequestChannel()));
      return;
    }

    if (request.getLeadingByte() == LeadingByte.RESPONSE.valueOf()) {
      (((IAgent) srcDevice).getRequestChannel()).onDefaultResponseReceived(request);
      return;
    }

    CompletableFuture.supplyAsync(() -> decodeAsCoapMessage(request.getPayload()))
                     .thenApplyAsync(coapMessage -> Optional.ofNullable(coapMessage)
                                                            .map(sameCoapMessage -> processCoapMessage(request,
                                                                                                       sameCoapMessage))
                                                            .orElse(null))
                     .thenAcceptAsync(coapMessage -> Optional.ofNullable(coapMessage)
                                                             .ifPresent(sameCoapMessage -> dispatchCoapRequest(request,
                                                                                                               srcDevice,
                                                                                                               sameCoapMessage)));
  }

  private CoapMessage processCoapMessage(ILinkMessage request, CoapMessage orig) {
    // change token, remove observe
    byte[] newToken = frameToken(request.getIlinkMessageId(), orig.getToken());
    if (newToken == null) {
      // no response
      return null;
    }
    orig.setToken(newToken);
    // always without observe
    orig.setObserve(Observe.NOTHING);

    // if it is a ILINK_PLAIN, mark the COAP message with a eTag which means it doesn't need a
    // response. next request handlers will check the etag and decide who is going to have a
    // response handler
    if (LeadingByte.PLAIN.equals(request.getLeadingByte())) {
      orig.addOption(4, COAP_MESSAGE_ETAG);
    }

    // add timestamp
    Optional.ofNullable(request.getTimeStamp())
            .filter(ts -> !ts.isEmpty())
            .ifPresent(ts -> orig.setUriQuery("time=" + ts));
    return orig;
  }

  private void dispatchCoapRequest(ILinkMessage request, Device srcDevice, CoapMessage cm) {
    boolean dispatchToJms =
        Optional.ofNullable(cm.getUriPath())
                .map(uripath -> ConnectorPool.getConnectionWithMinMatch(uripath))
                .map(channel -> false)
                .orElse(true);
    logger.debug(String.format("dispatch %s to the %s",
                               cm,
                               dispatchToJms ? "message queue" : "COAP+TCP service"));
    if (!dispatchToJms) {
      resourcesForCoapOverTcp.onRequestReceived(srcDevice, cm);
    } else {
      jmsHandler.onDefaultRequestReceived(srcDevice, cm, request);
    }
  }

  /** tempory token = ilink message id + original token */
  public static byte[] frameToken(byte[] ilinkMessageId, byte[] origToken) {
    if (ilinkMessageId.length != 4 || origToken.length == 0) {
      logger.warn(String.format("an invalide ilink message id(%s) or token(%s)",
                                ilinkMessageId.length != 4,
                                origToken.length == 0));
      return null;
    }

    byte[] token = new byte[ilinkMessageId.length + origToken.length];

    // ilink message id first 4 bytes
    int i = 0;
    for (; i < ilinkMessageId.length && i < token.length; i++) {
      token[i] = ilinkMessageId[i];
    }

    // original token last 4 or 8 bytes
    for (int j = 0; j < origToken.length && i < token.length; j++, i++) {
      token[i] = origToken[j];
    }

    logger.debug(String.format("--> [token] aft frame %s, %s -> %s",
                               Arrays.toString(ilinkMessageId),
                               Arrays.toString(origToken),
                               Arrays.toString(token)));

    return token;
  }

  public static ILinkMessage restoreMessageIDAndToken(byte[] token, CoapMessage cm,
                                                      ILinkMessage ilink) {
    if (token == null || token.length == 0 || token.length < 8) {
      return null;
    }

    // ilink message id is first 4 bytes
    byte[] ilinkMessageId = new byte[4];
    for (int j = 0; j < 4; j++) {
      ilinkMessageId[j] = token[j];
    }

    // original token is next 4 or 8 bytes
    byte[] origToken = new byte[token.length - 4];
    int i = 4;
    int j = 0;
    for (; j < origToken.length && i < token.length; j++, i++) {
      origToken[j] = token[i];
    }
    cm.setToken(origToken);

    logger.debug(String.format("--> [token] aft restore %s -> %s, %s",
                               Arrays.toString(token),
                               Arrays.toString(ilinkMessageId),
                               Arrays.toString(origToken)));

    ilink.setIlinkMessageId(ilinkMessageId);
    return ilink;
  }

  public static int peekILinkMessageId(byte[] token) {
    if (token.length == 0) {
      logger.warn("--> [token] an empty token");
      return 0;
    }

    byte[] messageId = Arrays.copyOfRange(token, 0, 4);
    return MessageIdMaker.bytesToInteger(messageId);
  }

  public static ILinkMessage encodeCoapMessageAsPayload(CoapMessage cm, ILinkMessage ilink) {
    ByteBuf bb = Unpooled.buffer();
    Method encode = ReflectionUtils.findMethod(CoapEncoder.class,
                                               "encode",
                                               ChannelHandlerContext.class,
                                               CoapMessage.class,
                                               ByteBuf.class);
    ReflectionUtils.makeAccessible(encode);
    CoapEncoder ce = new CoapEncoder();
    ReflectionUtils.invokeMethod(encode, ce, null, cm, bb);

    byte[] payload = new byte[bb.readableBytes()];
    bb.readBytes(payload);
    bb.release();

    return ilink.setPayload(payload);
  }

  public static CoapMessage decodeAsCoapMessage(byte[] payload) {

    try {
      ByteBuf bb = Unpooled.copiedBuffer(payload);
      List<CoapMessage> out = new ArrayList<>();

      Method decode = ReflectionUtils.findMethod(CoapDecoder.class,
                                                 "decode",
                                                 ChannelHandlerContext.class,
                                                 ByteBuf.class,
                                                 List.class);
      ReflectionUtils.makeAccessible(decode);
      CoapDecoder cd = new CoapDecoder();
      ReflectionUtils.invokeMethod(decode, cd, null, bb, out);
      bb.release();

      return out.get(0);
    } catch (Exception e) {
      logger.warn(BaseUtil.getStackTrace(e));
      return null;
    }
  }
}
