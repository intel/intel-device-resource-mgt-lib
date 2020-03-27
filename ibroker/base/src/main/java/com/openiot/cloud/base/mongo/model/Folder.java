/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.help.Untouchable;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = ConstDef.C_FOLDER)
public class Folder {
  @Untouchable
  @Id
  @JsonProperty(ConstDef.F_IID)
  String id;

  @Field(ConstDef.F_NAME)
  @JsonProperty(ConstDef.F_NAME)
  String name;

  @Field(ConstDef.F_FLDTYPE)
  @JsonProperty(ConstDef.F_TYPE)
  String fldType;

  @Field(ConstDef.F_PARENT)
  @JsonProperty(ConstDef.F_PARENT)
  String parent;

  @Field(ConstDef.F_DEPTH)
  int depth;
}
