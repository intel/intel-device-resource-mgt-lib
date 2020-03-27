/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.openiot.cloud.sdk", "com.openiot.cloud.base.mongo",
    "com.openiot.cloud.base.influx"})
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class);
  }
}
