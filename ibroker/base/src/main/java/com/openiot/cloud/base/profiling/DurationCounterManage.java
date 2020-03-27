/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.profiling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy;
import java.util.concurrent.TimeUnit;

public class DurationCounterManage {

  private static final Logger logger = LoggerFactory.getLogger(DurationCounterManage.class);

  private ConcurrentHashMap<String, DurationCounter> counters;

  private ScheduledExecutorService executor;

  public DurationCounterManage() {
    counters = new ConcurrentHashMap<>();
    executor = new ScheduledThreadPoolExecutor(1, new DiscardOldestPolicy());
  }

  public void putCounter(String name, DurationCounter counter) {
    counters.computeIfAbsent(name, n -> counter);
  }

  // has to call it, after register all counters
  public void initialize() {
    LocalDateTime startUpTime = findAProperStartUpTime();
    long delay = LocalDateTime.now(ZoneOffset.UTC).until(startUpTime, ChronoUnit.MILLIS);
    delay = Math.max(0, delay);
    logger.debug("startup time is " + startUpTime + " and delay is " + delay + " ms");
    executor.schedule(() -> {
      for (DurationCounter counter : counters.values()) {
        counter.reset();
      }
      executor.schedule(this::checkAndDump, nextEndCounterMillis(), TimeUnit.MILLISECONDS);
    }, delay, TimeUnit.MILLISECONDS);
  }

  LocalDateTime findAProperStartUpTime() {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

    int minPeriodDuration = counters.values()
                                    .stream()
                                    .map(DurationCounter::getStatisticPeriodMillis)
                                    .min((l1, l2) -> (int) (l1 - l2))
                                    .get()
                                    .intValue();

    logger.debug("minimum period of all counters is " + minPeriodDuration + " ms");

    // millis to min
    minPeriodDuration = (int) Duration.ofMillis(minPeriodDuration).toMinutes();

    // just support a duration less than 60 and greater than 0
    // since it is going to align to minutes clock
    minPeriodDuration = Math.min(60, minPeriodDuration);

    if (minPeriodDuration == 0) {
      logger.info("minimum period is less than 1 minute, to start now");
      return now;
    }

    int minuteOffset =
        (now.getMinute() / minPeriodDuration + 1) * minPeriodDuration - now.getMinute();

    return now.withSecond(0).withNano(0).plusMinutes(minuteOffset);
  }

  public DurationCounter getCounter(String name) {
    return counters.getOrDefault(name, null);
  }

  long nextEndCounterMillis() {
    return counters.values()
                   .stream()
                   .map(DurationCounter::periodRemainingMillis)
                   .min((l1, l2) -> (int) (l1.longValue() - l2.longValue()))
                   .get();
  }

  void checkAndDump() {
    logger.debug("--> checkAndDump " + Thread.currentThread().getName());

    // if there is no registered counter, quit
    if (counters.isEmpty()) {
      logger.debug("--> there is no registered counter, quit " + Thread.currentThread().getName());
      return;
    }

    for (DurationCounter counter : counters.values()) {
      logger.debug("check and dump {}", counter.getName());
      counter.checkAndDump();
    }

    executor.schedule(this::checkAndDump, nextEndCounterMillis(), TimeUnit.MILLISECONDS);
  }
}
