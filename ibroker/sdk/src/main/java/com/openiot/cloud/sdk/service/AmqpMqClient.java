/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.service;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.sdk.utilities.UrlUtil;
import org.apache.qpid.amqp_1_0.jms.*;
import org.apache.qpid.amqp_1_0.jms.impl.AmqpMessageImpl;
import org.apache.qpid.amqp_1_0.jms.impl.ConnectionFactoryImpl;
import org.apache.qpid.amqp_1_0.jms.impl.DestinationImpl;
import org.apache.qpid.amqp_1_0.jms.impl.QueueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AmqpMqClient extends MqClient {
  private static final Logger logger = LoggerFactory.getLogger(AmqpMqClient.class);

  private String mqUrl;
  private Session amqpSession;
  private Connection amqpConnection;
  private ScheduledFuture<?> timeoutFuture;
  private Map<String, MessageProducer> producerPool =
      new ConcurrentHashMap<String, MessageProducer>();

  public AmqpMqClient(String mqServerHost, String mqUser, String mqPassword) {
    this.mqUrl = "amqp://" + mqUser + ":" + mqPassword + "@" + mqServerHost + ":5672";
    amqpConnection();
    registerResponseHandler();

    // @Scheduled(fixedRate = 1000)
    timeoutFuture = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
      try {
        super.checkMessageIfTimeout();
      } catch (Exception e) {
        logger.error("just met an exception during timeout check " + e.getLocalizedMessage());
        logger.error(BaseUtil.getStackTrace(e));
      }
    }, 5, 10, TimeUnit.SECONDS);
  }

  public void registerResponseHandler() {
    try {
      Destination dst = QueueImpl.createQueue(responseDst);
      amqpSession.createConsumer(dst).setMessageListener(message -> onMessageResponse(message));
      logger.info("listening:  " + responseDst);
    } catch (JMSException e) {
      logger.error("listening[FAIL]:  " + responseDst + "  for Exception: " + e.getMessage());
    }
  }

  @Override
  public void addMessageListener(MessageListener listener) {

    IConnectAmqpMessageListener jmsListener = (IConnectAmqpMessageListener) listener;

    IConnectService service = jmsListener.getAttachedService();
    Set<String> urls = service.getServiceUrl();
    for (String url : urls) {
      logger.info("listening:  " + url);
      try {
        Destination dst = QueueImpl.createQueue(url.trim());
        amqpSession.createConsumer(dst).setMessageListener(jmsListener);
      } catch (JMSException e) {
        logger.error("listening[Fail]: " + url + " for: " + e.getMessage());
      }
    }
  }

  public void amqpConnection() {
    try {
      logger.info("trying to setup a connection with url: " + mqUrl);
      ConnectionFactory factory = ConnectionFactoryImpl.createFromURL(mqUrl);
      amqpConnection = factory.createConnection();
      if (amqpConnection == null) {
        logger.warn("can not create a connection with " + mqUrl + " from the connection factory");
        return;
      }

      amqpConnection.start();
      int majorVersion = amqpConnection.getMetaData().getAMQPMajorVersion();
      int minorVersion = amqpConnection.getMetaData().getAMQPMinorVersion();
      logger.info("establish a connection which follows AMQP version " + majorVersion + "."
          + minorVersion);

      amqpSession = amqpConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      if (amqpSession == null) {
        logger.warn("can not create a session with " + mqUrl + " from the connection factory");
      } else {
        logger.info("hold a session " + amqpSession);
      }

      amqpConnection.setExceptionListener(exception -> {
        logger.error("the AMQP client losts its connection \n" + BaseUtil.getStackTrace(exception));
        Executors.newSingleThreadExecutor().submit(() -> {
          try {
            logger.info("going to start re-connect in 3 seconds");
            amqpSession = null;
            amqpConnection = null;
            TimeUnit.SECONDS.sleep(3);
            amqpConnection();
          } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception: " + BaseUtil.getStackTrace(e));
          }
        });
      });
    } catch (Exception e) {
      logger.error("Exception: " + BaseUtil.getStackTrace(e));
    }
  }

  @Override
  public void send(String dst, IConnectResponse response) {
    if (dst == null) {
      logger.error("No dst set for sending IConnectResponse");
      return;
    }
    try {
      BytesMessage message = amqpSession.createBytesMessage();
      message.setReplyTo(responseDst);
      message.setJMSCorrelationID(response.getMessageID());
      message.setStringProperty(ConstDef.JMS_MSG_KEY_STATUS, response.getStatus().name());
      message.setStringProperty(ConstDef.JMS_MSG_KEY_PAYLOAD_FMT, response.getFormat().toString());
      if (response.getPayload() != null)
        message.writeBytes(response.getPayload());

      if (producerPool.get(dst.trim()) == null) {
        producerPool.put(dst.trim(), amqpSession.createProducer(QueueImpl.createQueue(dst.trim())));
      }
      producerPool.get(dst.trim()).send(message,
                                        DeliveryMode.NON_PERSISTENT,
                                        Message.DEFAULT_PRIORITY,
                                        Message.DEFAULT_TIME_TO_LIVE);

      logger.info("send:  " + response.toString() + "   to " + dst);
    } catch (Exception e) {
      logger.info("[FAIL] send:  " + response.toString() + "   to " + dst);
    }
  }

  @Override
  public IConnectResponse send(IConnectRequest iConnectRequest) {
    if (iConnectRequest == null) {
      logger.error("Null iConnectRequest instance to send");
      return IConnectResponse.createFromRequest(iConnectRequest,
                                                HttpStatus.BAD_REQUEST,
                                                MediaType.TEXT_PLAIN,
                                                "an empty request".getBytes());
    }
    String url = UrlUtil.getPath(iConnectRequest.getUrl());
    String dst = iConnectRequest.getDestination();
    if (dst != null && !url.startsWith(dst)) {
      logger.warn("the destination " + dst + " has to be the prefix of the url " + url);
      return IConnectResponse.createFromRequest(iConnectRequest,
                                                HttpStatus.BAD_REQUEST,
                                                MediaType.TEXT_PLAIN,
                                                "the destination has to be the prefix of the url".getBytes());
    }

    dst = dst == null || dst.isEmpty() ? url : dst.trim();

    try {
      BytesMessage message = amqpSession.createBytesMessage();
      // for system properties
      message.setReplyTo(responseDst);
      message.setJMSCorrelationID(iConnectRequest.getMessageID());
      message.setJMSType(ConstDef.JMSTYPE_REQUEST);

      // for application properties
      message.setStringProperty(ConstDef.JMS_MSG_KEY_ACTION, iConnectRequest.getAction().name());
      message.setStringProperty(ConstDef.JMS_MSG_KEY_PAYLOAD_FMT,
                                iConnectRequest.getFormat().toString());
      message.setStringProperty(ConstDef.JMS_MSG_KEY_URI, iConnectRequest.getUrl());

      // for application properties
      Map params = iConnectRequest.getTokenInfo();
      if (params != null && !params.isEmpty()) {
        message.setMapProperty(ConstDef.JMS_MSG_KEY_EXTRA, params);
      }

      // for message body
      if (iConnectRequest.getPayload() != null)
        message.writeBytes(iConnectRequest.getPayload());

      if (producerPool.get(dst) == null) {
        producerPool.put(dst, amqpSession.createProducer(QueueImpl.createQueue(dst)));
      }
      producerPool.get(dst).send(message,
                                 DeliveryMode.NON_PERSISTENT,
                                 Message.DEFAULT_PRIORITY,
                                 Message.DEFAULT_TIME_TO_LIVE);

      logger.info("send:  " + iConnectRequest + "   to " + dst + " with JMS "
          + getJmsHeadInfo(message));
    } catch (Exception e) {
      logger.info("[FAIL] send:  " + iConnectRequest + "   to " + iConnectRequest.getUrl());
      logger.error("Exception: " + e.getMessage());
      return IConnectResponse.createFromRequest(iConnectRequest,
                                                HttpStatus.EXPECTATION_FAILED,
                                                MediaType.TEXT_PLAIN,
                                                "an empty request".getBytes());
    }

    return IConnectResponse.createFromRequest(iConnectRequest,
                                              HttpStatus.PROCESSING,
                                              MediaType.TEXT_PLAIN,
                                              "send successful".getBytes());
  }

  @Override
  public void send(Destination dst, IConnectResponse response) {
    if (dst == null) {
      logger.error("No dst set for sending IConnectResponse");
      return;
    }
    String strDst = ((DestinationImpl) dst).getAddress();
    try {
      BytesMessage message = amqpSession.createBytesMessage();
      message.setReplyTo(responseDst);
      message.setJMSCorrelationID(response.getMessageID());
      message.setStringProperty(ConstDef.JMS_MSG_KEY_STATUS, response.getStatus().name());
      message.setStringProperty(ConstDef.JMS_MSG_KEY_PAYLOAD_FMT, response.getFormat().toString());
      if (response.getPayload() != null)
        message.writeBytes(response.getPayload());
      if (producerPool.get(strDst) == null) {
        producerPool.put(strDst, amqpSession.createProducer(dst));
      }
      producerPool.get(strDst).send(message,
                                    DeliveryMode.NON_PERSISTENT,
                                    Message.DEFAULT_PRIORITY,
                                    Message.DEFAULT_TIME_TO_LIVE);

      logger.info("send:  " + response.toString() + "   to " + strDst);
    } catch (Exception e) {
      logger.info("[FAIL] send:  " + response.toString() + "   to " + strDst);
    }
  }

  public void deregisterClient() {
    try {
      timeoutFuture.cancel(true);

      for (String key : producerPool.keySet()) {
        producerPool.get(key).close();
      }

      amqpSession.close();
      amqpSession = null;
      amqpConnection.stop();
      amqpConnection.close();
      amqpConnection = null;
      logger.info("close amqp session");
    } catch (Exception e) {
      logger.error("Fail to close amqp session for: " + e.getMessage());;
    }
  }

  @Override
  protected void onMessageResponse(javax.jms.Message message) {
    try {
      String messageID = message.getJMSCorrelationID();
      if (messageID == null) {
        logger.info(System.identityHashCode(message) + " incoming message doesn't have messageID");
        return;
      }

      logger.debug(System.identityHashCode(message) + " incoming message id is " + messageID);

      if (!BytesMessage.class.isAssignableFrom(message.getClass())) {
        logger.debug(messageID + " can not cast to ByteMessage");
        if (AmqpMessageImpl.class.isAssignableFrom(message.getClass())) {
          logger.info("incoming response " + (AmqpMessageImpl) message);
        } else {
          logger.debug(messageID + "can not cast to AmqpMessageImpl");
        }
        return;
      }

      ResponseHandlerSet handler = responseHandlers.get(messageID);
      if (handler != null) {
        BytesMessage responseMessage = (BytesMessage) message;
        MediaType fmt =
            MediaType.valueOf(responseMessage.getStringProperty(ConstDef.JMS_MSG_KEY_PAYLOAD_FMT));
        HttpStatus status =
            HttpStatus.valueOf(responseMessage.getStringProperty(ConstDef.JMS_MSG_KEY_STATUS));

        byte[] msgBody = new byte[(int) ((BytesMessage) message).getBodyLength()];
        ((BytesMessage) message).readBytes(msgBody, msgBody.length);
        IConnectResponse response =
            IConnectResponse.createFromRequest(handler.getiConnectRequest(), status, fmt, msgBody);
        logger.info("receive: " + response);
        handler.getHandler().onResponse(response);
        responseHandlers.remove(messageID);
      } else {
        logger.error("No response handler for the message id: " + messageID + " on "
            + System.identityHashCode(this));
        logger.debug("dump current responseHandlers keys " + responseHandlers.keySet());
      }
    } catch (Exception e1) {
      logger.error("Exception: " + BaseUtil.getStackTrace(e1));
    }
  }
}
