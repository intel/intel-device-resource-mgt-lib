/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */


/** */
package com.openiot.cloud.projectcenter.server;

import com.openiot.cloud.base.ilink.ILinkDecoder;
import com.openiot.cloud.base.ilink.ILinkEncoder;
import com.openiot.cloud.sdk.service.ApplicationContextProvider;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import org.springframework.stereotype.Component;
import javax.net.ssl.SSLEngine;

/** @author sunny */
@Component
public class SecureSocketServerILinkMessageInitializer extends ChannelInitializer<SocketChannel> {

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();

    // Add SSL handler first to encrypt and decrypt everything.
    // In this example, we use a bogus certificate in the server side
    // and accept any invalid certificates in the client side.
    // You will need something more complicated to identify both
    // and server in the real world.
    //
    // Read SecureChatSslContextFactory
    // if you need client certificate authentication.

    SSLEngine engine = SecureSocketSslContextFactory.getServerContext().createSSLEngine();
    engine.setUseClientMode(false);
    pipeline.addLast("ssl", new SslHandler(engine));
    pipeline.addLast("decoder", new ILinkDecoder());
    pipeline.addLast("encoder", new ILinkEncoder());
    // and then business logic.
    pipeline.addLast("handler",
                     ApplicationContextProvider.getBean(SecureSocketServerHandler.class));
  }
}
