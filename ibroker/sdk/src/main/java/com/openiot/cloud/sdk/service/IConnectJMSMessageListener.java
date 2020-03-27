/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.service;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.sdk.utilities.UrlUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.Map;
import java.util.Objects;

@Slf4j
class IConnectJMSMessageListener implements MessageListener {
  IConnectService attached_service;
  private boolean isListening;

  public IConnectJMSMessageListener(IConnectService service) {
    isListening = false;
    attached_service = service;
  }

  @Override
  public void onMessage(Message message) {
    log.debug("onMessage {}", message);

    if (attached_service != null) {
      try {
        IConnectRequest request = null;
        if (message instanceof ActiveMQBytesMessage) {
          log.debug("parse it as a ActiveMQBytesMessage");
          ActiveMQBytesMessage bytesMessage = (ActiveMQBytesMessage) message;

          request = new IConnectRequest();
          request.setMessageID(message.getJMSCorrelationID());
          request.setResponseSender(Objects.nonNull(message.getJMSReplyTo())
              ? new JMSResponseSender(message.getJMSReplyTo())
              : null);

          // everything is in properties and applicationProperties
          Map<String, Object> properties = bytesMessage.getProperties();
          log.debug("properties is {}", properties);

          // ACTION
          UTF8Buffer buffer = ((UTF8Buffer) properties.get(ConstDef.JMS_MSG_KEY_ACTION));
          if (Objects.isNull(buffer)) {
            log.warn("there is no action field in {}", message);
            return;
          }
          request.setAction(HttpMethod.valueOf(buffer.toString()));

          // URL
          buffer = ((UTF8Buffer) properties.get(ConstDef.JMS_MSG_KEY_URI));
          if (Objects.isNull(buffer)) {
            log.warn("there is no url field in {}", message);
            return;
          }
          request.setUrl(buffer.toString());

          // PAYLOAD_FORMAT
          buffer = ((UTF8Buffer) properties.get(ConstDef.JMS_MSG_KEY_PAYLOAD_FMT));
          request.setFormat(Objects.isNull(buffer) || buffer.isEmpty() ? null
              : MediaType.valueOf(buffer.toString()));

          // PAYLOAD
          buffer = ((UTF8Buffer) properties.get(ConstDef.JMS_MSG_KEY_PAYLOAD));
          request.setPayload(Objects.nonNull(buffer) && !buffer.isEmpty() ? buffer.data : null);

          Map<String, Object> extraParams =
              (Map<String, Object>) properties.get(ConstDef.JMS_MSG_KEY_EXTRA);
          if (Objects.nonNull(extraParams)) {
            for (Map.Entry<String, Object> e : extraParams.entrySet()) {
              request.setTokenInfo(e.getKey(), (String) e.getValue());
            }
          }
        } else if (message instanceof ActiveMQMapMessage) {
          log.debug("parse it as a ActiveMQMapMessage");
          ActiveMQMapMessage mapMessage = (ActiveMQMapMessage) message;

          request = new IConnectRequest();
          request.setMessageID(message.getJMSCorrelationID());
          request.setResponseSender(Objects.nonNull(message.getJMSReplyTo())
              ? new JMSResponseSender(message.getJMSReplyTo())
              : null);

          request.setAction(HttpMethod.valueOf(mapMessage.getString(ConstDef.JMS_MSG_KEY_ACTION)));
          request.setUrl(mapMessage.getString(ConstDef.JMS_MSG_KEY_URI));
          request.setFormat(MediaType.valueOf(mapMessage.getString(ConstDef.JMS_MSG_KEY_PAYLOAD_FMT)));
          request.setPayload(mapMessage.getBytes(ConstDef.JMS_MSG_KEY_PAYLOAD));

          Map<String, Object> extraParams =
              (Map<String, Object>) mapMessage.getObject(ConstDef.JMS_MSG_KEY_EXTRA);
          if (Objects.nonNull(extraParams)) {
            for (Map.Entry<String, Object> e : extraParams.entrySet()) {
              request.setTokenInfo((String) e.getKey(), (String) e.getValue());
            }
          }
        } else {
          log.warn("unexpected Message type {}", message.getClass());
        }

        log.debug("going to send {}", request);
        attached_service.onServiceRequest(getPathWithoutQueryParam(request.getUrl()), request);
      } catch (Exception e) {
        log.warn("meet an exception {}", e);
      }
    } else {
      log.warn("No any message handler for message {}", message.toString());
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
