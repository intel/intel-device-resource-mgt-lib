/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.influx;

public class InfluxUtils {
  public static String doubleQuote(String s) {
    return String.format("\"%s\"", s);
  }

  public static String singleQuote(String s) {
    return String.format("\'%s\'", s);
  }
}
