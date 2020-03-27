/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker.base.server;

import com.openiot.cloud.base.ilink.ILinkDecoder;
import com.openiot.cloud.base.ilink.ILinkEncoder;
import io.netty.channel.ChannelHandler;
import org.iotivity.cloud.base.server.Server;
import java.net.InetSocketAddress;

public class ILinkServer extends Server {

  public ILinkServer(InetSocketAddress inetSocketAddress) {
    super(inetSocketAddress);
  }

  @Override
  protected ChannelHandler[] onQueryDefaultHandler() {
    return new ChannelHandler[] {new ILinkDecoder(), new ILinkEncoder()};
  }
}
