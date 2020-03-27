/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker.mq;

import com.openiot.cloud.ibroker.base.device.IAgent;
import com.openiot.cloud.ibroker.base.device.IAgentCache;
import com.openiot.cloud.sdk.service.IConnectRequest;
import com.openiot.cloud.sdk.service.IConnectServiceHandler;
import com.openiot.cloud.sdk.utilities.UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class IagentOnlineJmsReqHandler implements IConnectServiceHandler {

  public static final Logger logger = LoggerFactory.getLogger(IagentOnlineJmsReqHandler.class);

  @Autowired
  private IAgentCache dc;

  @Autowired
  private String ibrokerId;

  @Override
  public void onRequest(IConnectRequest request) {

    String url = request.getUrl();
    Map<String, String> params = UrlUtil.getAllQueryParam(url);

    String fromIBroker = params.get("broker");
    String onlineIagentId = new String(request.getPayload());
    logger.debug(String.format("receive a broadcast messsage from %s about %s online",
                               fromIBroker,
                               onlineIagentId));

    // if it comes from itselft
    if (fromIBroker.compareTo(ibrokerId) == 0) {
      logger.debug("it is my new iagent, skip");
      return;
    }

    if (!dc.containsKey(onlineIagentId)) {
      logger.debug("it is not connectted with me");
    } else {
      IAgent device = dc.removeAgent(onlineIagentId, false);
      logger.debug("remove it from my side " + device);
      device.onDisconnected();
      device.getRequestChannel().disconnect();
    }
  }
}
