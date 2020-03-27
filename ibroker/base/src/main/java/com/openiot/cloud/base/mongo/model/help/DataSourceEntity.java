/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model.help;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.validator.CreateValidator;
import com.openiot.cloud.base.mongo.model.validator.UpdateValidator;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Field;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.*;
import java.util.stream.Collectors;

@JsonInclude(Include.NON_EMPTY)
public class DataSourceEntity {
  @Field(ConstDef.F_DATASOURCENAME)
  @JsonProperty(ConstDef.F_DATASOURCENAME)
  private String dsn;

  @Field(ConstDef.F_DATASOURCETYPE)
  @JsonProperty(ConstDef.F_DATASOURCETYPE)
  @Pattern(regexp = ConstDef.F_DATASOURCETABLE + "|" + ConstDef.F_DATASOURCEREF,
      groups = {CreateValidator.class, UpdateValidator.class})
  private String dst;

  /** it is an internal table id. to keep relative data */
  @Field(ConstDef.F_DATASOURCEINTLID)
  @JsonIgnore
  private String dsintId;

  @Field(ConstDef.F_DATASOURCEDEFS)
  @JsonProperty(ConstDef.F_DATASOURCEDEFS)
  private List<Reference> dsdefs;

  // TODO: may not necessary for a group data source. just for a group type data source template
  @Field(ConstDef.F_REFERRULE)
  @JsonProperty(ConstDef.F_REFERRULE)
  private ResAndResProID defRule;

  @Field(ConstDef.F_TITLE)
  @JsonProperty(ConstDef.F_TITLE)
  private String title;

  @Field(ConstDef.F_CLASS)
  @JsonProperty(ConstDef.F_CLASS)
  private String classInfo;

  @Field(ConstDef.F_DESCRIPTION)
  @JsonProperty(ConstDef.F_DESCRIPTION)
  private String description;

  @Field(ConstDef.F_UNIT)
  @JsonProperty(ConstDef.F_UNIT)
  private String unit;

  @Field(ConstDef.F_THRESHOLD_HIGH)
  @JsonProperty(ConstDef.F_THRESHOLD_HIGH)
  private Long threshHigh;

  @Field(ConstDef.F_THRESHOLD_LOW)
  @JsonProperty(ConstDef.F_THRESHOLD_LOW)
  private Long threshLow;

  @Field(ConstDef.F_REPORT_INTERVAL)
  @JsonProperty(ConstDef.F_REPORT_INTERVAL)
  private Long interval;

  @Field(ConstDef.F_ATTRS)
  @JsonProperty(ConstDef.F_ATTRS)
  private List<AttributeEntity> attributeList;

  @Field(ConstDef.F_OPERATE)
  @JsonProperty(ConstDef.F_OPERATE)
  private OperateEntity operate;

  public DataSourceEntity(String dsn, String dst, List<Reference> dsdefs) {
    this();
    setDsn(dsn);
    setDst(dst);
    setDsdefs(dsdefs);
  }

  public DataSourceEntity(String dsn, String dst) {
    this();
    setDsn(dsn);
    setDst(dst);
  }

  public DataSourceEntity() {
    this.dsintId = new ObjectId().toHexString();
  }

  public String getDsn() {
    return dsn;
  }

  public void setDsn(String dsn) {
    this.dsn = dsn;
  }

  public String getDst() {
    return dst;
  }

  public void setDst(String dst) {
    this.dst = dst;
  }

  public String getDsintId() {
    return dsintId;
  }

  public void setDsintId(String dsintId) {
    this.dsintId = dsintId;
  }

  public List<Reference> getDsdefs() {
    return dsdefs;
  }

  @JsonIgnore
  public Reference getLatestReference() {
    return this.dsdefs == null || this.dsdefs.isEmpty() ? null
        : this.dsdefs.get(this.dsdefs.size() - 1);
  }

  @JsonIgnore
  public List<Reference> getReferenceByTimeBetween(long from, long to) {
    if (isDssTypeTable()) {
      return Collections.emptyList();
    }

    if (this.dsdefs == null) {
      return Collections.emptyList();
    }

    long fromAdjust = from == 0 ? BaseUtil.getStartOfTodayAsEpochMillis() : from;
    long toAdjust = to == 0 ? BaseUtil.getNowAsEpochMillis() : to;

    return this.dsdefs.stream()
                      .filter(reference -> Objects.nonNull(reference.getDsri()))
                      .filter(referenceDataSource -> referenceDataSource.dsrf <= toAdjust
                          && (referenceDataSource.dsrt == 0 ? Long.MAX_VALUE
                              : referenceDataSource.dsrt) >= fromAdjust)
                      .collect(Collectors.toList());
  }

  public void setDsdefs(List<Reference> dsdefs) {
    if (dsdefs == null || dsdefs.isEmpty()) {
      this.dsdefs = null;
    } else {
      if (this.dsdefs == null) {
        this.dsdefs = new LinkedList<>();
      } else {
        this.dsdefs.clear();
      }
      this.dsdefs.addAll(dsdefs);
    }
  }

  // TODO: should move it to service layer
  @JsonIgnore
  public void setDsdefItem(Reference dsdef) {
    if (this.dsdefs == null) {
      this.dsdefs = new LinkedList<>();
    }

    Reference latestRefernce = getLatestReference();
    if (latestRefernce != null) {
      if (latestRefernce.getDsrt() == 0) {
        latestRefernce.setDsrt(BaseUtil.getNowAsEpochMillis());
      }
    }

    dsdef.verify();
    this.dsdefs.add(dsdef);
  }

  public ResAndResProID getDefRule() {
    return defRule;
  }

  public void setDefRule(ResAndResProID defRule) {
    this.defRule = defRule;
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

  public Long getThreshHigh() {
    return threshHigh;
  }

  public void setThreshHigh(Long threshHigh) {
    this.threshHigh = threshHigh;
  }

  public Long getThreshLow() {
    return threshLow;
  }

  public void setThreshLow(Long threshLow) {
    this.threshLow = threshLow;
  }

  public Long getInterval() {
    return interval;
  }

  public void setInterval(Long interval) {
    this.interval = interval;
  }

  public List<AttributeEntity> getAttributeList() {
    return attributeList;
  }

  public void setAttributeList(List<AttributeEntity> attributeList) {
    if (attributeList == null || attributeList.isEmpty()) {
      this.attributeList = null;
    } else {
      if (this.attributeList == null) {
        this.attributeList = new LinkedList<>();
      } else {
        this.attributeList.clear();
      }
      this.attributeList.addAll(attributeList);
    }
  }

  public void addDsAttrItem(AttributeEntity attribute) {
    this.attributeList = this.attributeList == null ? new LinkedList<>() : this.attributeList;

    int index = this.attributeList.indexOf(attribute);
    if (index > -1) {
      this.attributeList.set(index, attribute);
    } else {
      this.attributeList.add(attribute);
    }
  }

  public OperateEntity getOperate() {
    return operate;
  }

  public void setOperate(OperateEntity operate) {
    this.operate = operate;
  }

  @Override
  public String toString() {
    return "DataSourceEntity{" + "dsn='" + dsn + '\'' + ", dst='" + dst + '\'' + ", dsintId='"
        + dsintId + '\'' + ", dsdefs=" + dsdefs + ", defRule=" + defRule + ", title='" + title
        + '\'' + ", classInfo='" + classInfo + '\'' + ", description='" + description + '\''
        + ", unit='" + unit + '\'' + ", threshHigh=" + threshHigh + ", threshLow=" + threshLow
        + ", interval=" + interval + ", attributeList=" + attributeList + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    DataSourceEntity that = (DataSourceEntity) o;
    return Objects.equals(dsn, that.dsn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dsn);
  }

  @JsonIgnore
  public boolean isDssTypeTable() {
    return this.dst.equals(ConstDef.F_DATASOURCETABLE);
  }

  @JsonIgnore
  public boolean isDssTypeRef() {
    return this.dst.equals(ConstDef.F_DATASOURCEREF);
  }

  @JsonInclude(Include.NON_EMPTY)
  public static class Reference {
    @Field(ConstDef.F_DATASOURCEREFNAME)
    @JsonIgnore
    private String dsrurl;

    @Field(ConstDef.F_DSRESPROPGLOBALID)
    @JsonProperty(ConstDef.F_DSRESPROPGLOBALID)
    @NotNull(groups = {CreateValidator.class})
    @Valid
    private ResAndResProID dsri;

    @Field(ConstDef.F_DATASOURCEREFFROM)
    @JsonProperty(ConstDef.F_DATASOURCEREFFROM)
    private long dsrf;

    @Field(ConstDef.F_DATASOURCEREFTO)
    @JsonProperty(ConstDef.F_DATASOURCEREFTO)
    private long dsrt;

    public Reference() {}

    public Reference(String di, String res, String pt, long from, long to) {
      this.dsri = new ResAndResProID(di, res, pt);
      this.dsrf = from;
      this.dsrt = to;
    }

    public String getDsrurl() {
      this.dsrurl =
          Optional.ofNullable(this.dsrurl)
                  .orElse(BaseUtil.formAFullUrl(dsri.getDi(), dsri.getResUri(), dsri.getPt()));
      return this.dsrurl;
    }

    public void setDsrurl(String dsrurl) {
      this.dsrurl = dsrurl;
    }

    public ResAndResProID getDsri() {
      return dsri;
    }

    public void setDsri(ResAndResProID dsri) {
      this.dsri = dsri;
      this.dsrurl = BaseUtil.formAFullUrl(dsri.getDi(), dsri.getResUri(), dsri.getPt());
    }

    public long getDsrt() {
      return dsrt;
    }

    public void setDsrt(long dsrt) {
      if (dsrt != 0 && this.dsrf != 0) {
        this.dsrt = Math.max(this.dsrf, dsrt);
      } else {
        this.dsrt = dsrt;
      }
    }

    public long getDsrf() {
      return dsrf;
    }

    public void setDsrf(long dsrf) {
      // make sure dsrf <= dsrt
      if (dsrf != 0 && this.dsrt != 0) {
        this.dsrf = Math.min(this.dsrt, dsrf);
      } else {
        this.dsrf = dsrf;
      }
    }

    // TODO: move to service layer
    public void verify() {
      this.dsrf = this.dsrf == 0 ? BaseUtil.getNowAsEpochMillis() : this.dsrf;
    }

    @Override
    public String toString() {
      return "Reference{" + "dsrurl='" + dsrurl + '\'' + ", dsri=" + dsri + ", dsrf=" + dsrf
          + ", dsrt=" + dsrt + '}';
    }
  }

  @JsonInclude(Include.NON_NULL)
  public static class DataEntity {
    @JsonProperty(ConstDef.F_TIME)
    long time;

    @JsonProperty(ConstDef.F_DATA)
    Object data;

    public Object getData() {
      return data;
    }

    public void setData(Object data) {
      this.data = data;
    }

    public long getTime() {
      return time;
    }

    public void setTime(long time) {
      this.time = time;
    }

    @Override
    public String toString() {
      return "DataEntity{" + "time=" + time + ", data=" + data + '}';
    }
  }

  @JsonInclude(Include.NON_NULL)
  public static class OperateEntity {
    @JsonProperty(ConstDef.F_OP_TYPE)
    String type;

    @JsonProperty(ConstDef.F_OP_BG_STATE)
    String background_state;

    @JsonProperty(ConstDef.F_OP_DI)
    String di;

    @JsonProperty(ConstDef.F_OP_URL)
    String url;

    @JsonProperty(ConstDef.F_OP_PN)
    String pn;

    @JsonProperty(ConstDef.F_OP_SCHD)
    String sched;

    @JsonProperty(ConstDef.F_OP_ST_CMDS)
    String state_cmds;

    @JsonProperty(ConstDef.F_OP_REPEAT)
    Integer repeat;

    @JsonCreator
    public OperateEntity() {}

    public OperateEntity(String type, String background_state, String di, String url, String pn,
        String sched, String state_cmds, Integer repeat) {
      this.type = type == null || type.isEmpty() ? null : type;
      this.background_state =
          background_state == null || background_state.isEmpty() ? null : background_state;
      this.di = di == null || di.isEmpty() ? null : di;
      this.url = url == null || url.isEmpty() ? null : url;
      this.pn = pn == null || pn.isEmpty() ? null : pn;
      this.sched = sched == null || sched.isEmpty() ? null : sched;
      this.state_cmds = state_cmds == null || state_cmds.isEmpty() ? null : state_cmds;
      this.repeat = repeat;
    }

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

    @Override
    public String toString() {
      return "OperateEntity{" + "type='" + type + '\'' + ", background_state='" + background_state
          + '\'' + ", di='" + di + '\'' + ", url='" + url + '\'' + ", pn='" + pn + '\''
          + ", sched='" + sched + '\'' + ", state_cmds='" + state_cmds + '\'' + ", repeat=" + repeat
          + '}';
    }
  }
}
