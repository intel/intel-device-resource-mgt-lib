/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker.base.protocols.ilink;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.ilink.ILinkMessage;
import com.openiot.cloud.ibroker.base.device.IAgent;
import com.openiot.cloud.ibroker.internal.HandShake;
import com.openiot.cloud.ibroker.internal.Ping;
import org.iotivity.cloud.base.device.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
// import com.openiot.cloud.ibroker.proxy.prov.ProvisionProxy;

@Component
public class ILinkIntelIAgentMessageHandler {
  private static final Logger logger =
      LoggerFactory.getLogger(ILinkIntelIAgentMessageHandler.class);

  @Autowired
  HandShake handshake;

  @Autowired
  Ping ping;

  public void onMessage(Device srcDevice, ILinkMessage request) {
    if (request.getTag() == null) {
      logger.warn("invalid null tag, ignore the message");
      return;
    }

    if (((IAgent) srcDevice).getSessionFlag() != true
        && !request.getTag().equals(ConstDef.FH_V_HAN1)
        && !request.getTag().equals(ConstDef.FH_V_HAN2)) {
      logger.error("iAgent-iBroker session has not been created yet!");
      return;
    }

    try {
      // for sure that HANDSHAKE is only with INTEL_IAGENT
      // message
      switch (request.getTag()) {
        case ConstDef.FH_V_HAN1:
        case ConstDef.FH_V_HAN2:
          handshake.onDefaultRequestReceived(srcDevice, request);
          break;
        case ConstDef.FH_V_PING:
          ping.onDefaultRequestReceived(srcDevice, request);
          break;
        default:
          break;
      }
    } catch (Exception e) {
      logger.warn(BaseUtil.getStackTrace(e));
    }
    return;
  }
}
