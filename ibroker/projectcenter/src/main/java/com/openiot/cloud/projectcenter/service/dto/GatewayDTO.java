/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.help.ConstDef;
import lombok.Data;

@Data
public class GatewayDTO {
  @JsonProperty(ConstDef.F_PROV_IAGENTID)
  private String iAgentId;
  @JsonProperty(ConstDef.F_PROV_SERIALNUM)
  private String hwSn;
  private String provKey;
  private long provTime;
  private boolean reset;
  private String newHwSn;
  private String domain;
  private String projectId;
  private boolean manual;
}
