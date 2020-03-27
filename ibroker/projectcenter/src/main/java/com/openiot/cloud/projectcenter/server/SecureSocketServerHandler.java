/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.server;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.ilink.ILinkMessage;
import com.openiot.cloud.base.ilink.MessageType;
import com.openiot.cloud.projectcenter.controller.ssl.ProvisionSslHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class SecureSocketServerHandler extends SimpleChannelInboundHandler<ILinkMessage> {

  static final Logger LOG = LoggerFactory.getLogger(SecureSocketServerHandler.class);

  @Autowired
  private ProvisionSslHandler provisionSslHandler;

  @Override
  public boolean acceptInboundMessage(Object msg) throws Exception {
    LOG.debug("acceptInboundMessage:" + msg.getClass().getName());
    return super.acceptInboundMessage(msg);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    LOG.debug("channelActive:");
    super.channelActive(ctx);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    LOG.debug("channelInactive:");
    super.channelInactive(ctx);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    LOG.debug("channelReadComplete:");
    super.channelReadComplete(ctx);
  }

  @Override
  public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
    LOG.debug("channelRegistered:");
    super.channelRegistered(ctx);
  }

  @Override
  public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
    LOG.debug("channelUnregistered:");
    super.channelUnregistered(ctx);
  }

  @Override
  public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
    LOG.debug("channelWritabilityChanged:");
    super.channelWritabilityChanged(ctx);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    LOG.debug("userEventTriggered:");
    super.userEventTriggered(ctx, evt);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    LOG.debug("Unexpected exception from downstream.", cause);
    ctx.close();
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ILinkMessage request) throws Exception {
    LOG.info(this + " :channelRead0");
    LOG.info(String.format("Request message:/n %s/n", request.toString()));
    ILinkMessage response = null;
    MessageType mt = MessageType.fromValue(request.getMessageType());
    if (mt != null) {
      switch (mt) {
        case INTEL_IAGENT:
          String tag = request.getTag();
          if (tag != null && tag.equals(ConstDef.FH_V_PRO)) {
            response = provisionSslHandler.onMessage(request);
            LOG.info(String.format("Response message:/n %s/n", response.toString()));
            ctx.writeAndFlush(response);
          }
          break;
        default:
          LOG.error(String.format("Unsupported message type: %s", mt.valueOf()));
          break;
      }
    }
  }
}
