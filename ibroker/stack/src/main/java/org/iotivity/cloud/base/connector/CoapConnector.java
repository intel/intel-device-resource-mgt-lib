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
package org.iotivity.cloud.base.connector;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import org.iotivity.cloud.base.OICConstants;
import org.iotivity.cloud.base.exception.ClientException;
import org.iotivity.cloud.base.protocols.coap.CoapDecoder;
import org.iotivity.cloud.base.protocols.coap.CoapEncoder;
import org.iotivity.cloud.base.protocols.coap.CoapLogHandler;
import org.iotivity.cloud.base.protocols.coap.CoapResponse;
import org.iotivity.cloud.util.Log;
import javax.net.ssl.SSLException;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class CoapConnector {

  public CoapConnector() {

    mBootstrap.group(mConnectorGroup);
    mBootstrap.channel(NioSocketChannel.class);
    mBootstrap.option(ChannelOption.TCP_NODELAY, true);
    mBootstrap.option(ChannelOption.SO_KEEPALIVE, true);
    mBootstrap.option(ChannelOption.SO_REUSEADDR, true);
  }

  @Sharable
  private class CoapPacketHandler extends SimpleChannelInboundHandler<CoapResponse> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CoapResponse msg) throws Exception {
      if (!mChannelMap.containsKey(ctx.channel())) {
        Log.w(
            "--> CoapConnector can not locate response handler from "
                + ctx.channel().remoteAddress());
        return;
      }

      Optional.ofNullable(mChannelMap.get(ctx.channel()))
          .ifPresent(
              coapClient -> {
                try {
                  coapClient.onResponseReceived(msg);
                } catch (ClientException e) {
                  e.printStackTrace();
                  Log.w("--> dispatch msg to coapClient and fail " + e.getLocalizedMessage());
                }
              });
      ReferenceCountUtil.release(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      Log.e("--> exceptionCaught @ " + ctx.channel().remoteAddress() + " " + cause.getCause());
    }
  }

  public static class KeepAliveHandler extends ChannelDuplexHandler {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
      if (evt instanceof IdleStateEvent) {
        IdleStateEvent event = (IdleStateEvent) evt;
        if (event.state() == IdleState.WRITER_IDLE) {
          Log.d("Connection with" + ctx.channel().remoteAddress().toString() + " is writer idle");
        }
        if (event.state() == IdleState.READER_IDLE) {
          Log.d("Connection with" + ctx.channel().remoteAddress().toString() + " is reader idle");
          // ctx.close();
        }
      }
    }
  }

  public static class CoapConnectorInitializer extends ChannelInitializer<SocketChannel> {

    private List<ChannelHandler> additionalHandlers = new ArrayList<>();

    private Boolean mTlsMode = false;
    private Boolean mKeepAlive = false;
    InetSocketAddress mInetSocketAddress = null;
    String mRootCertFiePath = null;

    public void setTlsMode(Boolean tlsMode) {
      this.mTlsMode = tlsMode;
    }

    public void setKeepAlive(Boolean keepAlive) {
      this.mKeepAlive = keepAlive;
    }

    public void setInetSocketAddress(InetSocketAddress address) {
      this.mInetSocketAddress = address;
    }

    public void setRootCertFilePath(String path) {
      this.mRootCertFiePath = path;
    }

    public void addHandler(ChannelHandler handler) {
      additionalHandlers.add(handler);
    }

    @Override
    public void initChannel(SocketChannel ch) {
      ChannelPipeline p = ch.pipeline();

      SslContext sslContext = null;

      if (mTlsMode.equals(true)) {

        File rootCert = new File(mRootCertFiePath);

        try {
          sslContext =
              SslContextBuilder.forClient()
                  .sslProvider(SslProvider.JDK)
                  .trustManager(rootCert)
                  .build();
        } catch (SSLException e) {
          e.printStackTrace();
        }

        final SslContext sslCtx = sslContext;
        p.addLast(
            sslCtx.newHandler(
                ch.alloc(), mInetSocketAddress.getHostString(), mInetSocketAddress.getPort()));
      }

      p.addLast(new CoapDecoder());
      p.addLast(new CoapEncoder());
      p.addLast(new CoapLogHandler());

      //            if (mKeepAlive.equals(true)) {
      //                p.addLast(new IdleStateHandler(100, 45, 0));
      //                p.addLast(new KeepAliveHandler());
      //            }

      for (ChannelHandler handler : additionalHandlers) {
        p.addLast(handler);
      }
    }
  }

  ConcurrentHashMap<Channel, CoapClient> mChannelMap = new ConcurrentHashMap<>();
  Bootstrap mBootstrap = new Bootstrap();
  EventLoopGroup mConnectorGroup =
      new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() << 2);
  AtomicBoolean mStop = new AtomicBoolean(false);

  public void connect(
      final String connectionName,
      final InetSocketAddress inetSocketAddress,
      boolean tlsMode,
      boolean keepAlive) {

    CoapConnectorInitializer initializer = new CoapConnectorInitializer();

    if (tlsMode == true) {
      initializer.setTlsMode(true);
      initializer.setInetSocketAddress(inetSocketAddress);
      initializer.setRootCertFilePath(OICConstants.ROOT_CERT_FILE);
    }

    initializer.setKeepAlive(keepAlive);
    initializer.addHandler(new CoapPacketHandler());
    mBootstrap.handler(initializer);
    mStop.set(false);
    doConnect(connectionName, inetSocketAddress, tlsMode);
  }

  private void doConnect(
      final String connectionName,
      final InetSocketAddress inetSocketAddress,
      final boolean tlsMode) {
    if (mStop.get()) return;

    mBootstrap
        .connect(inetSocketAddress)
        .addListener(
            new ChannelFutureListener() {
              @Override
              public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                  Log.w(
                      "Connection to "
                          + inetSocketAddress.getHostString()
                          + ":"
                          + inetSocketAddress.getPort()
                          + " was not successful. Retrying...");
                  future.channel().close();
                  if (!mStop.get()) {
                    scheduleConnect(connectionName, inetSocketAddress, tlsMode, 5000);
                  }
                } else {
                  connectionEstablished(connectionName, future.channel());
                  addCloseDetectListener(future.channel());
                }
              }

              private void addCloseDetectListener(Channel channel) {
                channel
                    .closeFuture()
                    .addListener(
                        (ChannelFutureListener)
                            future -> {
                              mChannelMap.remove(channel);
                              if (mStop.get()) return;
                              Log.w(
                                  "Connection to "
                                      + inetSocketAddress.getHostString()
                                      + ":"
                                      + inetSocketAddress.getPort()
                                      + " was lost. Retrying...");
                              scheduleConnect(connectionName, inetSocketAddress, tlsMode, 5);
                            });
              }
            });
  }

  private void scheduleConnect(
      String connectionName, InetSocketAddress inetSocketAddress, boolean tlsMode, long millis) {
    if (mStop.get()) return;

    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Log.w("while scheduleConnect", e);
    } finally {
      if (!mStop.get()) {
        Log.i(
            "to reconnect to "
                + inetSocketAddress.getHostString()
                + ":"
                + inetSocketAddress.getPort());
        Executors.newSingleThreadExecutor()
            .submit(() -> doConnect(connectionName, inetSocketAddress, tlsMode));
      }
    }
  }

  public void connectionEstablished(String connectionName, Channel channel) {
    CoapClient coapClient = new CoapClient(channel);
    mChannelMap.put(channel, coapClient);
    ConnectorPool.addConnection(connectionName, coapClient);
  }

  public void disconnect() throws Exception {
    Log.i("to disconnect");
    mStop.set(true);
    mChannelMap.keySet().forEach(ch -> ch.close().awaitUninterruptibly());
  }

  public void shutdown() {
    Log.i("to shutdown");
    mConnectorGroup.shutdownGracefully().awaitUninterruptibly();
  }
}
