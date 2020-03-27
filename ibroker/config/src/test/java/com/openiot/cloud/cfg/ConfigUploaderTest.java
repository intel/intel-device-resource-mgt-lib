/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.cfg;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.dao.ConfigRepository;
import com.openiot.cloud.base.mongo.model.Config;
import com.openiot.cloud.cfg.helper.ConfigAMSResponse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class}, properties = {"mongo.db = test_openiot"})
public class ConfigUploaderTest {
  @Autowired
  private ConfigRepository configRepository;
  @Autowired
  private ConfigUploader configUploader;
  @Autowired
  private RestTemplate testRestTemplate;

  @Value(value = "${ams.addr:127.0.0.1}")
  private String amsAddr;

  @Value(value = "${ams.port}")
  private String amsPort;

  @Value(value = "${auth.addr}")
  private String authCenterAddress;

  @Value(value = "${auth.port}")
  private String authCenterPort;

  @Before
  public void setup() throws Exception {
    configRepository.deleteAll();
  }

  @Test
  public void testBasic() throws Exception {
    Config config = new Config();
    config.setTargetType(ConstDef.CFG_TT_DEVONGW);
    config.setTargetId("apple");
    config.setConfig("peach");
    configRepository.save(config);
    assertThat(configRepository.count()).isEqualTo(1);

    ReflectionTestUtils.invokeMethod(configUploader, "uploadToAms");
    assertThat(configRepository.count()).isEqualTo(0);

    // /api/user/login
    Map<String, String> loginData = new HashMap<>();
    loginData.put("username", "beihai");
    loginData.put("password", "intel@123");

    final String authCenterUrl =
        String.format("http://%s:%s/api/user/login", authCenterAddress, authCenterPort);
    ResponseEntity<Map> responseFromAuthCenter =
        testRestTemplate.postForEntity(authCenterUrl, loginData, Map.class);
    assertThat(responseFromAuthCenter.getStatusCode().is2xxSuccessful()).isTrue();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth((String) responseFromAuthCenter.getBody().getOrDefault("token", ""));
    HttpEntity<byte[]> requestEntity = new HttpEntity<>(null, headers);
    final String amsUrl =
        String.format("http://%s:%s/ams_user_cloud/ams/v1/config/instance?product_name=iagent&path_name=%s&target_type=%s&target_id=%s",
                      amsAddr,
                      amsPort,
                      ConstDef.CFG_PTN_DEVCFG,
                      ConstDef.CFG_TT_DEVONGW,
                      config.getTargetId());
    ResponseEntity<ConfigAMSResponse[]> responseFromAmsUserCloud =
        testRestTemplate.exchange(amsUrl, HttpMethod.GET, requestEntity, ConfigAMSResponse[].class);
    assertThat(responseFromAmsUserCloud.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(responseFromAmsUserCloud.getBody()[0]).isNotNull()
                                                     .hasFieldOrPropertyWithValue("targetID",
                                                                                  config.getTargetId())
                                                     .hasFieldOrPropertyWithValue("content",
                                                                                  config.getConfig());
  }
}
