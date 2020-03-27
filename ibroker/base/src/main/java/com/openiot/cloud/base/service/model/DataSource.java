/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.service.model;

import com.openiot.cloud.base.help.Untouchable;
import lombok.Data;
import java.util.LinkedList;
import java.util.List;

@Data
public class DataSource {
  @Untouchable
  private String name;
  private DataSourceType type;
  private String title;
  private String classInfo;
  private String description;

  private String unit;
  private Long threshLow;
  private Long threshHigh;
  private Long interval;

  private List<GeneralKeyValuePair> attributeList;
  private List<ReferenceDefinition> referenceList;
  private Operate operate;

  public void addAttribute(GeneralKeyValuePair attribute) {
    this.attributeList = this.attributeList == null ? new LinkedList<>() : this.attributeList;
    this.attributeList.add(attribute);
  }

  public void addReference(ReferenceDefinition definition) {
    this.referenceList = this.referenceList == null ? new LinkedList<>() : this.referenceList;
    // TODO: here or Service ?
    ReferenceDefinition latestReference =
        this.referenceList.isEmpty() ? null : this.referenceList.get(this.referenceList.size() - 1);
    if (latestReference != null) {
      // want to close the latest one
      if (latestReference.getDevId().equals(definition.getDevId())
          && latestReference.getResUrl().equals(definition.getResUrl())
          && latestReference.getPropName().equals(definition.getPropName())
          && latestReference.getFrom() == definition.getFrom()) {
        latestReference.setTo(definition.getTo());
      } else {
        // want to create a new one
        if (latestReference.getTo() == 0) {
          latestReference.setTo(definition.getFrom());
        }
        this.referenceList.add(definition);
      }
    } else {
      // want to create a new one
      this.referenceList.add(definition);
    }
  }

  public void setOperate(Operate operate) {
    this.operate = operate;
  }
}
