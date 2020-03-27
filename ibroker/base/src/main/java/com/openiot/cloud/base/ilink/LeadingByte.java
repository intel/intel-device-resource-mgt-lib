/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.ilink;

public enum LeadingByte {
  REQUEST((byte) 0xFA), RESPONSE((byte) 0xCE), PLAIN((byte) 0xBA);

  public static LeadingByte fromValue(byte value) {
    for (LeadingByte code : values()) {
      if (code.value == value) {
        return code;
      }
    }

    return null;
  }

  private final byte value;

  LeadingByte(byte value) {
    this.value = value;
  }

  public byte valueOf() {
    return value;
  }
}
