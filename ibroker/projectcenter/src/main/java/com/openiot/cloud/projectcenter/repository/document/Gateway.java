/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.repository.document;

import com.openiot.cloud.base.help.ConstDef;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(ConstDef.C_GATEWAY)
@Data
public class Gateway {
  @Id
  private String id;
  private String iAgentId;
  private String hwSn;
  private String provKey;
  private long provTime;
  private long lastAuthTime;
  private boolean reset;
  private String newHwSn;
  private String domain;
  private String projectId;
  private boolean manual;
}
