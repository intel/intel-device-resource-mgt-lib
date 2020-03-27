/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.help.ConstDef;
import org.springframework.data.mongodb.core.mapping.Field;

@lombok.Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Data { // jackson specific
  @Field(ConstDef.F_STATTYPE)
  @JsonProperty(ConstDef.F_STATTYPE)
  // TODO: user StatsType
  String type;

  // TODO: user Object
  @Field(ConstDef.F_STATVALUE)
  @JsonProperty(ConstDef.F_STATVALUE)
  String value;
}
