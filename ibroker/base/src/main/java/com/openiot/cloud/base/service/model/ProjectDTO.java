/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.service.model;

import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import lombok.*;
import java.util.*;

/**
 * should be as same as com.openiot.cloud.provisionex.service.dto.ProjectDTO;
 */
@Data
public class ProjectDTO {
  private String id;
  private String name;
  private String group_title;
  private Long time_created;
  private String description;
  private String location;
  private List<UserAndRole> user;
  private List<ConfigurationEntity> cs;
  private List<AttributeEntity> as;
}
