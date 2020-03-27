/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.service.model;

import com.openiot.cloud.base.help.BaseUtil;

public class GeneralKeyValuePair {
  private String key;
  private Object value;
  private long accessTime;

  public GeneralKeyValuePair() {}

  public GeneralKeyValuePair(String key, Object value) {
    this.key = key;
    this.value = value;
    this.accessTime = BaseUtil.getNowAsEpochMillis();
  }

  public String getKey() {
    return key;
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
    this.accessTime = BaseUtil.getNowAsEpochMillis();
  }

  public long getAccessTime() {
    return accessTime;
  }

  @Override
  public String toString() {
    return "GeneralKeyValuePair{" + "key='" + key + '\'' + ", value=" + value + ", accessTime="
        + accessTime + '}';
  }
}
