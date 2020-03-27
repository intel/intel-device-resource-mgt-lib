/*
* Copyright (C) 2020 Intel Corporation. All rights reserved.
* SPDX-License-Identifier: Apache-2.0
*/

/*
 * //******************************************************************
 * //
 * // Copyright 2016 Samsung Electronics All Rights Reserved.
 * //
 * //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * //
 * // Licensed under the Apache License, Version 2.0 (the "License");
 * // you may not use this file except in compliance with the License.
 * // You may obtain a copy of the License at
 * //
 * //      http://www.apache.org/licenses/LICENSE-2.0
 * //
 * // Unless required by applicable law or agreed to in writing, software
 * // distributed under the License is distributed on an "AS IS" BASIS,
 * // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * // See the License for the specific language governing permissions and
 * // limitations under the License.
 * //
 * //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package org.iotivity.cloud.base.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.iotivity.cloud.base.OICConstants;
import org.iotivity.cloud.util.Log;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public abstract class Server {

  EventLoopGroup acceptorGroup = new NioEventLoopGroup();

  // [WATERMARK] each iLink server supports 50 gateways
  EventLoopGroup workerGroup =
      new NioEventLoopGroup(Math.max(Runtime.getRuntime().availableProcessors() << 3, 50));

  ServerInitializer mServerInitializer = new ServerInitializer();

  InetSocketAddress mInetSocketAddress = null;

  SslContext mSslContext = null;

  private class ServerInitializer extends ChannelInitializer<SocketChannel> {
    private List<ChannelHandler> additionalHandlers = new ArrayList<>();

    public ServerInitializer() {}

    public void addHandler(ChannelHandler handler) {
      additionalHandlers.add(handler);
    }

    @Override
    public void initChannel(SocketChannel ch) {
      ChannelPipeline p = ch.pipeline();

      if (mSslContext != null) {
        p.addLast(mSslContext.newHandler(ch.alloc()));
      }

      p.addLast(onQueryDefaultHandler());

      for (ChannelHandler handler : additionalHandlers) {
        p.addLast(handler);
      }
    }
  }

  public Server(InetSocketAddress inetSocketAddress) {
    mInetSocketAddress = inetSocketAddress;
  }

  public void startServer(boolean tlsMode) throws Exception {
    try {
      if (tlsMode) Log.i("Server starts with TLS!");

      if (tlsMode == true) {
        File serverCert = new File(OICConstants.CLOUD_CERT_FILE);
        File serverKey = new File(OICConstants.CLOUD_KEY_FILE);
        mSslContext = SslContextBuilder.forServer(serverCert, serverKey).build();
      }

      ServerBootstrap b = new ServerBootstrap();
      b.group(acceptorGroup, workerGroup);
      b.channel(NioServerSocketChannel.class);
      b.handler(new LoggingHandler(LogLevel.DEBUG));
      b.option(ChannelOption.TCP_NODELAY, true);
      b.option(ChannelOption.SO_KEEPALIVE, true);
      // [WATERMARK] iLink servers and coap servers are sharing this option
      // [WATERMARK] assume each iLink server is able to support 50 gateways
      // [WATERMARK] every gateway will send messages from their devices every 5 second
      // [WATERMARK] each gateway would have 10 devices or more specifically 1000 endpoints
      // [WATERMARK] it means each iLink server should support 10,000 message / second at least
      // [WATERMARK] if the average message size is 128 bytes, and consider every message is able to
      // [WATERMARK] stay 20s(timeout), its highest water mark is 25M (10,000 * 128 * 20)
      // [WATERMARK] assume each message is 128 Bytes, high water mark is 1M messages, and low water
      // mark is 75% of high value
      final int oneMega = 1024 * 1024;
      final int lowWaterMark = 35 * oneMega;
      final int highWaterMark = 50 * oneMega;
      b.option(
          ChannelOption.WRITE_BUFFER_WATER_MARK,
          new WriteBufferWaterMark(lowWaterMark, highWaterMark));

      b.childHandler(mServerInitializer);
      b.bind(mInetSocketAddress);
    } catch (Exception e) {
      Log.e("Server exception " + e);
      stopServer();
    }
  }

  public void stopServer() throws Exception {
    acceptorGroup.shutdownGracefully();
    workerGroup.shutdownGracefully();
  }

  public void addHandler(ChannelHandler handler) {
    mServerInitializer.addHandler(handler);
  }

  protected abstract ChannelHandler[] onQueryDefaultHandler();
}
