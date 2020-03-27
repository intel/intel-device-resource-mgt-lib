/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.alarm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Hello world! */
@SpringBootApplication(scanBasePackages = {"com.openiot.cloud"})
public class AlarmMain {

  public static void main(String[] args) {
    SpringApplication.run(AlarmMain.class, args);
  }
}
