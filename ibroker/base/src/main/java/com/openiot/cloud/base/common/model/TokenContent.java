/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.help.UserRole;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenContent implements Serializable {
  private static final long serialVersionUID = -9162264954595883033L;
  @JsonProperty(ConstDef.MSG_KEY_USR)
  private String user;
  @JsonProperty(ConstDef.MSG_KEY_PRJ)
  private String project;
  @JsonProperty(ConstDef.MSG_KEY_ROLE)
  private UserRole role;
}
