/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.help.ConstDef;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = ConstDef.C_CONFIG)
@JsonInclude(Include.NON_NULL)
public class Config {

  @Id
  String id;

  @Field(ConstDef.F_TGTTYPE)
  @JsonProperty(ConstDef.F_TGTTYPE)
  String targetType;

  @Field(ConstDef.F_TGTID)
  @JsonProperty(ConstDef.F_TGTID)
  String targetId;

  @Field(ConstDef.F_CONFIGS)
  @JsonProperty(ConstDef.F_CONFIGS)
  String config;

  public Config() {}

  public Config(String targetType, String targetId, String config) {
    super();
    this.targetType = targetType;
    this.targetId = targetId;
    this.config = config;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTargetType() {
    return targetType;
  }

  public void setTargetType(String targetType) {
    this.targetType = targetType;
  }

  public String getTargetId() {
    return targetId;
  }

  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }

  public String getConfig() {
    return config;
  }

  public void setConfig(String config) {
    this.config = config;
  }

  @Override
  public String toString() {
    return "Config [targetType=" + targetType + ", targetId=" + targetId + "] [config=" + config
        + "]";
  }
}
