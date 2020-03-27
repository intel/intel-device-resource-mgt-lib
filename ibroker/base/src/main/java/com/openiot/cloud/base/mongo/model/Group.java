/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
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
import com.openiot.cloud.base.mongo.model.validator.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.function.Predicate;

@Document(collection = ConstDef.C_GRP)
@JsonInclude(Include.NON_EMPTY)
public class Group {
  @Untouchable
  @Id
  @JsonProperty(ConstDef.F_NAME)
  @CheckName(value = Group.class, message = "need an unique name", groups = {CreateValidator.class})
  String n;

  @Field(ConstDef.F_DISPLAYNAME)
  @JsonProperty(ConstDef.F_DISPLAYNAME)
  @NotNull(groups = {CreateValidator.class})
  String dpn;

  @Field(ConstDef.F_DESCRIPTION)
  @JsonProperty(ConstDef.F_DESCRIPTION)
  String d;

  @Field(ConstDef.F_PROJECT)
  @JsonProperty(ConstDef.F_PROJECT)
  String prj;

  @JsonProperty(ConstDef.F_GRPTYPEDPN)
  @Transient
  String gtdpn;

  @Field(ConstDef.F_GRPTYPE)
  @JsonProperty(ConstDef.F_GRPTYPE)
  @CheckGt(value = Group.class, message = "need an valid group type name",
      groups = {CreateValidator.class, UpdateValidator.class})
  String gt;

  @Field(ConstDef.F_MD)
  @JsonProperty(ConstDef.F_MD)
  List<String> md;

  @Field(ConstDef.F_MR)
  @JsonProperty(ConstDef.F_MR)
  List<MemberResRef> mr;

  /**
   * both a group and its group type have configrations.
   *
   * <p>A group can inherit its group type's configrations. In that case, the group and its group
   * type share the same value unless the group overwrites the configration with its own value.
   *
   * <p>Basically a. if the group and its group type have the same configration, always use the
   * value of group. b. if only the group type has the configration, it means the group inherits the
   * configration and the value of group type should be returned.
   */
  @Field(ConstDef.F_CONFIGS)
  @JsonProperty(ConstDef.F_CONFIGS)
  List<ConfigurationEntity> cs;

  @Field(ConstDef.F_ATTRS)
  @JsonProperty(ConstDef.F_ATTRS)
  List<AttributeEntity> as;

  @Field(ConstDef.F_DATASOURCES)
  @JsonProperty(ConstDef.F_DATASOURCES)
  @CheckDs(value = Group.class, message = "all dsn should be unique",
      groups = {CreateValidator.class, UpdateValidator.class})
  @Valid
  List<DataSourceEntity> dss;

  @Field(ConstDef.F_GTDETAIL)
  @JsonIgnore
  // aggregate $lookup result
  GroupType gtdtl;

  // null: initilization
  // []: clean
  // [...]: normal
  // null: init
  public Group() {
    // has to be a string instead of a objectId
    this.n = UUID.randomUUID().toString();
  }

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

  public Group setN(String n) {
    this.n = n;
    return this;
  }

  public String getDpn() {
    return dpn;
  }

  public Group setDpn(String dpn) {
    this.dpn = dpn;
    return this;
  }

  public String getGtdpn() {
    return gtdpn;
  }

  public void setGtdpn(String gtdpn) {
    this.gtdpn = gtdpn;
  }

  public String getD() {
    return d;
  }

  public void setD(String d) {
    this.d = d;
  }

  public String getGt() {
    return gt;
  }

  public Group setGt(String gt) {
    this.gt = gt;
    return this;
  }

  public List<AttributeEntity> getAs() {
    return as;
  }

  public void setAs(List<AttributeEntity> as) {
    this.as = as;
  }

  public List<ConfigurationEntity> getCs() {
    return cs;
  }

  public void setCs(List<ConfigurationEntity> cs) {
    this.cs = cs;
  }

  public List<DataSourceEntity> getDss() {
    return dss;
  }

  public void setDss(List<DataSourceEntity> dss) {
    this.dss = dss;
  }

  public List<String> getMd() {
    return md;
  }

  public void setMd(List<String> md) {
    this.md = md;
  }

  public List<MemberResRef> getMr() {
    return mr;
  }

  public void setMr(List<MemberResRef> mr) {
    this.mr = mr;
  }

  public GroupType getGtdtl() {
    return gtdtl;
  }

  public void setGtdtl(GroupType gtdtl) {
    this.gtdtl = gtdtl;
  }

  public String getPrj() {
    return prj;
  }

  public Group setPrj(String project) {
    this.prj = project;
    return this;
  }

  public AttributeEntity getAttrByName(String name) {
    return findByName(this.as, a -> a.getAn().equals(name));
  }

  // AttributeEntity is "cn" based
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
    Objects.requireNonNull(names);

    Optional.ofNullable(this.as)
            .ifPresent(asList -> names.forEach(toRemove -> removeByName(asList,
                                                                        a -> a.getAn()
                                                                              .equals(toRemove))));
  }

  public ConfigurationEntity getCfgByName(String name) {
    return findByName(this.cs, c -> c.getCn().equals(name));
  }

  // ConfigurationEntity is "cn" based
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
    Objects.requireNonNull(names);

    Optional.ofNullable(this.cs)
            .ifPresent(csList -> names.forEach(toRemove -> removeByName(csList,
                                                                        c -> c.getCn()
                                                                              .equals(toRemove))));
  }

  public void insertOrUpdateMds(String devId) {
    this.md = (List<String>) BaseUtil.insertOrUpdate(this.md, devId);
  }

  public void insertOrUpdateMds(List<String> devIdList) {
    Optional.ofNullable(devIdList).ifPresent(devIds -> devIds.forEach(this::insertOrUpdateMds));
  }

  public void replaceOrClearMds(List<String> mdList) {
    this.md = (List<String>) BaseUtil.replaceOrClear(mdList, this.md);
  }

  public void removeItemFromMd(String devId) {
    Optional.ofNullable(this.md).ifPresent(list -> list.removeIf(device -> device.equals(devId)));
  }

  public void removeItemsFromMd(List<String> devIdList) {
    Optional.ofNullable(this.md)
            .ifPresent(mdList -> Optional.ofNullable(devIdList).ifPresent(mdList::removeAll));
  }

  public void insertOrUpdateMrs(MemberResRef resRef) {
    this.mr = (List<MemberResRef>) BaseUtil.insertOrUpdate(this.mr, resRef);
  }

  public void insertOrUpdateMrs(List<MemberResRef> resList) {
    Optional.ofNullable(resList).ifPresent(reses -> reses.forEach(this::insertOrUpdateMrs));
  }

  public void replaceOrClearMrs(List<MemberResRef> mrList) {
    this.mr = (List<MemberResRef>) BaseUtil.replaceOrClear(mrList, this.mr);
  }

  public void removeItemFromMr(MemberResRef resRef) {
    Optional.ofNullable(this.mr)
            .ifPresent(mrList -> mrList.removeIf(resource -> resource.equals(resRef)));
  }

  public void removeItemsFromMr(List<MemberResRef> resList) {
    Optional.ofNullable(this.mr)
            .ifPresent(mrList -> Optional.ofNullable(resList)
                                         .ifPresent(reses -> reses.forEach(this::removeItemFromMr)));
  }

  public DataSourceEntity getDsByName(String name) {
    return findByName(this.dss, ds -> ds.getDsn().equals(name));
  }

  public void insertOrUpdateDss(DataSourceEntity ds) {
    this.dss = (List<DataSourceEntity>) BaseUtil.insertOrUpdate(this.dss, ds);
  }

  public void replaceOrClearDss(List<DataSourceEntity> dsList) {
    this.dss = (List<DataSourceEntity>) BaseUtil.replaceOrClear(dsList, this.dss);
  }

  public static List<String> allFields() {
    return Arrays.asList(new String[] {ConstDef.F_ID, ConstDef.F_DISPLAYNAME,
        ConstDef.F_DESCRIPTION, ConstDef.F_GRPTYPE, ConstDef.F_ATTRS, ConstDef.F_CONFIGS,
        ConstDef.F_DATASOURCES, ConstDef.F_MBR, ConstDef.F_PROJECT});
  }

  @JsonInclude(Include.NON_NULL)
  public static class MemberResRef {
    @Field(ConstDef.F_DEVID)
    @JsonProperty(ConstDef.F_DEVID)
    String di;

    @Field(ConstDef.F_RESURI)
    @JsonProperty(ConstDef.F_RESURI)
    String uri;

    @JsonCreator
    public MemberResRef(@JsonProperty(ConstDef.F_DEVID) String di,
        @JsonProperty(ConstDef.F_RESURI) String uri) {
      this.di = di;
      this.uri = uri;
    }

    public String getDi() {
      return di;
    }

    public void setDi(String di) {
      this.di = di;
    }

    public String getUri() {
      return uri;
    }

    public void setUri(String uri) {
      this.uri = uri;
    }

    @Override
    public String toString() {
      return "MemberResRef [di=" + this.di + ", resuri=" + this.uri + "]";
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((di == null) ? 0 : di.hashCode());
      result = prime * result + ((uri == null) ? 0 : uri.hashCode());
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
      MemberResRef other = (MemberResRef) obj;
      if (di == null) {
        if (other.di != null)
          return false;
      } else if (!this.di.equals(other.di))
        return false;
      if (this.uri == null) {
        if (other.uri != null)
          return false;
      } else if (!uri.equals(other.uri))
        return false;
      return true;
    }
  }

  @Override
  public String toString() {
    return "Group [n=" + n + ", dpn=" + dpn + ", d=" + d + ", prj=" + prj + ", gtdpn=" + gtdpn
        + ", gt=" + gt + ", md=" + md + ", mr=" + mr + ", cs=" + cs + ", as=" + as + ", dss=" + dss
        + ", gtdtl=" + gtdtl + "]";
  }
}
