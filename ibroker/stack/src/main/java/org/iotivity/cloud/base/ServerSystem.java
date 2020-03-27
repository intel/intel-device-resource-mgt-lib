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
package org.iotivity.cloud.base;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.iotivity.cloud.base.connector.CoapClient;
import org.iotivity.cloud.base.device.CoapDevice;
import org.iotivity.cloud.base.device.Device;
import org.iotivity.cloud.base.device.IRequestChannel;
import org.iotivity.cloud.base.exception.ClientException;
import org.iotivity.cloud.base.exception.ServerException;
import org.iotivity.cloud.base.exception.ServerException.InternalServerErrorException;
import org.iotivity.cloud.base.protocols.MessageBuilder;
import org.iotivity.cloud.base.protocols.coap.CoapMessage;
import org.iotivity.cloud.base.protocols.coap.CoapRequest;
import org.iotivity.cloud.base.protocols.coap.CoapResponse;
import org.iotivity.cloud.base.protocols.enums.ResponseStatus;
import org.iotivity.cloud.base.resource.ResourceManager;
import org.iotivity.cloud.base.server.CoapServer;
import org.iotivity.cloud.base.server.HttpServer;
import org.iotivity.cloud.base.server.Server;
import org.iotivity.cloud.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class ServerSystem extends ResourceManager {
  private List<Server> mServerList = new ArrayList<>();
  protected static AttributeKey<Device> keyDevice = AttributeKey.newInstance("device");

  @Sharable
  public class PersistentPacketReceiver extends SimpleChannelInboundHandler<CoapMessage> {
    @Override
    public void channelActive(ChannelHandlerContext ctx) {

      StringBuilder deviceId = new StringBuilder(ctx.channel().id().asLongText().substring(26));
      deviceId.deleteCharAt(25);
      deviceId.insert(13, '-');
      deviceId.insert(18, '-');
      deviceId.insert(23, '-');
      CoapDevice device = new CoapDevice(ctx);
      device.updateDevice(deviceId.toString(), null, null);
      ctx.channel().attr(keyDevice).set(device);

      device.onConnected();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CoapMessage msg) {
      try {
        // Find proper device and raise event.
        Device targetDevice = ctx.channel().attr(keyDevice).get();

        if (targetDevice == null) {
          throw new InternalServerErrorException("Unable to find device");
        }

        if (msg instanceof CoapRequest) {
          onRequestReceived(targetDevice, (CoapRequest) msg);
        } else if (msg instanceof CoapResponse) {
          // TODO: Re-architecturing required
          IRequestChannel reqChannel = ((CoapDevice) targetDevice).getRequestChannel();
          CoapClient coapClient = (CoapClient) reqChannel;
          coapClient.onResponseReceived(msg);
        }
      } catch (ServerException e) {
        ctx.writeAndFlush(MessageBuilder.createResponse(msg, e.getErrorResponse()));
        Log.f(ctx.channel(), e);
      } catch (ClientException e) {
        Log.f(ctx.channel(), e);
      } catch (Throwable t) {
        Log.f(ctx.channel(), t);
        if (msg instanceof CoapRequest) {
          ctx.writeAndFlush(
              MessageBuilder.createResponse(msg, ResponseStatus.INTERNAL_SERVER_ERROR));
        }
      } finally {
        ReferenceCountUtil.release(msg);
      }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      Device device = ctx.channel().attr(keyDevice).get();
      device.onDisconnected();
      ctx.channel().attr(keyDevice).remove();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      Log.e(cause.getMessage());
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw, true);
      cause.printStackTrace(pw);
      Log.e(sw.toString());
      ctx.channel().disconnect();
      ctx.channel().close();
    }
  }

  @Sharable
  public class NonPersistentPacketReceiver extends ChannelDuplexHandler {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      // onDeviceConnected(ctx)
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      // Find proper device and raise event.
      // onRequestReceived(new Device(ctx), msg);
    }
  }

  public void addServer(Server server) {
    if (server instanceof CoapServer) {
      server.addHandler(new PersistentPacketReceiver());
    }

    if (server instanceof HttpServer) {
      server.addHandler(new NonPersistentPacketReceiver());
    }
    mServerList.add(server);
  }

  public void startSystem(boolean tlsMode) throws Exception {
    for (Server server : mServerList) {
      server.startServer(tlsMode);
    }
  }

  public void stopSystem() throws Exception {
    for (Server server : mServerList) {
      server.stopServer();
    }
  }
}
