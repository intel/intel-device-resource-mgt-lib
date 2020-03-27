/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker.base.device;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.ilink.ILinkMessage;
import com.openiot.cloud.base.mongo.model.help.ShortSession;
import com.openiot.cloud.ibroker.base.connector.ILinkClient;
import com.openiot.cloud.ibroker.utils.ILinkMessageBuilder;
import io.netty.channel.ChannelHandlerContext;
import org.iotivity.cloud.base.device.Device;
import org.iotivity.cloud.base.protocols.IResponse;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class IAgent extends Device implements Comparable<IAgent> {
  private String agentId = null;
  private ILinkClient ilinkClient = null;
  private volatile boolean sessionFlag = false;

  private ConcurrentMap<String, Boolean> epList = new ConcurrentHashMap<>();

  public IAgent(ChannelHandlerContext ctx) {
    super(ctx);
  }

  @Override
  public boolean equals(Object obj) {
    return this.agentId.compareTo(((IAgent) obj).agentId) == 0;
  }

  /**
   * getDeviceId() and getAgentId() are same except different names getDeviceId() is a requirment
   * from the parent
   */
  @Override
  public String getDeviceId() {
    return agentId;
  }

  public String getAgentId() {
    return agentId;
  }

  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  public boolean getSessionFlag() {
    if (!this.sessionFlag) {
      synchronized ((Object) this.sessionFlag) {
        return sessionFlag;
      }
    }
    return sessionFlag;
  }

  public void setSessionFlag(boolean sessionFlag) {
    synchronized ((Object) this.sessionFlag) {
      this.sessionFlag = sessionFlag;
    }
  }

  public ILinkClient getRequestChannel() {
    if (ilinkClient == null) {
      ilinkClient = new ILinkClient(ctx.channel());
    }

    return ilinkClient;
  }

  @Override
  public int hashCode() {
    return this.agentId.hashCode();
  }

  @Override
  public void onDisconnected() {
    this.setSessionFlag(false);
    if (ilinkClient != null) {
      ilinkClient.onDisconnected();
    }
  }

  public void sendMessage(ILinkMessage message) {
    getRequestChannel().sendMessage(message, null);
  }

  public void sendMessage(ILinkMessage message, Function<ILinkMessage, Void> responseHandle) {
    if (getRequestChannel() != null) {
      getRequestChannel().sendMessage(message, responseHandle);
    } else {
      responseHandle.apply(ILinkMessageBuilder.createResponse(message, ConstDef.FH_V_FAIL));
    }
  }

  @Override
  public String toString() {
    return String.format("iAgent: %s, channel: %s, epList: [%s]",
                         this.agentId,
                         this.ctx == null ? "null" : this.ctx.channel().id().asShortText(),
                         this.epList);
  }

  @Override
  public void sendResponse(IResponse response) {
    // TODO Auto-generated method stub
    sendMessage((ILinkMessage) response);
  }

  public void cacheConnectedDevice(ShortSession[] ssList) {
    for (ShortSession ss : ssList) {
      epList.put(ss.getDi(), true);
    }
  }

  public void removeConnectedDevice(String di) {
    epList.remove(di);
  }

  public boolean containsDi(String di) {
    if (di == null) {
      return false;
    }
    if (agentId != null && agentId.compareTo(di) == 0) {
      return true;
    }
    return epList.containsKey(di);
  }

  public Set<String> getEpSet() {
    return epList.keySet();
  }

  @Override
  public int compareTo(IAgent another) {
    return this.agentId == null ? 0 : this.agentId.compareTo(another.getAgentId());
  }

  /* dummy functions **/
  @Override
  public String getAccessToken() {
    return null;
  }

  @Override
  public String getUserId() {
    return null;
  }

  @Override
  public void onConnected() {}
}
