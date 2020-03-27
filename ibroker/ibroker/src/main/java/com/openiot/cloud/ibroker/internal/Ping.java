/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker.internal;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.ilink.ILinkMessage;
import com.openiot.cloud.base.ilink.LeadingByte;
import com.openiot.cloud.base.ilink.MessageType;
import com.openiot.cloud.ibroker.base.device.IAgent;
import com.openiot.cloud.ibroker.base.device.IAgentCache;
import org.iotivity.cloud.base.device.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Ping {
  private static final Logger logger = LoggerFactory.getLogger(Ping.class);

  @Autowired
  private IAgentCache dc;

  public void onDefaultRequestReceived(Device srcDevice, ILinkMessage request) {
    logger.debug(String.format("#Request %s -> ping", request.getMessageId()));

    dc.resetMissedPingCount((IAgent) srcDevice);

    ILinkMessage message =
        new ILinkMessage(LeadingByte.RESPONSE.valueOf(),
                         (byte) MessageType.INTEL_IAGENT.valueOf()).setTag(ConstDef.FH_V_PING)
                                                                   .setResponseCode(ConstDef.FH_V_SUCC)
                                                                   .setIlinkMessageId(request.getIlinkMessageId());
    ((IAgent) srcDevice).sendMessage(message);
  }
}
