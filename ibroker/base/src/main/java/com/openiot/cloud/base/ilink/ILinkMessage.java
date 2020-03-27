/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.ilink;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.help.MessageIdMaker;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** the messagetype field is necessary. */
public class ILinkMessage {
  /*
   * fileds in a ilink message
   */
  protected byte leadingByte;
  /** 4 bytes max. only for requests and responses. */
  protected byte[] ilinkMessageId = new byte[4];
  // protected MessageType messageType;
  protected byte messageType;

  protected Map<String, Object> flexHeader;
  protected byte[] payload;

  /*
   * since a ilinkMessageId from one gateway might have conflict with another one from another
   * gateway. *messageId* will be
   */
  protected long messageId;

  public ILinkMessage(byte leadingByte, byte messageType) {
    this(leadingByte, messageType, 0);
  }

  /**
   * the length of {@code ilinkMessageId} has to be <= 4. if the message type is PLAIN, a NULL value
   * is allowed.
   */
  public ILinkMessage(byte leadingByte, byte messageType, long messageId) {
    this.leadingByte = leadingByte;
    this.messageType = messageType;

    if (messageId == 0) {
      this.messageId = MessageIdMaker.getMessageIdAsInteger();
    } else {
      this.messageId = messageId;
    }
  }

  /*
   * getter and setter
   */
  public byte getLeadingByte() {
    return leadingByte;
  }

  public byte[] getIlinkMessageId() {
    return ilinkMessageId;
  }

  /**
   * in case receiving a too long byte[], we pick the last four bytes
   *
   * @param ilinkMessageId is a big endian byte[]
   * @return
   */
  public ILinkMessage setIlinkMessageId(byte[] ilinkMessageId) {
    if (ilinkMessageId != null) {
      int length = ilinkMessageId.length;
      if (length < 4) {
        // copyof will make sure ilinkMessageId is exactly 4 bytes
        this.ilinkMessageId = Arrays.copyOf(ilinkMessageId, 4);
      } else {
        this.ilinkMessageId = Arrays.copyOfRange(ilinkMessageId, length - 4, length);
      }
    }
    return this;
  }

  public byte[] getPayload() {
    return payload;
  }

  public ILinkMessage setPayload(byte[] payload) {
    this.payload = payload;
    return this;
  }

  public byte getMessageType() {
    return messageType;
  }

  public Map<String, Object> getFlexHeader() {
    return flexHeader;
  }

  public ILinkMessage setFlexHeadre(Map<String, Object> flexHeader) {
    this.flexHeader = flexHeader;
    return this;
  }

  public ILinkMessage setFlexHeaderEntry(String key, Object value) {
    if (flexHeader == null) {
      flexHeader = new HashMap<>();
    }
    flexHeader.put(key, value);
    return this;
  }

  public Object getFlexHeaderValue(String key) {
    if (flexHeader == null) {
      return null;
    }
    return flexHeader.get(key);
  }

  public long getMessageId() {
    return messageId;
  }

  public int getFLexHeaderSize() {
    return flexHeader == null ? 0 : flexHeader.size();
  }

  public int getPayloadSize() {
    return payload == null ? 0 : payload.length;
  }

  /** utils for Flex Header */
  public String getAgentId() {
    return (String) getFlexHeaderValue(ConstDef.FH_K_AID);
  }

  public ILinkMessage setAgentId(String value) {
    return setFlexHeaderEntry(ConstDef.FH_K_AID, value);
  }

  public String getEndPoint() {
    return (String) getFlexHeaderValue(ConstDef.FH_K_EP);
  }

  public int getResponseCode() {
    return Optional.ofNullable(getFlexHeaderValue(ConstDef.FH_K_REP))
                   .map(obj -> (Integer) obj)
                   .orElse(ConstDef.FH_V_SUCC);
  }

  public ILinkMessage setResponseCode(int value) {
    return setFlexHeaderEntry(ConstDef.FH_K_REP, value);
  }

  public String getTag() {
    return (String) getFlexHeaderValue(ConstDef.FH_K_TAG);
  }

  public ILinkMessage setTag(String value) {
    return setFlexHeaderEntry(ConstDef.FH_K_TAG, value);
  }

  public String getTimeStamp() {
    return (String) getFlexHeaderValue(ConstDef.FH_K_TIME);
  }

  public ILinkMessage setTimeInfoForSync(long seconds, long micro) {
    setFlexHeaderEntry(ConstDef.FH_K_SECONDS, seconds);
    setFlexHeaderEntry(ConstDef.FH_K_MICRO, micro);
    return this;
  }

  public boolean isRequest() {
    return this.leadingByte == LeadingByte.REQUEST.valueOf();
  }

  public boolean isResponse() {
    return this.leadingByte == LeadingByte.RESPONSE.valueOf();
  }

  @Override
  public String toString() {
    return "ILinkMessage [leadingByte=" + leadingByte + ", ilinkMessageId="
        + Arrays.toString(ilinkMessageId) + ", messageType=" + MessageType.fromValue(messageType)
        + ", flexHeader=" + flexHeader + ", payload SZ="
        + Optional.ofNullable(payload).map(p -> p.length).orElse(0) + ", messageId=" + messageId
        + "]";
  }
}
