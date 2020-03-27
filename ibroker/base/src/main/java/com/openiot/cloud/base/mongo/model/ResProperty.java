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
import com.openiot.cloud.base.mongo.model.ResourceType.PropertyType;
import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import com.openiot.cloud.base.mongo.model.validator.CreateValidator;
import com.openiot.cloud.base.mongo.model.validator.UpdateValidator;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import javax.validation.constraints.Pattern;
import java.util.*;

@Document(collection = ConstDef.C_RESPRO)
@JsonInclude(Include.NON_NULL)
public class ResProperty {
  @Untouchable
  @Id
  @JsonIgnore
  private String id;

  @Field(ConstDef.F_ACCESS)
  @JsonProperty(ConstDef.F_ACCESS)
  @Pattern(
      regexp = ConstDef.AC_EXE + "|" + ConstDef.AC_EXE + ConstDef.AC_WRITE + "|" + ConstDef.AC_EXE
          + ConstDef.AC_READ + "|" + ConstDef.AC_READ + "|" + ConstDef.AC_READ + ConstDef.AC_WRITE
          + "|" + ConstDef.AC_READ + ConstDef.AC_EXE + "|" + ConstDef.AC_WRITE + "|"
          + ConstDef.AC_WRITE + ConstDef.AC_READ + "|" + ConstDef.AC_WRITE + ConstDef.AC_EXE,
      groups = {CreateValidator.class, UpdateValidator.class})
  private String access;

  @Untouchable
  @Field(ConstDef.F_DEVID)
  @JsonProperty(ConstDef.F_DEVID)
  private String devId;

  @Field(ConstDef.F_IMPLED)
  @JsonProperty(ConstDef.F_IMPLED)
  private Boolean implemented;

  @Untouchable
  @Field(ConstDef.F_NAME)
  @JsonProperty(ConstDef.F_NAME)
  private String name;

  @Untouchable
  @Field(ConstDef.F_RES)
  @JsonProperty(ConstDef.F_RES)
  private String res;

  @Field(ConstDef.F_TYPE)
  @JsonProperty(ConstDef.F_TYPE)
  private String type;

  @Field(ConstDef.F_UNIT)
  @JsonProperty(ConstDef.F_UNIT)
  private String unit;

  @Field(ConstDef.F_USERCFG)
  @JsonProperty(ConstDef.F_USERCFG)
  private List<ConfigurationEntity> userCfgs;

  @Field(ConstDef.F_RESTYPE)
  @JsonIgnore
  private List<String> resTypes;

  @Untouchable
  @Field(ConstDef.F_FULLURL)
  @JsonIgnore
  @Indexed
  private String fullUrl;

  public String getAccess() {
    return access;
  }

  public String getDevId() {
    return devId;
  }

  public String getFullUrl() {
    return Optional.ofNullable(fullUrl).orElse(BaseUtil.formAFullUrl(devId, res, name));
  }

  public String getId() {
    return id;
  }

  public Boolean getImplemented() {
    return implemented;
  }

  public String getName() {
    return name;
  }

  public String getRes() {
    return res;
  }

  public String getType() {
    return type;
  }

  public String getUnit() {
    return unit;
  }

  public List<ConfigurationEntity> getUserCfgs() {
    return userCfgs;
  }

  public void setAccess(String access) {
    this.access = access;
  }

  public void setDevId(String devId) {
    this.devId = devId;
    setFullUrl(BaseUtil.formAFullUrl(this.devId, res, name));
  }

  public void setFullUrl(String fullUrl) {
    this.fullUrl = fullUrl;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setImplemented(Boolean implemented) {
    this.implemented = implemented;
  }

  public List<String> getResTypes() {
    return resTypes;
  }

  public void setResTypes(List<String> resTypes) {
    this.resTypes = resTypes;
  }

  public void setName(String name) {
    this.name = name;
    setFullUrl(BaseUtil.formAFullUrl(this.devId, res, name));
  }

  public void setRes(String res) {
    this.res = res;
    setFullUrl(BaseUtil.formAFullUrl(this.devId, res, name));
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public void setUserCfgs(List<ConfigurationEntity> userCfgs) {
    this.userCfgs = userCfgs;
  }

  public void addUserCfgsItem(ConfigurationEntity userCfg) {
    this.userCfgs = Objects.isNull(this.userCfgs) ? new ArrayList<>() : this.userCfgs;

    boolean found = false;
    for (ConfigurationEntity c : this.userCfgs) {
      if (Objects.equals(userCfg.getCn(), c.getCn())) {
        c.setCv(userCfg.getCv());
        found = true;
        break;
      }
    }
    if (!found) {
      this.userCfgs.add(userCfg);
    }
  }

  @JsonIgnore
  public void setObserved(boolean observed, int frequence) {
    addUserCfgsItem(ConfigurationEntity.from("o", Boolean.toString(observed)));
    // addUserCfgsItem(ConfigurationEntity.from("omin", Integer.toString(frequence)));
  }

  @Override
  public String toString() {
    return "ResProperty{" + "id='" + id + '\'' + ", access='" + access + '\'' + ", devId='" + devId
        + '\'' + ", implemented=" + implemented + ", name='" + name + '\'' + ", res='" + res + '\''
        + ", type='" + type + '\'' + ", unit='" + unit + '\'' + ", userCfgs=" + userCfgs
        + ", resTypes=" + resTypes + ", fullUrl='" + fullUrl + '\'' + '}';
  }

  public static ResProperty from(PropertyType propt, Resource res, String devId,
                                 Boolean implemented, List<String> resTypes) {
    return from(devId,
                res.getUrl(),
                propt.getName(),
                propt.getType(),
                propt.getAccess(),
                propt.getUnit(),
                implemented,
                resTypes);
  }

  public static ResProperty from(String devId, String resUrl, String propName, String dataType,
                                 String access, String unit, Boolean implemented,
                                 List<String> resTypes) {
    ResProperty prop = new ResProperty();
    prop.setDevId(devId);
    prop.setRes(resUrl);
    prop.setName(propName);
    prop.setAccess(access);
    prop.setType(dataType);
    prop.setUnit(unit);
    prop.setImplemented(implemented);
    prop.setResTypes(resTypes);
    prop.setFullUrl(BaseUtil.formAFullUrl(devId, resUrl, propName));
    return prop;
  }
}
