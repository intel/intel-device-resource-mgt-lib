/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.profiling;

// TODO

/*
 * @formatter:off
 * a typical configuration file
 *
 * [
 *   {
 *     "tag": "function",
 *     "statistic": {
 *       "period": 60000,
 *       "bucketSize": 500
 *     },
 *     "threshold": {
 *       "level": 500,
 *       "overTimes": 50
 *     },
 *     "reportPath": "/tmp/path_to_report",
 *     "alarmPath":  "/tmp/path_to_alarm"
 *   },
 *   {
 *     "tag": "urlProcessing",
 *     "statistic": {
 *       "period": 120000,
 *       "bucketSize": 1000
 *     },
 *     "threshold": {
 *       "level": 1500,
 *       "overTimes": 20
 *     },
 *     "reportPath": "/tmp/path_to_report",
 *     "alarmPath":  "/tmp/path_to_alarm"
 *   }
 * ]
 *
 * @formatter:on
 */

public class DurationCounterConfiguration {

  private String tag;

  private long statisticPeriodMillis;
  private long executionTimeBucketSize;

  private long thresholdLevel;
  private long thresholdOverTimes;

  private String reportOutputPath;
  private String alarmOutputPath;

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public long getThresholdLevel() {
    return thresholdLevel;
  }

  public void setThresholdLevel(long thresholdLevel) {
    this.thresholdLevel = thresholdLevel;
  }

  public long getThresholdOverTimes() {
    return thresholdOverTimes;
  }

  public void setThresholdOverTimes(long thresholdOverTimes) {
    this.thresholdOverTimes = thresholdOverTimes;
  }

  public long getStatisticPeriodMillis() {
    return statisticPeriodMillis;
  }

  public void setStatisticPeriodMillis(long statisticPeriodMillis) {
    this.statisticPeriodMillis = statisticPeriodMillis;
  }

  public long getExecutionTimeBucketSize() {
    return executionTimeBucketSize;
  }

  public void setExecutionTimeBucketSize(long executionTimeBucketSize) {
    this.executionTimeBucketSize = executionTimeBucketSize;
  }

  public String getReportOutputPath() {
    return reportOutputPath;
  }

  public void setReportOutputPath(String reportOutputPath) {
    this.reportOutputPath = reportOutputPath;
  }

  public String getAlarmOutputPath() {
    return alarmOutputPath;
  }

  public void setAlarmOutputPath(String alarmOutputPath) {
    this.alarmOutputPath = alarmOutputPath;
  }
}
