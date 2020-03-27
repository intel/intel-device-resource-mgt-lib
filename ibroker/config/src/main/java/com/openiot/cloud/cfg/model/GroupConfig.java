/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.cfg.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.mongo.model.Group;
import com.openiot.cloud.base.mongo.model.Group.MemberResRef;
import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import com.openiot.cloud.base.mongo.model.help.DataSourceEntity;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

// @formatter:off
// {
//  "cs": {
//    "d": "",
//    "n": "",
//    "s": ""
//  },
//  "ct": "",
//  "d": "",
//  "dpn": "",
//  "dss": {
//    "as": "",
//    "c": "",
//    "d": {
//      "d": "",
//      "pn": "",
//      "s": "",
//      "url": ""
//    },
//    "dscp": "",
//    "n": "",
//    "t": "",
//    "ttle": "",
//    "t_l": "",
//    "t_h": "",
//    "u": "",
//  },
//  "gt": "",
//  "md": [
//    "devId1",
//    "devId2"
//  ],
//  "mr": {
//    "di": "xxx",
//    "res": "xxx/xxx/xxx"
//  },
//  "n": "",
//  "project": ""
// }
// @formatter:on

@JsonInclude(Include.NON_EMPTY)
public class GroupConfig {
  @JsonInclude(Include.NON_EMPTY)
  public static class MemberRes {
    @JsonProperty("di")
    String di;

    @JsonProperty("res")
    String resUri;

    public String getDi() {
      return di;
    }

    public void setDi(String di) {
      this.di = di;
    }

    public String getResUri() {
      return resUri;
    }

    public void setResUri(String resUri) {
      this.resUri = resUri;
    }

    @Override
    public String toString() {
      return "MemberRes [di=" + di + ", resUri=" + resUri + "]";
    }

    public static MemberRes from(Optional<MemberResRef> mbrRes) {
      return mbrRes.map(res -> {
        MemberRes m = new MemberRes();

        Optional.ofNullable(res.getDi()).ifPresent(di -> m.setDi(di));
        Optional.ofNullable(res.getUri()).ifPresent(uri -> m.setResUri(uri));

        return m;
      }).orElse(null);
    }
  }

  @JsonInclude(Include.NON_EMPTY)
  public static class Configuration {
    @JsonProperty("n")
    String n;

    @JsonProperty("d")
    String d;

    @JsonProperty("s")
    String s;

    public String getN() {
      return n;
    }

    public void setN(String n) {
      this.n = n;
    }

    public String getD() {
      return d;
    }

    public void setD(String d) {
      this.d = d;
    }

    public String getS() {
      return s;
    }

    public void setS(String s) {
      this.s = s;
    }

    @Override
    public String toString() {
      return "Configuration [n=" + n + ", d=" + d + ", s=" + s + "]";
    }

    public static Configuration from(Optional<ConfigurationEntity> ce) {
      return ce.map(entity -> {
        Configuration c = new Configuration();
        Optional.ofNullable(entity.getCn()).ifPresent(c::setN);
        Optional.ofNullable(entity.getCv()).ifPresent(c::setD);
        Optional.ofNullable(entity.getCts())
                .map(createTime -> Long.toString(createTime))
                .ifPresent(c::setS);
        return c;
      }).orElse(null);
    }
  }

  @JsonInclude(Include.NON_EMPTY)
  public static class DataSource {
    @JsonProperty("n")
    String n;

    @JsonProperty("t")
    String t;

    @JsonProperty("ttle")
    String title;

    @JsonProperty("c")
    String classInfo;

    @JsonProperty("dscp")
    String description;

    @JsonProperty("unit")
    String unit;

    @JsonProperty("t_l")
    Long threshLow;

    @JsonProperty("t_h")
    Long threshHigh;

    @JsonProperty("d")
    DssReference d;

    @JsonProperty("freq")
    Long interval;

    @JsonProperty("operate")
    Operate operate;

    @JsonProperty("as")
    List<AttributeEntity> attributeList;

    public String getN() {
      return n;
    }

    public void setN(String n) {
      this.n = n;
    }

    public String getT() {
      return t;
    }

    public void setT(String t) {
      this.t = t;
    }

    public DssReference getD() {
      return d;
    }

    public void setD(DssReference d) {
      this.d = d;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getClassInfo() {
      return classInfo;
    }

    public void setClassInfo(String classInfo) {
      this.classInfo = classInfo;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getUnit() {
      return unit;
    }

    public void setUnit(String unit) {
      this.unit = unit;
    }

    public Long getThreshLow() {
      return threshLow;
    }

    public void setThreshLow(Long threshLow) {
      this.threshLow = threshLow;
    }

    public Long getThreshHigh() {
      return threshHigh;
    }

    public void setThreshHigh(Long threshHigh) {
      this.threshHigh = threshHigh;
    }

    public Long getInterval() {
      return interval;
    }

    public void setInterval(Long interval) {
      this.interval = interval;
    }

    public Operate getOperate() {
      return operate;
    }

    public void setOperate(Operate operate) {
      this.operate = operate;
    }

    public List<AttributeEntity> getAttributeList() {
      return attributeList;
    }

    public void setAttributeList(List<AttributeEntity> attributeList) {
      this.attributeList = attributeList;
    }

    @Override
    public String toString() {
      return "DataSource{" + "n='" + n + '\'' + ", t='" + t + '\'' + ", title='" + title + '\''
          + ", classInfo='" + classInfo + '\'' + ", description='" + description + '\'' + ", unit='"
          + unit + '\'' + ", threshLow=" + threshLow + ", threshHigh=" + threshHigh + ", d=" + d
          + ", interval=" + interval + ", operate=" + operate + ", attributeList=" + attributeList
          + '}';
    }

    public static DataSource from(Optional<DataSourceEntity> de) {
      return de.map(entity -> {
        DataSource forConfig = new DataSource();

        Optional.ofNullable(entity.getAttributeList())
                .ifPresent(attributeList -> forConfig.setAttributeList(attributeList));
        Optional.ofNullable(entity.getClassInfo())
                .ifPresent(classInfo -> forConfig.setClassInfo(classInfo));
        forConfig.setD(DssReference.from(Optional.ofNullable(entity.getLatestReference())));
        Optional.ofNullable(entity.getDescription())
                .ifPresent(description -> forConfig.setDescription(description));
        Optional.ofNullable(entity.getDsn()).ifPresent(dsn -> forConfig.setN(dsn));
        Optional.ofNullable(entity.getDst()).ifPresent(dst -> forConfig.setT(dst));
        Optional.ofNullable(entity.getTitle()).ifPresent(title -> forConfig.setTitle(title));
        Optional.ofNullable(entity.getThreshLow())
                .ifPresent(threshLow -> forConfig.setThreshLow(threshLow));
        Optional.ofNullable(entity.getThreshHigh())
                .ifPresent(threshHigh -> forConfig.setThreshHigh(threshHigh));
        Optional.ofNullable(entity.getUnit()).ifPresent(unit -> forConfig.setUnit(unit));
        Optional.ofNullable(entity.getInterval())
                .ifPresent(interval -> forConfig.setInterval(interval));
        Optional.ofNullable(entity.getOperate())
                .ifPresent(op -> forConfig.setOperate(Operate.from(Optional.ofNullable(op))));
        return forConfig;
      }).orElse(null);
    }
  }

  @JsonInclude(Include.NON_EMPTY)
  public static class DssReference {
    @JsonProperty("s")
    long s;

    @JsonProperty("url")
    String url;

    @JsonProperty("d")
    String d;

    @JsonProperty("pn")
    String pn;

    public long getS() {
      return s;
    }

    public static DssReference from(Optional<DataSourceEntity.Reference> reference) {
      return reference.map(DataSourceEntity.Reference::getDsri).map(definition -> {
        DssReference forConfig = new DssReference();
        forConfig.setD(definition.getDi());
        forConfig.setUrl(definition.getResUri());
        forConfig.setPn(definition.getPt());
        forConfig.setS(reference.get().getDsrf());
        return forConfig;
      }).orElse(null);
    }

    public void setS(long s) {
      this.s = s;
    }

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public String getD() {
      return d;
    }

    public void setD(String d) {
      this.d = d;
    }

    public String getPn() {
      return pn;
    }

    public void setPn(String pn) {
      this.pn = pn;
    }
  }

  @JsonInclude(Include.NON_EMPTY)
  public static class Operate {
    String type;
    String background_state;
    String di;
    String url;
    String pn;
    String sched;
    String state_cmds;
    Integer repeat;

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getBackground_state() {
      return background_state;
    }

    public void setBackground_state(String background_state) {
      this.background_state = background_state;
    }

    public String getDi() {
      return di;
    }

    public void setDi(String di) {
      this.di = di;
    }

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public String getPn() {
      return pn;
    }

    public void setPn(String pn) {
      this.pn = pn;
    }

    public String getSched() {
      return sched;
    }

    public void setSched(String sched) {
      this.sched = sched;
    }

    public String getState_cmds() {
      return state_cmds;
    }

    public void setState_cmds(String state_cmds) {
      this.state_cmds = state_cmds;
    }

    public Integer getRepeat() {
      return repeat;
    }

    public void setRepeat(Integer repeat) {
      this.repeat = repeat;
    }

    public static Operate from(Optional<DataSourceEntity.OperateEntity> opr) {
      return opr.map(op -> {
        Operate operate = new Operate();
        operate.setBackground_state(op.getBackground_state());
        operate.setDi(op.getDi());
        operate.setPn(op.getPn());
        operate.setUrl(op.getUrl());
        operate.setSched(op.getSched());
        operate.setState_cmds(op.getState_cmds());
        operate.setType(op.getType());
        operate.setRepeat(op.getRepeat());
        return operate;
      }).orElse(null);
    }

    @Override
    public String toString() {
      return "Operate{" + "type='" + type + '\'' + ", background_state='" + background_state + '\''
          + ", di='" + di + '\'' + ", url='" + url + '\'' + ", pn='" + pn + '\'' + ", sched='"
          + sched + '\'' + ", state_cmds='" + state_cmds + '\'' + ", repeat=" + repeat + '}';
    }
  }

  @JsonProperty("n")
  String n;

  @JsonProperty("ct")
  long ct;

  @JsonProperty("d")
  String d;

  @JsonProperty("gt")
  String gt;

  @JsonProperty("mr")
  List<MemberRes> mr;

  @JsonProperty("md")
  List<String> md;

  @JsonProperty("cs")
  List<Configuration> cs;

  @JsonProperty("dss")
  List<DataSource> dss;

  @JsonProperty("dpn")
  String dpn;

  @JsonProperty("prj")
  String project;

  public GroupConfig() {
    this.ct = BaseUtil.getNowAsEpochMillis();
  }

  public String getN() {
    return n;
  }

  public void setN(String n) {
    this.n = n;
  }

  public long getCt() {
    return ct;
  }

  public void setCt(long ct) {
    this.ct = ct;
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

  public void setGt(String gt) {
    this.gt = gt;
  }

  public List<MemberRes> getMr() {
    return mr;
  }

  public void setMr(List<MemberRes> mr) {
    this.mr = mr;
  }

  public void setMrItem(MemberRes m) {
    if (this.mr == null) {
      this.mr = new LinkedList<>();
    }
    this.mr.add(m);
  }

  public List<String> getMd() {
    return md;
  }

  public void setMd(List<String> md) {
    this.md = md;
  }

  public List<Configuration> getCs() {
    return cs;
  }

  public void setCs(List<Configuration> cs) {
    this.cs = cs;
  }

  public void setCsItem(Configuration c) {
    if (this.cs == null) {
      this.cs = new LinkedList<>();
    }
    this.cs.add(c);
  }

  public List<DataSource> getDss() {
    return dss;
  }

  public void setDss(List<DataSource> ds) {
    this.dss = ds;
  }

  public void setDssItem(DataSource d) {
    if (this.dss == null) {
      this.dss = new LinkedList<>();
    }
    this.dss.add(d);
  }

  public String getDpn() {
    return dpn;
  }

  public void setDpn(String dpn) {
    this.dpn = dpn;
  }

  public String getProject() {
    return project;
  }

  public void setProject(String project) {
    this.project = project;
  }

  @Override
  public String toString() {
    return "GroupCfg [n=" + n + ", d=" + d + ", gt=" + gt + ", mr=" + mr + ", md=" + md + ", cs="
        + cs + ", ds=" + dss + "]";
  }

  public static GroupConfig from(Optional<Group> group) {
    return group.map(g -> {
      GroupConfig groupConfig = new GroupConfig();

      groupConfig.setN(g.getN());
      groupConfig.setD(g.getD());
      groupConfig.setGt(g.getGt());
      groupConfig.setDpn(g.getDpn());
      groupConfig.setMd(g.getMd());
      groupConfig.setProject(g.getPrj());

      Optional.ofNullable(g.getMr())
              .ifPresent(mr -> mr.forEach(m -> groupConfig.setMrItem(MemberRes.from(Optional.of(m)))));

      Optional.ofNullable(g.getCs())
              .ifPresent(cs -> cs.forEach(c -> groupConfig.setCsItem(Configuration.from(Optional.of(c)))));

      Optional.ofNullable(g.getDss())
              .ifPresent(dss -> dss.forEach(ds -> groupConfig.setDssItem(DataSource.from(Optional.of(ds)))));

      return groupConfig;
    }).orElse(null);
  }

  public String toJsonString() {
    try {
      return new ObjectMapper().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return null;
    }
  }
}
