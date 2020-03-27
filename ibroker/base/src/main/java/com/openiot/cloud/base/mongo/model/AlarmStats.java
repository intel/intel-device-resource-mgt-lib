/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model;

public class AlarmStats {

  public Integer active_num;
  public Integer clear_num;

  public AlarmStats(Integer active_num, Integer clear_num) {
    super();
    this.active_num = active_num;
    this.clear_num = clear_num;
  }

  public AlarmStats() {}
}
