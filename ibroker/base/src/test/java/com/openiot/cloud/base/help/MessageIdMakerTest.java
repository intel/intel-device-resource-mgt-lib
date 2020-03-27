/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.help;

import com.openiot.cloud.base.ilink.ILinkMessage;
import com.openiot.cloud.base.ilink.LeadingByte;
import com.openiot.cloud.base.ilink.MessageType;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MessageIdMakerTest {

  @Test
  public void testCase1() throws Exception {
    byte[] midAsBytes = MessageIdMaker.getLongMessageIdAsBytes();
    long midAsLong = MessageIdMaker.bytesToLong(midAsBytes);

    ILinkMessage msg =
        new ILinkMessage(LeadingByte.REQUEST.valueOf(), (byte) MessageType.COAP_OVER_TCP.valueOf());
    msg.setIlinkMessageId(midAsBytes);

    System.out.println(Arrays.toString(midAsBytes));
    System.out.println(Arrays.toString(msg.getIlinkMessageId()));

    int midFromMsg = MessageIdMaker.bytesToInteger(msg.getIlinkMessageId());
    assertThat(midFromMsg).isEqualTo((int) midAsLong);
  }

  @Test
  public void testCase2() throws Exception {
    byte[] mid = MessageIdMaker.getIntMessageIdAsBytes();

    ILinkMessage msg =
        new ILinkMessage(LeadingByte.REQUEST.valueOf(), (byte) MessageType.COAP_OVER_TCP.valueOf());
    msg.setIlinkMessageId(mid);

    System.out.println(Arrays.toString(mid));
    System.out.println(Arrays.toString(msg.getIlinkMessageId()));

    assertThat(MessageIdMaker.bytesToInteger(msg.getIlinkMessageId())).isEqualTo(MessageIdMaker.bytesToInteger(mid));
  }
}
