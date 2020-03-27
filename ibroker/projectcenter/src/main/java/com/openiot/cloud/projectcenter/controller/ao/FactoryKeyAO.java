/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.ao;

import lombok.Data;

@Data
public class FactoryKeyAO {
  private String keyName;
  private String keyType;
  private String domain;
}
