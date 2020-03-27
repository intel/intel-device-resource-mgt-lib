/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.utils;

import lombok.extern.slf4j.Slf4j;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Slf4j
public class RandomKeyGen {
  public static byte[] generate() {
    return getStrMD5Hash(UUID.randomUUID().toString());
  }

  private static byte[] getStrMD5Hash(String datastr) {
    try {
      MessageDigest digester = MessageDigest.getInstance("MD5");
      digester.update(datastr.getBytes());
      return digester.digest();
    } catch (NoSuchAlgorithmException e) {
      log.error("can not genrate random key", e);
      throw new RuntimeException("No Such Algorithm");
    }
  }
}
