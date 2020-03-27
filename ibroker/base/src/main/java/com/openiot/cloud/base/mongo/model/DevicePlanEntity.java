/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.help.ConstDef;
import org.springframework.data.mongodb.core.mapping.Field;

@JsonInclude(Include.NON_NULL)
public class DevicePlanEntity {
  @Field(ConstDef.F_IID)
  @JsonProperty(ConstDef.F_IID)
  String id;

  @Field(ConstDef.F_NAME)
  @JsonProperty(ConstDef.F_NAME)
  String n;

  @Field(ConstDef.F_DESCRIPTION)
  @JsonProperty(ConstDef.F_DESCRIPTION)
  String d;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getN() {
    return n;
  }

  public void setN(String n) {
    this.n = n;
  }

  public String getD() {
    return d;
  }

  public void setD(String d) {
    this.d = d;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
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
    DevicePlanEntity other = (DevicePlanEntity) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    return true;
  }
}
