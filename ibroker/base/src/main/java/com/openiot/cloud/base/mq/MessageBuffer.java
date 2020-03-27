/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mq;

import com.openiot.cloud.base.help.BaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MessageBuffer<T> {
  public static class Message<T> {
    private T data;
    private long timestamp;

    Message(T data) {
      this.data = data;
      this.timestamp = Instant.now().toEpochMilli();
    }

    public T getData() {
      return data;
    }

    public long getTimestamp() {
      return timestamp;
    }

    @Override
    public String toString() {
      return "Message{" + "data=" + data + ", timestamp=" + timestamp + '}';
    }
  }

  private BlockingQueue<Message<T>> queue;
  private int capacity;
  // in millis
  private long oldestLimitation;
  private int batchSize;
  private Consumer<List<T>> consumer;
  private List<Message<T>> batch;
  private static final Logger logger = LoggerFactory.getLogger(MessageBuffer.class);

  public MessageBuffer(int capacity, int batchSize, long oldestLimitation,
      Consumer<List<T>> consumer) {
    this.queue = new LinkedBlockingQueue<>(capacity);
    this.capacity = capacity;
    this.oldestLimitation = oldestLimitation;
    this.batchSize = batchSize;
    this.consumer = consumer;
    this.batch = new LinkedList<>();

    // selector
    Executors.newSingleThreadExecutor().execute(() -> {
      while (true) {
        try {
          logger.debug("to take ... ");

          Optional.ofNullable(queue.poll(100, TimeUnit.MILLISECONDS))
                  .ifPresent(message -> batch.add(message));
          if (batch.isEmpty()) {
            continue;
          }

          int curCapacity = batch.size();
          Message<T> oldest = batch.get(0);
          if (curCapacity >= batchSize
              || Instant.now().toEpochMilli() - oldest.getTimestamp() >= oldestLimitation) {
            logger.debug("batch opt " + batch.size() + " messages");
            consumer.accept(batch.stream().map(Message::getData).collect(Collectors.toList()));
            batch.clear();
          } else {
            logger.debug("keep waiting");
          }
        } catch (InterruptedException e) {
          logger.warn("receive " + e.getLocalizedMessage() + " and break");
          consumer.accept(batch.stream().map(Message::getData).collect(Collectors.toList()));
          queue.clear();
          break;
        } catch (Exception e) {
          logger.error("exception caught " + BaseUtil.getStackTrace(e));
        }
      }
    });
  }

  public void add(T data) {
    if (queue.size() >= capacity) {
      logger.info(String.format("[MessageBuffer] full. The first one is about %s seconds ago",
                                ((Instant.now().toEpochMilli() - queue.peek().getTimestamp())
                                    * 1000)));
      logger.debug("[MessageBuffer] to remove one before insert ...");
      queue.drainTo(new LinkedList<>(), Math.max(1, queue.size() - capacity));
    }
    try {
      queue.put(new Message<T>(data));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    if (!queue.isEmpty() && queue.size() % 1024 == 0) {
      logger.debug(toString());
    }
  }

  public String toString() {
    return String.format("Message[%s], current size %d, capacity=%s, oldestLimitation=%sms, batchSize=%s",
                         System.identityHashCode(this),
                         queue.size(),
                         capacity,
                         oldestLimitation,
                         batchSize);
  }
}
