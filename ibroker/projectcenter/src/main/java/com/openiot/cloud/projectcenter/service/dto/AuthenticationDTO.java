/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.service.dto;

import lombok.Data;

@Data
public class AuthenticationDTO {
  private String username;
  private String password;
}
