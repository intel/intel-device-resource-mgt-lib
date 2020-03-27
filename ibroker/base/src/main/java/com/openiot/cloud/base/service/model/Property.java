/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.service.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;


// TODO: under discussion
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Property {
  @JsonProperty(ConstDef.F_DEVID)
  private String devId;

  @JsonProperty(ConstDef.F_RES)
  private String res;

  @JsonProperty(ConstDef.F_NAME)
  private String name;

  @JsonProperty(ConstDef.F_ACCESS)
  private String access;

  @JsonProperty(ConstDef.F_IMPLED)
  private Boolean implemented;

  @JsonProperty(ConstDef.F_TYPE)
  private String type;

  @JsonProperty(ConstDef.F_UNIT)
  private String unit;

  @JsonProperty(ConstDef.F_USERCFG)
  private List<ConfigurationEntity> userCfgs;

  @JsonProperty(ConstDef.F_DEVNAME)
  private String devName;
}
