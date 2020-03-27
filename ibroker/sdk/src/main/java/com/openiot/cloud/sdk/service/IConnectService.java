/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.service;

import com.openiot.cloud.base.help.BaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Set;

/**
 * The Api Service abstract class, where there is only one abstract method getApiServiceRegInfo()
 * need to be implemented.<br>
 * <br>
 * All Rest Api and its handler will be defined in ApiServiceRegInfo as return of
 * getApiServiceRegInfo()
 */
@Component
@Qualifier("IConnectService")
@Scope("prototype")
public class IConnectService {
  private static final Logger logger = LoggerFactory.getLogger(IConnectService.class.getName());
  protected HashMap<String, IConnectServiceHandler> handlers =
      new HashMap<String, IConnectServiceHandler>();

  public static IConnectService create() {
    return ApplicationContextProvider.getBean("IConnectService", IConnectService.class);
  }

  public void addHandler(String url, IConnectServiceHandler handler) {
    // since we are not sure users' attempts, we are going to always remove tailing slashes
    url = BaseUtil.removeTrailingSlash(url);
    handlers.put(url, handler);
  }

  public void onInit() {}

  public void onServiceRequest(String path, IConnectRequest req) {
    if (path == null || path.length() == 0) {
      logger.warn("path is null for msg: " + req.toString());
      return;
    }
    IConnectServiceHandler listener = handlers.get(path);
    while (listener == null) {
      int index = path.lastIndexOf("/");
      if (index > 0) {
        path = path.substring(0, index);
        listener = handlers.get(path);
      } else {
        break;
      }
    }
    if ((listener != null)) {
      logger.info("receive:  " + req);
      listener.onRequest(req);
    } else {
      logger.warn("No message handler for path: " + path + "   for msg:" + req.toString());
    }
  }

  public Set<String> getServiceUrl() {
    return handlers.keySet();
  }
}
