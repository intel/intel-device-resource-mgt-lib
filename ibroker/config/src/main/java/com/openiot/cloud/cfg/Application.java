/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.cfg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication(
    scanBasePackages = {"com.openiot.cloud.base", "com.openiot.cloud.cfg", "com.openiot.cloud.sdk"})
public class Application {
  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    // TODO: might need Authentication, set user and password here
    return builder.build();
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class);
  }
}
