/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker.utils;


import lombok.Data;

@Data
public class DeviceLite {
  private String id;
  private String st;
  private String dt;
  private String ia;
  private boolean c;
}
