/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.cfg.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.service.model.ProjectDTO;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectCfg {

  @JsonProperty("id")
  private String id;

  @JsonProperty("name")
  private String name;

  @JsonProperty("desc")
  private String description;

  @JsonProperty("group_title")
  private String group_title;

  @JsonProperty("location")
  private String location;

  // @JsonIgnore
  @JsonProperty("attr")
  Map<String, String> attr;

  @JsonProperty("cfg")
  Map<String, String> cfg;

  public String getProject() {
    return id;
  }

  public void setProject(String project) {
    this.id = project;
  }

  public String getGroup_title() {
    return group_title;
  }

  public void setGroup_title(String group_title) {
    this.group_title = group_title;
  }

  public Map<String, String> getAttr() {
    return attr;
  }

  public void setAttr(Map<String, String> attr) {
    this.attr = attr;
  }

  public Map<String, String> getCfg() {
    return cfg;
  }

  public void setCfg(Map<String, String> cfg) {
    this.cfg = cfg;
  }

  public ProjectCfg() {
    super();
  }

  public ProjectCfg(String id, String name, String group_title, String description,
      String location) {
    this.id = id;
    this.name = name;
    this.group_title = group_title;
    this.description = description;
    this.location = location;
  }

  public String toJsonString() {
    try {
      return new ObjectMapper().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static ProjectCfg from(ProjectDTO prj) {
    Optional<ProjectCfg> prjCfg = Optional.ofNullable(prj).map(p -> {
      ProjectCfg cfg = new ProjectCfg(p.getId(),
                                      p.getName(),
                                      p.getGroup_title(),
                                      p.getDescription(),
                                      p.getLocation());
      if (p.getAs() != null) {
        cfg.setAttr(p.getAs().stream().collect(Collectors.toMap(x -> x.getAn(), x -> x.getAv())));
      }
      if (p.getCs() != null) {
        cfg.setCfg(p.getCs().stream().collect(Collectors.toMap(x -> x.getCn(), x -> x.getCv())));
      }
      return cfg;
    });
    return prjCfg.orElse(null);
  }
}
