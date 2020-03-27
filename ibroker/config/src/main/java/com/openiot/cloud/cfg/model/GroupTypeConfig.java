/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.cfg.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.mongo.model.GroupType;
import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import com.openiot.cloud.base.mongo.model.help.DataSourceEntity;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/*
 * grouptype.cfg in a form likes: { "cs": { "d": "" "n": "" "s": "" }, "ct" : "", "dss": { "d": {
 * "d" : "", "t" : "" }, "n": "" "t": "" }, "n": "", "d": "" }
 */
@JsonInclude(Include.NON_EMPTY)
public class GroupTypeConfig {
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

        Optional.ofNullable(entity.getCn()).ifPresent(cn -> c.setN(cn));
        Optional.ofNullable(entity.getCv()).ifPresent(cv -> {
          c.setD(cv);
          c.setS("s");
        });

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

    @Override
    public String toString() {
      return "DataSource [n=" + n + ", t=" + t + "]";
    }

    public static DataSource from(Optional<DataSourceEntity> de) {
      return de.map(entity -> {
        DataSource ds = new DataSource();

        Optional.ofNullable(entity.getDsn()).ifPresent(dsn -> ds.setN(dsn));
        Optional.ofNullable(entity.getDst()).ifPresent(dst -> ds.setT(dst));

        return ds;
      }).orElse(null);
    }
  }

  @JsonProperty("n")
  String n;

  @JsonProperty("d")
  String d;

  @JsonProperty("cs")
  List<Configuration> cs;

  @JsonProperty("as")
  List<DataSource> dss;

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

  @Override
  public String toString() {
    return "GroupCfg [n=" + n + ", d=" + d + ", cs=" + cs + ", ds=" + dss + "]";
  }

  public static GroupTypeConfig from(Optional<GroupType> group) {
    return group.map(g -> {
      GroupTypeConfig groupTypeConfig = new GroupTypeConfig();

      groupTypeConfig.setN(g.getN());
      groupTypeConfig.setD(g.getD());

      Optional.ofNullable(g.getCs())
              .ifPresent(cs -> cs.forEach(c -> groupTypeConfig.setCsItem(Configuration.from(Optional.of(c)))));

      Optional.ofNullable(g.getDss())
              .ifPresent(dss -> dss.forEach(ds -> groupTypeConfig.setDssItem(DataSource.from(Optional.of(ds)))));

      return groupTypeConfig;
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
