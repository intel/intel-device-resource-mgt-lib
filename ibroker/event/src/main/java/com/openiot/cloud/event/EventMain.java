/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.openiot.cloud.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.openiot.cloud"})
public class EventMain {

  public static void main(String[] args) {
    SpringApplication.run(EventMain.class, args);
  }
}
