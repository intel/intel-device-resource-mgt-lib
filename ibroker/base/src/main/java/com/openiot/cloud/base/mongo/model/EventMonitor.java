/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.validator.CheckName;
import com.openiot.cloud.base.mongo.model.validator.CreateValidator;
import com.openiot.cloud.base.mongo.model.validator.UpdateValidator;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Date;
import java.util.List;

@Document(collection = "EventMonitor")
@JsonInclude(Include.NON_EMPTY)
public class EventMonitor {
  @Id
  ObjectId id;

  @Field("name")
  @JsonProperty("name")
  @NotNull(groups = CreateValidator.class)
  @CheckName(value = EventMonitor.class, message = "need an unique name",
      groups = CreateValidator.class)
  // Event Monitor name
  String name;

  @Field("project")
  @JsonProperty("project")
  String project;

  // @Field(ConstDef.F_TYPE)
  // @JsonProperty(ConstDef.F_TYPE)
  // @NotNull(groups = CreateValidator.class)
  // @Pattern(
  // regexp = ConstDef.EVENT_TYPE_NEW_DATA + "|" + ConstDef.EVENT_TYPE_CFG_SYNC + "|"
  // + ConstDef.EVENT_TYPE_DEV_STAT,
  // message = "has to one of those: NEW_DATA, CFG_SYNC and DEV_STAT",
  // groups = {CreateValidator.class, UpdateValidator.class})
  // // Service handled task type
  // String type;

  @Field("desc")
  @JsonProperty("desc")
  // description
  String desc;

  @Field("regTime")
  @JsonProperty("regTime")
  // register time
  Date regTime;

  @Field("eventTypes")
  @JsonProperty("eventTypes")
  // event types
  List<EventType> eventTypes;

  public ObjectId getId() {
    return id;
  }

  public void setId(ObjectId id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getProject() {
    return project;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public Date getRegTime() {
    return regTime;
  }

  public void setRegTime(Date regTime) {
    this.regTime = regTime;
  }

  public List<EventType> getEventTypes() {
    return eventTypes;
  }

  public void setEventTypes(List<EventType> eventTypes) {
    this.eventTypes = eventTypes;
  }

  public static boolean isValidTaskType(String taskType) {
    return taskType.compareTo(ConstDef.EVENT_TYPE_CFG_SYNC) == 0
        || taskType.compareTo(ConstDef.EVENT_TYPE_DEV_STAT) == 0
        || taskType.compareTo(ConstDef.EVENT_TYPE_PROVISOIN) == 0
        || taskType.compareTo(ConstDef.EVENT_TYPE_FIRST_ONLINE) == 0
        || taskType.compareTo(ConstDef.EVENT_TYPE_NEW_DATA) == 0;
  }

  public static boolean isValidTargetTypInEventType(EventType eventType) {
    String targetType = eventType.getTargetType();
    return targetType.compareTo(ConstDef.EVENT_TARGET_TYPE_DEVICE) == 0
        || targetType.compareTo(ConstDef.EVENT_TARGET_TYPE_GROUP) == 0;
  }

  @Override
  public String toString() {
    return "EventMonitor [id=" + id + ", name=" + name + ", project=" + project + ", eventTypes="
        + eventTypes + ", desc=" + desc + ", regTime=" + regTime + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((project == null) ? 0 : project.hashCode());
    result = prime * result + ((eventTypes == null) ? 0 : eventTypes.hashCode());
    result = prime * result + ((desc == null) ? 0 : desc.hashCode());
    result = prime * result + ((regTime == null) ? 0 : regTime.hashCode());
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
    EventMonitor other = (EventMonitor) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (eventTypes == null) {
      if (other.eventTypes != null)
        return false;
    } else if (!eventTypes.equals(other.eventTypes))
      return false;
    if (project == null) {
      if (other.project != null)
        return false;
    } else if (!project.equals(other.project))
      return false;
    if (regTime == null) {
      if (other.regTime != null)
        return false;
    } else if (!regTime.equals(other.regTime))
      return false;
    return true;
  }

  public static class EventType {

    @Field("eventType")
    @JsonProperty("eventType")
    String eventType;

    @Field("targetType")
    @JsonProperty("targetType")
    @Pattern(regexp = ConstDef.EVENT_TARGET_TYPE_DEVICE + "|" + ConstDef.EVENT_TARGET_TYPE_GROUP,
        message = "has to one of those: DEVICE, GROUP",
        groups = {CreateValidator.class, UpdateValidator.class})
    // task target type
    String targetType;

    @Field("targetId")
    @JsonProperty("targetId")
    // task target id
    String targetId;

    @Field("lifeTime")
    @JsonProperty("lifeTime")
    // lifeTime
    Integer lifeTime;

    public EventType() {}

    public EventType(String targetType, String targetId) {
      this.targetType = targetType;
      this.targetId = targetId;
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

    public String getEventType() {
      return eventType;
    }

    public void setEventType(String eventType) {
      this.eventType = eventType;
    }

    public Integer getLifeTime() {
      return lifeTime;
    }

    public void setLifeTime(Integer lifeTime) {
      this.lifeTime = lifeTime;
    }

    @Override
    public String toString() {
      return "EventType [eventType = " + eventType + "targetType=" + targetType + ", targetId="
          + targetId + ", lifeTime=" + lifeTime + "]";
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((eventType == null) ? 0 : eventType.hashCode());
      result = prime * result + ((targetId == null) ? 0 : targetId.hashCode());
      result = prime * result + ((targetType == null) ? 0 : targetType.hashCode());
      result = prime * result + ((lifeTime == null) ? 0 : lifeTime.hashCode());
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
      EventType other = (EventType) obj;
      if (targetId == null) {
        if (other.targetId != null)
          return false;
      } else if (!targetId.equals(other.targetId))
        return false;
      if (targetType == null) {
        if (other.targetType != null)
          return false;
      } else if (!targetType.equals(other.targetType))
        return false;
      if (eventType == null) {
        if (other.eventType != null)
          return false;
      } else if (!eventType.equals(other.eventType))
        return false;
      if (lifeTime == null) {
        if (other.lifeTime != null)
          return false;
      } else {
        if (other.lifeTime == null)
          return false;
        else if (lifeTime.intValue() != other.lifeTime.intValue())
          return false;
      }

      return true;
    }
  }
}
