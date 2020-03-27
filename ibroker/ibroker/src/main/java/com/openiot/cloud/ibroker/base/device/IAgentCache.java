/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker.base.device;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.ibroker.proxy.rd.RDProxy;
import io.netty.channel.Channel;
import lombok.Data;
import org.iotivity.cloud.base.connector.CoapClient;
import org.iotivity.cloud.base.connector.ConnectorPool;
import org.iotivity.cloud.base.device.IRequestChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class IAgentCache {
  private static final Logger logger = LoggerFactory.getLogger(IAgentCache.class);

  private AtomicBoolean lostRdConnection = new AtomicBoolean(false);
  @Autowired
  private RDProxy rdProxy;

  @Data
  class IAgentOnlineInfo {
    IAgent agent;
    int missedPingCount;

    public IAgentOnlineInfo(IAgent agent) {
      this.agent = agent;
      missedPingCount = 0;
    }
  }

  private ConcurrentMap<String, IAgentOnlineInfo> agentPool = new ConcurrentHashMap<>();
  private ScheduledExecutorService ses = new ScheduledThreadPoolExecutor(1);

  public IAgentCache() {
    ses.scheduleAtFixedRate(() -> {
      try {
        maintainConnectionWithRd();
        maintainMissedPingCount();
      } catch (Exception e) {
        logger.error("meet an exception during missed ping check " + BaseUtil.getStackTrace(e));
      }
    }, 1, 60, TimeUnit.SECONDS);
  }


  public IAgent getAgent(String di) {
    if (di == null)
      return null;

    logger.debug(String.format("looking for %s in %s", di, this));
    for (Map.Entry<String, IAgentOnlineInfo> entry : agentPool.entrySet()) {
      if (entry.getValue().getAgent().containsDi(di)) {
        return entry.getValue().getAgent();
      }
    }
    return null;
  }

  public boolean containsKey(String aid) {
    return agentPool.containsKey(aid);
  }

  public void addAgent(IAgent agent) {
    if (agent == null)
      return;

    addAgent(agent.getAgentId(), agent);
  }

  public void addAgent(String aid, IAgent agent) {
    if (agent == null)
      return;

    // if there is existing socket for the same agent id,
    // we need to disconnect it before we setup the new context for the aid.
    if (agentPool.containsKey(aid)) {
      IAgent device = agentPool.get(aid).getAgent();
      logger.warn("find another agent with same agent id " + aid + " @ "
          + device.getRequestChannel());

      removeAgent(device.getAgentId(), false);

      // ensure when a channel inactive event recieved later,
      // we won't remove the iagent id associated context
      // since the new socket context is already inserted for this iagent id.
      device.setAgentId(null);
      device.onDisconnected();
      device.getRequestChannel().disconnect();
    }

    agentPool.put(aid, new IAgentOnlineInfo(agent));
    logger.info(String.format("aft add %s to agentPool %s  ", agent, this));
  }

  public IAgent removeAgent(String aid, boolean report) {
    if (aid == null)
      return null;

    IAgent result = agentPool.remove(aid).getAgent();
    logger.info(String.format("aft remove %s from agentPool %s", aid, this));

    if (report && result != null) {
      if (ConnectorPool.getConnection((ConstDef.RD_URI)) != null) {
        ConnectorPool.getConnection(ConstDef.RD_URI)
                     .sendRequest(rdProxy.reportDeviceDisconnected((IAgent) result), null);
      }
    }

    return result;
  }

  public void resetMissedPingCount(IAgent agent) {
    String agentId = agent.getAgentId();
    // happens when ping messages come before handshake messages
    if (agentId == null) {
      logger.debug(" agentId is null " + agent);
      return;
    }
    IAgentOnlineInfo info = agentPool.get(agentId);
    if (info == null) {
      agentPool.put(agentId, new IAgentOnlineInfo(agent));
    } else {
      info.setMissedPingCount(0);
    }
  }

  private void maintainMissedPingCount() {
    // missedPingCount++
    // if missedPing > 3, then remove
    List<IAgent> timeoutAgent = new LinkedList<>();
    for (Map.Entry<String, IAgentOnlineInfo> entry : agentPool.entrySet()) {
      IAgentOnlineInfo info = entry.getValue();
      IAgent agent = info.getAgent();
      info.setMissedPingCount(info.getMissedPingCount() + 1);
      if (info.getMissedPingCount() > 3) {
        timeoutAgent.add(agent);
      }
    }

    for (IAgent d : timeoutAgent) {
      logger.info(String.format("such agent has lost connection for quite a long", d));
      removeAgent(d.getAgentId(), true);
      d.onDisconnected();
      d.getRequestChannel().disconnect();
    }
  }

  private void maintainConnectionWithRd() {
    IRequestChannel rdChannel = ConnectorPool.getConnection(ConstDef.RD_URI);
    if (Objects.isNull(rdChannel) || !((CoapClient) rdChannel).getChannel().isWritable()) {
      // losing the connection with rd
      lostRdConnection.set(true);
    } else {
      if (lostRdConnection.get()) {
        // lost the connection before, re-connected. should sync connection status with rd now
        for (String iagentId : agentPool.keySet()) {
          IAgent iAgent = agentPool.get(iagentId).agent;
          for (String deviceId : iAgent.getEpSet()) {
            rdProxy.syncDeviceConnectedStatus(iagentId, deviceId);
          }
        }
        lostRdConnection.set(false);
      } else {
        // keep the connection with rd well
      }
    }
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(System.identityHashCode(this));
    sb.append("[\n");

    for (Map.Entry<String, IAgentOnlineInfo> item : agentPool.entrySet()) {
      sb.append(String.format("    <%s - %s>\n", item.getKey(), item.getValue()));
    }

    sb.append("]\n");
    return sb.toString();
  }
}
