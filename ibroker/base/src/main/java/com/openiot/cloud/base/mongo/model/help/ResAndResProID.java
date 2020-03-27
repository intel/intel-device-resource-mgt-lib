/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model.help;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.validator.CreateValidator;
import org.springframework.data.mongodb.core.mapping.Field;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@JsonInclude(Include.NON_NULL)
public class ResAndResProID {

  @Field(ConstDef.F_DEVID)
  @JsonProperty(ConstDef.F_DEVID)
  @NotNull(groups = {CreateValidator.class})
  String di;

  @Field(ConstDef.F_RESURI)
  @JsonProperty(ConstDef.F_RESURI)
  @NotNull(groups = {CreateValidator.class})
  String resUri;

  @Field(ConstDef.F_PROPNAME)
  @JsonProperty(ConstDef.F_PROPNAME)
  @NotNull(groups = {CreateValidator.class})
  String pt;

  @JsonCreator
  public ResAndResProID(@JsonProperty(ConstDef.F_DEVID) String di,
      @JsonProperty(ConstDef.F_RESURI) String resUri,
      @JsonProperty(ConstDef.F_PROPNAME) String pt) {
    this.di = di;
    this.resUri = resUri;
    this.pt = pt;
  }

  public String getDi() {
    return di;
  }

  public void setDi(String di) {
    this.di = di;
  }

  public String getResUri() {
    return resUri;
  }

  public void setResUri(String resUri) {
    this.resUri = resUri;
  }

  public String getPt() {
    return pt;
  }

  public void setPt(String pt) {
    this.pt = pt;
  }

  @Override
  public String toString() {
    return "ResAndResProID{" + "di='" + di + '\'' + ", resUri='" + resUri + '\'' + ", pt='" + pt
        + '\'' + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ResAndResProID that = (ResAndResProID) o;
    return Objects.equals(di, that.di) && Objects.equals(resUri, that.resUri)
        && Objects.equals(pt, that.pt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(di, resUri, pt);
  }
}
