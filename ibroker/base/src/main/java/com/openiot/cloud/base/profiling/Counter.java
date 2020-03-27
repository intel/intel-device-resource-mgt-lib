/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.profiling;

import java.util.concurrent.atomic.AtomicInteger;

public class Counter {
  private boolean resetAble = false;
  private AtomicInteger current = new AtomicInteger();

  Counter(boolean resetAble) {
    this.resetAble = resetAble;
  }

  public int incrementAndGet() {
    return current.incrementAndGet();
  }

  public int decrementAndGet() {
    return current.decrementAndGet();
  }

  public int getCurrentValue() {
    return current.intValue();
  }

  public void reset() {
    if (resetAble) {
      synchronized (current) {
        current.set(0);
      }
    }
  }
}
