/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.mongo.model.validator.CreateValidator;
import org.bson.types.Binary;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

@Document(collection = "TaskNew")
@JsonInclude(Include.NON_NULL)
public class TaskNew {

  @Id
  String id;

  @Field("monitorName")
  @JsonProperty("monitorName")
  @NotNull(groups = CreateValidator.class)
  String monitorName;

  @Field("eventType")
  @JsonProperty("eventType")
  String eventType;

  @Field("targetType")
  @JsonProperty("targetType")
  String targetType;

  @Field("targetId")
  @JsonProperty("targetId")
  String targetId;

  @Field("desc")
  @JsonProperty("desc")
  // Task description
  String desc;

  @Field("createTime")
  @JsonProperty("createTime")
  // Task creation time
  Date createTime;

  @Field("deadline")
  @JsonProperty("deadline")
  // Task deadline
  Date deadline;

  @Field("dataFmt")
  @JsonProperty("dataFmt")
  // Task data format
  String dataFmt;

  @Field("data")
  @JsonProperty("data")
  // Task data
  Binary data;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getMonitorName() {
    return monitorName;
  }

  public void setMonitorName(String monitorName) {
    this.monitorName = monitorName;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public Date getDeadline() {
    return deadline;
  }

  public void setDeadline(Date deadline) {
    this.deadline = deadline;
  }

  public String getDataFmt() {
    return dataFmt;
  }

  public void setDataFmt(String dataFmt) {
    this.dataFmt = dataFmt;
  }

  public byte[] getData() {
    return Optional.ofNullable(data).map(Binary::getData).orElse(null);
  }

  public void setData(byte[] data) {
    this.data = data == null ? null : new Binary(data);
  }

  public String getTargetType() {
    return targetType;
  }

  public void setTargetType(String targetType) {
    this.targetType = targetType;
  }

  public String getTargetId() {
    return targetId;
  }

  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }

  @Override
  public String toString() {
    return "TaskNew [id=" + id + ", monitorName=" + monitorName + ", eventType=" + eventType
        + ", targetType=" + targetType + ", targetId=" + targetId + ", desc=" + desc
        + ", createTime=" + createTime + ", deadline=" + deadline + ", dataFmt=" + dataFmt
        + ", data=" + Arrays.toString(getData()) + "]";
  }
}
