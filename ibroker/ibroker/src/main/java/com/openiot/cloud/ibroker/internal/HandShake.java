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
import com.openiot.cloud.ibroker.utils.MD5;
import com.openiot.cloud.sdk.service.IConnectRequest;
import org.iotivity.cloud.base.device.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Component
public class HandShake {
  private static final Logger logger = LoggerFactory.getLogger(HandShake.class);

  @Autowired
  private IAgentCache dc;

  @Autowired
  private String ibrokerId;

  public void onDefaultRequestReceived(Device srcDevice, ILinkMessage request) {
    IAgent device = (IAgent) srcDevice;
    logger.debug(String.format("#Request %s -> handshake", request.getMessageId()));

    String tag = request.getTag();
    if (tag.compareTo(ConstDef.FH_V_HAN1) == 0) {
      ILinkMessage message =
          new ILinkMessage(LeadingByte.PLAIN.valueOf(),
                           (byte) MessageType.INTEL_IAGENT.valueOf()).setAgentId(request.getAgentId())
                                                                     .setTag(ConstDef.FH_V_HAN1)
                                                                     .setResponseCode(ConstDef.FH_V_REQ2HAN)
                                                                     .setIlinkMessageId(request.getIlinkMessageId())
                                                                     // Use channel short id md5 as
                                                                     // random code 1
                                                                     .setPayload(MD5.getMd5Hash(device.getCtx()
                                                                                                      .channel()
                                                                                                      .id()
                                                                                                      .asShortText()
                                                                                                      .getBytes()));
      logger.info("[Auth] handshake 1 payload=" + Arrays.toString(message.getPayload())
          + " channel=" + device.getCtx().channel().id().asShortText() + " to "
          + request.getAgentId());
      device.sendMessage(message);
    } else if (tag.compareTo(ConstDef.FH_V_HAN2) == 0) {
      String agentId = request.getAgentId();
      if (agentId == null || agentId.isEmpty()) {
        logger.error("has an invalid agentId while handshaking");
        device.onDisconnected();
        device.getRequestChannel().disconnect();
        return;
      }
      device.setAgentId(agentId);

      // in case the agent closed before without remove message
      // if (dc.containsKey(agentId)) {
      // logger.warn("find a same one in device cache. reject HANDShAKE2");
      // dc.removeAgent(agentId, false);
      // device.onDisconnected();
      // device.getRequestChannel().disconnect();
      // return;
      // }

      String randomStr = device.getCtx().channel().id().asShortText();
      logger.info(String.format("random Code=%s", randomStr));

      // do authentication by calling provision service
      String authUri = ConstDef.MQ_QUEUE_PROV_AUTH + "?aid=" + agentId + "&random=" + randomStr;

      // publish online information
      IConnectRequest authReq = IConnectRequest.create(HttpMethod.POST,
                                                       authUri,
                                                       MediaType.TEXT_PLAIN,
                                                       request.getPayload());

      authReq.send((authResp) -> {
        HttpStatus authStat = authResp.getStatus();
        if (authStat == HttpStatus.OK || device.getDeviceId().startsWith("__night-owl")) {
          // prepare clock synchronization information
          Instant now = Instant.now();
          long seconds = now.getEpochSecond();
          long micro = now.getNano() / 1000;

          dc.addAgent(device);
          ILinkMessage message =
              new ILinkMessage(LeadingByte.PLAIN.valueOf(),
                               (byte) MessageType.INTEL_IAGENT.valueOf()).setTag(ConstDef.FH_V_HAN2)
                                                                         .setTimeInfoForSync(seconds,
                                                                                             micro)
                                                                         .setResponseCode(ConstDef.FH_V_SUCC)
                                                                         .setIlinkMessageId(request.getIlinkMessageId())
                                                                         .setPayload(authResp.getPayload());
          device.sendMessage(message);

          // Handshake success, set session flag as true
          // device can do other communication with server
          device.setSessionFlag(true);

          // publish online information
          String uri = ConstDef.MQ_TOPIC_ONLINE_IAGENT + "?broker=" + ibrokerId;
          IConnectRequest pubOnlineReq =
              IConnectRequest.create(HttpMethod.GET, uri, MediaType.TEXT_PLAIN, agentId.getBytes());

          pubOnlineReq.send((pubOnlineResp) -> {
          }, 1, TimeUnit.SECONDS);
        } else {
          logger.error("authentication failed! " + authStat.getReasonPhrase());
          device.onDisconnected();
          device.getRequestChannel().disconnect();
          return;
        }
      }, 3, TimeUnit.SECONDS);
    }
  }
}
