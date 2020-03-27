/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum StatsType {
  COUNT("cnt"), MAX("max"), MIN("min"), SUM("sum"), AVG("avg"), DURATION("dur"), SWITCH(
      "sw"), RANGECOUNT("rngc"), DURMAX("dur_max"), DURMIN("dur_min"), DURAVG("dur_avg");

  private String value;

  private StatsType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return this.value;
  }

  @JsonCreator
  public StatsType forValue(String strValue) {
    for (StatsType type : StatsType.values()) {
      if (type.getValue().equals(strValue))
        return type;
    }
    return COUNT;
  }

  public static StatsType getFromValue(String strValue) {
    for (StatsType type : StatsType.values()) {
      if (type.getValue().equals(strValue))
        return type;
    }
    return null;
  }
}
