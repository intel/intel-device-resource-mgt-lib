/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller;


import lombok.Data;
import lombok.NonNull;

@Data
public class ErrorMessage {
  @NonNull
  String error;
}
