/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.ssl;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.ilink.ILinkMessage;
import com.openiot.cloud.base.ilink.LeadingByte;
import com.openiot.cloud.base.ilink.MessageType;
import com.openiot.cloud.projectcenter.service.FactoryKeyService;
import com.openiot.cloud.projectcenter.service.GatewayService;
import com.openiot.cloud.projectcenter.service.dto.FactoryKeyDTO;
import com.openiot.cloud.projectcenter.service.dto.GatewayDTO;
import com.openiot.cloud.projectcenter.utils.AES128CBC;
import com.openiot.cloud.projectcenter.utils.MD5;
import com.openiot.cloud.projectcenter.utils.RandomKeyGen;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

@Slf4j
@Component
public class ProvisionSslHandler {
  @Autowired
  private GatewayService gatewayService;
  @Autowired
  private FactoryKeyService factoryKeyService;
  // Default factory key
  @Value("${provision.factory-key:hello iagent}")
  private String DEFAULT_FACTORY_KEY;
  // Global reset flag
  @Value("${provision.global.reset:false}")
  private boolean GLOBAL_RESET;

  public ILinkMessage onMessage(ILinkMessage request) {
    log.debug("a message {} is coming", request);
    // INTEL_IAGENT messages are always PLAIN
    ILinkMessage response =
        new ILinkMessage(LeadingByte.PLAIN.valueOf(), (byte) MessageType.INTEL_IAGENT.valueOf());
    response.setIlinkMessageId(request.getIlinkMessageId());

    String serialNumber = (String) request.getFlexHeaderValue("sn");
    byte[] payload = request.getPayload();
    if (Objects.isNull(serialNumber) || serialNumber.length() == 0 || Objects.isNull(payload)
        || payload.length == 0) {
      log.error("{} is an invalid provision message", request);
      response.setResponseCode(ConstDef.FH_V_FAIL);
      response.setTag(ConstDef.FH_V_PRO);
      return response;
    }

    // use the default key if there is no such key in the database
    String factoryKey = DEFAULT_FACTORY_KEY;
    String keyName = (String) request.getFlexHeaderValue("key_name");
    String keyType = (String) request.getFlexHeaderValue("key_type");
    if (Objects.nonNull(keyName) && Objects.nonNull(keyType)) {
      FactoryKeyDTO key = factoryKeyService.findByNameAndType(keyName, keyType);
      if (Objects.nonNull(key)) {
        log.info("find a key in database with {} and {}, use it", keyName, keyType);
        factoryKey = key.getKeyValue();
      }
    }

    byte[] token = decryptVerifyAndGetToken(payload, factoryKey, serialNumber);
    if (token == null) {
      log.error("message {} verification failed!", request);
      response.setResponseCode(ConstDef.FH_V_FAIL);
      response.setTag(ConstDef.FH_V_PRO);
      return response;
    }

    // generate a new key
    byte[] provKey = RandomKeyGen.generate();

    // let us see whether the gateway is in database or not
    GatewayDTO gateway = gatewayService.findBySerialNumber(serialNumber, false);
    if (gateway == null) {
      // never provision before

      // but still may be a new serial number of an existing gateway
      gateway = gatewayService.findBySerialNumber(serialNumber, true);
      if (gateway != null) {
        // still not working if there are two same serial number
        if (Objects.equals(gateway.getHwSn(), gateway.getNewHwSn())) {
          log.error("The gateway {} has a new serial number same with its old serial number",
                    gateway);
          response.setResponseCode(ConstDef.FH_V_FAIL);
          response.setTag(ConstDef.FH_V_PRO);
          return response;
        }

        gateway.setProvKey(new String(Base64.encode(provKey)));
        gateway.setProvTime(Instant.now(Clock.systemUTC()).toEpochMilli());
        // set the serial number with the new serial number
        gateway.setHwSn(serialNumber);
        // clear the new serial number
        gateway.setNewHwSn(null);
        gateway.setReset(false);
      } else {
        // it is a newer
        gateway = new GatewayDTO();
        gateway.setProvKey(new String(Base64.encode(provKey)));
        gateway.setProvTime(Instant.now(Clock.systemUTC()).toEpochMilli());
        gateway.setHwSn(serialNumber);
        // use the serial number as iAgentId
        gateway.setIAgentId(serialNumber);
      }
    } else {
      // has a provision record

      // if the gateway is marked as RESET or GLOBAL_RESET is triggered
      if (!gateway.isReset() && !GLOBAL_RESET) {
        log.error("iagent {} can not be provisioned again until RESET", gateway.getIAgentId());
        response.setResponseCode(ConstDef.FH_V_FAIL);
        response.setTag(ConstDef.FH_V_PRO);
        return response;
      } else {
        gateway.setProvKey(new String(Base64.encode(provKey)));
        gateway.setProvTime(Instant.now(Clock.systemUTC()).toEpochMilli());
        // clear the reset flag
        gateway.setReset(false);
      }
    }

    Objects.requireNonNull(gateway);
    Objects.requireNonNull(gateway.getIAgentId());
    Objects.requireNonNull(gateway.getProvKey());
    Objects.requireNonNull(gateway.getHwSn());
    gatewayService.save(gateway);

    // form a token as the payload
    byte[] provKeyMd5 = MD5.getMd5Hash(provKey);
    // raw = token + provKeyMd5 + provKey
    byte[] raw = new byte[token.length + +provKeyMd5.length + provKey.length];
    System.arraycopy(token, 0, raw, 0, token.length);
    System.arraycopy(provKeyMd5, 0, raw, token.length, provKeyMd5.length);
    System.arraycopy(provKey, 0, raw, token.length + provKeyMd5.length, provKey.length);

    byte[] key = MD5.getMd5Hash(factoryKey.getBytes());
    byte[] iv = MD5.getMd5Hash(serialNumber.getBytes());
    byte[] resp_payload = AES128CBC.encrypt(raw, key, iv);
    if (resp_payload == null) {
      log.error("encrypt failed");
      response.setResponseCode(ConstDef.FH_V_FAIL);
      response.setTag(ConstDef.FH_V_PRO);
      return response;
    }

    response.setFlexHeaderEntry("_aid", gateway.getIAgentId());
    response.setResponseCode(ConstDef.FH_V_SUCC);
    response.setTag(ConstDef.FH_V_PRO);
    response.setPayload(resp_payload);
    return response;
  }

  // decrypt(payload, MD5(password), MD5(serialNumber)) -> [TOKEN, MD5(TOKEN)]
  byte[] decryptVerifyAndGetToken(byte[] payload, String factoryKey, String serialNumber) {
    byte[] key = MD5.getMd5Hash(factoryKey.getBytes());
    byte[] iv = MD5.getMd5Hash(serialNumber.getBytes());
    if (key == null || iv == null) {
      return null;
    }

    byte[] decrypt = AES128CBC.decrypt(payload, key, iv);
    if (decrypt == null || decrypt.length != 32) {
      log.error("decrypted failed or payload length is not 32!");
      return null;
    }

    byte[] token = new byte[16];
    byte[] tokenMd5 = new byte[16];
    System.arraycopy(decrypt, 0, token, 0, 16);
    System.arraycopy(decrypt, 16, tokenMd5, 0, 16);

    // verify token MD5
    if (!Arrays.equals(MD5.getMd5Hash(token), tokenMd5)) {
      log.error("Verify token Md5 Failed!");
      return null;
    }

    return token;
  }
}
