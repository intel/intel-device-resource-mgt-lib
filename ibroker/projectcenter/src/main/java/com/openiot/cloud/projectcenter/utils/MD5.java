/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.utils;

import lombok.extern.slf4j.Slf4j;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
public class MD5 {
  // 16 bytes MD5 Hash
  public static byte[] getMd5Hash(byte[] buffer) {
    try {
      return MessageDigest.getInstance("MD5").digest(buffer);
    } catch (NoSuchAlgorithmException e) {
      log.error("md5hash failed", e);
      return null;
    }
  }
}
