/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.service.model;

public class Operate {
  String type;
  String background_state;
  String di;
  String url;
  String pn;
  String sched;
  String state_cmds;
  Integer repeat;

  public Operate() {}

  public Operate(String type, String background_state, String di, String url, String pn,
      String sched, String state_cmds, Integer repeat) {
    this.type = type == null || type.isEmpty() ? null : type;
    this.background_state =
        background_state == null || background_state.isEmpty() ? null : background_state;
    this.di = di == null || di.isEmpty() ? null : di;
    this.url = url == null || url.isEmpty() ? null : url;
    this.pn = pn == null || pn.isEmpty() ? null : pn;
    this.sched = sched == null || sched.isEmpty() ? null : sched;
    this.state_cmds = state_cmds == null || state_cmds.isEmpty() ? null : state_cmds;
    this.repeat = repeat;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getBackground_state() {
    return background_state;
  }

  public void setBackground_state(String background_state) {
    this.background_state = background_state;
  }

  public String getDi() {
    return di;
  }

  public void setDi(String di) {
    this.di = di;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getPn() {
    return pn;
  }

  public void setPn(String pn) {
    this.pn = pn;
  }

  public String getSched() {
    return sched;
  }

  public void setSched(String sched) {
    this.sched = sched;
  }

  public String getState_cmds() {
    return state_cmds;
  }

  public Integer getRepeat() {
    return repeat;
  }

  public void setRepeat(Integer repeat) {
    this.repeat = repeat;
  }

  public void setState_cmds(String state_cmds) {
    this.state_cmds = state_cmds;
  }

  @Override
  public String toString() {
    return "Operate{" + "type='" + type + '\'' + ", background_state='" + background_state + '\''
        + ", di='" + di + '\'' + ", url='" + url + '\'' + ", pn='" + pn + '\'' + ", sched='" + sched
        + '\'' + ", state_cmds='" + state_cmds + '\'' + ", repeat=" + repeat + '}';
  }
}
