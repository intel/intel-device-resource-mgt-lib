/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.service;

import com.openiot.cloud.base.help.MessageIdMaker;
import com.openiot.cloud.sdk.utilities.UrlUtil;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class IConnectRequest {
  public String destination;
  public String messageID;
  public HttpMethod action;
  public String url;
  public MediaType format;
  public byte[] payload;
  public Map<String, String> tokenInfo = new HashMap<String, String>();

  // static Map <String, IConnectRequestSender> senders; //TODO used in future

  IConnectResponseSender responseSender;

  public static IConnectRequest create(HttpMethod action, String url, MediaType format,
                                       byte[] payload) {
    return create(action, url, format, payload, MessageIdMaker.getMessageIdAsInteger());
  }

  public static IConnectRequest create(HttpMethod action, String url, MediaType format,
                                       byte[] payload, int messageID) {
    return create(null, action, url, format, payload, messageID);
  }

  public static IConnectRequest create(String destination, HttpMethod action, String url,
                                       MediaType format, byte[] payload, int messageID) {
    IConnectRequest req = new IConnectRequest();
    req.setMessageID(Integer.toString(messageID));
    req.setAction(action);
    req.setUrl(url);
    req.setFormat(format);
    req.setPayload(payload);
    req.setDestination(destination);
    return req;
  }

  public String getMessageID() {
    return messageID;
  }

  public void setMessageID(String messageID) {
    this.messageID = messageID;
  }

  public HttpMethod getAction() {
    return action;
  }

  public void setAction(HttpMethod action) {
    this.action = action;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String path) {
    this.url = path;
  }

  public MediaType getFormat() {
    return format;
  }

  public void setFormat(MediaType format) {
    this.format = format == null ? MediaType.TEXT_PLAIN : format;
  }

  public Map<String, String> getTokenInfo() {
    return tokenInfo;
  }

  public String getTokenInfo(String key) {
    return tokenInfo.get(key);
  }

  public void setTokenInfo(String key, String value) {
    this.tokenInfo.put(key, value);
  }

  public byte[] getPayload() {
    return payload;
  }

  public void setPayload(byte[] payload) {
    this.payload = payload;
  }

  public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public void send(IConnectResponseHandler handler, int timeout, TimeUnit unit) {

    String protocol = UrlUtil.getPotocol(url);

    if ("http".equals(protocol)) {
    } else if ("coap_tcp".equals(protocol)) {
      // support in future
    } else { // jms:// as default
      IConnect.getInstance().send(this, handler, timeout, unit);
    }
  }

  private String dumpByteArray(byte[] data) {
    if (Objects.isNull(data) || data.length == 0)
      return "";
    StringBuilder builder = new StringBuilder();
    for (byte b : data) {
      if (builder.length() != 0) {
        builder.append(",");
      }
      builder.append(String.format("0x%02x", b));
      if (Character.isLetterOrDigit(b)) {
        builder.append(String.format("[%s]", (char) b));
      }
    }
    return builder.toString();
  }

  @Override
  public String toString() {
    String strPayload = dumpByteArray(payload);
    return "IConnectRequest [messageID=" + messageID + ", action=" + action + ", uri=" + url
        + ", format=" + format + ", tokenInfo:" + tokenInfo + ", payload=" + strPayload + "]";
  }

  public void setResponseSender(JMSResponseSender jmsResponseSender) {
    responseSender = jmsResponseSender;
  }
}
