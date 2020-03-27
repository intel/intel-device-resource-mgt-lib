/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.help.ConstDef;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.List;

@Document(collection = ConstDef.C_DEVTYPE)
@JsonInclude(Include.NON_NULL)
public class DeviceType {

  @Id
  String id;

  @Field(ConstDef.F_NAME)
  @JsonProperty(ConstDef.F_NAME)
  String name;

  @Field(ConstDef.F_RESTYPES)
  @JsonProperty(ConstDef.F_RESTYPES)
  List<String> resTypeIds;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getResTypeIds() {
    return resTypeIds;
  }

  public void setResTypeIds(List<String> resTypeIds) {
    this.resTypeIds = resTypeIds;
  }
}
