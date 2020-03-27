/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.repository.document;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import com.openiot.cloud.base.service.model.UserAndRole;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;


@Document(ConstDef.C_PROJECT)
@Data
public class Project {
  @Id
  private String id;
  private String name;
  private String group_title;
  @CreatedDate
  private Long time_created;
  private String description;
  private String location;
  private List<UserAndRole> user;
  private List<ConfigurationEntity> cs;
  private List<AttributeEntity> as;
}
