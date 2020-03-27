/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class MqClient {
  public static class ResponseHandlerSet {
    private IConnectResponseHandler handler;
    private Instant timeoutDate;
    private IConnectRequest iConnectRequest;

    public ResponseHandlerSet(IConnectRequest iConnectRequest, IConnectResponseHandler handler,
        int timeout, TimeUnit unit) {
      this.handler = handler;
      this.iConnectRequest = iConnectRequest;
      this.timeoutDate = Instant.now().plusSeconds(unit.toSeconds(timeout));
    }

    public IConnectResponseHandler getHandler() {
      return handler;
    }

    public Instant getTimeoutDate() {
      return timeoutDate;
    }

    public IConnectRequest getiConnectRequest() {
      return iConnectRequest;
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(MqClient.class);
  protected Map<String, ResponseHandlerSet> responseHandlers = new ConcurrentHashMap<>();
  protected String responseDst;

  protected ArrayList<IConnectJMSMessageListener> serviceListeners =
      new ArrayList<IConnectJMSMessageListener>();

  public MqClient() {
    // responseDst = "app." + Constants.generateUnicTopicByName(getJarName());
    responseDst = "app." + getJarName() + UUID.randomUUID().toString();
  }

  public abstract void addMessageListener(MessageListener serviceListener);

  public abstract void send(String dst, IConnectResponse response);

  abstract IConnectResponse send(IConnectRequest iConnectRequest);

  public abstract void send(Destination dst, IConnectResponse response);

  protected abstract void onMessageResponse(Message message);

  public void send(IConnectRequest iConnectRequest, IConnectResponseHandler handler, int timeout,
                   TimeUnit unit) {
    logger.debug("send " + iConnectRequest + " and respond with " + handler);
    if (iConnectRequest == null) {
      logger.error("Null iConnectRequest instance to send");
      return;
    }
    if (handler == null) {
      logger.warn("Will Not listen response [since handler is null] for message: "
          + iConnectRequest);
    } else {
      responseHandlers.put(iConnectRequest.getMessageID(),
                           new ResponseHandlerSet(iConnectRequest, handler, timeout, unit));
    }

    IConnectResponse ret = send(iConnectRequest);
    // only use that response from send() when there is exceptions
    if (ret.getStatus().isError()) {
      logger.warn("send " + iConnectRequest + " failed " + ret);
      if (handler != null) {
        responseHandlers.remove(iConnectRequest.getMessageID());
        handler.onResponse(ret);
      }
    } else {
      logger.debug("send " + iConnectRequest + " successful");
    }
  }

  protected String getJarName() {
    return Optional.ofNullable(Thread.currentThread().getStackTrace())
                   .filter(stacks -> stacks.length != 0)
                   .map(stacks -> stacks[stacks.length - 1].getClassName())
                   .map(mainClassName -> {
                     try {
                       return Class.forName(mainClassName);
                     } catch (ClassNotFoundException e) {
                       return null;
                     }
                   })
                   .map(mainClass -> mainClass.getProtectionDomain())
                   .map(domain -> domain.getCodeSource())
                   .map(srcCode -> srcCode.getLocation())
                   .map(location -> location.getFile())
                   .filter(fullPath -> !fullPath.isEmpty())
                   .map(fullPath -> fullPath.substring(fullPath.lastIndexOf(File.separator) + 1))
                   .filter(fileName -> !fileName.isEmpty())
                   .filter(fileName -> fileName.contains(".jar") || fileName.contains(".war"))
                   .map(fileName -> fileName.replace(".jar", ""))
                   .map(fileName -> fileName.replace(".war", ""))
                   .orElseGet(() -> "SDK");
  }

  public void checkMessageIfTimeout() {
    if (responseHandlers.size() == 0)
      return;

    ArrayList<String> idsToBeDel = new ArrayList<String>();

    for (String messageID : responseHandlers.keySet()) {
      ResponseHandlerSet handler = responseHandlers.get(messageID);
      if (handler != null) {
        if (handler.getTimeoutDate().isBefore(Instant.now())) {
          idsToBeDel.add(messageID);
        }
      } else {
        idsToBeDel.add(messageID);
      }
    }

    if (idsToBeDel.size() > 0)
      idsToBeDel.forEach(messageID -> {
        logger.debug("remove " + messageID + " by timeout checker");
        ResponseHandlerSet handler = responseHandlers.remove(messageID);

        if (handler != null) {
          IConnectResponse response =
              IConnectResponse.createFromRequest(handler.getiConnectRequest(),
                                                 HttpStatus.REQUEST_TIMEOUT,
                                                 MediaType.TEXT_PLAIN,
                                                 null);
          logger.warn("generated: " + response);
          handler.getHandler().onResponse(response);
        }
      });
  }

  protected String getJmsHeadInfo(Message message) {
    StringBuilder sb = new StringBuilder("[ ");
    try {
      sb.append("corId=" + message.getJMSCorrelationID())
        .append(" replyto=" + message.getJMSReplyTo());
    } catch (JMSException e) {
      logger.error("Exception: " + e.getMessage());
      sb.append("fail to get jms header");
    }
    sb.append(" ]");
    return sb.toString();
  }
}
