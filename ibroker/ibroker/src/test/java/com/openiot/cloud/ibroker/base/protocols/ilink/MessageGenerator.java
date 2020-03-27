/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker.base.protocols.ilink;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.ilink.ILinkEncoder;
import com.openiot.cloud.base.ilink.ILinkMessage;
import com.openiot.cloud.base.ilink.LeadingByte;
import com.openiot.cloud.base.ilink.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.iotivity.cloud.base.protocols.MessageBuilder;
import org.iotivity.cloud.base.protocols.coap.CoapEncoder;
import org.iotivity.cloud.base.protocols.coap.CoapMessage;
import org.iotivity.cloud.base.protocols.enums.ContentFormat;
import org.iotivity.cloud.base.protocols.enums.RequestMethod;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MessageGenerator {
  private final String POST_RD_GW_PAYLOAD_FORMAT =
      "[{" + "\"di\":\"%s\"," + "\"aid\":\"%s\"," + "\"status\":\"on\"," + "\"st\":\"\","
          + "\"addr\":\"\"," + "\"dt\":\"intel.iagent\"," + "\"set\":\"\"," + "\"links\":[]" + "}]";

  private final String POST_RD_DEV_PAYLOAD_FORMAT = "[{" + "\"di\":\"%s\"," + "\"aid\":\"%s\","
      + "\"status\":\"on\"," + "\"st\":\"modbus\"," + "\"addr\":\"gwbus://modbus\","
      + "\"dt\":\"imrt.smb350\"," + "\"set\":\"\"," + "\"links\":[%s]" + "}]";

  private final String POST_RD_RES_FORMAT = "{\"href\":\"%s\",\"rt\":[\"%s\"]}";

  private String[] deviceIdSurfix = new String[] {"-di17", "-di19", "-di23", "-di27"};

  private String[] resource = new String[] {"/common_voltage", "/monitor", "/airflow", "/boiler",
      "/battery", "/current_per_phase", "/power_per_phase", "/switch", "/airflow", "/washer",
      "/dryer", "/refrigerator", "/dishwasher", "/cooktops", "/oven", "/range_hoods", "/blender",
      "/juicer", "/iron", "/sewing"};

  private int messageAmout = 10;
  private Random random = new Random(1024);
  private List<String> aidBucket = buildAidList();

  List<String> buildAidList() {
    List<String> aidList = new LinkedList<>();
    for (int i = 0; i < 1000; i++) {
      aidList.add("gwX_" + i);
    }
    return aidList;
  }

  private List<String> urlPathBucket = buildUrlPathList();

  List<String> buildUrlPathList() {
    String commonPrefix = "/dp";
    String deviceType = "/modbus";
    String[] property = new String[] {"/M", "/N", "/O", "/P", "/37", "/43", "/47"};

    List<String> urlPathList = new LinkedList<>();
    for (String aid : this.aidBucket) {
      for (String deviceId : deviceIdSurfix) {
        for (String res : resource) {
          for (String pro : property) {
            urlPathList.add(String.format("%s%s/%s%s%s%s",
                                          commonPrefix,
                                          deviceType,
                                          aid,
                                          deviceId,
                                          res,
                                          pro));
          }
        }
      }
    }
    return urlPathList;
  }

  private List<String> device = buildDeviceList();

  List<String> buildDeviceList() {
    List<String> ret = new LinkedList<>();
    for (String aid : aidBucket) {
      for (String dev : deviceIdSurfix) {
        ret.add(aid + dev);
      }
    }
    return ret;
  }

  private void dumpByteArray(byte[] bs) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bs) {
      sb.append(String.format("%02x", b));
    }
    System.out.println(sb.toString());
  }

  public ILinkMessage produceOneRequest(Map<String, Object> flexHeader, String urlPath,
                                        String urlQuery, RequestMethod action, ContentFormat format,
                                        byte[] payload) {
    return produceOneMessage(LeadingByte.REQUEST,
                             flexHeader,
                             urlPath,
                             urlQuery,
                             action,
                             format,
                             payload);
  }

  public ILinkMessage produceOneResponse(Map<String, Object> flexHeader, String urlPath,
                                         String urlQuery, RequestMethod action,
                                         ContentFormat format, byte[] payload) {
    return produceOneMessage(LeadingByte.RESPONSE,
                             flexHeader,
                             urlPath,
                             urlQuery,
                             action,
                             format,
                             payload);
  }

  ILinkMessage produceOneMessage(LeadingByte leadingByte, Map<String, Object> flexHeader,
                                 String urlPath, String urlQuery, RequestMethod action,
                                 ContentFormat format, byte[] payload) {
    // frame a CoapMessage
    CoapMessage cm =
        (CoapMessage) MessageBuilder.createRequest(action, urlPath, urlQuery, format, payload);
    ByteBuf bb = Unpooled.buffer();
    CoapEncoder ce = new CoapEncoder();
    ReflectionTestUtils.invokeMethod(ce, "encode", null, cm, bb);
    byte[] cmBytes = new byte[bb.readableBytes()];
    bb.getBytes(bb.readerIndex(), cmBytes);
    bb.release();

    // frame a ILinkCoapOverTcpMessage
    int randomMid = this.random.nextInt(this.messageAmout);
    ILinkMessage message =
        new ILinkMessage(leadingByte.valueOf(),
                         (byte) MessageType.COAP_OVER_TCP.valueOf()).setIlinkMessageId(Integer.toString(randomMid).getBytes()).setFlexHeadre(flexHeader).setPayload(cmBytes);
    return message;
  }

  byte[] serializeMessage(ILinkMessage message) {
    ILinkEncoder ie = new ILinkEncoder();
    ByteBuf bb = Unpooled.buffer();
    ReflectionTestUtils.invokeMethod(ie, "encode", null, message, bb);

    byte[] slice = new byte[bb.readableBytes()];
    bb.getBytes(0, slice);
    bb.release();
    return slice;
  }

  String randomPickOne(List<String> list) {
    return list.get(this.random.nextInt(list.size()));
  }

  String produceHandShake() {
    StringBuilder sb = new StringBuilder();
    for (String aid : this.aidBucket) {
      byte[] randomMid = Integer.toString(this.random.nextInt(0xFFFF)).getBytes();
      ILinkMessage message =
          new ILinkMessage(LeadingByte.REQUEST.valueOf(),
                           (byte) MessageType.INTEL_IAGENT.valueOf()).setAgentId(aid)
                                                                     .setTag(ConstDef.FH_V_HAN2)
                                                                     .setIlinkMessageId(randomMid);
      byte[] binary = serializeMessage(message);
      sb.append(byteArrayToHexString(binary));
    }

    return sb.toString();
  }

  String produceRDGWData() {
    StringBuilder sb = new StringBuilder();
    Map<String, Object> flexHeader = new HashMap<>();
    for (String aid : this.aidBucket) {
      flexHeader.put("_aid", aid);
      String urlPath = "/rd";
      byte[] payload = String.format(POST_RD_GW_PAYLOAD_FORMAT, aid, aid).getBytes();
      ILinkMessage putRD = produceOneRequest(flexHeader,
                                             urlPath,
                                             null,
                                             RequestMethod.POST,
                                             ContentFormat.APPLICATION_TEXTPLAIN,
                                             payload);
      byte[] binary = serializeMessage(putRD);
      sb.append(byteArrayToHexString(binary));
    }
    return sb.toString();
  }

  String produceRDDEVData() {
    StringBuilder sb = new StringBuilder();
    Map<String, Object> flexHeader = new HashMap<>();
    for (String aid : this.aidBucket) {
      flexHeader.put("_aid", aid);
      String urlPath = "/rd";
      for (String dev : this.deviceIdSurfix) {
        StringBuilder href = new StringBuilder();
        for (String res : this.resource) {
          href.append(String.format(POST_RD_RES_FORMAT, res, res));
          href.append(",");
        }
        href.deleteCharAt(href.length() - 1);

        byte[] payload =
            String.format(POST_RD_DEV_PAYLOAD_FORMAT, aid + dev, aid, href.toString()).getBytes();
        ILinkMessage putRD = produceOneRequest(flexHeader,
                                               urlPath,
                                               null,
                                               RequestMethod.POST,
                                               ContentFormat.APPLICATION_TEXTPLAIN,
                                               payload);
        byte[] binary = serializeMessage(putRD);
        sb.append(byteArrayToHexString(binary));
      }
    }
    return sb.toString();
  }

  String produceRDData() {
    StringBuilder sb = new StringBuilder();
    sb.append(produceRDGWData());
    sb.append(produceRDDEVData());
    return sb.toString();
  }

  List<String> buildPayloadList() {
    return null;
  }

  void init() {
    // keep the order
    this.urlPathBucket = buildUrlPathList();
    // this.payloadBucket = buildPayloadList();
  }

  String produceDPData() {
    StringBuilder ret = new StringBuilder();
    Map<String, Object> flexHeader = new HashMap<>();
    for (int i = 0; i < this.messageAmout; i++) {
      flexHeader.put("_aid", randomPickOne(aidBucket));
      String urlPath = randomPickOne(urlPathBucket);
      String urlQuery = null;
      RequestMethod action = RequestMethod.PUT;
      ContentFormat format = ContentFormat.APPLICATION_TEXTPLAIN;
      // byte[] payload = randomPickOne(payloadBucket).getBytes();
      byte[] payload = Integer.toString(random.nextInt(0x7FFFFFFF)).getBytes();
      byte[] binary = serializeMessage(produceOneRequest(flexHeader,
                                                         urlPath,
                                                         urlQuery,
                                                         action,
                                                         format,
                                                         payload));
      ret.append(byteArrayToHexString(binary));
    }

    return ret.toString();
  }

  String produceDPTemplate() {
    StringBuilder ret = new StringBuilder();
    Map<String, Object> flexHeader = new HashMap<>();
    byte[] payload = "1235713".getBytes();
    for (String url : this.urlPathBucket) {
      String[] tmp = url.split("/");
      if (tmp.length < 3) {
        System.out.println("invalid url " + url);
        continue;
      }
      // System.out.println(Arrays.toString(tmp));
      String aid = tmp[3].split("-")[0];

      flexHeader.put("_aid", randomPickOne(aidBucket));
      String urlPath = url;
      String urlQuery = null;
      RequestMethod action = RequestMethod.PUT;
      ContentFormat format = ContentFormat.APPLICATION_TEXTPLAIN;
      byte[] binary = serializeMessage(produceOneRequest(flexHeader,
                                                         urlPath,
                                                         urlQuery,
                                                         action,
                                                         format,
                                                         payload));
      byte[] newBinary = Arrays.copyOf(binary, binary.length - payload.length);
      ret.append(byteArrayToHexString(newBinary));
    }

    return ret.toString();
  }

  String byteArrayToHexString(byte[] binary) {
    StringBuilder sb = new StringBuilder();
    for (byte b : binary) {
      sb.append(String.format("%02x", b));
    }
    sb.append(",\n");
    return sb.toString();
  }

  // @Test
  public void testCase() {
    MessageGenerator mg = new MessageGenerator();
    mg.init();

    // handshake2 messages, each aid has
    String content = mg.produceHandShake();
    // System.out.println(content);
    try {
      Files.write(Paths.get("handshake2_data.csv"), content.getBytes(), StandardOpenOption.CREATE);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    // POST /rd
    content = mg.produceRDData();
    // System.out.println(content);
    try {
      Files.write(Paths.get("rd_data.csv"), content.getBytes(), StandardOpenOption.CREATE);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    // POST /dp
    content = mg.produceDPTemplate();
    // System.out.println(content);
    try {
      Files.write(Paths.get("dp_data.csv"), content.getBytes(), StandardOpenOption.CREATE);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
