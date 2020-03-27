/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.service.model;

import com.openiot.cloud.base.mongo.model.help.ResAndResProID;
import java.util.List;

public class DataSourceTemplate {

  private String name;
  private DataSourceType type;
  private String title;
  private String classInfo;
  private String description;

  // TODO: maybe a specific class
  private ResAndResProID rule;
  private ReferenceDefinition basicRule;

  private String unit;

  private Long thresholdLow;
  private Long thresholdHigh;

  private List<GeneralKeyValuePair> attributeList;

  public DataSourceTemplate() {}

  public DataSourceTemplate(String name, DataSourceType type) {
    this();
    this.name = name;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DataSourceType getType() {
    return type;
  }

  public void setType(DataSourceType type) {
    this.type = type;
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

  public ResAndResProID getRule() {
    return rule;
  }

  public void setRule(ResAndResProID rule) {
    this.rule = rule;
  }

  public ReferenceDefinition getBasicRule() {
    return basicRule;
  }

  public void setBasicRule(ReferenceDefinition basicRule) {
    this.basicRule = basicRule;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public Long getThresholdLow() {
    return thresholdLow;
  }

  public void setThresholdLow(Long thresholdLow) {
    this.thresholdLow = thresholdLow;
  }

  public Long getThresholdHigh() {
    return thresholdHigh;
  }

  public void setThresholdHigh(Long thresholdHigh) {
    this.thresholdHigh = thresholdHigh;
  }

  public List<GeneralKeyValuePair> getAttributeList() {
    return attributeList;
  }

  public void setAttributeList(List<GeneralKeyValuePair> attributeList) {
    if (attributeList == null || attributeList.isEmpty()) {
      this.attributeList = null;
    } else {
      this.attributeList = attributeList;
    }
  }
}
