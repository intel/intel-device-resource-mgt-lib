/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.common.model.StatsType;
import com.openiot.cloud.base.help.ConstDef;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(Include.NON_NULL)
public class RawAggregationData {

  @JsonProperty(ConstDef.F_COUNT)
  Integer cnt;

  @JsonProperty(ConstDef.F_RANGECOUNT)
  Integer rngc;

  @JsonProperty(ConstDef.F_MIN)
  Double min;

  @JsonProperty(ConstDef.F_MAX)
  Double max;

  @JsonProperty(ConstDef.F_AVG)
  Double avg;

  @JsonProperty(ConstDef.F_SUM)
  Double sum;

  @JsonProperty(ConstDef.F_SW)
  Integer sw;

  @JsonProperty(ConstDef.F_DUR)
  Long dur;

  @JsonProperty(ConstDef.F_DURMAX)
  Long durMax;

  @JsonProperty(ConstDef.F_DURMIN)
  Long durMin;

  @JsonProperty(ConstDef.F_DURAVG)
  Double durAvg;

  public Integer getCnt() {
    return cnt;
  }

  public void setCnt(Integer cnt) {
    this.cnt = cnt;
  }

  public Integer getRngc() {
    return rngc;
  }

  public void setRngc(Integer rngc) {
    this.rngc = rngc;
  }

  public Double getMin() {
    return min;
  }

  public void setMin(Double min) {
    this.min = min;
  }

  public Double getMax() {
    return max;
  }

  public void setMax(Double max) {
    this.max = max;
  }

  public Double getAvg() {
    return avg;
  }

  public void setAvg(Double avg) {
    this.avg = avg;
  }

  public Double getSum() {
    return sum;
  }

  public void setSum(Double sum) {
    this.sum = sum;
  }

  public Integer getSw() {
    return sw;
  }

  public void setSw(Integer sw) {
    this.sw = sw;
  }

  public Long getDur() {
    return dur;
  }

  public void setDur(Long dur) {
    this.dur = dur;
  }

  public Long getDurMax() {
    return durMax;
  }

  public void setDurMax(Long durMax) {
    this.durMax = durMax;
  }

  public Long getDurMin() {
    return durMin;
  }

  public void setDurMin(Long durMin) {
    this.durMin = durMin;
  }

  public Double getDurAvg() {
    return durAvg;
  }

  public void setDurAvg(Double durAvg) {
    this.durAvg = durAvg;
  }

  public boolean hasData() {
    if (cnt == null && rngc == null && min == null && max == null && avg == null && sum == null
        && sw == null && dur == null && durMax == null && durMin == null && durAvg == null) {
      return false;
    }

    return true;
  }

  public List<Data> serialize() {
    if (cnt == null && rngc == null && min == null && max == null && avg == null && sum == null
        && sw == null && dur == null && durMax == null && durMin == null && durAvg == null) {
      return null;
    }

    List<Data> ds = new ArrayList<>();

    if (cnt != null) {
      Data d = new Data();
      d.setType(StatsType.COUNT.toString());
      d.setValue(cnt.toString());
      ds.add(d);
    }

    if (rngc != null) {
      Data d = new Data();
      d.setType(StatsType.RANGECOUNT.toString());
      d.setValue(rngc.toString());
      ds.add(d);
    }

    if (max != null) {
      Data d = new Data();
      d.setType(StatsType.MAX.toString());
      d.setValue(max.toString());
      ds.add(d);
    }

    if (min != null) {
      Data d = new Data();
      d.setType(StatsType.MIN.toString());
      d.setValue(min.toString());
      ds.add(d);
    }

    if (avg != null) {
      Data d = new Data();
      d.setType(StatsType.AVG.toString());
      d.setValue(avg.toString());
      ds.add(d);
    }

    if (sum != null) {
      Data d = new Data();
      d.setType(StatsType.SUM.toString());
      d.setValue(sum.toString());
      ds.add(d);
    }

    if (dur != null) {
      Data d = new Data();
      d.setType(StatsType.DURATION.toString());
      d.setValue(dur.toString());
      ds.add(d);
    }

    if (durMax != null) {
      Data d = new Data();
      d.setType(StatsType.DURMAX.toString());
      d.setValue(durMax.toString());
      ds.add(d);
    }

    if (durMin != null) {
      Data d = new Data();
      d.setType(StatsType.DURMIN.toString());
      d.setValue(durMin.toString());
      ds.add(d);
    }

    if (durAvg != null) {
      Data d = new Data();
      d.setType(StatsType.DURAVG.toString());
      d.setValue(durAvg.toString());
      ds.add(d);
    }

    if (sw != null) {
      Data d = new Data();
      d.setType(StatsType.SWITCH.toString());
      d.setValue(sw.toString());
      ds.add(d);
    }

    return ds;
  }
}
