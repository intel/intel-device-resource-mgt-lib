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
import com.openiot.cloud.base.mongo.model.help.DataSourceEntity;
import com.openiot.cloud.base.mongo.model.validator.CreateValidator;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Document(collection = ConstDef.C_GRPTYPE)
@JsonInclude(Include.NON_EMPTY)
public class GroupType {
  @Untouchable
  @Id
  @JsonProperty(ConstDef.F_NAME)
  @NotNull(groups = {CreateValidator.class})
  // @CheckName(value = GroupType.class, message = "need an unique name", groups =
  // {CreateValidator.class})
  String n;

  @Field(ConstDef.F_DISPLAYNAME)
  @JsonProperty(ConstDef.F_DISPLAYNAME)
  @NotNull(groups = {CreateValidator.class})
  String dpn;

  @Field(ConstDef.F_DESCRIPTION)
  @JsonProperty(ConstDef.F_DESCRIPTION)
  String d;

  @Field(ConstDef.F_CONFIGS)
  @JsonProperty(ConstDef.F_CONFIGS)
  @Valid
  List<ConfigurationEntity> cs;

  @Field(ConstDef.F_ATTRS)
  @JsonProperty(ConstDef.F_ATTRS)
  @Valid
  List<AttributeEntity> as;

  @Field(ConstDef.F_DATASOURCES)
  @JsonProperty(ConstDef.F_DATASOURCES)
  @Valid
  List<DataSourceEntity> dss;

  @Field(ConstDef.F_PROJECT)
  @JsonProperty(ConstDef.F_PROJECT)
  String prj;

  @Field(ConstDef.F_DEVICE_PLAN)
  @JsonProperty(ConstDef.F_DEVICE_PLAN)
  List<DevicePlanEntity> devicePlan;

  public GroupType() {}

  private static <T> T findByName(List<T> list, Predicate<T> filter) {
    return Optional.ofNullable(list)
                   .map(l -> l.stream().filter(item -> filter.test(item)).findFirst().orElse(null))
                   .orElse(null);
  }

  private static <T> void removeByName(List<T> list, Predicate<T> filter) {
    Optional.ofNullable(list).ifPresent(l -> l.removeIf(item -> filter.test(item)));
  }

  public String getN() {
    return n;
  }

  public GroupType setN(String name) {
    this.n = name;
    return this;
  }

  public String getDpn() {
    return dpn;
  }

  public GroupType setDpn(String dpn) {
    this.dpn = dpn;
    return this;
  }

  public String getD() {
    return d;
  }

  public void setD(String description) {
    this.d = description;
  }

  public List<AttributeEntity> getAs() {
    return as;
  }

  public void setAs(List<AttributeEntity> attributes) {
    this.as = attributes;
  }

  public List<DevicePlanEntity> getDevicePlan() {
    return devicePlan;
  }

  public void setDevicePlan(List<DevicePlanEntity> devicePlan) {
    this.devicePlan = devicePlan;
  }

  public List<ConfigurationEntity> getCs() {
    return cs;
  }

  public void setCs(List<ConfigurationEntity> configurations) {
    this.cs = configurations;
  }

  public List<DataSourceEntity> getDss() {
    return dss;
  }

  public void setDss(List<DataSourceEntity> datasources) {
    this.dss = datasources;
  }

  public String getPrj() {
    return prj;
  }

  public GroupType setPrj(String prj) {
    this.prj = prj;
    return this;
  }

  @JsonIgnore
  public AttributeEntity getAttrByName(String name) {
    return findByName(this.as, a -> a.getAn().equals(name));
  }

  public void insertOrUpdateAs(AttributeEntity a) {
    this.as = (List<AttributeEntity>) BaseUtil.insertOrUpdate(this.as, a);
  }

  public void insertOrUpdateAs(List<AttributeEntity> aList) {
    Optional.ofNullable(aList).ifPresent(list -> list.forEach(this::insertOrUpdateAs));
  }

  public void replaceOrClearAs(List<AttributeEntity> aList) {
    this.as = (List<AttributeEntity>) BaseUtil.replaceOrClear(aList, this.as);
  }

  public void removeItemsFromAs(List<String> names) {
    Optional.ofNullable(names).ifPresent(this.as::removeAll);
  }

  @JsonIgnore
  public DevicePlanEntity getDevicePlanById(String id) {
    return findByName(this.devicePlan, a -> a.getId().equals(id));
  }

  public void insertOrUpdateDevicePlans(DevicePlanEntity a) {
    this.devicePlan = (List<DevicePlanEntity>) BaseUtil.insertOrUpdate(this.devicePlan, a);
  }

  public void insertOrUpdateDevicePlans(List<DevicePlanEntity> aList) {
    Optional.ofNullable(aList).ifPresent(list -> list.forEach(this::insertOrUpdateDevicePlans));
  }

  public void replaceOrClearDevicePlans(List<DevicePlanEntity> devicePlanList) {
    this.devicePlan =
        (List<DevicePlanEntity>) BaseUtil.replaceOrClear(devicePlanList, this.devicePlan);
  }

  @JsonIgnore
  public ConfigurationEntity getCfgByName(String name) {
    return findByName(this.cs, c -> c.getCn().equals(name));
  }

  public void insertOrUpdateCs(ConfigurationEntity c) {
    this.cs = (List<ConfigurationEntity>) BaseUtil.insertOrUpdate(this.cs, c);
  }

  public void insertOrUpdateCs(List<ConfigurationEntity> cList) {
    Optional.ofNullable(cList).ifPresent(list -> list.forEach(this::insertOrUpdateCs));
  }

  public void replaceOrClearCs(List<ConfigurationEntity> cList) {
    this.cs = (List<ConfigurationEntity>) BaseUtil.replaceOrClear(cList, this.cs);
  }

  public void removeItemsFromCs(List<String> names) {
    Optional.ofNullable(names).ifPresent(this.cs::removeAll);
  }

  @JsonIgnore
  public DataSourceEntity getDsByName(String name) {
    return findByName(this.dss, ds -> ds.getDsn().equals(name));
  }

  public void insertOrUpdateDss(DataSourceEntity ds) {
    this.dss = (List<DataSourceEntity>) BaseUtil.insertOrUpdate(this.dss, ds);
  }

  public void insertOrUpdateDss(List<DataSourceEntity> dsList) {
    Optional.ofNullable(dsList).ifPresent(list -> list.forEach(this::insertOrUpdateDss));
  }

  public void replaceOrClearDss(List<DataSourceEntity> dsList) {
    this.dss = (List<DataSourceEntity>) BaseUtil.replaceOrClear(dsList, this.dss);
  }

  public void removeItemsFromDss(List<String> names) {
    Optional.ofNullable(names).ifPresent(this.dss::removeAll);
  }

  @Override
  public String toString() {
    return "GroupType{" + "n='" + n + '\'' + ", dpn='" + dpn + '\'' + ", d='" + d + '\'' + ", cs="
        + cs + ", as=" + as + ", dss=" + dss + ", prj='" + prj + '\'' + ", devicePlan=" + devicePlan
        + '}';
  }

  // replace dst fields with src fields
  public static GroupType replace(GroupType src, GroupType dst) {
    if (src == null) {
      return dst;
    }

    // actually both name should be same, but should be careful
    Optional.ofNullable(src.getDpn()).ifPresent(displayName -> dst.setDpn(displayName));
    Optional.ofNullable(src.getD()).ifPresent(description -> dst.setD(description));
    Optional.ofNullable(src.getPrj()).ifPresent(project -> dst.setPrj(project));

    // null means x is not mentioned, empty means clean X, all original items will be removed
    dst.replaceOrClearAs(src.getAs());
    dst.replaceOrClearCs(src.getCs());
    dst.replaceOrClearDevicePlans(src.getDevicePlan());
    dst.replaceOrClearDss(src.getDss());

    return dst;
  }

  public static List<String> allFields() {
    return Arrays.asList(new String[] {ConstDef.F_NAME, ConstDef.F_DISPLAYNAME,
        ConstDef.F_DESCRIPTION, ConstDef.F_CONFIGS, ConstDef.F_ATTRS, ConstDef.F_DATASOURCES});
  }
}
