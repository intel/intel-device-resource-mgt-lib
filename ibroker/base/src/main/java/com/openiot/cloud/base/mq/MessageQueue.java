/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MessageQueue<T> {
  public static class DataAndConsumer<T> {
    private T data;
    private long timestamp;
    private Consumer<T> consumer;

    DataAndConsumer(T data, Consumer<T> consumer) {
      this.data = data;
      this.consumer = consumer;
      this.timestamp = Instant.now().toEpochMilli();
    }

    public T getData() {
      return data;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public Consumer<T> getConsumer() {
      return consumer;
    }

    @Override
    public String toString() {
      return "DataAndConsumer{" + "data=" + data + ", timestamp=" + timestamp + ", consumer="
          + consumer + '}';
    }
  }

  private BlockingQueue<DataAndConsumer<T>> queue;
  private int maxCapacity;
  private static final Logger logger = LoggerFactory.getLogger(MessageQueue.class);

  public MessageQueue(int maxCapacity, Executor executor) {
    queue = new LinkedBlockingQueue<>(maxCapacity);
    this.maxCapacity = maxCapacity;

    // selector
    Executors.newSingleThreadExecutor().execute(() -> {
      while (true) {
        try {
          logger.debug("[MessageQueue] to poll ... ");

          DataAndConsumer<T> item = queue.take();

          // dispose data added one hour ago, 3600 * 1000
          // if (isOneHourAgo(item.getTimestamp())) {
          // disposeUnderCondition(timestamp -> isOneHourAgo(timestamp));
          // }

          int curCapacity = queue.size();
          if (curCapacity > 0 && curCapacity % (1024 * 1024) == 0) {
            logger.debug(String.format("[MessageQueue] current size : %s", curCapacity));
          }

          item.getConsumer().accept(item.getData());
        } catch (InterruptedException e) {
          logger.warn("[MessageQueue] receive " + e.getLocalizedMessage() + " and break");
          break;
        } catch (Exception e) {
          logger.error("[MessageQueue] exception caught " + e.getLocalizedMessage());
        }
      }
    });
  }

  public void add(T data, Consumer<T> consumer) {
    if (queue.size() >= maxCapacity) {
      logger.info(String.format("[MessageQueue] full. The first one is about %s seconds ago",
                                ((Instant.now().toEpochMilli() - queue.peek().getTimestamp())
                                    * 1000)));
      queue.poll();
    }

    queue.offer(new DataAndConsumer<T>(data, consumer));
  }

  private boolean isOneHourAgo(long timestamp) {
    return (Instant.now().toEpochMilli() - timestamp) > 3600000;
  }

  private void disposeMessagesUnderCondition(Predicate<Long> condition) {
    int origSize = queue.size();
    while (!queue.isEmpty() && condition.test(queue.peek().getTimestamp())) {
      queue.poll();
    }
    int newSize = queue.size();
    logger.info("[MessageQueue] dispose the old data, from " + origSize + " to " + newSize);
  }
}
