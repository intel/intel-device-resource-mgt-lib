/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.projectcenter.controller.amqp.*;
import com.openiot.cloud.projectcenter.server.SecureSocketServer;
import com.openiot.cloud.sdk.service.IConnect;
import com.openiot.cloud.sdk.service.IConnectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import java.util.concurrent.Executors;

@Slf4j
@Configuration
@EnableMongoRepositories(basePackages = "com.openiot.cloud.projectcenter.repository")
public class ApplicationConfiguration {
  @Autowired
  private IConnect iConnect;
  @Autowired
  private IConnectService iConnectService;
  @Autowired
  private ApiProjectAmqpHandler apiProjectAMQPHandler;
  @Autowired
  private ApiUserAmqpHandler apiUserAMQPHandler;
  @Autowired
  private AuthAmqpHandler authAmqpHandler;
  @Autowired
  private ProvisionAuthAmqpHandler provisionAuthAmqpHandler;
  @Autowired
  private ProvisionInfoAmqpHandler provisionInfoAmqpHandler;
  @Autowired
  private ProvisionProjectAmqpHandler provisionProjectAmqpHandler;
  @Autowired
  private ProvisionReplaceAmqpHandler provisionReplaceAmqpHandler;
  @Autowired
  private ProvisionResetAmqpHandler provisionResetAmqpHandler;
  @Autowired
  private ProvisionManuallyAmqpHandler provisionManuallyAmqpHandler;
  @Autowired
  private SecureSocketServer secureSocketServer;

  @EventListener
  public void onApplicationReady(final ApplicationReadyEvent event) {
    // /prov/**
    iConnectService.addHandler(ConstDef.MQ_QUEUE_PROV_AUTH, provisionAuthAmqpHandler);
    iConnectService.addHandler(ConstDef.MQ_QUEUE_PROV_INFO, provisionInfoAmqpHandler);
    iConnectService.addHandler(ConstDef.MQ_QUEUE_PROV_PROJECT, provisionProjectAmqpHandler);
    iConnectService.addHandler(ConstDef.MQ_QUEUE_PROV_REPLACE, provisionReplaceAmqpHandler);
    iConnectService.addHandler(ConstDef.MQ_QUEUE_PROV_RESET, provisionResetAmqpHandler);
    iConnectService.addHandler("/prov/manually", provisionManuallyAmqpHandler);

    // /api/project/**
    iConnectService.addHandler(ConstDef.U_PROJECT, apiProjectAMQPHandler);
    iConnectService.addHandler(ConstDef.U_PROJECT_ATTR, apiProjectAMQPHandler);
    iConnectService.addHandler(ConstDef.U_PROJECT_CFG, apiProjectAMQPHandler);
    iConnectService.addHandler(ConstDef.U_PROJECT_MEMBER, apiProjectAMQPHandler);
    // /api/user
    iConnectService.addHandler(ConstDef.U_USER, apiUserAMQPHandler);
    // /api/user/**
    iConnectService.addHandler(ConstDef.U_USER_LOGIN, authAmqpHandler);
    iConnectService.addHandler(ConstDef.U_USER_SELECTPROJECT, authAmqpHandler);
    iConnectService.addHandler(ConstDef.U_USER_REFRESH, authAmqpHandler);
    iConnectService.addHandler(ConstDef.U_USER_VALIDATION, authAmqpHandler);
    // Start RESTFul Service over AMQP MQ
    iConnect.startService(iConnectService);

    // Start SSL server
    Executors.newSingleThreadExecutor().submit(() -> {
      try {
        secureSocketServer.run();
      } catch (Exception e) {
        log.error("meet an exception when starting the ssl server", e);
        throw new RuntimeException("start SSL server failed");
      }
    });
  }

  @EventListener
  public void onContextClosed(final ContextClosedEvent event) {
    iConnect.disConnectAll();
  }
}
