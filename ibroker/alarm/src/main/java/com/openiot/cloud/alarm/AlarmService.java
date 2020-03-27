/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.alarm;

import com.openiot.cloud.sdk.service.IConnect;
import com.openiot.cloud.sdk.service.IConnectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AlarmService {
  public static final Logger logger = LoggerFactory.getLogger(AlarmService.class);

  @Autowired
  private AlarmRequestHandler alarmHandler;

  @Autowired
  private IConnect iConnect;

  @Autowired
  private IConnectService iConnectService;

  @EventListener
  public void onApplicationReady(final ApplicationReadyEvent event) {
    iConnectService.addHandler(ConstDef.URL_ALARM, alarmHandler);
    iConnectService.addHandler(ConstDef.URL_DSS_TOTAL_STATS, alarmHandler);
    iConnectService.addHandler(ConstDef.URL_ALARM_CNT, alarmHandler);
    iConnect.startService(iConnectService);
  }
}
