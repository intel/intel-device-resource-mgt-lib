/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.help.Untouchable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "Alarm")
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Alarm {
  @Untouchable
  @Id
  String id;

  @Untouchable
  @Field(ConstDef.F_ALARMID)
  @JsonProperty(ConstDef.F_ALARMID)
  String alarmid;

  @Untouchable
  @Field("project")
  @JsonProperty("project")
  String project;

  @Untouchable
  @Field(ConstDef.F_ALARMTAGTYPE)
  @JsonProperty(ConstDef.F_ALARMTAGTYPE)
  String targettype;

  @Untouchable
  @Field(ConstDef.F_ALARMTAGID)
  @JsonProperty(ConstDef.F_ALARMTAGID)
  String targetid;

  @Field(ConstDef.F_ALARMDETAILS)
  @JsonProperty(ConstDef.F_ALARMDETAILS)
  String content;

  @Untouchable
  @Field(ConstDef.F_ALARMBEGINNINGTIME)
  @JsonProperty("set_t")
  Long settime;

  @Field(ConstDef.F_ALARMENDTIME)
  @JsonProperty("clear_t")
  Long cleartime;

  @Field(ConstDef.F_ALARMSTATUS)
  @JsonProperty(ConstDef.F_ALARMSTATUS)
  Status status;

  @Untouchable
  @Field(ConstDef.F_TAGGRP)
  @JsonProperty(ConstDef.F_TAGGRP)
  String group;

  @Field(ConstDef.F_ALARMTITLE)
  @JsonProperty(ConstDef.F_ALARMTITLE)
  String title;

  @Transient
  @JsonProperty(ConstDef.F_ALARMSEVERITY)
  private String sev;

  public enum Status {
    ACTIVE(0), CLEARED(1), SOLVED(2);
    private int level;

    Status(int level) {
      this.level = level;
    }

    public int compare(Status dst) {
      return this.level - dst.level;
    }
  }

  public enum TargetType {
    RESOURCE, DEVICE, PROPERTY, DATASOURCE, GROUP;
  }
  // public enum Status {
  // ACTIVE("active"), CLEARED("cleared"), SOLVED("solved");
  //
  // private String value;
  //
  // Status(String value) {
  // this.value = value;
  // }
  //
  // @JsonValue
  // public String getValue() {
  // return this.value;
  // }
  //
  // @JsonCreator
  // public Status forValue(String strValue) {
  // for (Status type : Status.values()) {
  // if (type.getValue().equals(strValue))
  // return type;
  // }
  // return null;
  // }
  //
  // public Status getFromValue(String strValue) {
  // for (Status type : Status.values()) {
  // if (type.getValue().equals(strValue))
  // return type;
  // }
  // return null;
  // }
  // }

  // public enum TargetType {
  // RESOURCE("resource"),
  // DEVICE("device"),
  // PROPERTY("property"),
  // DATASOURCE("datasource"),
  // GROUP("group");
  //
  // private String value;
  //
  // TargetType(String value) {
  // this.value = value;
  // }
  //
  // @JsonValue
  // public String getValue() {
  // return this.value;
  // }
  //
  // @JsonCreator
  // public TargetType forValue(String strValue) {
  // for (TargetType type : TargetType.values()) {
  // if (type.getValue().equals(strValue))
  // return type;
  // }
  // return null;
  // }
  //
  // public TargetType getFromValue(String strValue) {
  // for (TargetType type : TargetType.values()) {
  // if (type.getValue().equals(strValue))
  // return type;
  // }
  // return null;
  // }
  // }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Long getSettime() {
    return settime;
  }

  public void setSettime(Long bt) {
    this.settime = bt;
  }

  public Long getCleartime() {
    return cleartime;
  }

  public void setCleartime(Long et) {
    this.cleartime = et;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status s) {
    this.status = s;
  }

  public String getTargettype() {
    return targettype;
  }

  public void setTargettype(String tt) {
    this.targettype = tt;
  }

  public String getTargetid() {
    return targetid;
  }

  public void setTargetid(String tid) {
    this.targetid = tid;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String details) {
    this.content = details;
  }

  public String getAid() {
    return alarmid;
  }

  public void setAlarmid(String aid) {
    this.alarmid = aid;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String desc) {
    this.title = desc;
  }

  public String getSev() {
    return sev;
  }

  public void setSev(String sev) {
    this.sev = sev;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String grp) {
    this.group = grp;
  }

  public String getProject() {
    return project;
  }

  public void setProject(String projectID) {
    this.project = projectID;
  }

  @Override
  public String toString() {
    return "Alarm [project=" + project + ", group=" + group + ", alarmid=" + alarmid
        + ", targettype=" + targettype + ", targetid=" + targetid + ", content=" + content
        + ", set_t=" + settime + ", status=" + status + ", clear_t=" + cleartime + "]";
  }
}
