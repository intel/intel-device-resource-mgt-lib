/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConfigurableRoutingTable;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.ibroker.base.IBrokerServerSystem;
import com.openiot.cloud.ibroker.base.server.ILinkServer;
import com.openiot.cloud.ibroker.mq.IagentOnlineJmsReqHandler;
import com.openiot.cloud.ibroker.mq.OptJmsReqHandler;
import com.openiot.cloud.sdk.service.IConnect;
import com.openiot.cloud.sdk.service.IConnectService;
import org.iotivity.cloud.base.connector.ConnectorPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import java.net.InetSocketAddress;
import java.util.Map;

@Component
public class ApplicationEventListener {
  private static final Logger logger = LoggerFactory.getLogger(ApplicationEventListener.class);

  @Autowired
  private IBrokerServerSystem iss;

  @Autowired
  private IagentOnlineJmsReqHandler iagentOnlineHandler;

  @Autowired
  private OptJmsReqHandler optHandler;

  @Autowired
  private IConnect iConnect;

  @Autowired
  private IConnectService iConnectService;

  @EventListener
  public void onApplicationReady(final ApplicationReadyEvent event) {
    logger.info("onApplicationReady ... ");

    try {
      coapTcpConnections();

      // Start RESTful Service
      iConnectService.addHandler(ConstDef.MQ_TOPIC_ONLINE_IAGENT, iagentOnlineHandler);
      iConnectService.addHandler(ConstDef.MQ_QUEUE_OPT_REQUEST, optHandler);
      iConnect.startService(iConnectService);

      iss.addServer(new ILinkServer(new InetSocketAddress(ConstDef.ILINK_PORT)));
      iss.startSystem();
      logger.info("iBroker is online now");
    } catch (Exception e) {
      logger.error(BaseUtil.getStackTrace(e));
    }
  }

  @EventListener
  public void onContextClosed(final ContextClosedEvent event) {
    logger.info("ContextClosedEvent ... ");

    try {
      if (iss != null) {
        iss.stopSystem();
      }
      ConnectorPool.disconnectAll();
      ConnectorPool.shutdown();
      logger.info("iBroker is offline now");
    } catch (Exception e) {
      logger.warn(BaseUtil.getStackTrace(e));
    } finally {
      iss = null;
    }
  }

  @EventListener
  public void onContextRefreshed(final ContextRefreshedEvent event) {
    logger.debug("onContextRefreshed");
  }

  @EventListener
  public void onContextStarted(final ContextStartedEvent event) {
    logger.debug("onContextStarted");
  }

  @EventListener
  public void onApplicationStarted(final ApplicationStartedEvent event) {
    logger.debug("onApplicationStarted");
  }

  @EventListener
  public void onApplicationEnvironmentPrepared(final ApplicationEnvironmentPreparedEvent event) {
    logger.debug("onApplicationEnvironmentPrepared");
  }

  @EventListener
  public void onApplicationPrepared(final ApplicationPreparedEvent event) {
    logger.debug("onApplicationPrepared");
  }

  Map<String, InetSocketAddress> coapTcpConnections() {
    Map<String, InetSocketAddress> routingTable = null;
    try {

      Resource resource = new ClassPathResource("routing_table.json");
      if (resource.exists()) {
        logger.debug("resource length " + resource.contentLength());
        routingTable = ConfigurableRoutingTable.readRoutingTable(resource.getInputStream());
      } else {
        logger.error("resource/routing_table.json doesn't exist ");
        return null;
      }

      if (routingTable == null || routingTable.isEmpty()) {
        logger.error("routing table is empty or invalid, either way there is no useable routing item");
        return null;
      }

      for (Map.Entry<String, InetSocketAddress> item : routingTable.entrySet()) {
        ConnectorPool.addConnection(item.getKey(), item.getValue(), false);
      }

      logger.info("set up connections with remote services "
          + ConfigurableRoutingTable.dump(routingTable));
    } catch (Exception e) {
      logger.error("a exception has been thrown when parsing routing_table "
          + e.getLocalizedMessage());
      logger.error(BaseUtil.getStackTrace(e));
    }

    return routingTable;
  }
}
