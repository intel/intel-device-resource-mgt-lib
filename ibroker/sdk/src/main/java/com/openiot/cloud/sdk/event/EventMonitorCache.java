/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.event;

import com.openiot.cloud.base.mongo.model.EventMonitor;
import java.util.List;

class EventMonitorCache {

  private List<EventMonitor> monitors = null;

  private static EventMonitorCache instance = new EventMonitorCache();

  private EventMonitorCache() {}

  static EventMonitorCache getInstance() {
    return instance;
  }

  List<EventMonitor> getMonitors() {
    return this.monitors;
  }

  void setMonitors(List<EventMonitor> monitors) {
    this.monitors = monitors;
  }
}
