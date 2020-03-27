/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.service;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.sdk.utilities.UrlUtil;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jms.JmsException;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListenerConfigurer;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.jms.*;
import java.util.Map;
import java.util.Set;

@Component
@EnableScheduling
@EnableJms
@Configuration
public class JmsMqClient extends MqClient {
  private static final Logger logger = LoggerFactory.getLogger(JmsMqClient.class.getName());

  // @Autowired
  private JmsListenerEndpointRegistrar registar;

  @Autowired
  private JmsListenerEndpointRegistry registry;

  @Autowired
  @Qualifier(value = "sdkJmsListenerContainerFactory")
  private DefaultJmsListenerContainerFactory sdkJmsListenerContainerFactory;

  @Autowired
  private JmsListenerConfigurer jmsListenerConfigurer;

  @Autowired
  @Qualifier(value = "sdkJmsTemplate")
  private JmsTemplate jmsTemplateSimpleConvert;

  private void addJmsListener(IConnectJMSMessageListener jmsListener) {
    if (registar != null && jmsListener != null) {

      if (!jmsListener.isListening()) {
        IConnectService service = jmsListener.getAttachedService();
        Set<String> urls = service.getServiceUrl();
        for (String url : urls) {
          registar.setEndpointRegistry(registry);
          registar.setContainerFactory(sdkJmsListenerContainerFactory);
          SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
          endpoint.setId("rest_" + url);
          endpoint.setConcurrency("1");
          endpoint.setDestination(url);
          endpoint.setMessageListener(jmsListener);
          registar.registerEndpoint(endpoint, sdkJmsListenerContainerFactory);
          logger.info("listening: " + url + " [for requests]");
        }
      }
    }
  }

  @Bean
  JmsListenerConfigurer jmsListenerConfigurer(@Qualifier(
      value = "sdkJmsListenerContainerFactory") DefaultJmsListenerContainerFactory sdkJmsListenerContainerFactory) {
    logger.debug("create jmsListenerConfigurer");
    return new JmsListenerConfigurer() {
      @Override
      public void configureJmsListeners(JmsListenerEndpointRegistrar reg) {
        JmsMqClient iconn = ApplicationContextProvider.getBean(JmsMqClient.class);
        iconn.registar = reg;
        iconn.registar.setEndpointRegistry(registry);
        iconn.registar.setContainerFactory(sdkJmsListenerContainerFactory);

        // listening response for all IConnectRequest send by service as
        // client
        SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
        endpoint.setId(responseDst);
        endpoint.setConcurrency("1-4");
        endpoint.setDestination(responseDst);
        endpoint.setMessageListener(message -> onMessageResponse(message));
        registar.registerEndpoint(endpoint, sdkJmsListenerContainerFactory);
        logger.info("listening:  " + responseDst + " [for response]");

        // listening url of all service
        for (IConnectJMSMessageListener listener : serviceListeners) {
          if (!listener.isListening()) {
            addJmsListener(listener);
            listener.setListening(true);
          }
        }
      }
    };
  }

  @Bean
  public ConnectionFactory connectionFactory(@Value(value = "${mq.host}") String mqHost) {
    String url = "tcp://" + mqHost + ":61616";
    logger.info("trying to connect: " + url);
    ActiveMQConnectionFactory activeMQConnection = new ActiveMQConnectionFactory(url);
    activeMQConnection.setExceptionListener(exception -> {
      logger.error("current activemq connection meets an exception " + exception);
      logger.error(BaseUtil.getStackTrace(exception));
    });

    PooledConnectionFactory connFactory = new PooledConnectionFactory();
    connFactory.setConnectionFactory(activeMQConnection);
    connFactory.setReconnectOnException(true);
    return connFactory;
  }

  @Bean(name = "sdkJmsListenerContainerFactory")
  public DefaultJmsListenerContainerFactory
      sdkJmsListenerContainerFactory(ConnectionFactory connectionFactory) {

    DefaultJmsListenerContainerFactory listenerContainerFactory =
        new DefaultJmsListenerContainerFactory();
    listenerContainerFactory.setConnectionFactory(connectionFactory);
    listenerContainerFactory.setSessionTransacted(true);
    listenerContainerFactory.setMessageConverter(new SimpleMessageConverter());
    listenerContainerFactory.setErrorHandler(exception -> {
      logger.error("there is an exception during jms listener processing incoming messages");
      logger.error(BaseUtil.getStackTrace(exception));
    });
    // listenerContainerFactory.setPubSubDomain(true);

    logger.debug("create DefaultJmsListenerContainerFactory " + listenerContainerFactory);

    return listenerContainerFactory;
  }

  @Bean(name = "sdkJmsTemplate")
  public JmsTemplate jmsTemplateSimpleConvert(ConnectionFactory connectionFactory) {
    JmsTemplate template = new JmsTemplate();
    template.setConnectionFactory(connectionFactory);
    template.setMessageConverter(new SimpleMessageConverter());

    template.setExplicitQosEnabled(false);
    return template;
  }

  public void addMessageListener(MessageListener listener) {
    IConnectJMSMessageListener serviceListener = (IConnectJMSMessageListener) listener;

    // add listener into JMS if registar bean available
    if (registar != null) {
      addJmsListener(serviceListener);
      serviceListener.setListening(true);
    }

    // need to hold the listener object in runtiming
    serviceListeners.add(serviceListener);
  }

  protected void onMessageResponse(Message message) {
    ActiveMQMapMessage responseMessage = (ActiveMQMapMessage) message;
    String messageID = responseMessage.getJMSCorrelationID();

    ResponseHandlerSet handler = responseHandlers.get(messageID);
    if (handler != null) {
      MediaType fmt;
      try {
        fmt = MediaType.valueOf(responseMessage.getString(ConstDef.JMS_MSG_KEY_PAYLOAD_FMT));
        HttpStatus status =
            HttpStatus.valueOf(responseMessage.getString(ConstDef.JMS_MSG_KEY_STATUS));
        IConnectResponse response =
            IConnectResponse.createFromRequest(handler.getiConnectRequest(),
                                               status,
                                               fmt,
                                               responseMessage.getBytes(ConstDef.JMS_MSG_KEY_PAYLOAD));
        logger.info("receive: " + response);
        handler.getHandler().onResponse(response);
        responseHandlers.remove(messageID);
      } catch (JMSException e) {
        logger.error("Exception: " + BaseUtil.getStackTrace(e));;
      }
    } else {
      logger.error("No response handler for mesasage id: " + messageID);
    }
  }

  @Scheduled(fixedRate = 1000)
  private void checkMessageTimeout() {
    try {
      super.checkMessageIfTimeout();
    } catch (Exception e) {
      logger.error("met an exception during time out chekcing " + e);
      logger.error(BaseUtil.getStackTrace(e));
    }
  }

  public void send(String dst, IConnectResponse response) {
    if (dst == null) {
      logger.error("No dst set for sending IConnectRequest");
      return;
    }

    jmsTemplateSimpleConvert.send(dst, session -> {
      ActiveMQMapMessage message = (ActiveMQMapMessage) session.createMapMessage();
      message.setJMSCorrelationID(response.getMessageID());
      message.setString(ConstDef.JMS_MSG_KEY_STATUS, response.getStatus().name());
      message.setString(ConstDef.JMS_MSG_KEY_PAYLOAD_FMT, response.getFormat().toString());
      message.setBytes(ConstDef.JMS_MSG_KEY_PAYLOAD, response.getPayload());
      logger.info("send response 1:  " + response.toString() + "   to " + dst);
      return message;
    });
  }

  public IConnectResponse send(IConnectRequest iConnectRequest) {
    if (iConnectRequest == null) {
      logger.warn("Null iConnectRequest instance to send");
      return IConnectResponse.createFromRequest(iConnectRequest,
                                                HttpStatus.BAD_REQUEST,
                                                MediaType.TEXT_PLAIN,
                                                "an empty request".getBytes());
    }

    // since we are not sure users' attempts, we are going to always remove tailing slashes
    String url = UrlUtil.getPath(iConnectRequest.getUrl());
    url = BaseUtil.removeTrailingSlash(url);

    String dst = iConnectRequest.getDestination();
    if (dst != null) {
      dst = BaseUtil.removeTrailingSlash(dst);
    }

    if (dst != null && !dst.isEmpty() && !url.startsWith(dst)) {
      logger.warn("the destination " + dst + " has to be the prefix of the url " + url);
      return IConnectResponse.createFromRequest(iConnectRequest,
                                                HttpStatus.BAD_REQUEST,
                                                MediaType.TEXT_PLAIN,
                                                "the destination has to be the prefix of the url".getBytes());
    }

    dst = dst == null || dst.isEmpty() ? url : dst;
    try {
      String finalDst = dst;
      jmsTemplateSimpleConvert.send(finalDst, session -> {
        ActiveMQMapMessage message = (ActiveMQMapMessage) session.createMapMessage();
        message.setJMSReplyTo(new ActiveMQQueue(responseDst));
        message.setJMSCorrelationID(iConnectRequest.getMessageID());
        message.setString(ConstDef.JMS_MSG_KEY_ACTION, iConnectRequest.getAction().name());
        message.setString(ConstDef.JMS_MSG_KEY_PAYLOAD_FMT, iConnectRequest.getFormat().toString());
        message.setString(ConstDef.JMS_MSG_KEY_URI, iConnectRequest.getUrl());
        message.setBytes(ConstDef.JMS_MSG_KEY_PAYLOAD, iConnectRequest.getPayload());

        Map<String, String> params = iConnectRequest.getTokenInfo();
        if (params != null && !params.isEmpty()) {
          message.setObjectProperty(ConstDef.JMS_MSG_KEY_EXTRA, params);
        }

        logger.info("send request:  " + iConnectRequest + "   to " + finalDst + " with JMS "
            + getJmsHeadInfo(message));
        return message;
      });
    } catch (JmsException e) {
      logger.error(BaseUtil.getStackTrace(e));;
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

  public void send(Destination dst, IConnectResponse response) {
    if (dst == null) {
      logger.error("No dst set for sending IConnectRequest");
      return;
    }

    jmsTemplateSimpleConvert.send(dst, session -> {
      ActiveMQMapMessage message = (ActiveMQMapMessage) session.createMapMessage();
      message.setJMSCorrelationID(response.getMessageID());
      message.setString(ConstDef.JMS_MSG_KEY_STATUS, response.getStatus().name());
      message.setString(ConstDef.JMS_MSG_KEY_PAYLOAD_FMT, response.getFormat().toString());
      message.setBytes(ConstDef.JMS_MSG_KEY_PAYLOAD, response.getPayload());
      logger.info("send response 2:  " + response.toString() + "   to " + dst + " with JMS "
          + getJmsHeadInfo(message));
      return message;
    });
  }
}
