/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.help;

import com.openiot.cloud.base.Application;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class})
public class ConcurrentRequestMapTest {

  @Test
  public synchronized void testPutAndGet() throws Exception {
    AtomicInteger rejected = new AtomicInteger();
    ConcurrentRequestMap<Integer, String> requestMap =
        new ConcurrentRequestMap<>(3, 2, 5, (k, v) -> rejected.getAndIncrement());

    // should not be removed
    requestMap.put(1, "a");
    requestMap.put(2, "b");
    requestMap.put(3, "c");
    assertThat(requestMap.containsKey(1)).isTrue();
    assertThat(requestMap.containsKey(2)).isTrue();
    assertThat(requestMap.get(3)).isEqualTo("c");

    // should not be inserted since it is full and all are new and will be rejected
    requestMap.put(4, "d");
    assertThat(requestMap.get(1)).isEqualTo("a");
    assertThat(rejected.get()).isEqualTo(1);
    assertThat(requestMap.get(4)).isNull();

    // after expired check
    TimeUnit.SECONDS.sleep(10);
    requestMap.put(5, "e");
    assertThat(requestMap.containsKey(5)).isTrue();
    requestMap.put(6, "f");
    assertThat(requestMap.containsKey(6)).isTrue();

    // after expired check, all gone
    TimeUnit.SECONDS.sleep(10);
    assertThat(requestMap.get(4)).isNull();
    assertThat(requestMap.get(5)).isNull();
    assertThat(requestMap.get(6)).isNull();
  }

  @Test
  public void testRemove() throws Exception {
    AtomicInteger rejected = new AtomicInteger();
    ConcurrentRequestMap<Integer, String> requestMap =
        new ConcurrentRequestMap<>(3, 2, 5, (k, v) -> rejected.getAndIncrement());

    requestMap.put(1, "a");
    requestMap.put(2, "b");
    requestMap.put(3, "c");
    assertThat(requestMap.get(1)).isEqualTo("a");
    assertThat(requestMap.get(2)).isEqualTo("b");
    assertThat(requestMap.get(3)).isEqualTo("c");

    requestMap.remove(1);
    requestMap.remove(3);
    assertThat(requestMap.get(1)).isNull();
    assertThat(requestMap.get(2)).isEqualTo("b");
    assertThat(requestMap.get(3)).isNull();

    TimeUnit.MILLISECONDS.sleep(5);
    requestMap.put(4, "f");
    assertThat(requestMap.get(2)).isEqualTo("b");
    assertThat(requestMap.get(4)).isEqualTo("f");
  }

  @Test
  public void testMultipleThreads() throws Exception {
    AtomicInteger rejected = new AtomicInteger();
    int smallNumber = 10;

    ConcurrentRequestMap<Integer, String> requestMap =
        new ConcurrentRequestMap<>(smallNumber, 2, 5, (k, v) -> rejected.getAndIncrement());

    // add normally
    CompletableFuture[] putFuture = new CompletableFuture[smallNumber];
    ExecutorService executorService = Executors.newFixedThreadPool(4);
    for (int i = 0; i < smallNumber; i++) {
      int finalI = i;
      putFuture[i] = CompletableFuture.runAsync(() -> {
        requestMap.put(finalI, String.valueOf(finalI + 'a'));
      }, executorService);
    }
    CompletableFuture.allOf(putFuture).get(30, TimeUnit.SECONDS);
    assertThat(rejected.get()).isEqualTo(0);
    assertThat(requestMap.get(7)).isEqualTo(String.valueOf('a' + 7));

    // meet full
    requestMap.put(smallNumber, String.valueOf(smallNumber + 'a'));
    assertThat(rejected.get()).isEqualTo(1);
    assertThat(requestMap.get(smallNumber)).isNull();

    requestMap.clear();
    assertThat(requestMap.size()).isEqualTo(0);
    rejected.set(0);

    int bigNumber = 1000;
    int bucketSize = 125;
    CompletableFuture[] putAndRemoveFuture = new CompletableFuture[bigNumber * 2];
    executorService = Executors.newFixedThreadPool(32);
    int futureIndex = 0;
    ConcurrentRequestMap<Integer, String> largetRequestMap =
        new ConcurrentRequestMap<>(bigNumber, 2, 5, (k, v) -> rejected.getAndIncrement());

    for (int i = 1; i <= bigNumber; i++) {
      int toPut = i;

      putAndRemoveFuture[futureIndex++] =
          CompletableFuture.runAsync(() -> largetRequestMap.put(toPut, "lime_" + toPut),
                                     executorService);

      if (i - bucketSize >= 0) {
        int toRemove = i - bucketSize;
        putAndRemoveFuture[futureIndex++] =
            CompletableFuture.runAsync(() -> largetRequestMap.remove(toRemove), executorService);
      }
    }

    System.out.println("futureIndex = " + futureIndex);
    futureIndex--;
    for (int i = bigNumber - bucketSize + 1; i <= bigNumber; i++) {
      int toRemove = i;
      putAndRemoveFuture[futureIndex++] =
          CompletableFuture.runAsync(() -> largetRequestMap.remove(toRemove), executorService);
    }

    assertThat(futureIndex).isEqualTo(bigNumber * 2);
    CompletableFuture.allOf(putAndRemoveFuture).get(30, TimeUnit.SECONDS);
  }
}
