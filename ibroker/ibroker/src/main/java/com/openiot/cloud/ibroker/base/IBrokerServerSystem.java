/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker.base;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.ilink.ILinkMessage;
import com.openiot.cloud.base.ilink.MessageType;
import com.openiot.cloud.ibroker.base.device.IAgent;
import com.openiot.cloud.ibroker.base.device.IAgentCache;
import com.openiot.cloud.ibroker.base.protocols.ilink.ILinkCoapOverTcpMessageHandler;
import com.openiot.cloud.ibroker.base.protocols.ilink.ILinkIntelIAgentMessageHandler;
import com.openiot.cloud.ibroker.base.server.ILinkServer;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.iotivity.cloud.base.device.Device;
import org.iotivity.cloud.base.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class IBrokerServerSystem {
  private static final Logger logger = LoggerFactory.getLogger(IBrokerServerSystem.class);
  protected static AttributeKey<Device> keyDevice = AttributeKey.newInstance("device");
  private List<Server> mServerList = new ArrayList<>();

  @Autowired
  private IAgentCache dc;

  @Autowired
  private ILinkCoapOverTcpMessageHandler ilinkCoapMsgHandler;

  @Autowired
  private ILinkIntelIAgentMessageHandler iagentMsgHandler;

  @Sharable
  public class PersistentPacketReceiver extends SimpleChannelInboundHandler<ILinkMessage> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
      logger.info(String.format("channel #%s active with %s %s",
                                ctx.channel().id().asShortText(),
                                ctx.channel().remoteAddress(),
                                ctx.channel().config().getOptions()));

      // TODO: maybe we should check if the agent is in the agent cache here,
      // to refuse the new connection or to disconnect the old connection
      Device device = new IAgent(ctx);
      device.onConnected();
      ctx.channel().attr(keyDevice).set(device);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
      logger.info(String.format("channel #%s inactive with %s",
                                ctx.channel().id().asShortText(),
                                ctx.channel().remoteAddress()));
      Device device = ctx.channel().attr(keyDevice).get();
      String agentId = ((IAgent) device).getAgentId();
      // in case, there are two or more channels with the same agent
      //
      if (agentId != null && !agentId.isEmpty() && dc.containsKey(agentId)
          && dc.getAgent(agentId)
               .getCtx()
               .channel()
               .id()
               .asShortText()
               .equals(ctx.channel().id().asShortText())) {
        dc.removeAgent(agentId, true);
      }

      device.onDisconnected();
      ctx.channel().attr(keyDevice).set(null);

      ctx.channel().disconnect();
      ctx.channel().close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ILinkMessage msg) {
      if (msg == null) {
        logger.error(String.format("channel #%s receives an invalid msg",
                                   ctx.channel().id().asShortText()));
        return;
      }

      // Find proper device and raise event.
      Device srcDevice = ctx.channel().attr(keyDevice).get();
      if (srcDevice == null) {
        logger.error(String.format("channel #%s unable to find device",
                                   ctx.channel().id().asShortText()));
        ReferenceCountUtil.release(msg);
        return;
      }

      String deviceAgentId = ((IAgent) srcDevice).getAgentId();
      if (msg.getAgentId() != null && deviceAgentId != null
          && !deviceAgentId.equals(msg.getAgentId())) {
        logger.warn(String.format("channel #%s(%s) receives messages %s from another device %s",
                                  ctx.channel().id().asShortText(),
                                  ((IAgent) srcDevice).getAgentId(),
                                  msg,
                                  msg.getAgentId()));
        ctx.channel().disconnect();
        return;
      }

      MessageType mt = MessageType.fromValue(msg.getMessageType());
      if (mt == null) {
        logger.warn("it is not a correct message without message type information " + msg);
        // no response
        ReferenceCountUtil.release(msg);
        return;
      }

      logger.info(String.format("channel #%s receive a msg %s",
                                ctx.channel().id().asShortText(),
                                msg));

      try {
        switch (mt) {
          case COAP_OVER_TCP:
            ilinkCoapMsgHandler.onMessage(srcDevice, msg);
            break;
          case INTEL_IAGENT:
            iagentMsgHandler.onMessage(srcDevice, msg);
            break;
          default:
            logger.error(String.format("Unsupported message type: %s", mt.valueOf()));
            break;
        }
      } catch (Exception e) {
        logger.error(String.format("an exception %s happens during dispatch", e.getMessage()));
      } finally {
        ReferenceCountUtil.release(msg);
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      logger.error(String.format("channel #%s has an exception %s,",
                                 ctx.channel().id().asShortText(),
                                 cause.getLocalizedMessage()));
      logger.error(BaseUtil.getStackTrace(cause));
      ctx.channel().disconnect();
      ctx.channel().close();
    }
  }

  public void addServer(Server server) {
    if (server instanceof ILinkServer) {
      server.addHandler(new PersistentPacketReceiver());
    }

    mServerList.add(server);
  }

  public void startSystem() {
    for (Server server : mServerList) {
      try {
        server.startServer(false);
      } catch (Exception e) {
        logger.warn(BaseUtil.getStackTrace(e));
      }
    }
  }

  public void stopSystem() {
    for (Server server : mServerList) {
      try {
        server.stopServer();
      } catch (Exception e) {
        logger.warn(BaseUtil.getStackTrace(e));
      }
    }
  }
}
