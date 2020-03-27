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
import com.openiot.cloud.base.mongo.model.Device;
import com.openiot.cloud.base.mongo.model.Group;
import com.openiot.cloud.base.mongo.model.Group.MemberResRef;
import com.openiot.cloud.base.mongo.model.ResProperty;
import com.openiot.cloud.base.mongo.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/*
 * device.cfg in a form likes: { --> Device "attr": { "": "" }, "cfg": { "": "" }, "ct" : "", "di":
 * "", "dl": "", "grp": [ "", "" ], "links": [ --> Resource { "attr": { "": "" }, "cfg": { "": "" },
 * "dl": "", "grp": [ "", "" ], "href": "", "props": [ --> Property { "cfg" : { "": "" }, "n": "" },
 * ] } ], "rn": "" }
 */

@JsonInclude(Include.NON_EMPTY)
public class DeviceConfig {
  private static Logger logger = LoggerFactory.getLogger(DeviceConfig.class);

  @JsonInclude(Include.NON_EMPTY)
  public static class PropsEntity {
    @JsonProperty("cfg")
    Map<String, String> cfg;

    @JsonProperty("n")
    String name;

    public PropsEntity() {
      cfg = new HashMap<>();
    }

    public Map<String, String> getCfg() {
      return cfg;
    }

    public void setCfg(Map<String, String> pcfg) {
      this.cfg = pcfg;
    }

    public void setPcfg(String key, String value) {
      if (this.cfg == null) {
        this.cfg = new HashMap<>();
      }
      this.cfg.put(key, value);
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return "PropsEntity [pcfg=" + cfg + ", name=" + name + "]";
    }

    static PropsEntity from(Optional<ResProperty> resPro) {
      return resPro.map(p -> {
        PropsEntity pEntity = new PropsEntity();

        Optional.ofNullable(p.getName()).ifPresent(n -> pEntity.setName(n));
        Optional.ofNullable(p.getUserCfgs())
                .ifPresent(cfgs -> cfgs.forEach(cfg -> pEntity.setPcfg(cfg.getCn(), cfg.getCv())));
        return pEntity;
      }).orElse(null);
    }
  }

  @JsonInclude(Include.NON_EMPTY)
  public static class LinksEntity {
    @JsonProperty("attr")
    Map<String, String> attr;

    @JsonProperty("cfg")
    Map<String, String> cfg;

    @JsonProperty("href")
    String href;

    @JsonProperty("props")
    List<PropsEntity> props;

    @JsonProperty("grp")
    List<String> grp;

    @JsonProperty("dl")
    Integer dl;

    public LinksEntity() {
      attr = new HashMap<>();
      cfg = new HashMap<>();
      props = new LinkedList<>();
      grp = new LinkedList<>();
    }

    public Integer getDl() {
      return dl;
    }

    public void setDl(Integer dl) {
      this.dl = dl;
    }

    public Map<String, String> getAttr() {
      return attr;
    }

    public void setAttr(Map<String, String> attr) {
      this.attr = attr;
    }

    public void setAttr(String key, String value) {
      if (this.attr == null) {
        this.attr = new HashMap<>();
      }
      this.attr.put(key, value);
    }

    public String getHref() {
      return href;
    }

    public void setHref(String href) {
      this.href = href;
    }

    public List<String> getGrp() {
      return grp;
    }

    public void setGrp(List<String> grp) {
      this.grp = grp;
    }

    public void setGrpItem(String grpName) {
      if (this.grp == null) {
        this.grp = new LinkedList<>();
      }
      this.grp.add(grpName);
    }

    public Map<String, String> getCfg() {
      return cfg;
    }

    public void setCfg(Map<String, String> cfg) {
      this.cfg = cfg;
    }

    public void setCfg(String key, String value) {
      if (this.cfg == null) {
        this.cfg = new HashMap<>();
      }
      this.cfg.put(key, value);
    }

    public List<PropsEntity> getProps() {
      return props;
    }

    public void setProps(List<PropsEntity> props) {
      this.props = props;
    }

    public void setPropItem(PropsEntity prop) {
      if (this.props == null) {
        this.props = new LinkedList<>();
      }
      this.props.add(prop);
    }

    @Override
    public String toString() {
      return "LinksEntity [attr=" + attr + ", cfg=" + cfg + ", href=" + href + ", props=" + props
          + ", grp=" + grp + ", dl=" + dl + "]";
    }

    static LinksEntity from(Optional<Resource> resource, Optional<List<ResProperty>> properties,
                            Optional<List<Group>> resGroups) {
      return resource.map(res -> {
        logger.debug("--> res in LinksEntity from " + res);

        LinksEntity lEntity = new LinksEntity();

        lEntity.setHref(res.getUrl());

        Optional.ofNullable(res.getConfig()).ifPresent(rescfg -> {
          logger.debug("--> rescfg in LinksEntity from " + rescfg);
          logger.debug("--> lEntity in LinksEntity from " + lEntity);

          lEntity.setDl(rescfg.getDataLife());

          Optional.ofNullable(rescfg.getAttributes())
                  .ifPresent(attrs -> attrs.forEach(item -> lEntity.setAttr(item.getAn(),
                                                                            item.getAv())));

          Optional.ofNullable(rescfg.getUserCfgs())
                  .ifPresent(cfgs -> cfgs.forEach(item -> lEntity.setCfg(item.getCn(),
                                                                         item.getCv())));
        });

        MemberResRef mbr = new MemberResRef(res.getDevId(), res.getUrl());
        resGroups.ifPresent(resGs -> resGs.stream()
                                          .filter(resG -> resG.getMr().contains(mbr))
                                          .forEach(resG -> lEntity.setGrpItem(resG.getN())));

        properties.ifPresent(props -> props.stream().filter(prop -> {
          return prop.getDevId().equals(res.getDevId()) && prop.getRes().equals(res.getUrl());
        }).forEach(prop -> lEntity.setPropItem(PropsEntity.from(Optional.of(prop)))));
        return lEntity;
      }).orElse(null);
    }
  }

  @JsonProperty("di")
  String di;

  @JsonProperty("ct")
  long ct;

  @JsonProperty("dl")
  Integer dl;

  @JsonProperty("rn")
  Integer rn;

  @JsonProperty("attr")
  Map<String, String> attr;

  @JsonProperty("cfg")
  Map<String, String> cfg;

  @JsonProperty("grp")
  List<String> grp;

  @JsonProperty("links")
  List<LinksEntity> links;

  @JsonProperty("prj")
  String project;

  public DeviceConfig() {
    ct = BaseUtil.getNowAsEpochMillis();
    attr = new HashMap<>();
    cfg = new HashMap<>();
    grp = new LinkedList<>();
    links = new LinkedList<>();
  }

  public String getDi() {
    return di;
  }

  public void setDi(String di) {
    this.di = di;
  }

  public Map<String, String> getAttr() {
    return attr;
  }

  public void setAttr(Map<String, String> attr) {
    this.attr = attr;
  }

  public void setAttr(String key, String value) {
    if (this.attr == null) {
      this.attr = new HashMap<>();
    }
    this.attr.put(key, value);
  }

  public List<String> getGrp() {
    return grp;
  }

  public void setGrp(List<String> grp) {
    this.grp = grp;
  }

  public void setGrpItem(String grpName) {
    if (this.grp == null) {
      this.grp = new LinkedList<>();
    }
    this.grp.add(grpName);
  }

  public List<LinksEntity> getLinks() {
    return links;
  }

  public void setLinks(List<LinksEntity> links) {
    this.links = links;
  }

  public void setLinkItem(LinksEntity link) {
    if (this.links == null) {
      this.links = new LinkedList<>();
    }
    this.links.add(link);
  }

  public Integer getDl() {
    return dl;
  }

  public void setDl(Integer dl) {
    this.dl = dl;
  }

  public Integer getRn() {
    return rn;
  }

  public void setRn(Integer rn) {
    this.rn = rn;
  }

  public String getProject() {
    return project;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public Map<String, String> getCfg() {
    return cfg;
  }

  public void setCfg(Map<String, String> cfg) {
    this.cfg = cfg;
  }

  public void setCfg(String key, String value) {
    if (this.cfg == null) {
      this.cfg = new HashMap<>();
    }
    this.cfg.put(key, value);
  }

  @Override
  public String toString() {
    return "DeviceConfig [di=" + di + ", dl=" + dl + ", rn=" + rn + ", attr=" + attr + ", cfg="
        + cfg + ", grp=" + grp + ", links=" + links + "]";
  }

  public static DeviceConfig from(Optional<Device> device, Optional<List<Resource>> resources,
                                  Optional<List<ResProperty>> properties,
                                  Optional<List<Group>> devGroups,
                                  Optional<List<Group>> resGroups) {
    return device.map(d -> {
      DeviceConfig devCfg = new DeviceConfig();

      devCfg.setDi(d.getId());

      Optional.ofNullable(d.getPrj()).ifPresent(prj -> {
        devCfg.setProject(prj);
      });
      Optional.ofNullable(d.getConfig()).ifPresent(config -> {
        devCfg.setRn(config.getRefNum());
        devCfg.setDl(config.getDataLife());

        Optional.ofNullable(config.getAttributes())
                .ifPresent(attrs -> attrs.forEach(item -> devCfg.setAttr(item.getAn(),
                                                                         item.getAv())));
        Optional.ofNullable(config.getUserCfgs())
                .ifPresent(cfgs -> cfgs.forEach(item -> devCfg.setCfg(item.getCn(), item.getCv())));
      });

      devGroups.ifPresent(dgs -> dgs.forEach(dg -> devCfg.setGrpItem(dg.getN())));

      resources.ifPresent(reses -> reses.forEach(res -> devCfg.setLinkItem(LinksEntity.from(Optional.of(res),
                                                                                            properties,
                                                                                            resGroups))));

      return devCfg;
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
