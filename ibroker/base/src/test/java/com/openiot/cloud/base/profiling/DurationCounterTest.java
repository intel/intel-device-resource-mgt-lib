/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.profiling;

import com.openiot.cloud.base.Application;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StopWatch;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class})
public class DurationCounterTest {

  @Test
  public void testBasic() throws Exception {
    DurationCounterConfiguration configuration = new DurationCounterConfiguration();
    configuration.setStatisticPeriodMillis(1000);
    configuration.setExecutionTimeBucketSize(500);
    configuration.setThresholdLevel(800);
    configuration.setThresholdOverTimes(3);

    DurationCounter counter =
        new DurationCounter("fake", configuration, new SimpleAlarmHandler("/tmp/alarm"));

    assertThat(counter).isNotNull();
    assertThat(counter.periodRemainingMillis()).isGreaterThan(0l)
                                               .isLessThanOrEqualTo(configuration.getStatisticPeriodMillis());

    counter.count(100);
    counter.count(100);
    assertThat(((Map<Long, Long>) ReflectionTestUtils.getField(counter,
                                                               "executionTimeStatistic")).get(Long.valueOf(0))).isEqualTo(2);

    counter.count(1200);
    counter.count(2400);
    counter.count(5400);

    assertThat(((Map<Long, Long>) ReflectionTestUtils.getField(counter,
                                                               "executionTimeStatistic")).size()).isEqualTo(4);

    System.out.println(counter.toString());
    assertThat(counter.toString()).contains("title",
                                            "date",
                                            "period",
                                            "duration",
                                            "Count distribution");
  }

  @Test
  public void testCalling() throws Exception {
    DurationCounterConfiguration configuration = new DurationCounterConfiguration();
    configuration.setStatisticPeriodMillis(1000);
    configuration.setExecutionTimeBucketSize(500);
    configuration.setThresholdLevel(800);
    configuration.setThresholdOverTimes(3);

    int j = 10;
    Function<Integer, Integer> accessLocalVariable = i -> {
      return i + j;
    };

    Function<Integer, Integer> accessMemberFiled = i -> {
      return i + (int) configuration.getStatisticPeriodMillis();
    };

    StopWatch stopWatch = new StopWatch();

    stopWatch.start("access local variable");
    IntStream.range(0, 10000000).forEach(i -> accessLocalVariable.apply(i));
    stopWatch.stop();

    stopWatch.start("access member field");
    IntStream.range(0, 10000000).forEach(i -> accessMemberFiled.apply(i));
    stopWatch.stop();

    System.out.println(stopWatch.prettyPrint());
  }
}
