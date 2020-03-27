/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.repository.document;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.help.UserRole;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(ConstDef.C_USER)
@Data
public class User {
  @Id
  private String id;
  // id is same with name
  private String name;
  private String nickname;
  private String tel;
  private String email;
  private String password;
  private String location;
  // should be lastPasswordModificationTime
  private long time_reg;
  private String recentProject;
  private UserRole role;
}
