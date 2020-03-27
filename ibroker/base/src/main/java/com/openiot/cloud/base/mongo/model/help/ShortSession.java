/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model.help;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.help.ConstDef;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ShortSession {

  @JsonProperty(ConstDef.Q_DEVID)
  String di; // device id

  @JsonProperty(ConstDef.Q_SESID)
  String s; // session id

  @JsonCreator
  public ShortSession(@JsonProperty(ConstDef.Q_DEVID) String di,
      @JsonProperty(ConstDef.Q_SESID) String s) {
    super();
    this.di = di;
    this.s = s;
  }

  public String getDi() {
    return di;
  }

  public void setDi(String di) {
    this.di = di;
  }

  public String getS() {
    return s;
  }

  public void setS(String s) {
    this.s = s;
  }

  @Override
  public String toString() {
    return String.format("<%s, %s>", di, s);
  }
}
