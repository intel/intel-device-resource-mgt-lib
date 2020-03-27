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

@JsonInclude(Include.NON_EMPTY)
public class AttributeEntity {
  @Field(ConstDef.F_ATTRNAME)
  @JsonProperty(ConstDef.F_ATTRNAME)
  String an;

  @Field(ConstDef.F_ATTRVALUE)
  @JsonProperty(ConstDef.F_ATTRVALUE)
  String av;

  @Field(ConstDef.F_ATTRTIMESTAMP)
  @JsonProperty(ConstDef.F_ATTRTIMESTAMP)
  long ats;

  @JsonCreator
  public AttributeEntity(@JsonProperty(ConstDef.F_ATTRNAME) String an,
      @JsonProperty(ConstDef.F_ATTRVALUE) String av) {
    super();
    this.an = an;
    this.av = av;
    this.ats = BaseUtil.getNowAsEpochMillis();
  }

  public String getAn() {
    return an;
  }

  public void setAn(String an) {
    this.an = an;
  }

  public String getAv() {
    return av;
  }

  public void setAv(String av) {
    this.av = av;
  }

  public void setAts(long ats) {
    this.ats = ats;
  }

  public long getAts() {
    return ats;
  }

  @Override
  public String toString() {
    return "AttributeEntity [an=" + an + ", av=" + av + ", ats=" + ats + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((an == null) ? 0 : an.hashCode());
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
    AttributeEntity other = (AttributeEntity) obj;
    if (an == null) {
      if (other.an != null)
        return false;
    } else if (!an.equals(other.an))
      return false;
    return true;
  }
}
