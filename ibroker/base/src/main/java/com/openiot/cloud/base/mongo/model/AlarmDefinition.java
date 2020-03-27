/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.Alarm.TargetType;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.List;

@Document(collection = "AlarmDefinition")
@JsonInclude(Include.NON_NULL)
public class AlarmDefinition {

  @Field(ConstDef.F_ALARMID)
  @JsonProperty(ConstDef.F_ALARMID)
  Integer aid;

  @Field(ConstDef.F_ALARMTITLE)
  @JsonProperty(ConstDef.F_ALARMTITLE)
  String desc;

  @Field(ConstDef.F_ALARMSEVERITY)
  @JsonProperty(ConstDef.F_ALARMSEVERITY)
  String sev;

  @Field(ConstDef.F_ALARMTAGTYPE)
  @JsonProperty(ConstDef.F_ALARMTAGTYPE)
  List<TargetType> tt;

  @Field(ConstDef.F_ALADEFGENECONDITION)
  @JsonProperty(ConstDef.F_ALADEFGENECONDITION)
  String genCond;

  @Field(ConstDef.F_ALADEFCLEANCONDITION)
  @JsonProperty(ConstDef.F_ALADEFCLEANCONDITION)
  String cleanCond;

  @Field(ConstDef.F_ALADEFTRIGGERACTION)
  @JsonProperty(ConstDef.F_ALADEFTRIGGERACTION)
  String trigAction;

  @Field(ConstDef.F_ALADEFCLEANACTION)
  @JsonProperty(ConstDef.F_ALADEFCLEANACTION)
  String cleanAction;

  @Field(ConstDef.F_ALADEFNOTES)
  @JsonProperty(ConstDef.F_ALADEFNOTES)
  String notes;

  public Integer getAid() {
    return aid;
  }

  public void setAid(Integer aid) {
    this.aid = aid;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public String getSev() {
    return sev;
  }

  public void setSev(String sev) {
    this.sev = sev;
  }

  public List<TargetType> getTt() {
    return tt;
  }

  public void setTt(List<TargetType> tt) {
    this.tt = tt;
  }

  public String getGenCond() {
    return genCond;
  }

  public void setGenCond(String genCond) {
    this.genCond = genCond;
  }

  public String getCleanCond() {
    return cleanCond;
  }

  public void setCleanCond(String cleanCond) {
    this.cleanCond = cleanCond;
  }

  public String getTrigAction() {
    return trigAction;
  }

  public void setTrigAction(String trigAction) {
    this.trigAction = trigAction;
  }

  public String getCleanAction() {
    return cleanAction;
  }

  public void setCleanAction(String cleanAction) {
    this.cleanAction = cleanAction;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
