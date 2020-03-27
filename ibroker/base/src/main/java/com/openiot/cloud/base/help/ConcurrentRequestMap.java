/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.help;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * it is a thread safe map to store sent requests, in order to maintain the relationship between a
 * request and its socket and use it to send the response back in a right way.
 *
 * <p>it is also a sorted data structure based on requests timestamp which is created when adding.
 * it is going to reject requests based on their timestamps.
 *
 * <p>we are using two kinds policies to reject incoming requests when the map is full.
 *
 * <p>- If the first request is younger than new age threshold, the incoming request will be
 * rejected. It might represent a too fast producer and the capacity is smaller. All previous
 * requests have been dispatched to corresponding handlers but not return yet. We want to make sure
 * all processing requests can be responded correctly.
 *
 * <p>- If the first request is older than old age threshold, the incoming request will be stored
 * and old requests will be rejected. It might represent a too slow consumer and all previous
 * requests can not be handled in time. We want to dispose them to make sure all new requests will
 * be processed properly.
 *
 * <p>the new age threshold will be the estimated requests average response time. the old age
 * threshold will be greater than new age the threshold. we are sure requests older than it have not
 * been processed.
 *
 * <p>Also, a continuously background cleaning thread make sure all old age requests will not stay
 * in memory.
 *
 * @param <V> stored requests
 */
public class ConcurrentRequestMap<K, V> {
  private Logger logger = LoggerFactory.getLogger(ConcurrentRequestMap.class);

  // default capacity value is 128
  private int capacity;

  // default oldAgePeriod value is 30s. it is in seconds
  private int oldAgePeriod;

  // default newAgePeriod value is 10s. it is in seconds
  private int newAgePeriod;

  private BiConsumer<K, V> rejectHandler;

  private ScheduledExecutorService ses = new ScheduledThreadPoolExecutor(1);

  private ConcurrentHashMap<K, V> requestMap;
  private ConcurrentSkipListMap<Long, List<K>> timestampMap;

  // with default configuration
  public ConcurrentRequestMap() {
    this(128, 10, 30, null);
  }

  public ConcurrentRequestMap(int capacity, int newAgePeriod, int oldAgePeriod,
      BiConsumer<K, V> rejectHandler) {
    requestMap = new ConcurrentHashMap<>();
    timestampMap = new ConcurrentSkipListMap<>();
    this.capacity = capacity;
    this.newAgePeriod = newAgePeriod;
    this.oldAgePeriod = oldAgePeriod > newAgePeriod ? oldAgePeriod : (newAgePeriod << 1);
    this.rejectHandler = rejectHandler;

    ses.scheduleAtFixedRate(() -> {
      try {
        removeExpiredEntry();
      } catch (Exception e) {
        logger.error("scheduled clean just failed as " + BaseUtil.getStackTrace(e));
      }
    }, 0, this.oldAgePeriod, TimeUnit.SECONDS);
  }

  public void put(K key, V value) {
    // don't accept null as key nor value
    if (key == null || value == null) {
      return;
    }

    if (checkCapacityOrReject()) {
      logger.debug("put (" + key + "," + value + ")");
      long now = System.currentTimeMillis();
      List<K> keysWithSameTimestamp = timestampMap.getOrDefault(now, new LinkedList<>());
      keysWithSameTimestamp.add(key);
      timestampMap.put(now, keysWithSameTimestamp);

      requestMap.put(key, value);
    } else {
      if (null != rejectHandler) {
        rejectHandler.accept(key, value);
      }
      logger.info("reject when it is full and the first is still young (" + key + "," + value + "),"
          + toString());
    }
  }

  public V get(K key) {
    return requestMap.getOrDefault(key, null);
  }

  public V remove(K key) {
    V result = requestMap.remove(key);
    logger.debug("remove (" + key + "," + result + ")");
    return result;
  }

  public long size() {
    return requestMap.mappingCount();
  }

  public void clear() {
    logger.debug("clear " + this.toString());
    requestMap.clear();
    timestampMap.clear();
  }

  public boolean containsKey(K key) {
    return requestMap.containsKey(key);
  }

  @Override
  public String toString() {
    return "ConcurrentRequestMap{" + "capacity=" + capacity + ", oldAgePeriod=" + oldAgePeriod
        + ", newAgePeriod=" + newAgePeriod + ",size=" + requestMap.mappingCount() + '}';
  }

  private boolean checkCapacityOrReject() {
    if (requestMap.mappingCount() + 1 <= capacity) {
      return true;
    }

    synchronized (requestMap) {
      Map.Entry<Long, List<K>> firstEntry = firstEntry();
      if (firstEntry == null) {
        // an empty map
        return true;
      }
      if (System.currentTimeMillis()
          - firstEntry.getKey() <= TimeUnit.SECONDS.toMillis(newAgePeriod)) {
        // reject incoming requests
        return false;
      } else {
        // remove the least recently requests
        firstEntry.getValue().forEach(k -> {
          V v = requestMap.remove(k);
          if (null != rejectHandler) {
            rejectHandler.accept(k, v);
          }
          logger.info("reject when it is full and the first is expiring " + k + " and " + v + ","
              + toString());
        });
        return true;
      }
    }
  }

  private void removeExpiredEntry() {
    // do nothing
    if (timestampMap.isEmpty() && requestMap.isEmpty()) {
      return;
    }

    // sync maps
    if (timestampMap.isEmpty()) {
      requestMap.forEach((k, v) -> {
        if (rejectHandler != null) {
          rejectHandler.accept(k, v);
        }
        logger.info("reject when it is already removed" + k + " and " + v + "," + toString());
      });
      requestMap.clear();
      return;
    }

    if (requestMap.isEmpty()) {
      timestampMap.clear();
      return;
    }

    // locate the first entry of timestamp in which contains at least one key which is in requestMap
    Map.Entry<Long, List<K>> firstEntry = timestampMap.firstEntry();

    // do nothing if the least recent entry is still in new age
    if (System.currentTimeMillis()
        - firstEntry.getKey() > TimeUnit.SECONDS.toMillis(newAgePeriod)) {
      long deadline = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(oldAgePeriod);
      Map<Long, List<K>> expiringMap = timestampMap.headMap(deadline);

      logger.debug("expiringMap.size() " + expiringMap.size() + " by " + deadline + ","
          + LocalDateTime.ofInstant(Instant.ofEpochMilli(deadline), ZoneOffset.UTC));

      expiringMap.values().stream().flatMap(List::stream).forEach(key -> {
        // may have been removed by remove()
        if (requestMap.containsKey((key))) {
          V value = requestMap.remove(key);
          if (rejectHandler != null) {
            rejectHandler.accept(key, value);
          }
          logger.info("reject when it is timeout (" + key + "," + value + ")," + toString());
        }
      });
      expiringMap.clear();
    } else {
      logger.debug("there is no items older than "
          + LocalDateTime.now(ZoneOffset.UTC).minusSeconds(oldAgePeriod) + ", the first is "
          + LocalDateTime.ofInstant(Instant.ofEpochMilli(firstEntry.getKey()), ZoneOffset.UTC));
    }
  }

  private Map.Entry<Long, List<K>> firstEntry() {
    Map.Entry<Long, List<K>> firstEntry = timestampMap.firstEntry();
    while (firstEntry != null && !containsAny(requestMap, firstEntry.getValue())) {
      timestampMap.remove(firstEntry.getKey());
      firstEntry = timestampMap.firstEntry();
    }
    return firstEntry;
  }

  private boolean containsAny(Map<K, V> map, List<K> keys) {
    return keys.stream().filter(k -> map.containsKey(k)).findAny().isPresent();
  }
}
