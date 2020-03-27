/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.help.Untouchable;
import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Document(collection = ConstDef.C_DEV)
@JsonInclude(Include.NON_NULL)
public class Device {
  @Untouchable
  @Id
  String id;

  @Field(ConstDef.F_STAND)
  @JsonProperty(ConstDef.F_STAND)
  String standard;

  @Field(ConstDef.F_NAME)
  @JsonProperty(ConstDef.F_NAME)
  // TODO: check duplicate name
  String name;

  @Field(ConstDef.F_DEVTYPE)
  @JsonProperty(ConstDef.F_DEVTYPE)
  String deviceType;

  @Field(ConstDef.F_IAGENT)
  @JsonProperty(ConstDef.F_IAGENT)
  String iAgentId;

  @Field(ConstDef.F_FOLDER)
  @JsonProperty(ConstDef.F_FOLDER)
  String folderId;

  @Field(ConstDef.F_IBROKER)
  @JsonProperty(ConstDef.F_IBROKER)
  String iBroker;

  @Field(ConstDef.F_CONNED)
  @JsonProperty(ConstDef.F_CONNED)
  Boolean connected;

  @Field(ConstDef.F_ENABLED)
  @JsonProperty(ConstDef.F_ENABLED)
  Boolean enabled;

  @Field(ConstDef.F_CONFIGS)
  @JsonProperty(ConstDef.F_CONFIGS)
  Config config;

  // TODO: AO should have such field. Document should not
  @Field(ConstDef.F_GRPS)
  @JsonProperty(ConstDef.F_GRPS)
  // group name here
  List<String> grps;

  @Field(ConstDef.F_PROJECT)
  @JsonProperty(ConstDef.F_PROJECT)
  String prj;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getStandard() {
    return standard;
  }

  public void setStandard(String standard) {
    this.standard = standard;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getiAgentId() {
    return iAgentId;
  }

  public void setiAgentId(String iAgentId) {
    this.iAgentId = iAgentId;
  }

  public String getFolderId() {
    return folderId;
  }

  public void setFolderId(String folderId) {
    this.folderId = folderId;
  }

  public String getiBroker() {
    return iBroker;
  }

  public void setiBroker(String iBroker) {
    this.iBroker = iBroker;
  }

  public Boolean getConnected() {
    return connected;
  }

  public void setConnected(Boolean connected) {
    this.connected = connected;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Config getConfig() {
    return config;
  }

  public void setConfig(Config config) {
    this.config = config;
  }

  public List<String> getGrps() {
    return grps;
  }

  public void setGrps(List<String> grps) {
    this.grps = grps;
  }

  public String getDeviceType() {
    return deviceType;
  }

  public void setDeviceType(String deviceType) {
    this.deviceType = deviceType;
  }

  public String getPrj() {
    return prj;
  }

  public void setPrj(String project) {
    this.prj = project;
  }

  public Device addGroup(String grpName) {
    this.grps = Optional.ofNullable(this.grps).orElseGet(() -> new LinkedList<>());
    if (!this.grps.contains(grpName)) {
      this.grps.add(grpName);
    }
    return this;
  }

  public Device removeGroup(String grpName) {
    Optional.ofNullable(this.grps).ifPresent(groupList -> groupList.remove(grpName));
    return this;
  }

  @Override
  public String toString() {
    return "Device{" + "id='" + id + '\'' + ", standard='" + standard + '\'' + ", name='" + name
        + '\'' + ", deviceType='" + deviceType + '\'' + ", iAgentId='" + iAgentId + '\''
        + ", folderId='" + folderId + '\'' + ", iBroker='" + iBroker + '\'' + ", connected="
        + connected + ", enabled=" + enabled + ", config=" + config + ", grps=" + grps + ", prj="
        + prj + '}';
  }

  @JsonInclude(Include.NON_NULL)
  public static class Config {
    @Field(ConstDef.F_RFRNUM)
    @JsonProperty(ConstDef.F_RFRNUM)
    Integer refNum;

    @Field(ConstDef.F_DATALIFE)
    @JsonProperty(ConstDef.F_DATALIFE)
    Integer dataLife;

    @Field(ConstDef.F_ATTRS)
    @JsonProperty(ConstDef.F_ATTRS)
    List<AttributeEntity> attributes;

    @Field(ConstDef.F_USERCFG)
    @JsonProperty(ConstDef.F_USERCFG)
    List<ConfigurationEntity> userCfgs;

    public Integer getRefNum() {
      return refNum;
    }

    public void setRefNum(Integer refNum) {
      this.refNum = refNum;
    }

    public Integer getDataLife() {
      return dataLife;
    }

    public void setDataLife(Integer dataLife) {
      this.dataLife = dataLife;
    }

    public List<AttributeEntity> getAttributes() {
      return attributes;
    }

    public void setAttributes(List<AttributeEntity> attributes) {
      this.attributes = attributes;
    }

    public List<ConfigurationEntity> getUserCfgs() {
      return userCfgs;
    }

    public void setUserCfgs(List<ConfigurationEntity> userCfgs) {
      this.userCfgs = userCfgs;
    }

    @Override
    public String toString() {
      return "Config [refNum=" + refNum + ", dataLife=" + dataLife + ", attributes=" + attributes
          + ", userCfgs=" + userCfgs + "]";
    }
  }
}
