/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model.help;

import lombok.ToString;

@ToString
public enum UserRole {
  SYS_ADMIN("ROLE_SYS_ADMIN"), ADMIN("ROLE_ADMIN"), USER("ROLE_USER");
  private String value;

  UserRole(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
