/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.ao;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.openiot.cloud.base.mongo.model.help.UserRole;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAO {
  private String name;
  private String nickname;
  private String password;
  private String tel;
  private String email;
  private String location;
  private UserRole role;
}
