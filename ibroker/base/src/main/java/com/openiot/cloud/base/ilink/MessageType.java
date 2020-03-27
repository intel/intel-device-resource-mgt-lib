/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.ilink;

public enum MessageType {
  COAP_OVER_TCP(0), HTTP_OVER_TCP(1), TCP_UDP_TUNNEL(2), TCF_TUNNEL(3), INTEL_IAGENT(4), EVENT_DATA(
      5), COMMENT_SETTING(6), SYS_MAX(30);

  public static MessageType fromValue(byte value) {
    for (MessageType code : values()) {
      if (code.value == value) {
        return code;
      }
    }

    return null;
  }

  private final int value;

  MessageType(int value) {
    this.value = value;
  }

  public int valueOf() {
    return value;
  }
}
