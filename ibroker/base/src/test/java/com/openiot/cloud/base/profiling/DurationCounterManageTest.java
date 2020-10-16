/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.profiling;

import com.openiot.cloud.base.Application;
import com.openiot.cloud.base.profiling.DurationCounterManageTest.DurationCounterManageConfiguration;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import org.mockito.internal.verification.VerificationModeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class})
@Import(DurationCounterManageConfiguration.class)
public class DurationCounterManageTest {

  @TestConfiguration
  static class DurationCounterManageConfiguration {
    @Bean
    public DurationCounterManage counterManage() {
      return new DurationCounterManage();
    }

    @Bean
    public DurationCounterConfiguration longPeriodConfiguration() {
      DurationCounterConfiguration configuration = new DurationCounterConfiguration();
      configuration.setTag("long");
      configuration.setStatisticPeriodMillis(1000);
      configuration.setExecutionTimeBucketSize(500);
      configuration.setThresholdLevel(800);
      configuration.setThresholdOverTimes(3);
      return configuration;
    }

    @Bean
    public DurationCounterConfiguration shortPeriodConfiguration() {
      DurationCounterConfiguration configuration = new DurationCounterConfiguration();
      configuration.setTag("short");
      configuration.setStatisticPeriodMillis(800);
      configuration.setExecutionTimeBucketSize(500);
      configuration.setThresholdLevel(800);
      configuration.setThresholdOverTimes(3);
      return configuration;
    }

    @Bean
    public AlarmHandler simpleAlarmHandler(DurationCounterConfiguration longPeriodConfiguration) {
      return new SimpleAlarmHandler(longPeriodConfiguration.getAlarmOutputPath());
    }

    @Bean
    public DurationCounter getRdDeviceCounter(DurationCounterConfiguration longPeriodConfiguration,
                                              AlarmHandler simpleAlarmHandler) {
      return new DurationCounter("get /rd/device", longPeriodConfiguration, simpleAlarmHandler);
    }

    @Bean
    public DurationCounter postRdCounter(DurationCounterConfiguration shortPeriodConfiguration,
                                         AlarmHandler simpleAlarmHandler) {
      return new DurationCounter("post /rd", shortPeriodConfiguration, simpleAlarmHandler);
    }
  }

  @Autowired
  private DurationCounterManage counterManage;

  @Autowired
  private DurationCounter getRdDeviceCounter;

  @Autowired
  private DurationCounter postRdCounter;

  @Mock
  private DurationCounter fakeCounter;

  @Before
  public void setUp() {
    counterManage.putCounter("GET_RD_DEVICE", getRdDeviceCounter);
    counterManage.putCounter("POST_RD", postRdCounter);
    counterManage.putCounter("FAKE", fakeCounter);
  }

  @After
  public void tearDown() {
    ((Map<String, DurationCounter>) ReflectionTestUtils.getField(counterManage,
                                                                 "counters")).clear();
  }

  @Test
  public void testStatic() throws Exception {
    // these 2 beasns are created early, maybe periodRemainingMillis is decounted less than 10ms here
    getRdDeviceCounter.reset();
    postRdCounter.reset();

    assertThat(counterManage).isNotNull();
    assertThat(counterManage.getCounter("GET_RD_DEVICE")).isEqualTo(getRdDeviceCounter);

    doAnswer(invocation -> 10l).when(fakeCounter).periodRemainingMillis();
    assertThat(counterManage.nextEndCounterMillis()).isEqualTo(10l);

    doAnswer(invocation -> 1200l).when(fakeCounter).periodRemainingMillis();
    assertThat(counterManage.nextEndCounterMillis()).isLessThanOrEqualTo(800l);
  }

  @Test
  public void testDynamic() throws Exception {
    // tell manage after 10ms, the fakeCounter statistic period will end
    doAnswer(invocation -> 10l).when(fakeCounter).periodRemainingMillis();

    doAnswer(invocation -> {
      assertThat(true).isTrue();
      return null;
    }).when(fakeCounter).checkAndDump();

    doAnswer(invocation -> {
      return 10l;
    }).when(fakeCounter).getStatisticPeriodMillis();

    counterManage.initialize();
    TimeUnit.MILLISECONDS.sleep(300);

    verify(fakeCounter, VerificationModeFactory.atLeast(2)).periodRemainingMillis();
    verify(fakeCounter, VerificationModeFactory.atLeast(2)).checkAndDump();
  }

  @Test
  public void testDurationCounterofUrlBuilder() throws Exception {
    assertThat(DurationCounterOfUrlBuilder.readCounterOfUrl("counter_of_urls.json")).asList()
                                                                                    .hasSize(3)
                                                                                    .extracting("url")
                                                                                    .containsOnly("/rd/device",
                                                                                                  "/rd/resource",
                                                                                                  "/rd/group");
  }

  @Test
  public void testFindAProperStartUpTime() throws Exception {
    Map<String, DurationCounter> counters =
        ((Map<String, DurationCounter>) ReflectionTestUtils.getField(counterManage, "counters"));
    counters.clear();

    DurationCounterConfiguration config1 = new DurationCounterConfiguration();
    config1.setStatisticPeriodMillis(4);

    DurationCounterConfiguration config2 = new DurationCounterConfiguration();
    config2.setStatisticPeriodMillis(8);

    DurationCounterConfiguration config3 = new DurationCounterConfiguration();
    config3.setStatisticPeriodMillis(24);

    DurationCounter counter1 = new DurationCounter("counter1", config1, null);
    DurationCounter counter2 = new DurationCounter("counter2", config2, null);
    DurationCounter counter3 = new DurationCounter("counter3", config3, null);

    counters.put(counter1.getName(), counter1);
    counters.put(counter2.getName(), counter2);
    counters.put(counter3.getName(), counter3);

    assertThat(counterManage.findAProperStartUpTime().until(LocalDateTime.now(ZoneOffset.UTC),
                                                            ChronoUnit.SECONDS)).isLessThan(1);

    counters.clear();
    config1.setStatisticPeriodMillis(TimeUnit.MINUTES.toMillis(4));
    config2.setStatisticPeriodMillis(TimeUnit.MINUTES.toMillis(8));
    config3.setStatisticPeriodMillis(TimeUnit.MINUTES.toMillis(24));

    counter1 = new DurationCounter("counter1", config1, null);
    counter2 = new DurationCounter("counter2", config2, null);
    counter3 = new DurationCounter("counter3", config3, null);

    counters.put(counter1.getName(), counter1);
    counters.put(counter2.getName(), counter2);
    counters.put(counter3.getName(), counter3);

    LocalDateTime next = counterManage.findAProperStartUpTime();
    assertThat(next.getNano()).isEqualTo(0);
    assertThat(next.getSecond()).isEqualTo(0);
    assertThat(next).isAfter(LocalDateTime.now(ZoneOffset.UTC));
    assertThat(next.getMinute()).isIn(0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 52, 56, 60);

    counters.clear();
    config1.setStatisticPeriodMillis(TimeUnit.MINUTES.toMillis(60));
    config2.setStatisticPeriodMillis(TimeUnit.MINUTES.toMillis(180));
    config3.setStatisticPeriodMillis(TimeUnit.MINUTES.toMillis(120));

    counter1 = new DurationCounter("counter1", config1, null);
    counter2 = new DurationCounter("counter2", config2, null);
    counter3 = new DurationCounter("counter3", config3, null);

    counters.put(counter1.getName(), counter1);
    counters.put(counter2.getName(), counter2);
    counters.put(counter3.getName(), counter3);

    next = counterManage.findAProperStartUpTime();
    assertThat(next.getNano()).isEqualTo(0);
    assertThat(next.getSecond()).isEqualTo(0);
    assertThat(next.getMinute()).isEqualTo(0);
    assertThat(next).isAfter(LocalDateTime.now(ZoneOffset.UTC));
    assertThat(next.getMinute()).isEqualTo(0);
  }
}
