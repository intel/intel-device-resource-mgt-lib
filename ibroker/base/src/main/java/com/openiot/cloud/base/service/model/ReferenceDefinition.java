/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.service.model;

public class ReferenceDefinition {

  private String devId;
  private String resUrl;
  private String propName;
  private long from;
  private long to;

  public ReferenceDefinition() {}

  public ReferenceDefinition(String devId, String resUrl, String propName, long from, long to) {
    this.devId = devId;
    this.resUrl = resUrl;
    this.propName = propName;
    this.from = from;
    this.to = to;
  }

  public String getDevId() {
    return devId;
  }

  public void setDevId(String devId) {
    this.devId = devId;
  }

  public String getResUrl() {
    return resUrl;
  }

  public void setResUrl(String resUrl) {
    this.resUrl = resUrl;
  }

  public String getPropName() {
    return propName;
  }

  public void setPropName(String propName) {
    this.propName = propName;
  }

  public long getFrom() {
    return from;
  }

  public void setFrom(long from) {
    this.from = from;
  }

  public long getTo() {
    return to;
  }

  public void setTo(long to) {
    this.to = to;
  }

  @Override
  public String toString() {
    return "ReferenceDefinition{" + "devId='" + devId + '\'' + ", resUrl='" + resUrl + '\''
        + ", propName='" + propName + '\'' + ", from=" + from + ", to=" + to + '}';
  }
}
