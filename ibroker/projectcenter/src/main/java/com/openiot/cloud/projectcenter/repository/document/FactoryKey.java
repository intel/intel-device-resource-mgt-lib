/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.repository.document;

import com.openiot.cloud.base.help.ConstDef;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(ConstDef.C_FACTORYKEY)
@Data
public class FactoryKey {
  @Id
  private String id;
  private String keyName;
  private String keyType;
  private String keyValue;
  private String domain;
}
