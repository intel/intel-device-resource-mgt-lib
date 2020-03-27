/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SecureSocketServer {
  private static final Logger logger = LoggerFactory.getLogger(SecureSocketServer.class.getName());

  @Autowired
  private SecureSocketServerILinkMessageInitializer initializer;

  private final int port = 1804;

  public void run() throws InterruptedException {
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap b = new ServerBootstrap();
      b.group(bossGroup, workerGroup)
       .channel(NioServerSocketChannel.class)
       .childHandler(initializer);
      logger.info("listening on port " + port);
      b.bind(port).sync().channel().closeFuture().sync();
    } catch (Exception e) {
      logger.info("meet an exception ", e);
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
      logger.info("shutdown listening on port " + port);
    }
  }
}
