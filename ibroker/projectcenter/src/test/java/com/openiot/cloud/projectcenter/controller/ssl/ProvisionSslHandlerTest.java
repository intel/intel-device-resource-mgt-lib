/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.ssl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.ilink.ILinkMessage;
import com.openiot.cloud.base.ilink.LeadingByte;
import com.openiot.cloud.base.ilink.MessageType;
import com.openiot.cloud.projectcenter.controller.amqp.ProvisionReplaceAmqpHandler;
import com.openiot.cloud.projectcenter.controller.amqp.ProvisionResetAmqpHandler;
import com.openiot.cloud.projectcenter.service.GatewayService;
import com.openiot.cloud.projectcenter.service.dto.GatewayDTO;
import com.openiot.cloud.projectcenter.utils.AES128CBC;
import com.openiot.cloud.projectcenter.utils.MD5;
import com.openiot.cloud.sdk.service.IConnectRequest;
import com.openiot.cloud.sdk.service.JMSResponseSender;
import lombok.extern.slf4j.Slf4j;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.util.UriComponentsBuilder;
import java.nio.ByteBuffer;
import java.util.Arrays;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class ProvisionSslHandlerTest {
  @Autowired
  private ProvisionSslHandler provisionSslHandler;
  @Autowired
  private ProvisionResetAmqpHandler provisionResetAmqpHandler;
  @Autowired
  private ProvisionReplaceAmqpHandler provisionReplaceAmqpHandler;
  @Autowired
  private GatewayService gatewayService;
  @Autowired
  private ObjectMapper objectMapper;
  @Value("${provision.factory-key:hello iagent}")
  private String DEFAULT_FACTORY_KEY;
  @Mock
  private JMSResponseSender fakeJMSResponseSender;
  private final byte[] token = "cantaloupebanana".getBytes();
  private final String serialNumber = "pomegranate";
  private final String newSerialNumber = "grape";
  // a surely valid data could be used as the payload
  private byte[] validEncryptedData1;
  private byte[] validEncryptedData2;

  @Before
  public void setup() throws Exception {
    assertThat(token.length).isEqualTo(16);
    byte[] tokenMD5 = MD5.getMd5Hash(token);
    assertThat(tokenMD5.length).isEqualTo(16);

    byte[] content = Arrays.copyOf(token, 32);
    System.arraycopy(tokenMD5, 0, content, 16, 16);

    // has to be 128 bits
    byte[] key = MD5.getMd5Hash(DEFAULT_FACTORY_KEY.getBytes());
    byte[] iv = MD5.getMd5Hash(serialNumber.getBytes());
    validEncryptedData1 = AES128CBC.encrypt(content, key, iv);

    iv = MD5.getMd5Hash(newSerialNumber.getBytes());
    validEncryptedData2 = AES128CBC.encrypt(content, key, iv);

    gatewayService.removeAll();
  }

  @Test
  public void testOnMessage() throws Exception {
    // a newer
    ILinkMessage request =
        new ILinkMessage(LeadingByte.PLAIN.valueOf(), (byte) MessageType.COAP_OVER_TCP.valueOf());
    request.setIlinkMessageId(ByteBuffer.allocate(4).putInt(1024).array());
    request.setFlexHeaderEntry("sn", serialNumber);
    request.setPayload(validEncryptedData1);

    ILinkMessage response = provisionSslHandler.onMessage(request);
    assertThat(response).isNotNull();
    assertThat(response.getResponseCode()).isEqualTo(ConstDef.FH_V_SUCC);
    assertThat(response.getTag()).isEqualTo(ConstDef.FH_V_PRO);
    assertThat(response.getPayload()).isNotEmpty();

    GatewayDTO gatewayDTO = gatewayService.findBySerialNumber(serialNumber, false);
    assertThat(gatewayDTO).hasFieldOrPropertyWithValue("hwSn", serialNumber)
                          .hasFieldOrPropertyWithValue("iAgentId", serialNumber)
                          .hasFieldOrPropertyWithValue("reset", false);

    // the newer again
    response = provisionSslHandler.onMessage(request);
    assertThat(response).isNotNull();
    assertThat(response.getResponseCode()).isEqualTo(ConstDef.FH_V_FAIL);
    assertThat(response.getTag()).isEqualTo(ConstDef.FH_V_PRO);
    assertThat(response.getPayload()).isNullOrEmpty();

    // reset the newer
    gatewayDTO.setReset(true);
    gatewayService.save(gatewayDTO);

    response = provisionSslHandler.onMessage(request);
    assertThat(response).isNotNull();
    assertThat(response.getResponseCode()).isEqualTo(ConstDef.FH_V_SUCC);
    assertThat(response.getTag()).isEqualTo(ConstDef.FH_V_PRO);
    assertThat(response.getPayload()).isNotEmpty();

    gatewayDTO = gatewayService.findBySerialNumber(serialNumber, false);
    assertThat(gatewayDTO).hasFieldOrPropertyWithValue("hwSn", serialNumber)
                          .hasFieldOrPropertyWithValue("iAgentId", serialNumber)
                          .hasFieldOrPropertyWithValue("reset", false);


    // replace the newer with a same serial number
    gatewayDTO.setNewHwSn(serialNumber);
    gatewayService.save(gatewayDTO);

    response = provisionSslHandler.onMessage(request);
    assertThat(response).isNotNull();
    assertThat(response.getResponseCode()).isEqualTo(ConstDef.FH_V_FAIL);
    assertThat(response.getTag()).isEqualTo(ConstDef.FH_V_PRO);
    assertThat(response.getPayload()).isNullOrEmpty();

    // replace the newer with a different serial number
    gatewayDTO.setNewHwSn(newSerialNumber);
    gatewayService.save(gatewayDTO);

    request.setFlexHeaderEntry("sn", newSerialNumber);
    request.setPayload(validEncryptedData2);
    response = provisionSslHandler.onMessage(request);
    assertThat(response).isNotNull();
    assertThat(response.getResponseCode()).isEqualTo(ConstDef.FH_V_SUCC);
    assertThat(response.getTag()).isEqualTo(ConstDef.FH_V_PRO);
    assertThat(response.getPayload()).isNotEmpty();
  }

  @Test
  public void testOnMessageEx() throws Exception {
    // a newer
    ILinkMessage request =
        new ILinkMessage(LeadingByte.PLAIN.valueOf(), (byte) MessageType.COAP_OVER_TCP.valueOf());
    request.setIlinkMessageId(ByteBuffer.allocate(4).putInt(1024).array());
    request.setFlexHeaderEntry("sn", serialNumber);
    request.setPayload(validEncryptedData1);

    ILinkMessage response = provisionSslHandler.onMessage(request);
    assertThat(response).isNotNull();
    assertThat(response.getResponseCode()).isEqualTo(ConstDef.FH_V_SUCC);
    assertThat(response.getTag()).isEqualTo(ConstDef.FH_V_PRO);
    assertThat(response.getPayload()).isNotEmpty();

    GatewayDTO gatewayDTO = gatewayService.findBySerialNumber(serialNumber, false);
    assertThat(gatewayDTO).hasFieldOrPropertyWithValue("hwSn", serialNumber)
                          .hasFieldOrPropertyWithValue("iAgentId", serialNumber)
                          .hasFieldOrPropertyWithValue("reset", false);

    // reset the newer
    IConnectRequest iConnectRequest =
        IConnectRequest.create(HttpMethod.POST,
                               UriComponentsBuilder.newInstance()
                                                   .path(ConstDef.MQ_QUEUE_PROV_RESET)
                                                   .queryParam("reset", "true")
                                                   .build()
                                                   .toString(),
                               MediaType.APPLICATION_JSON,
                               objectMapper.writeValueAsBytes(new String[] {serialNumber}));
    iConnectRequest.setResponseSender(fakeJMSResponseSender);
    provisionResetAmqpHandler.onRequest(iConnectRequest);

    response = provisionSslHandler.onMessage(request);
    assertThat(response).isNotNull();
    assertThat(response.getResponseCode()).isEqualTo(ConstDef.FH_V_SUCC);
    assertThat(response.getTag()).isEqualTo(ConstDef.FH_V_PRO);
    assertThat(response.getPayload()).isNotEmpty();

    gatewayDTO = gatewayService.findBySerialNumber(serialNumber, false);
    assertThat(gatewayDTO).hasFieldOrPropertyWithValue("hwSn", serialNumber)
                          .hasFieldOrPropertyWithValue("iAgentId", serialNumber)
                          .hasFieldOrPropertyWithValue("reset", false);

    // replace the newer with a different serial number
    iConnectRequest =
        IConnectRequest.create(HttpMethod.POST,
                               UriComponentsBuilder.newInstance()
                                                   .path(ConstDef.MQ_QUEUE_PROV_REPLACE)
                                                   .queryParam("aid", serialNumber)
                                                   .build()
                                                   .toString(),
                               MediaType.APPLICATION_JSON,
                               objectMapper.writeValueAsBytes(newSerialNumber));
    iConnectRequest.setResponseSender(fakeJMSResponseSender);
    provisionReplaceAmqpHandler.onRequest(iConnectRequest);

    request.setFlexHeaderEntry("sn", newSerialNumber);
    request.setPayload(validEncryptedData2);
    response = provisionSslHandler.onMessage(request);
    assertThat(response).isNotNull();
    assertThat(response.getResponseCode()).isEqualTo(ConstDef.FH_V_SUCC);
    assertThat(response.getTag()).isEqualTo(ConstDef.FH_V_PRO);
    assertThat(response.getPayload()).isNotEmpty();
  }
}
