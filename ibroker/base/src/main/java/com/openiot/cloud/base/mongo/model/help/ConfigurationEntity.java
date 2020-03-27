/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model.help;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import org.springframework.data.mongodb.core.mapping.Field;

@JsonInclude(Include.NON_NULL)
public class ConfigurationEntity {
  @Field(ConstDef.F_CONFIGNAME)
  @JsonProperty(ConstDef.F_CONFIGNAME)
  String cn;

  @Field(ConstDef.F_CONFIGVALUE)
  @JsonProperty(ConstDef.F_CONFIGVALUE)
  String cv;

  @Field(ConstDef.F_CONFIGTIMESTAMP)
  @JsonProperty(ConstDef.F_CONFIGTIMESTAMP)
  long cts;

  @JsonCreator
  public ConfigurationEntity(@JsonProperty(ConstDef.F_CONFIGNAME) String cn,
      @JsonProperty(ConstDef.F_CONFIGVALUE) String cv) {
    this.cn = cn;
    this.cv = cv;
    this.cts = BaseUtil.getNowAsEpochMillis();
  }

  public String getCn() {
    return cn;
  }

  public void setCn(String cn) {
    this.cn = cn;
  }

  public String getCv() {
    return cv;
  }

  public void setCv(String cv) {
    this.cv = cv;
  }

  public long getCts() {
    return cts;
  }

  public void setCts(long cts) {
    this.cts = cts;
  }

  @Override
  public String toString() {
    return "ConfigurationEntity [cn=" + cn + ", cv=" + cv + ", cts=" + cts + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((cn == null) ? 0 : cn.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ConfigurationEntity other = (ConfigurationEntity) obj;
    if (cn == null) {
      if (other.cn != null)
        return false;
    } else if (!cn.equals(other.cn))
      return false;
    return true;
  }

  public static ConfigurationEntity from(String key, String value) {
    return new ConfigurationEntity(key, value);
  }
}
