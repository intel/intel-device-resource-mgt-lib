/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.alarm.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.mongodb.core.mapping.Field;

@JsonInclude(Include.NON_EMPTY)
public class TargetEntity {

  @Field("di")
  @JsonProperty("di")
  String di;

  @Field("res")
  @JsonProperty("res")
  String res;

  @Field("pt")
  @JsonProperty("pt")
  String pt;

  @Field("grp")
  @JsonProperty("grp")
  String grp;

  @Field("dsn")
  @JsonProperty("dsn")
  String dsn;

  @JsonCreator
  public TargetEntity(@JsonProperty("di") String di, @JsonProperty("res") String res,
      @JsonProperty("pt") String pt, @JsonProperty("grp") String grp,
      @JsonProperty("dsn") String dsn) {
    super();
    this.di = di;
    this.res = res;
    this.pt = pt;
    this.grp = grp;
    this.dsn = dsn;
  }

  public String getDi() {
    return di;
  }

  public void setDi(String di) {
    this.di = di;
  }

  public String getRes() {
    return res;
  }

  public void setRes(String res) {
    this.res = res;
  }

  public String getPt() {
    return pt;
  }

  public void setPt(String pt) {
    this.pt = pt;
  }

  public String getGrp() {
    return grp;
  }

  public void setGrp(String grp) {
    this.grp = grp;
  }

  public String getDsn() {
    return dsn;
  }

  public void setDsn(String dsn) {
    this.dsn = dsn;
  }
}
