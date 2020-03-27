/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.service.model;

import com.openiot.cloud.base.help.ConstDef;

public enum DataSourceType {
  REFERENCE(ConstDef.F_DATASOURCEREF), TABLE(ConstDef.F_DATASOURCETABLE);

  private String value;

  DataSourceType(String value) {
    this.value = value;
  }

  public static DataSourceType fromString(String string) {
    if (string.equals(ConstDef.F_DATASOURCEREF)) {
      return REFERENCE;
    } else if (string.equals(ConstDef.F_DATASOURCETABLE)) {
      return TABLE;
    } else {
      return REFERENCE;
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
