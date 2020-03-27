/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.ilink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.openiot.cloud.base.help.BaseUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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

public class ILinkDecoder extends ByteToMessageDecoder {
  private static final Logger logger = LoggerFactory.getLogger(ILinkDecoder.class);
  private final CBORFactory factory = new CBORFactory();
  private final ObjectMapper mapper = new ObjectMapper(factory);

  private enum AssPhase {
    FINISH, IN_BODY_LEN, IN_FLEXHEADER, IN_LB, IN_MID, IN_PAYLOAD, IN_TYPE;
  }

  private byte leadingByte = 0;
  private byte[] ilinkMessageId = null;
  private int bodyLen = -1;
  private boolean hasFH = false;
  private int bufferToRead = 1;
  private ILinkMessage msg = null;
  private AssPhase phase = AssPhase.IN_LB;

  @Override
  protected synchronized void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    if (in == null) {
      logger.warn("in is null");
      return;
    }

    if (!in.isReadable()) {
      logger.warn("in is not readable");
      return;
    }

    while (in.isReadable(bufferToRead)) {
      logger.debug(String.format("phase %s, r=%d, w=%d, bufferToRead=%s",
                                 phase,
                                 in.readerIndex(),
                                 in.writerIndex(),
                                 bufferToRead));

      switch (phase) {
        case IN_LB:
          leadingByte = (byte) (in.readByte() & 0xFF);
          // REQUEST
          if (leadingByte == (byte) 0xFA) {
            phase = AssPhase.IN_MID;
            bufferToRead = 4;
          }
          // RESPONSE
          else if (leadingByte == (byte) 0xCE) {
            phase = AssPhase.IN_MID;
            bufferToRead = 4;
          }
          // PLAIN
          else if (leadingByte == (byte) 0xBA) {
            phase = AssPhase.IN_TYPE;
            ilinkMessageId = null;
            bufferToRead = 1;
          }
          // WRONG
          else {
            msg = null;
            logger.warn(String.format("an unexpected leading byte 0x%x\n", leadingByte));
            phase = AssPhase.FINISH;
            bufferToRead = 0;
          }
          break;
        case IN_MID:
          ilinkMessageId = new byte[4];
          in.readBytes(ilinkMessageId);

          phase = AssPhase.IN_TYPE;
          bufferToRead = 1;
          break;
        case IN_TYPE:
          byte mt = in.readByte();
          // the highest bit indicate whether there is a the flex
          // header or not
          hasFH = ((mt & 0x80) == 0x80);
          mt = (byte) (mt & 0x7f);

          msg = new ILinkMessage(leadingByte, mt);
          msg.setIlinkMessageId(ilinkMessageId);
          phase = AssPhase.IN_BODY_LEN;

          byte flag = in.getByte(in.readerIndex());
          bufferToRead = flag == 0 ? 2 : 4;
          break;
        case IN_BODY_LEN:
          // check highest bit firstly
          flag = in.getByte(in.readerIndex());
          boolean hasExtLen = ((flag & 0x80) != 0);

          if (hasExtLen) {
            bodyLen = in.readInt() & 0x7fffffff;
          } else {
            bodyLen = in.readShort() & 0x7fff;
          }

          logger.debug("  bodyLength " + bodyLen);

          if (hasFH) {
            phase = AssPhase.IN_FLEXHEADER;
          } else {
            phase = AssPhase.IN_PAYLOAD;
          }

          bufferToRead = bodyLen;
          break;
        case IN_FLEXHEADER:
          // TL. flex header size as bytes
          short flexHeaderSizeAsByte = in.readShort();
          // NN. how many entries are there
          short flexHeaderSizeAsEntry = in.readShort();
          logger.debug(String.format("  flexHeaderLen %d(asByte) %d(asEntry)",
                                     flexHeaderSizeAsByte,
                                     flexHeaderSizeAsEntry));

          byte[] flexHeaderByCBor = new byte[flexHeaderSizeAsByte];
          try {
            in.readBytes(flexHeaderByCBor);
            Map<String, Object> flexHeader = mapper.readValue(flexHeaderByCBor, Map.class);
            msg.setFlexHeadre(flexHeader);
            logger.debug("  flexHeader " + flexHeader);
          } catch (IOException e) {
            StringBuilder builder = new StringBuilder();
            for (byte b : flexHeaderByCBor) {
              builder.append(String.format("0x%h ", b));
            }
            logger.info("can not be de-serialized by CBOR " + e + "," + builder.toString());
            logger.info(BaseUtil.getStackTrace(e));
          } catch (IndexOutOfBoundsException e) {
            logger.info("it is a terrible value of body length " + bodyLen);
            logger.info(BaseUtil.getStackTrace(e));
          }

          // 4 bytes = TL(2 bytes) + NN(2 bytes)
          if (bodyLen > flexHeaderSizeAsByte + 4) {
            phase = AssPhase.IN_PAYLOAD;
            bufferToRead = bodyLen - flexHeaderSizeAsByte - 4;
          } else {
            phase = AssPhase.FINISH;
            bufferToRead = 0;
          }
          break;
        case IN_PAYLOAD:
          logger.debug("  payload length " + bufferToRead);
          byte[] payload = new byte[bufferToRead];
          in.readBytes(payload);
          msg = msg.setPayload(payload);
          phase = AssPhase.FINISH;
          bufferToRead = 0;
          break;
        case FINISH:
          if (msg != null) {
            logger.debug("frame such a message " + msg);
            out.add(msg);
          }

          leadingByte = 0;
          ilinkMessageId = null;
          bodyLen = -1;
          hasFH = false;
          bufferToRead = 1;
          msg = null;
          phase = AssPhase.IN_LB;
          break;
        default:
          logger.error("decoder detect a wrong state " + phase);
      }
    }

    logger.debug("decode has finished");
    in.discardReadBytes();
  }
}
