/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.utils;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Security;

@Slf4j
public class AES128CBC {
  public static byte[] encrypt(byte[] content, byte[] key, byte[] iv) {
    try {
      SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
      Security.addProvider(new BouncyCastleProvider());
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
      return cipher.doFinal(content);
    } catch (Exception e) {
      log.error("encrypt failed", e);
      return null;
    }
  }

  public static byte[] decrypt(byte[] cryptograph, byte[] key, byte[] iv) {
    try {
      SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
      Security.addProvider(new BouncyCastleProvider());
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
      cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
      return cipher.doFinal(cryptograph);
    } catch (Exception e) {
      log.error("decrypt failed", e);
      return null;
    }
  }

  public static String parseByte2HexStr(byte buf[]) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < buf.length; i++) {
      String hex = Integer.toHexString(buf[i] & 0xFF);
      if (hex.length() == 1) {
        hex = '0' + hex;
      }
      sb.append(hex.toUpperCase());
      sb.append(' ');
    }
    return sb.toString();
  }
}
