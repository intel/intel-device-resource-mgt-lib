/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.service;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.sdk.utilities.UrlUtil;
import org.apache.qpid.amqp_1_0.jms.BytesMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.Map;

class IConnectAmqpMessageListener implements MessageListener {
  private static Logger logger = LoggerFactory.getLogger(IConnectAmqpMessageListener.class);
  IConnectService attached_service;
  private boolean isListening;

  public IConnectAmqpMessageListener(IConnectService service) {
    isListening = false;
    attached_service = service;
  }

  @Override
  public void onMessage(Message message) {
    if (attached_service != null) {
      try {
        // if (!(message instanceof BytesMessage)) {
        if (!BytesMessage.class.isAssignableFrom(message.getClass())) {
          logger.error(System.identityHashCode(message) + " " + message.getJMSCorrelationID()
              + " unsupport message type: " + message.getClass());
          return;
        }

        BytesMessage restMessage = (BytesMessage) message;
        byte[] payload = new byte[(int) restMessage.getBodyLength()];
        restMessage.readBytes(payload);
        String action = restMessage.getStringProperty(ConstDef.JMS_MSG_KEY_ACTION);
        IConnectRequest req =
            IConnectRequest.create(action == null ? HttpMethod.GET : HttpMethod.valueOf(action),
                                   restMessage.getStringProperty(ConstDef.JMS_MSG_KEY_URI),
                                   MediaType.valueOf(restMessage.getStringProperty(ConstDef.JMS_MSG_KEY_PAYLOAD_FMT)),
                                   payload);
        req.setMessageID(restMessage.getJMSCorrelationID());
        // TOKEN INFO
        Map<Object, Object> extraParams = restMessage.getMapProperty(ConstDef.JMS_MSG_KEY_EXTRA);
        if (extraParams != null && !extraParams.isEmpty()) {
          extraParams.entrySet().stream().forEach(e -> {
            req.setTokenInfo((String) e.getKey(), (String) e.getValue());
          });
        }

        // logger.warn("receive rest requst [from:"+restMessage.getJMSReplyTo()+"
        // correlationId:"+restMessage.getJMSCorrelationID()+"]: "+restMessage);

        if (restMessage.getJMSReplyTo() != null)
          req.setResponseSender(new JMSResponseSender(restMessage.getJMSReplyTo()));

        attached_service.onServiceRequest(getPathWithoutQueryParam(restMessage.getStringProperty(ConstDef.JMS_MSG_KEY_URI)),
                                          req);
      } catch (Exception e) {
        logger.error("Exception: " + BaseUtil.getStackTrace(e));
      }
    } else {
      logger.warn("No message handler for message:" + message.toString());
    }
  }

  private String getPathWithoutQueryParam(String pathWithQueryParam) {
    return UrlUtil.getPath(pathWithQueryParam);
  }

  public IConnectService getAttachedService() {
    return attached_service;
  }

  public IConnectService getAttached_service() {
    return attached_service;
  }

  public void setAttached_service(IConnectService attached_service) {
    this.attached_service = attached_service;
  }

  public boolean isListening() {
    return isListening;
  }

  public void setListening(boolean isListening) {
    this.isListening = isListening;
  }
}
