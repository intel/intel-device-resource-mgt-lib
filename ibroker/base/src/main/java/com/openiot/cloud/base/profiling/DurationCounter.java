/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.profiling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DurationCounter {

  private static final Logger logger = LoggerFactory.getLogger(DurationCounter.class);
  private static final Logger counterLogger = LoggerFactory.getLogger("COUNTERLOGGER");

  // counter name. it is also the tile in the report
  private String name;

  private DurationCounterConfiguration configuration;

  // how many times the execution time exceeds configuration.thresholdLevel in current statistic
  // period
  private long thresholdOverTimes;

  // when do we start current statistic period
  private long currentPeriodStartMillis;

  private boolean alarmReported;

  private AlarmHandler alarmHandler;

  // in details
  private Map<Long, Long> executionTimeStatistic;

  private final String separatorLine = String.join("", Collections.nCopies(70, "="));

  public DurationCounter(String name, DurationCounterConfiguration configuration,
      AlarmHandler alarmHandler) {
    this.name = name;
    this.configuration = configuration;
    this.alarmHandler = alarmHandler;

    executionTimeStatistic = new ConcurrentHashMap<>();
    alarmReported = false;
    currentPeriodStartMillis = Instant.now(Clock.systemUTC()).toEpochMilli();
  }

  public void count(long executionSeconds) {
    long bucketName = executionSeconds / configuration.getExecutionTimeBucketSize()
        * configuration.getExecutionTimeBucketSize();
    executionTimeStatistic.put(bucketName, executionTimeStatistic.getOrDefault(bucketName, 0l) + 1);

    // if exceeds threshold
    if (executionSeconds > configuration.getThresholdLevel()) {
      ++thresholdOverTimes;
      if (thresholdOverTimes > configuration.getThresholdOverTimes()) {
        alarmReported = true;
        if (alarmHandler != null && !alarmReported) {
          // alarmHandler.triggerAlarm(toString());
          // logger.debug("generate an alarm from " + name);
        }
      }
    }
  }

  long getStatisticPeriodMillis() {
    return configuration.getStatisticPeriodMillis();
  }

  long periodRemainingMillis() {
    return Math.max(configuration.getStatisticPeriodMillis()
        - (Instant.now(Clock.systemUTC()).toEpochMilli() - currentPeriodStartMillis), 0);
  }

  void checkAndDump() {
    if (periodRemainingMillis() > 0) {
      logger.debug("{} remaining millis {} > 0", getName(), periodRemainingMillis());
      return;
    }

    if (executionTimeStatistic.isEmpty()) {
      logger.debug("{} empty statistic", getName());
      reset();
      return;
    }

    counterLogger.info(toString());
    reset();
  }

  void reset() {
    executionTimeStatistic.clear();
    alarmReported = false;
    currentPeriodStartMillis = Instant.now(Clock.systemUTC()).toEpochMilli();
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("\ntitle: " + name);
    builder.append("\n");
    builder.append("alarm: " + alarmReported);
    builder.append("\n");
    builder.append("date: "
        + LocalDateTime.ofInstant(Instant.ofEpochMilli(currentPeriodStartMillis), ZoneOffset.UTC)
                       .toLocalDate());
    builder.append("\n");
    builder.append("period: "
        + LocalDateTime.ofInstant(Instant.ofEpochMilli(currentPeriodStartMillis), ZoneOffset.UTC)
                       .toLocalTime());
    builder.append("\n");
    long toMinutes = TimeUnit.MILLISECONDS.toMinutes(configuration.getStatisticPeriodMillis());
    builder.append("duration: "
        + (toMinutes > 0 ? toMinutes + " min" : configuration.getStatisticPeriodMillis() + " ms"));
    builder.append("\n");
    builder.append("Count distribution: ");
    builder.append("\n");
    if (executionTimeStatistic.isEmpty()) {
      builder.append("[No stats]\n");
    } else {
      for (Map.Entry<Long, Long> entry : executionTimeStatistic.entrySet()) {
        builder.append(String.format("[%8d - %8d]: %d\n",
                                     entry.getKey(),
                                     entry.getKey() + configuration.getExecutionTimeBucketSize(),
                                     entry.getValue()));
      }
    }
    builder.append(separatorLine);
    builder.append("\n");
    return builder.toString();
  }
}
