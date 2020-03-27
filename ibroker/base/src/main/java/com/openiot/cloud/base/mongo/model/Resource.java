/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.help.Untouchable;
import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import javax.validation.constraints.NotNull;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Document(collection = ConstDef.C_RES)
@JsonInclude(Include.NON_EMPTY)
public class Resource {
  @Untouchable
  @Id
  @JsonIgnore
  String id;

  @Untouchable
  @Field(ConstDef.F_URL)
  @JsonProperty(ConstDef.F_URL)
  @NotNull
  String url;

  @Field(ConstDef.F_NAME)
  @JsonProperty(ConstDef.F_NAME)
  String name;

  @Field(ConstDef.F_RESTYPE)
  @JsonProperty(ConstDef.F_RESTYPE)
  @NotNull
  List<String> resTypes;

  @Untouchable
  @Field(ConstDef.F_DEVID)
  @JsonProperty(ConstDef.F_DEVID)
  @NotNull
  String devId;

  @Field(ConstDef.F_CONFIGS)
  @JsonProperty(ConstDef.F_CONFIGS)
  Config config;

  @Field(ConstDef.F_DEVNAME)
  @JsonProperty(ConstDef.F_DEVNAME)
  String devName;

  @Field(ConstDef.F_GRPS)
  @JsonProperty(ConstDef.F_GRPS)
  // groups names
  List<String> grps;

  @Untouchable
  @Field(ConstDef.F_FULLURL)
  @JsonIgnore
  @Indexed
  String fullUrl;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    if (url.startsWith("/")) {
      this.url = url;
    } else {
      this.url = "/" + url;
    }
    setFullUrl(BaseUtil.formAFullUrl(this.devId, url));
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getResTypes() {
    return resTypes;
  }

  public void setResTypes(List<String> resTypes) {
    this.resTypes = resTypes;
  }

  public String getDevId() {
    return devId;
  }

  public void setDevId(String devId) {
    this.devId = devId;
    setFullUrl(BaseUtil.formAFullUrl(this.devId, url));
  }

  public Config getConfig() {
    return config;
  }

  public void setConfig(Config config) {
    this.config = config;
  }

  public String getDevName() {
    return devName;
  }

  public void setDevName(String devName) {
    this.devName = devName;
  }

  public List<String> getGrps() {
    return grps;
  }

  public void setGrps(List<String> grps) {
    this.grps = grps;
  }

  public String getFullUrl() {
    return Optional.ofNullable(fullUrl).orElse(BaseUtil.formAFullUrl(devId, url));
  }

  public void setFullUrl(String fullUrl) {
    this.fullUrl = fullUrl;
  }

  @Override
  public String toString() {
    return "Resource [id=" + id + ", url=" + url + ", name=" + name + ", resTypes=" + resTypes
        + ", devId=" + devId + ", config=" + config + ", devName=" + devName + ", grps=" + grps
        + "]";
  }

  public static Resource from(String url, String name, List<String> resTypes, String devId,
                              Config config, List<String> grps) {
    Resource res = new Resource();
    res.setDevId(devId);
    res.setUrl(url);
    res.setName(name);
    res.setResTypes(resTypes);
    res.setGrps(grps);
    res.setConfig(config);

    res.setFullUrl(BaseUtil.formAFullUrl(devId, url));
    return res;
  }

  @JsonInclude(Include.NON_NULL)
  public static class Config {

    @Field(ConstDef.F_DATALIFE)
    @JsonProperty(ConstDef.F_DATALIFE)
    Integer dataLife;

    @Field(ConstDef.F_ATTRS)
    @JsonProperty(ConstDef.F_ATTRS)
    List<AttributeEntity> attributes;

    @Field(ConstDef.F_USERCFG)
    @JsonProperty(ConstDef.F_USERCFG)
    List<ConfigurationEntity> userCfgs;

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
      return "Config [dataLife=" + dataLife + ", attributes=" + attributes + ", userCfgs="
          + userCfgs + "]";
    }
  }
}
