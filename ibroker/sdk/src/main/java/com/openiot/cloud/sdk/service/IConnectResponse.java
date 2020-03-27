/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class IConnectResponse {
  private static final Logger logger = LoggerFactory.getLogger(IConnectResponse.class);
  public String messageID;
  public HttpStatus status;
  public MediaType format;
  public byte[] payload;

  private IConnectResponseSender responseSender;

  // users tend to give format null when there is no payload
  // but following code might depends on the format value
  public static IConnectResponse createFromRequest(IConnectRequest request, HttpStatus status,
                                                   MediaType format, byte[] payload) {
    IConnectResponse resp = new IConnectResponse();
    resp.setMessageID(request.getMessageID());
    resp.setStatus(status);
    resp.setFormat(format == null ? MediaType.TEXT_PLAIN : format);
    resp.setPayload(payload);
    resp.responseSender = request.responseSender;
    logger.debug("a response {} for {}", resp, request);
    return resp;
  }

  public String getMessageID() {
    return messageID;
  }

  public void setMessageID(String messageID) {
    this.messageID = messageID;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public void setStatus(HttpStatus status) {
    this.status = status;
  }

  public MediaType getFormat() {
    return format;
  }

  public void setFormat(MediaType format) {
    this.format = format;
  }

  public byte[] getPayload() {
    return payload;
  }

  public void setPayload(byte[] payload) {
    this.payload = payload;
  }

  public void send() {
    if (responseSender != null) {
      responseSender.send(this);
    } else {
      logger.error("will not send response {} since a null sender", toString());
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
    return "IConnectResponse [messageID=" + messageID + ", status=" + status + ", format=" + format
        + ", payload=" + strPayload + "]";
  }
}
