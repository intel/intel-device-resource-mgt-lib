/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.ilink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// @formatter:off
/**
 * ILinkMessage fields:
 *
 * <p>| Leading Byte(1 Byte) | Message Id(4 Bytes, Optional), if a message Leading Byte == PLAIN,
 * there is no message id | Message Type(1 Byte), the highest bit of message type will indicate if
 * there is a flex header, 1 means yes | Body Length(2 Bytes or 4 Bytes), body length = body length
 * + flex header length + 4 bytes(2 Bytes for TL and 2 Bytes for NN), if it > 0x7fff, use a integer
 * to represent, otherwise a short is enough | Flex Header TL(2 Bytes, Optional), size as byte |
 * Flex Header NN(2 Bytes, Optional), size as how many entries | Flex Header(By CBOR, Optional),
 * follow https://cbor.io/ | Body (Optional)
 */
// @formatter:on
public class ILinkEncoder extends MessageToByteEncoder<ILinkMessage> {
  private static final Logger logger = LoggerFactory.getLogger(ILinkEncoder.class);
  private final CBORFactory factory = new CBORFactory();
  private final ObjectMapper mapper = new ObjectMapper(factory);

  @Override
  protected void encode(ChannelHandlerContext ctx, ILinkMessage msg, ByteBuf out) {
    logger.debug("encode such a message " + msg);
    ByteBuf outBuf = Unpooled.directBuffer();
    outBuf.clear();

    int prevPhaseWriteIndex = 0;

    // leading byte
    outBuf.writeByte(msg.getLeadingByte());
    logger.debug(String.format("%d - %d is %s",
                               prevPhaseWriteIndex,
                               outBuf.writerIndex(),
                               "leading byte"));

    // message id
    prevPhaseWriteIndex = outBuf.writerIndex();
    if (msg.getLeadingByte() != LeadingByte.PLAIN.valueOf()) {
      outBuf.writeBytes(msg.getIlinkMessageId(), 0, 4);
    }
    logger.debug(String.format("%d - %d is %s",
                               prevPhaseWriteIndex,
                               outBuf.writerIndex(),
                               "message id"));

    // message type
    prevPhaseWriteIndex = outBuf.writerIndex();
    byte mt = msg.getMessageType();
    if (msg.getFLexHeaderSize() > 0) {
      mt = (byte) (0x80 | mt);
    }
    outBuf.writeByte(mt);
    logger.debug(String.format("%d - %d is %s",
                               prevPhaseWriteIndex,
                               outBuf.writerIndex(),
                               "message type"));

    // message BL + EL
    prevPhaseWriteIndex = outBuf.writerIndex();
    int bodyLength = msg.getPayloadSize();
    byte[] flexHeaderByCBor = null;
    // CBOR flex header and get its size
    if (msg.getFLexHeaderSize() > 0) {
      try {
        flexHeaderByCBor = mapper.writeValueAsBytes(msg.getFlexHeader());
        // include TL(2 bytes) and NN(2 bytes)
        bodyLength += 4;
        bodyLength += flexHeaderByCBor.length;
      } catch (JsonProcessingException e) {
        logger.info(String.format("%s can not be serialized by CBOR", msg.getFlexHeader()));
      }
    }

    // EL
    if (bodyLength > 0x7fff) {
      // 4 bytes, the highest bit is 1
      outBuf.writeInt(bodyLength | 0x80000000);
    } else {
      // 2 bytes, the highest bit is 0
      outBuf.writeShort(bodyLength & 0x7fff);
    }
    logger.debug(String.format("  bodyLength = %d, flexHeaderLen = %d",
                               bodyLength,
                               flexHeaderByCBor == null ? 0 : flexHeaderByCBor.length));
    logger.debug(String.format("%d - %d is %s",
                               prevPhaseWriteIndex,
                               outBuf.writerIndex(),
                               "body length"));

    // FLEXHEADER
    prevPhaseWriteIndex = outBuf.writerIndex();
    if (flexHeaderByCBor != null && flexHeaderByCBor.length > 0) {
      // TL. flex header size as bytes
      outBuf.writeShort(flexHeaderByCBor.length);
      logger.debug(String.format("%d - %d is %s",
                                 prevPhaseWriteIndex,
                                 outBuf.writerIndex(),
                                 "flex header TL(as byte)"));

      // NN. how many entries are there
      prevPhaseWriteIndex = outBuf.writerIndex();
      outBuf.writeShort(msg.getFLexHeaderSize());
      logger.debug(String.format("%d - %d is %s",
                                 prevPhaseWriteIndex,
                                 outBuf.writerIndex(),
                                 "flex header NN(as entry)"));

      // outBuf.writeShort(flexHeaderByCBor.length);
      prevPhaseWriteIndex = outBuf.writerIndex();
      outBuf.writeBytes(flexHeaderByCBor);
    }
    logger.debug(String.format("%d - %d is %s",
                               prevPhaseWriteIndex,
                               outBuf.writerIndex(),
                               "flex header"));

    // flush everything to dest
    if (outBuf.readableBytes() > 0) {
      out.writeBytes(outBuf);
    }
    outBuf.release();

    prevPhaseWriteIndex = out.writerIndex();
    if (msg.getPayloadSize() > 0) {
      out.writeBytes(msg.getPayload());
    }
    logger.debug(String.format("%d - %d is %s", prevPhaseWriteIndex, out.writerIndex(), "body"));
    // EOL
    // out.writeByte('\0');
  }
}
