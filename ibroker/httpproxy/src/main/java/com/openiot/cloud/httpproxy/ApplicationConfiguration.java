/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.httpproxy;

import com.openiot.cloud.base.profiling.DurationCounterConfiguration;
import com.openiot.cloud.base.profiling.DurationCounterManage;
import com.openiot.cloud.base.profiling.DurationCounterOfUrlBuilder;
import com.openiot.cloud.base.profiling.DurationCounterOfUrlBuilder.CounterOfUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class ApplicationConfiguration {
  @Value("${mq.host:localhost}")
  private String mqServerHost;

  @Autowired
  private Environment env;

  @Bean
  public List<CounterOfUrl> counterOfUrls() {
    return DurationCounterOfUrlBuilder.readCounterOfUrl("counters_of_url.json");
  }

  @Bean
  public DurationCounterManage counterManage() {
    return new DurationCounterManage();
  }

  @Bean
  public DurationCounterConfiguration urlConfiguration() {
    DurationCounterConfiguration configuration = new DurationCounterConfiguration();
    configuration.setTag("url");
    configuration.setStatisticPeriodMillis(TimeUnit.MINUTES.toMillis(30));
    configuration.setExecutionTimeBucketSize(500);
    configuration.setThresholdLevel(500);
    configuration.setThresholdOverTimes(3);
    configuration.setAlarmOutputPath("cloudlogs/http_proxy.alarm");
    return configuration;
  }

  @Bean
  public DurationCounterConfiguration methodConfiguration() {
    DurationCounterConfiguration configuration = new DurationCounterConfiguration();
    configuration.setTag("method");
    configuration.setStatisticPeriodMillis(TimeUnit.MINUTES.toMillis(30));
    configuration.setExecutionTimeBucketSize(50);
    configuration.setThresholdLevel(50);
    configuration.setThresholdOverTimes(1);
    configuration.setAlarmOutputPath("cloudlogs/http_proxy.alarm");
    return configuration;
  }
}
