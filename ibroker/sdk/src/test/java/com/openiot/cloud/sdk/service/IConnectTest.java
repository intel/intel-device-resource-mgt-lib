/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.service;

import com.openiot.cloud.sdk.Application;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class})
public class IConnectTest {
  @Autowired
  private IConnect iConnect;

  @Autowired
  private IConnectService iConnectService;

  @Test
  public void testBasic() throws Exception {
    assertThat(iConnect).isNotNull();
    assertThat(iConnectService).isNotNull();

    iConnectService.addHandler("/river", request -> {
      System.out.println("receive a request " + request);
      assertThat(request.getFormat().equals("water"));
      IConnectResponse response1 =
          IConnectResponse.createFromRequest(request, HttpStatus.OK, MediaType.TEXT_PLAIN, null);
      response1.send();
    });
    iConnect.startService(iConnectService);

    IConnectRequest request1 = IConnectRequest.create(HttpMethod.GET,
                                                      "/river",
                                                      MediaType.TEXT_PLAIN,
                                                      "a river is full of water".getBytes());
    AtomicBoolean result = new AtomicBoolean(false);
    CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
    request1.send(response -> {
      System.out.println("receive a response " + response);
      assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);

      result.set(true);
      completableFuture.complete(true);
    }, 5, TimeUnit.SECONDS);

    completableFuture.get(6, TimeUnit.SECONDS);
    assertThat(result.get()).isTrue();
  }

  @Test
  public void testTimeout() throws Exception {
    assertThat(iConnect).isNotNull();
    assertThat(iConnectService).isNotNull();

    iConnectService.addHandler("/timeout", request -> {
      System.out.println("receive a request " + request);
      assertThat(request.getFormat().equals(MediaType.TEXT_PLAIN));
    });
    iConnect.startService(iConnectService);

    IConnectRequest request1 = IConnectRequest.create(HttpMethod.GET,
                                                      "/timeout",
                                                      MediaType.TEXT_PLAIN,
                                                      "going to be timeout".getBytes());
    CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
    AtomicBoolean result = new AtomicBoolean(false);
    request1.send(response -> {
      System.out.println("receive a response " + response);
      assertThat(response.getStatus()).isEqualTo(HttpStatus.REQUEST_TIMEOUT);
      result.set(true);
      completableFuture.complete(true);
    }, 500, TimeUnit.MILLISECONDS);

    completableFuture.get(20, TimeUnit.SECONDS);
    assertThat(result.get()).isTrue();
  }

  @Test
  public void testParallel() throws Exception {
    assertThat(iConnect).isNotNull();
    assertThat(iConnectService).isNotNull();

    IConnectServiceHandler echoHandler = request -> {
      assertThat(request.getFormat().equals(MediaType.TEXT_PLAIN));
      IConnectResponse responseEcho = IConnectResponse.createFromRequest(request,
                                                                         HttpStatus.OK,
                                                                         MediaType.TEXT_PLAIN,
                                                                         request.getPayload());
      responseEcho.send();
    };
    iConnectService.addHandler("/echo", echoHandler);
    iConnect.startService(iConnectService);

    CompletableFuture[] tasks = new CompletableFuture[2];
    tasks[0] = new CompletableFuture<Boolean>();
    tasks[1] = new CompletableFuture<Boolean>();

    AtomicInteger counter = new AtomicInteger();
    ExecutorService executorService = Executors.newFixedThreadPool(2);

    executorService.submit(() -> {
      IConnectRequest echo1 =
          IConnectRequest.create(HttpMethod.GET, "/echo", MediaType.TEXT_PLAIN, "1234".getBytes());
      echo1.send(response -> {
        System.out.println("echo1 response " + response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getPayload()).isEqualTo("1234".getBytes());
        counter.incrementAndGet();
        tasks[0].complete(true);
      }, 2, TimeUnit.SECONDS);
    });

    executorService.submit(() -> {
      IConnectRequest echo2 =
          IConnectRequest.create(HttpMethod.GET, "/echo", MediaType.TEXT_PLAIN, "5678".getBytes());
      echo2.send(response -> {
        System.out.println("echo2 response " + response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getPayload()).isEqualTo("5678".getBytes());
        counter.incrementAndGet();
        tasks[1].complete(true);
      }, 2, TimeUnit.SECONDS);
    });

    CompletableFuture.allOf(tasks).get(5, TimeUnit.SECONDS);
    assertThat(counter.get()).isEqualTo(2);
  }

  @Test
  public void testMassive() throws Exception {
    assertThat(iConnect).isNotNull();
    assertThat(iConnectService).isNotNull();

    ExecutorService receiverPool = Executors.newFixedThreadPool(16);
    IConnectServiceHandler echoHandler = request -> {
      receiverPool.submit(() -> {
        assertThat(request.getFormat().equals(MediaType.TEXT_PLAIN));
        IConnectResponse responseEcho = IConnectResponse.createFromRequest(request,
                                                                           HttpStatus.OK,
                                                                           MediaType.TEXT_PLAIN,
                                                                           request.getPayload());
        responseEcho.send();
      });
    };

    for (int i = 0; i < 8; i++) {
      iConnectService.addHandler("/echo" + i, echoHandler);
    }
    iConnect.startService(iConnectService);

    int bigNumber = 10000;
    ExecutorService senderPool = Executors.newFixedThreadPool(24);

    Random random = new Random(37);
    AtomicInteger counter = new AtomicInteger();
    AtomicInteger errorCounter = new AtomicInteger();
    CompletableFuture[] tasks = new CompletableFuture[bigNumber];
    for (int i = 0; i < bigNumber; i++) {
      final int currentI = i;
      CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
      tasks[currentI] = completableFuture;

      senderPool.submit(() -> {
        IConnectRequest requestEcho =
            IConnectRequest.create(HttpMethod.GET,
                                   "/echo" + random.nextInt(8),
                                   MediaType.TEXT_PLAIN,
                                   Integer.valueOf(currentI).toString().getBytes());
        requestEcho.send(response -> {
          counter.incrementAndGet();

          if (!response.getStatus().equals(HttpStatus.OK)) {
            errorCounter.incrementAndGet();
          }

          if (!(Integer.parseInt(new String(response.getPayload())) == currentI)) {
            errorCounter.incrementAndGet();
          }

          tasks[currentI].complete(true);
        }, 1200, TimeUnit.SECONDS);
      });
    }

    CompletableFuture.allOf(tasks).get(1800, TimeUnit.SECONDS);
    assertThat(counter.get()).isEqualTo(bigNumber);
    assertThat(errorCounter.get()).isEqualTo(0);
  }

  @Test
  public void testDestinationInvalid() throws Exception {
    assertThat(iConnect).isNotNull();
    assertThat(iConnectService).isNotNull();

    AtomicBoolean testResult = new AtomicBoolean(false);
    iConnectService.addHandler("/plum", request -> {
      assertThat(false).isTrue();
      testResult.set(false);
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.INTERNAL_SERVER_ERROR,
                                         MediaType.TEXT_PLAIN,
                                         "arrive /plum".getBytes())
                      .send();
    });
    iConnect.startService(iConnectService);

    CompletableFuture<Boolean> firstTry = new CompletableFuture<>();
    IConnectRequest request = IConnectRequest.create(HttpMethod.GET, "/grape/is/sour", null, null);

    // send with a destination which is not a prefix of the url
    request.setDestination("/plum");
    request.send(response -> {
      assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
      firstTry.complete(true);
    }, 5, TimeUnit.SECONDS);
    firstTry.get(6, TimeUnit.SECONDS);
    assertThat(testResult.get()).isFalse();
  }

  @Test
  public void testDestinationValid() throws Exception {
    assertThat(iConnect).isNotNull();
    assertThat(iConnectService).isNotNull();

    AtomicBoolean testResult = new AtomicBoolean(false);
    iConnectService.addHandler("/grape", request -> {
      assertThat(request.getUrl()).isEqualTo("/grape/is/sour");
      testResult.set(true);
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.ACCEPTED,
                                         MediaType.TEXT_PLAIN,
                                         "arrive /grape".getBytes())
                      .send();
    });
    iConnect.startService(iConnectService);

    // send with a destination which is a prefix of the url
    IConnectRequest request = IConnectRequest.create(HttpMethod.GET, "/grape/is/sour", null, null);
    request.setDestination("/grape");

    CompletableFuture<Boolean> secondTry = new CompletableFuture<>();
    request.send(response -> {
      assertThat(response.getStatus()).isEqualTo(HttpStatus.ACCEPTED);
      assertThat(response.getPayload()).isEqualTo("arrive /grape".getBytes());
      secondTry.complete(true);
    }, 5, TimeUnit.SECONDS);
    secondTry.get(6, TimeUnit.SECONDS);
    assertThat(testResult.get()).isTrue();
  }

  @Test
  public void testUrlTailingSlash() throws Exception {
    assertThat(iConnect).isNotNull();
    assertThat(iConnectService).isNotNull();

    // it is listening a queue with tailing slashes in its name
    AtomicBoolean testResult = new AtomicBoolean(false);
    iConnectService.addHandler("/raspberry//", request -> {
      assertThat(request.getUrl().startsWith("/raspberry")).isTrue();
      testResult.set(true);
      IConnectResponse.createFromRequest(request, HttpStatus.ACCEPTED, null, null).send();
    });
    iConnect.startService(iConnectService);

    // send to a url without any tailing slash
    CompletableFuture<Boolean> responseFutureI = new CompletableFuture<>();
    IConnectRequest.create(HttpMethod.GET, "/raspberry", null, null).send(response -> {
      assertThat(response.getStatus()).isEqualTo(HttpStatus.ACCEPTED);
      responseFutureI.complete(true);
    }, 2, TimeUnit.SECONDS);
    responseFutureI.get(5, TimeUnit.SECONDS);
    assertThat(responseFutureI.get()).isTrue();

    // send to a url without tailing slashes
    CompletableFuture<Boolean> responseFutureII = new CompletableFuture<>();
    IConnectRequest.create(HttpMethod.GET, "/raspberry/////", null, null).send(response -> {
      assertThat(response.getStatus()).isEqualTo(HttpStatus.ACCEPTED);
      responseFutureII.complete(true);
    }, 2, TimeUnit.SECONDS);
    responseFutureII.get(5, TimeUnit.SECONDS);
    assertThat(responseFutureII.get()).isTrue();
  }
}
