/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.cfg;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.sdk.service.IConnectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class ApplicationConfig {
  private static Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);

  @Autowired
  private ConfigTaskHandler cfgTaskHandler;

  @EventListener
  public void onApplicationReady(final ApplicationReadyEvent event) {

    logger.info("config service starts working...");

    // pull task and handle
    Executors.newScheduledThreadPool(1).execute(() -> {
      while (true) {
        logger.info("config service is pulling a task...");
        Object taskLock = cfgTaskHandler.getTaskLock();
        String url = "/task?monitor=CFG_MONITOR";
        synchronized (taskLock) {
          try {
            cfgTaskHandler.setTaskHandleStatus(false);
            IConnectRequest request = IConnectRequest.create(HttpMethod.GET, url, null, null);
            request.send(cfgTaskHandler, 10, TimeUnit.SECONDS);
            taskLock.wait(5000);
            if (!cfgTaskHandler.getTaskHandleStatus()) {
              Thread.sleep(5000);
            }
          } catch (InterruptedException e) {
            logger.warn(BaseUtil.getStackTrace(e));
          } catch (Exception e) {
            logger.warn(BaseUtil.getStackTrace(e));
          }
        }
      }
    });
  }
}
