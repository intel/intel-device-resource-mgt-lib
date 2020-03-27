/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.help;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MessageIdMaker {
  private static final AtomicLong al = new AtomicLong(0);
  private static final AtomicInteger ai = new AtomicInteger(0);

  public static long getMessageIdAsLong() {
    return al.incrementAndGet();
  }

  public static int getMessageIdAsInteger() {
    return ai.incrementAndGet();
  }

  public static byte[] getIntMessageIdAsBytes() {
    return IntegerToBytes(ai.incrementAndGet());
  }

  /** @return a big endian [0, 0, 0, 8] */
  public static byte[] getLongMessageIdAsBytes() {
    return LongToBytes(al.incrementAndGet());
  }

  /**
   * @param bytes is big endian
   * @return
   */
  public static long bytesToLong(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.put(bytes, 0, Math.min(bytes.length, Long.BYTES));
    buffer.flip(); // need flip
    return buffer.getLong();
  }

  public static int bytesToInteger(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
    buffer.put(bytes, 0, Math.min(bytes.length, Integer.BYTES));
    buffer.flip(); // need flip
    return buffer.getInt();
  }

  public static byte[] IntegerToBytes(int ii) {
    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
    buffer.putInt(ii);
    return buffer.array();
  }

  public static byte[] LongToBytes(long ll) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.putLong(ll);
    return buffer.array();
  }
}
