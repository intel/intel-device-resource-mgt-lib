/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConcurrentRequestMap;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.help.MessageIdMaker;
import com.openiot.cloud.base.ilink.*;
import com.openiot.cloud.ibroker.IBrokerMain;
import com.openiot.cloud.ibroker.base.protocols.ilink.ILinkCoapOverTcpMessageHandler;
import com.openiot.cloud.ibroker.mq.DefaultJmsHandler;
import com.openiot.cloud.ibroker.utils.ILinkMessageBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4JLoggerFactory;
import lombok.Data;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.iotivity.cloud.base.device.Device;
import org.iotivity.cloud.base.protocols.IRequest;
import org.iotivity.cloud.base.protocols.IResponse;
import org.iotivity.cloud.base.protocols.MessageBuilder;
import org.iotivity.cloud.base.protocols.coap.CoapDecoder;
import org.iotivity.cloud.base.protocols.coap.CoapMessage;
import org.iotivity.cloud.base.protocols.coap.CoapRequest;
import org.iotivity.cloud.base.protocols.coap.CoapResponse;
import org.iotivity.cloud.base.protocols.enums.ContentFormat;
import org.iotivity.cloud.base.protocols.enums.RequestMethod;
import org.iotivity.cloud.base.protocols.enums.ResponseStatus;
import org.iotivity.cloud.base.resource.ResourceManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.isA;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StopWatch;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {IBrokerMain.class})
public class IBrokerMainTest {
  private static final Logger logger = LoggerFactory.getLogger(IBrokerMainTest.class);
  @Autowired
  private ObjectMapper objectMapper;

  class IBrokerClient {
    private final Logger logger = LoggerFactory.getLogger(IBrokerClient.class);

    @ChannelHandler.Sharable
    class ClientHandler extends SimpleChannelInboundHandler<ILinkMessage> {
      @Override
      protected void channelRead0(ChannelHandlerContext ctx, ILinkMessage msg) throws Exception {
        logger.debug("channel read a " + msg + " @" + System.identityHashCode(msg));
        if (ILinkMessage.class.isAssignableFrom(msg.getClass())) {
          ILinkMessage response = msg;
          byte[] iLinkMessageId = response.getIlinkMessageId();
          if (iLinkMessageId == null) {
            logger.warn(" can not get " + response + " ilink message id");
          } else {
            if (response.getFlexHeader() == null
                || !response.getFlexHeader().containsKey(ConstDef.FH_K_REP)) {
              response.setResponseCode(ConstDef.FH_V_SUCC);
            }
            int key = MessageIdMaker.bytesToInteger(iLinkMessageId);
            logger.debug("to process #" + key + " response@" + System.identityHashCode(response));
            RequestInformation requestInformation = requestMap.remove(key);
            if (requestInformation != null && requestInformation.responseHandler != null) {
              try {
                requestInformation.responseHandler.accept(response);
              } catch (Exception e) {
                logger.warn("meet an exception during a response handling \n"
                    + BaseUtil.getStackTrace(e));
              }
            } else {
              logger.warn(String.format("can not find a response handler for #%s response.", key));
              logger.info(String.format("RequestMap dumps info: size=%s", requestMap.size()));
            }
          }
        } else {
          logger.warn("%s is not a ILINK message", msg);
        }

        ReferenceCountUtil.release(msg);
      }

      @Override
      public synchronized void channelActive(ChannelHandlerContext context) throws Exception {
        logger.debug("channel is active");
        if (!writable) {
          synchronized (writableLock) {
            writable = true;
            logger.debug("gona to notify the writable flag I true");
            writableLock.notifyAll();
          }
        }
      }

      @Override
      public synchronized void channelInactive(ChannelHandlerContext context) throws Exception {
        logger.debug("channel is inactive");
        if (writable) {
          synchronized (writableLock) {
            writable = false;
            logger.debug("gona to notify the writable flag II false");
            writableLock.notifyAll();
          }
        }
      }

      @Override
      public synchronized void channelWritabilityChanged(ChannelHandlerContext context)
          throws Exception {
        logger.debug("channel writability changed " + context.channel().isWritable());
        if (context.channel().isWritable()) {
          if (!writable) {
            synchronized (writableLock) {
              writable = true;
              logger.debug("gona to notify the writable flag III true");
              writableLock.notifyAll();
            }
          }
        } else {
          if (writable) {
            synchronized (writableLock) {
              logger.debug("gona to notify the writable flag IV false");
              writable = false;
              writableLock.notifyAll();
            }
          }
        }
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext context, Throwable throwable) {
        logger.debug("catch an exception " + throwable.getLocalizedMessage()
            + ". record it and move on");
        logger.debug(BaseUtil.getStackTrace(throwable));
      }

      @Override
      public void userEventTriggered(ChannelHandlerContext context, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
          IdleStateEvent event = (IdleStateEvent) evt;
          if (event.state() == IdleState.READER_IDLE) {
            logger.info("haven't receive any message from the server, ping check");
          }
        }
      }
    }

    @Data
    class RequestInformation {
      ILinkMessage request;
      Consumer<ILinkMessage> responseHandler;
    }

    private String iBrokerAddress;
    private String gatewayId;
    private Channel channel;
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    private Consumer<ILinkMessage> defaultResponseHandler;
    // [WATERMARK] every gateway will send messages from their devices every 5 second
    // [WATERMARK] each gateway would have 10 devices or more specifically 1000 endpoints
    // [WATERMARK] it means each iLink client should support 200 message / second at least
    // [WATERMARK] consider every message is able to stay 25s(timeout), its capacity of requests map
    // [WATERMARK] is 5,000
    private ConcurrentRequestMap<Integer, RequestInformation> requestMap;
    private int capacity = 5000;
    private Bootstrap bootstrap = new Bootstrap();
    private Random r = new Random(379);
    private final AtomicInteger messageID = new AtomicInteger(0);
    private Boolean writable = false;
    private final Object writableLock = new Object();
    private AtomicBoolean stopSign = new AtomicBoolean(false);

    public IBrokerClient(String iBrokerAddress, String gatewayId) {
      this.iBrokerAddress = iBrokerAddress;
      this.gatewayId = gatewayId;
      this.requestMap =
          new ConcurrentRequestMap<>(capacity, 10, 25, (iLinkMessageId, requestInformation) -> {
            logger.warn(String.format("the #%s request(%s) has been rejected",
                                      iLinkMessageId,
                                      requestInformation));
            if (requestInformation != null && requestInformation.responseHandler != null) {
              requestInformation.responseHandler.accept(ILinkMessageBuilder.createResponse(requestInformation.request,
                                                                                           ConstDef.FH_V_FAIL));
            }
          });
      this.defaultResponseHandler = response -> {
        logger.info("the default response handler get " + response);
      };
    }

    // it is a synchronized function, you can not do anything more until a client connects the
    // server
    public void start() {
      logger.debug("starting a client");
      InternalLoggerFactory.setDefaultFactory(Log4JLoggerFactory.getDefaultFactory());
      bootstrap.group(workerGroup);
      bootstrap.channel(NioSocketChannel.class);
      bootstrap.option(ChannelOption.TCP_NODELAY, true);
      bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
      bootstrap.handler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
          ch.pipeline().addLast(new ILinkEncoder(),
                                new ILinkDecoder(),
                                new LoggingHandler(LogLevel.DEBUG),
                                new IdleStateHandler(180, 0, 0),
                                new ClientHandler());
        }
      });

      try {
        doConnection().get(30, TimeUnit.SECONDS);
        if (!writable) {
          logger.debug("waiting for the writable flag " + writable);
          synchronized (writableLock) {
            while (!writable) {
              writableLock.wait();
            }
          }
        }
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        logger.warn("thread interrupted " + BaseUtil.getStackTrace(e));
        stop();
      }
    }

    private synchronized CompletableFuture<Boolean> doConnection() {
      CompletableFuture<Boolean> connectResult = new CompletableFuture<>();
      if (stopSign.get()) {
        connectResult.complete(false);
        return connectResult;
      }

      try {
        logger.info("trying to connect the server " + this.iBrokerAddress + ":"
            + ConstDef.ILINK_PORT);
        ChannelFuture connectFuture = bootstrap.connect(this.iBrokerAddress, ConstDef.ILINK_PORT)
                                               // make sure the connection has been established
                                               .awaitUninterruptibly();
        if (connectFuture.isDone() && connectFuture.isSuccess()) {
          channel = connectFuture.channel();
          connectFuture.channel().closeFuture().addListener(closeFuture -> {
            logger.info("lost the connection");
            if (!closeFuture.isCancelled() && !stopSign.get()) {
              int shortBreak = r.nextInt(2000);
              logger.info("gona to reconnect after " + shortBreak + " ms");
              Executors.newSingleThreadScheduledExecutor()
                       .schedule(() -> doConnection(), shortBreak, TimeUnit.MILLISECONDS);
            }
          });
        } else if (connectFuture.isCancelled()) {
          // close if the user wants to quit
          stop();
        } else {
          if (!stopSign.get()) {
            int shortBreak = r.nextInt(2000);
            logger.info("fail to connect, try again after " + shortBreak + " ms");
            Executors.newSingleThreadScheduledExecutor()
                     .schedule(() -> doConnection(), shortBreak, TimeUnit.MILLISECONDS);
          }
        }
        connectResult.complete(connectFuture.isSuccess());
      } catch (Exception e) {
        logger.warn("meet an exception during connecting");
        logger.warn(BaseUtil.getStackTrace(e));
        stop();
        connectResult.complete(false);
      }
      return connectResult;
    }

    public void sendMessage(ILinkMessage request, Consumer<ILinkMessage> responseHandler) {
      if (stopSign.get()) {
        logger.debug("has stopped the client");
        responseHandler.accept(ILinkMessageBuilder.createResponse(request, ConstDef.FH_V_FAIL));
        return;
      }

      if (channel == null) {
        logger.debug("will not send the message since the channel is null");
        responseHandler.accept(ILinkMessageBuilder.createResponse(request, ConstDef.FH_V_FAIL));
        return;
      }

      if (!channel.isOpen() || !channel.isActive() || !channel.isWritable()
          || !channel.isRegistered()) {
        logger.warn(String.format("channel %s is not in a normal state open(%s),active(%s),writable(%s),registered(%s)",
                                  channel.id().asShortText(),
                                  channel.isOpen(),
                                  channel.isActive(),
                                  channel.isWritable(),
                                  channel.isRegistered()));
        responseHandler.accept(ILinkMessageBuilder.createResponse(request, ConstDef.FH_V_FAIL));
        return;
      }

      logger.debug("to send " + request);

      // the tie between a request and a response is based on iLink message id instead of messageid
      RequestInformation requestInformation = new RequestInformation();
      requestInformation.request = request;
      requestInformation.responseHandler =
          responseHandler == null ? defaultResponseHandler : responseHandler;
      int key = MessageIdMaker.bytesToInteger(request.getIlinkMessageId());
      requestMap.put(key, requestInformation);

      ChannelFuture channelFuture = channel.writeAndFlush(request);
      if (responseHandler != null) {
        channelFuture.addListener(future -> {
          if (!future.isSuccess()) {
            logger.debug("send failed " + request);
            requestMap.remove(key);
            responseHandler.accept(ILinkMessageBuilder.createResponse(request, ConstDef.FH_V_FAIL));
          }
        });
      }

      return;
    }

    public void stop() {
      logger.debug("a client is stopping");
      try {
        stopSign.set(true);
        if (channel != null) {
          channel.close();
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        workerGroup.shutdownGracefully();
      }
    }

    CompletableFuture<Boolean> handShake() {
      logger.debug(String.format("try to handshake with %s:%s",
                                 iBrokerAddress,
                                 ConstDef.ILINK_PORT));
      CompletableFuture<Boolean> handShakeFuture = new CompletableFuture<>();

      // handshake 1
      ILinkMessage handShake1Message = ILinkMessageBuilder.createHandShake1Request(gatewayId);

      sendMessage(handShake1Message, response -> {
        logger.debug("--> hand shake 1 response " + response + ","
            + System.identityHashCode(response));

        assertThat(response.getLeadingByte()).isEqualTo((byte) LeadingByte.PLAIN.valueOf());
        assertThat(response.getTag()).isEqualTo(ConstDef.FH_V_HAN1);
        assertThat(response.getAgentId()).isEqualTo(gatewayId);
        assertThat(response.getResponseCode()).isEqualTo(ConstDef.FH_V_REQ2HAN);

        if (response.getResponseCode() != ConstDef.FH_V_REQ2HAN) {
          handShakeFuture.complete(false);
        } else {
          // handshake 2
          ILinkMessage handShake2Message =
              ILinkMessageBuilder.createHandShake2Request(gatewayId, response.getPayload());
          sendMessage(handShake2Message, response2 -> {
            logger.debug("--> hand shake 2 response " + response2 + ","
                + System.identityHashCode(response2));
            assertThat(response2.getLeadingByte()).isEqualTo((byte) LeadingByte.PLAIN.valueOf());
            assertThat(response2.getResponseCode()).isEqualTo(ConstDef.FH_V_SUCC);
            handShakeFuture.complete(response2.getResponseCode() == ConstDef.FH_V_SUCC);
          });
        }
      });
      return handShakeFuture;
    }

    public boolean startAndHandShakeAndAwait() {
      boolean result = true;
      try {
        if (channel == null) {
          logger.info(String.format("try to connect to %s:%s",
                                    iBrokerAddress,
                                    ConstDef.ILINK_PORT));
          start();
        }

        result = handShake().get(5, TimeUnit.SECONDS);
      } catch (TimeoutException | InterruptedException | ExecutionException e) {
        logger.warn("the previous action finally failed");
        result = false;
      }
      return result;
    }

    public void postRD(String deviceId, Map<String, String[]> resourceAndResourceType,
                       Consumer<ILinkMessage> responseHandler) {
      Map[] links = new Map[resourceAndResourceType.size()];
      int iOfLinks = 0;
      for (Map.Entry<String, String[]> entry : resourceAndResourceType.entrySet()) {
        Map<String, Object> link = new HashMap<>();
        link.put("href", entry.getKey());
        link.put("rt", entry.getValue());
        links[iOfLinks++] = link;
      }

      Map<String, Object> resourceReport = new HashMap<>();
      resourceReport.put("aid", "__night-owl-17");
      resourceReport.put("di", deviceId);
      resourceReport.put("dt", "fake_device");
      resourceReport.put("status", "on");
      resourceReport.put("links", links);

      try {
        CoapMessage coapMessage =
            (CoapMessage) MessageBuilder.createRequest(RequestMethod.POST,
                                                       "/rd",
                                                       null,
                                                       ContentFormat.APPLICATION_TEXTPLAIN,
                                                       objectMapper.writeValueAsBytes(resourceReport));
        ILinkMessage request = ILinkMessageBuilder.createCOAPRequest(deviceId,
                                                                     messageID.getAndIncrement(),
                                                                     coapMessage);
        sendMessage(request, responseHandler);
      } catch (JsonProcessingException e) {
        logger.warn("serialize the resource port failed");
        ILinkMessage response = new ILinkMessage(LeadingByte.RESPONSE.valueOf(),
                                                 (byte) MessageType.COAP_OVER_TCP.valueOf());
        response.setResponseCode(ConstDef.FH_V_FAIL);
        responseHandler.accept(response);
      }
    }

    public void postDPTextPlainData(String deviceId, String resourceUrl, String propertyName,
                                    long time, Number data,
                                    Consumer<ILinkMessage> responseHandler) {
      String uriQuery = String.format("di=%s&ri=%s&pt=%s", deviceId, resourceUrl, propertyName);
      IRequest coapRequest = MessageBuilder.createRequest(RequestMethod.PUT,
                                                          "/dp/",
                                                          uriQuery,
                                                          ContentFormat.APPLICATION_TEXTPLAIN,
                                                          data.toString().getBytes());
      ((CoapRequest) coapRequest).setUser("__night-owl");
      sendMessage(ILinkMessageBuilder.createCOAPRequest(deviceId,
                                                        messageID.getAndIncrement(),
                                                        (CoapMessage) coapRequest),
                  responseHandler);
    }
  }

  @Mock
  private ResourceManager fakeResourcesForCoapOverTcp;
  @Mock
  private DefaultJmsHandler fakeJmsHandler;
  @Autowired
  private ILinkCoapOverTcpMessageHandler iLinkCoapOverTcpMessageHandler;

  @Before
  public void setup() throws Exception {
    ReflectionTestUtils.setField(iLinkCoapOverTcpMessageHandler, "jmsHandler", fakeJmsHandler);
    ReflectionTestUtils.setField(iLinkCoapOverTcpMessageHandler,
                                 "resourcesForCoapOverTcp",
                                 fakeResourcesForCoapOverTcp);

    doAnswer(invocationOnMock -> {
      Object[] arguments = invocationOnMock.getArguments();
      Device device = (Device) arguments[0];
      IRequest request = (IRequest) arguments[1];
      IResponse response = MessageBuilder.createResponse(request, ResponseStatus.CREATED);

      logger.info("a response from fake COAP+TCP Handler {}", device);
      device.sendResponse(response);
      return null;
    }).when(fakeResourcesForCoapOverTcp).onRequestReceived(isA(Device.class), isA(IRequest.class));

    doAnswer(invocationOnMock -> {
      Object[] arguments = invocationOnMock.getArguments();
      Device device = (Device) arguments[0];
      IRequest request = (IRequest) arguments[1];
      IResponse response = MessageBuilder.createResponse(request, ResponseStatus.CREATED);

      logger.info("a response from fake JMS Handler {}", device);
      device.sendResponse(response);
      return null;
    }).when(fakeJmsHandler)
      .onDefaultRequestReceived(isA(Device.class), isA(IRequest.class), isA(ILinkMessage.class));
  }

  @Test
  public void test() throws Exception {
    logger.info(String.format("I am a info message vi SLF4j and %s", "logback"));
    logger.debug(String.format("I am a debug message vi SLF4j and %s", "logback"));
  }

  @Test
  public void testConnection() throws Exception {
    IBrokerClient client = new IBrokerClient("127.0.0.1", "__night-owl-13");
    client.start();
    assertThat(client.writable).isTrue();
    TimeUnit.SECONDS.sleep(2);
    client.stop();
  }

  @Test
  public void testHandShake() throws Exception {
    IBrokerClient client = new IBrokerClient("127.0.0.1", "__night-owl-13");

    client.start();

    assertThat(client.handShake().get(10, TimeUnit.SECONDS)).isTrue();

    client.stop();
  }

  @Test
  public void testMultipleClients() throws Exception {
    int clientsAmount = 3;
    CompletableFuture<Boolean>[] tasks = new CompletableFuture[clientsAmount];
    ExecutorService executorService = Executors.newFixedThreadPool(3);

    for (int i = 0; i < clientsAmount; i++) {
      int finalI = i;
      tasks[finalI] = new CompletableFuture<>();
      executorService.submit(() -> {
        IBrokerClient client = new IBrokerClient("127.0.0.1", "__night-owl-1" + finalI);
        try {
          client.startAndHandShakeAndAwait();
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          client.stop();
        }

        tasks[finalI].complete(true);
      });
    }

    CompletableFuture.allOf(tasks).join();

    assertThat(tasks[1].get()).isTrue();
  }

  @Ignore
  public void testPostRD() throws Exception {
    String deviceId = "__night-owl-13";
    // IBrokerClient client = new IBrokerClient("10.238.151.118", deviceId);
    IBrokerClient client = new IBrokerClient("127.0.0.1", deviceId);
    client.startAndHandShakeAndAwait();

    // prepare a ilink+coap message
    CoapMessage coapMessage =
        (CoapMessage) MessageBuilder.createRequest(RequestMethod.POST,
                                                   "/rd",
                                                   null,
                                                   ContentFormat.APPLICATION_TEXTPLAIN,
                                                   "[{\"aid\":\"__night-owl-13\",\"di\":\"__night-owl-13\",\"dt\":\"intel.iagent\",\"links\":[],\"st\":\"iagent\",\"status\":\"on\"}]".getBytes());
    ILinkMessage postRdRequest = ILinkMessageBuilder.createCOAPRequest(deviceId, 1, coapMessage);
    CompletableFuture<ILinkMessage> sendRequestFuture = new CompletableFuture<>();
    client.sendMessage(postRdRequest, response -> {
      logger.info("response ");
      sendRequestFuture.complete(response);
    });

    // fire
    ILinkMessage postRdResponse = sendRequestFuture.join();
    assertThat(postRdResponse).isNotNull();
    assertThat(postRdResponse.isResponse()).isTrue();

    List<CoapMessage> outMessageList = new LinkedList<>();
    CoapDecoder decoder = new CoapDecoder();
    ByteBuf payloadBinary = Unpooled.copiedBuffer(postRdResponse.getPayload());
    ReflectionTestUtils.invokeMethod(decoder, "decode", null, payloadBinary, outMessageList);
    assertThat(outMessageList).asList().isNotEmpty();

    CoapResponse coapResponse = (CoapResponse) outMessageList.get(0);
    assertThat(coapResponse.getStatus()).isEqualTo(ResponseStatus.CHANGED);

    // device
    client.stop();
  }

  @Ignore
  public void testPostDP() throws Exception {
    String deviceId = "__night-owl-13/tree1";
    String switch1 = "/switch/1/";
    String switch2 = "/switch/2/";
    // IBrokerClient client = new IBrokerClient("10.238.151.118", deviceId);
    IBrokerClient client = new IBrokerClient("127.0.0.1", deviceId);
    client.startAndHandShakeAndAwait();

    Map<String, String[]> resourceAndResourceType = new HashMap<>();
    resourceAndResourceType.put(switch1, new String[] {"oic.r.energy"});
    resourceAndResourceType.put(switch2, new String[] {"oic.r.energy"});

    CompletableFuture<Boolean> postRDFuture = new CompletableFuture<>();
    client.postRD(deviceId, resourceAndResourceType, response -> postRDFuture.complete(true));
    assertThat(postRDFuture.get(5, TimeUnit.SECONDS)).isTrue();

    // fire
    Random r = new Random(23);
    // can not be too large, has to refer to IBrokerClient.capacity
    int bigNumber = Math.min(10, client.capacity);
    ExecutorService executor = Executors.newFixedThreadPool(4);

    AtomicInteger failed = new AtomicInteger(0);
    CompletableFuture<Boolean>[] responseTasks = new CompletableFuture[bigNumber];
    for (int i = 0; i < bigNumber; i++) {
      responseTasks[i] = new CompletableFuture();
      int finalI = i;

      executor.submit(() -> {
        client.postDPTextPlainData(deviceId,
                                   (finalI & 0x1) == 0 ? switch1 : switch2,
                                   "current",
                                   Instant.now().toEpochMilli(),
                                   r.nextInt(bigNumber << 2),
                                   response -> {
                                     logger.debug("the response is back " + response.toString());
                                     responseTasks[finalI].complete(true);
                                     if (response == null || (response.getFlexHeader() != null
                                         && response.getFlexHeader().containsKey(ConstDef.FH_K_REP)
                                         && response.getResponseCode() == ConstDef.FH_V_FAIL)) {
                                       failed.incrementAndGet();
                                     }
                                   });
      }).get();
    }

    StopWatch stopWatch = new StopWatch("PUT/POST /dp");
    stopWatch.start("send " + bigNumber + " requests to /dp and get responses:");

    CompletableFuture.allOf(responseTasks).get(45, TimeUnit.SECONDS);
    stopWatch.stop();
    logger.info(stopWatch.prettyPrint());
    logger.debug("all responses are received, failed rated " + failed.get() * 1.0 / bigNumber * 100
        + "%");
    client.stop();
  }

  @Ignore
  public void testPostDPMultipleClients() throws Exception {
    int clientAmount = 10;
    Random r = new Random(23);
    // can not be too large, has to refer to IBrokerClient.capacity
    int bigNumber = Math.min(500, 5000);
    ExecutorService executor = Executors.newFixedThreadPool(clientAmount);

    List<IBrokerClient> clients = new LinkedList<>();
    List<StopWatch> stopWatchList = new LinkedList<>();
    List<CompletableFuture<Boolean>> clientTask = new LinkedList<>();

    String deviceIdPrefix = "__night-owl-13/tree";
    String switch1 = "/switch/1/";
    String switch2 = "/switch/2/";

    Map<String, String[]> resourceAndResourceType = new HashMap<>();
    resourceAndResourceType.put(switch1, new String[] {"oic.r.energy"});
    resourceAndResourceType.put(switch2, new String[] {"oic.r.energy"});

    for (int clientId = 1; clientId <= clientAmount; clientId++) {
      String deviceId = deviceIdPrefix + clientId;
      // IBrokerClient client = new IBrokerClient("10.238.151.118", deviceId);
      clients.add(new IBrokerClient("127.0.0.1", deviceId));
      stopWatchList.add(new StopWatch("to calcuate time consumption for #" + clientId + " client"));
    }

    for (int clientId = 1; clientId <= clientAmount; clientId++) {
      int finalClientId = clientId;
      CompletableFuture<Boolean> currentClientResult = new CompletableFuture<>();
      clientTask.add(currentClientResult);

      executor.submit(() -> {
        IBrokerClient client = clients.get(finalClientId - 1);
        StopWatch stopWatch = stopWatchList.get(finalClientId - 1);
        String deviceId = deviceIdPrefix + finalClientId;

        // handshake
        stopWatch.start("build a connection and handshake");
        client.startAndHandShakeAndAwait();
        stopWatch.stop();

        // POST /rd
        CompletableFuture<Boolean> postRDFuture = new CompletableFuture<>();
        stopWatch.start("post /rd");
        client.postRD(deviceId, resourceAndResourceType, response -> postRDFuture.complete(true));
        stopWatch.stop();
        try {
          assertThat(postRDFuture.get(1, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
          assertThat(false).isTrue();
          currentClientResult.complete(false);
        }

        List<CompletableFuture<Boolean>> currentClientResponseResultList = new LinkedList<>();

        stopWatch.start("post " + bigNumber + " data to /dp");
        for (int i = 0; i < bigNumber; i++) {
          int finalI = clientAmount * (finalClientId - 1) + i;

          CompletableFuture<Boolean> currentResponseTask = new CompletableFuture<>();
          currentClientResponseResultList.add(currentResponseTask);

          client.postDPTextPlainData(deviceId,
                                     (finalI & 0x1) == 0 ? switch1 : switch2,
                                     "current",
                                     Instant.now().toEpochMilli(),
                                     r.nextInt(bigNumber << 2),
                                     response -> {
                                       logger.debug("the response is back " + response.toString());
                                       currentResponseTask.complete(true);
                                     });
        }

        boolean responseFailed = false;

        try {
          CompletableFuture.allOf(currentClientResponseResultList.toArray(new CompletableFuture[currentClientResponseResultList.size()]))
                           .get(20, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
          responseFailed = true;
        }
        logger.debug("#" + finalClientId + " client has received all responses");

        stopWatch.stop();
        assertThat(currentClientResponseResultList.size()).isEqualTo(bigNumber);
        currentClientResult.complete(true);
      });
    }

    CompletableFuture.allOf(clientTask.toArray(new CompletableFuture[clientTask.size()]))
                     .get(40, TimeUnit.SECONDS);
    for (StopWatch stopWatch : stopWatchList) {
      logger.debug(stopWatch.prettyPrint());
    }
  }
}
