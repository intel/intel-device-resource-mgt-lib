/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.service.dto;

import com.openiot.cloud.base.mongo.model.help.UserRole;
import lombok.Data;

@Data
public class UserDTO {
  private String name;
  private String nickname;
  private String password;
  private String tel;
  private String email;
  private String location;
  private String recentProject;
  private UserRole role;
}
