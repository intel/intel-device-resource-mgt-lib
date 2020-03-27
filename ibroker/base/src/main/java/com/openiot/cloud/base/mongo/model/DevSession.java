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

@Document(collection = ConstDef.C_DEVSESS)
@JsonInclude(Include.NON_NULL)
public class DevSession {
  @Id
  String id;

  @Field(ConstDef.F_DEV)
  @JsonProperty(ConstDef.F_DEV)
  String devId;

  @Field(ConstDef.F_BEGEIN)
  @JsonProperty(ConstDef.F_BEGEIN)
  long begin;

  @Field(ConstDef.F_END)
  @JsonProperty(ConstDef.F_END)
  long end;

  @Field(ConstDef.F_IAGENT)
  @JsonProperty(ConstDef.F_IAGENT)
  String iAgentId;

  @Field(ConstDef.F_IBROKER)
  @JsonProperty(ConstDef.F_IBROKER)
  String iBroker;

  public DevSession() {}

  public DevSession(String id, String devId, long begin, String iAgentId, String iBroker) {
    this.id = id;
    this.devId = devId;
    this.begin = begin;
    this.iAgentId = iAgentId;
    this.iBroker = iBroker;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDevId() {
    return devId;
  }

  public void setDevId(String devId) {
    this.devId = devId;
  }

  public long getBegin() {
    return begin;
  }

  public void setBegin(long begin) {
    this.begin = begin;
  }

  public long getEnd() {
    return end;
  }

  public void setEnd(long end) {
    this.end = end;
  }

  public String getiAgentId() {
    return iAgentId;
  }

  public void setiAgentId(String iAgentId) {
    this.iAgentId = iAgentId;
  }

  public String getiBroker() {
    return iBroker;
  }

  public void setiBroker(String iBroker) {
    this.iBroker = iBroker;
  }

  @Override
  public String toString() {
    return "DevSession{" + "id='" + id + '\'' + ", devId='" + devId + '\'' + ", begin=" + begin
        + ", end=" + end + ", iAgentId='" + iAgentId + '\'' + ", iBroker='" + iBroker + '\'' + '}';
  }
}
