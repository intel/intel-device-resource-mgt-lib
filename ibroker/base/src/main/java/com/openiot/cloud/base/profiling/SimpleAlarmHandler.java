/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.profiling;

import com.openiot.cloud.base.help.BaseUtil;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SimpleAlarmHandler implements AlarmHandler {
  private static final Logger logger = LoggerFactory.getLogger(DurationCounter.class);

  private String alarmOutputPath;

  public SimpleAlarmHandler(String alarmOutputPath) {
    this.alarmOutputPath = alarmOutputPath;
  }

  @Override
  public void triggerAlarm(String content) {
    Path report = Paths.get(alarmOutputPath);
    try (OutputStream out =
        new BufferedOutputStream(Files.newOutputStream(report, CREATE, APPEND))) {
      out.write(content.getBytes());
    } catch (IOException e) {
      logger.warn("IOException when output alarm  to " + alarmOutputPath);
      logger.warn(BaseUtil.getStackTrace(e));
    }
  }
}
