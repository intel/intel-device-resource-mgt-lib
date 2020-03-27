/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker.base.connector;

import com.openiot.cloud.base.help.ConcurrentRequestMap;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.help.MessageIdMaker;
import com.openiot.cloud.base.ilink.ILinkMessage;
import com.openiot.cloud.base.ilink.LeadingByte;
import com.openiot.cloud.ibroker.utils.ILinkMessageBuilder;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Random;
import java.util.function.Function;

/**
 * We create a {@code ILinkDevice} for each {@code Channel} when {@Code channelActive()}. Each
 * {@code ILinkDevice} has one {@code ILinkClient}. So every connection with a iLink device has one
 * {@code ILinkClient} to send numbers of {@code ILinkMessage}.
 *
 * <p>
 */
public class ILinkClient {
  private static final Logger logger = LoggerFactory.getLogger(ILinkClient.class);
  private Channel channel = null;
  private Random r = new Random(379);

  /** Use ilink message id. It is compatible with the default compactor. */
  private ConcurrentRequestMap<Integer, RequestContext> requestMap =
      new ConcurrentRequestMap<>(5120, 5, 30, null);

  public ILinkClient(Channel channel) {
    this.channel = channel;
  }

  public void onDisconnected() {
    requestMap.clear();
  }

  class RequestContext {
    Function<ILinkMessage, Void> responseHandler;

    public RequestContext(Function<ILinkMessage, Void> responseHandler) {
      this.responseHandler = responseHandler;
    }
  }

  public void onDefaultResponseReceived(ILinkMessage response) {
    int key = MessageIdMaker.bytesToInteger(response.getIlinkMessageId());
    RequestContext rc = requestMap.remove(key);

    if (rc == null) {
      logger.error("Unable to find " + key);
      return;
    }

    rc.responseHandler.apply(response);
  }

  public void sendMessage(ILinkMessage message, Function<ILinkMessage, Void> responseHandle) {
    if (channel == null) {
      responseHandle.apply(ILinkMessageBuilder.createResponse(message, ConstDef.FH_V_FAIL));
      return;
    }

    if (!channel.isOpen() || !channel.isActive() || !channel.isRegistered()
        || !channel.isWritable()) {
      logger.warn(String.format("channel %s is not in a normal state open(%s),active(%s),writable(%s),registered(%s)",
                                channel.id().asShortText(),
                                channel.isOpen(),
                                channel.isActive(),
                                channel.isWritable(),
                                channel.isRegistered()));
      responseHandle.apply(ILinkMessageBuilder.createResponse(message, ConstDef.FH_V_FAIL));
      return;
    }

    // store the request context for the REQUEST ilink message
    if (responseHandle != null && message.getLeadingByte() == LeadingByte.REQUEST.valueOf()) {
      int key = MessageIdMaker.bytesToInteger(message.getIlinkMessageId());
      RequestContext rc = new RequestContext(responseHandle);
      if (requestMap.containsKey(key)) {
        logger.warn(String.format("Request %l has already in requstMeap!!!", key));
        responseHandle.apply(ILinkMessageBuilder.createResponse(message, ConstDef.FH_V_FAIL));
        return;
      }
      requestMap.put(key, rc);
    }

    channel.writeAndFlush(message).addListener(future -> {
      if (!future.isSuccess()) {
        // requestMap might be too huge to looking for even it is O(logn) for search.
        if (responseHandle != null && message.getLeadingByte() == LeadingByte.REQUEST.valueOf()) {
          int key = MessageIdMaker.bytesToInteger(message.getIlinkMessageId());
          requestMap.remove(key);
          responseHandle.apply(ILinkMessageBuilder.createResponse(message, ConstDef.FH_V_FAIL));
        }
      }
    });
  }

  public void disconnect() {
    channel.disconnect().awaitUninterruptibly();;
  }

  @Override
  public String toString() {
    return "ILinkClient [channel=" + channel.id().asShortText() + "]";
  }
}
