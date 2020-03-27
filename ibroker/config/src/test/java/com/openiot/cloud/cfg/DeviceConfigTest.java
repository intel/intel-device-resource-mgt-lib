/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.cfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.dao.ConfigRepository;
import com.openiot.cloud.base.mongo.dao.DeviceRepository;
import com.openiot.cloud.base.mongo.dao.ResProRepository;
import com.openiot.cloud.base.mongo.dao.ResourceRepository;
import com.openiot.cloud.base.mongo.model.Config;
import com.openiot.cloud.base.mongo.model.Device;
import com.openiot.cloud.base.mongo.model.ResProperty;
import com.openiot.cloud.base.mongo.model.Resource;
import com.openiot.cloud.cfg.model.DeviceConfig;
import com.openiot.cloud.testbase.TestUtil;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.List;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class}, properties = {"mongo.db = test_openiot"})
public class DeviceConfigTest {

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  DeviceRepository devRepo;

  @Autowired
  ResourceRepository resRepo;

  @Autowired
  ResProRepository resProRepo;

  @Autowired
  ConfigRepository cfgRepo;

  @Autowired
  ConfigTaskHandler handler;

  @Before
  public void setUp() {
    try {
      devRepo.deleteAll();
      resRepo.deleteAll();
      resProRepo.deleteAll();
      cfgRepo.deleteAll();
      TestUtil.importTestDb(mongoTemplate,
                            ConstDef.C_DEV,
                            ConstDef.C_RES,
                            ConstDef.C_RESPRO,
                            ConstDef.C_TASKSRVREG);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testDeviceConfig() {
    try {
      String deviceId = "cardio";
      Device device = devRepo.findOneById(deviceId);
      System.out.println("--> " + device);

      DeviceConfig devCfg = DeviceConfig.from(Optional.ofNullable(device),
                                              Optional.ofNullable(null),
                                              Optional.ofNullable(null),
                                              Optional.ofNullable(null),
                                              Optional.ofNullable(null));
      System.out.println("--> " + devCfg);

      assertThat(devCfg).isNotNull()
                        .hasFieldOrPropertyWithValue("di", "cardio")
                        .hasFieldOrPropertyWithValue("dl", 3600)
                        .hasFieldOrPropertyWithValue("rn", 10)
                        .hasNoNullFieldsOrPropertiesExcept("grp", "links", "project");

      String devCfgAsJsonString = devCfg.toJsonString();
      System.out.println("--> " + devCfgAsJsonString);
      assertThat(devCfgAsJsonString).isNotNull()
                                    .isNotEmpty()
                                    .contains("\"attr\"")
                                    .contains("\"cfg\"")
                                    .doesNotContain("\"grp\"")
                                    .doesNotContain("\"links\"");

      List<Resource> resources = resRepo.findAllByDevId(deviceId);
      resources.forEach(res -> System.out.println("--> " + res));
      devCfg = DeviceConfig.from(Optional.ofNullable(device),
                                 Optional.of(resources),
                                 Optional.ofNullable(null),
                                 Optional.ofNullable(null),
                                 Optional.ofNullable(null));
      System.out.println("--> " + devCfg);

      assertThat(devCfg).isNotNull()
                        .hasFieldOrPropertyWithValue("di", "cardio")
                        .hasFieldOrPropertyWithValue("dl", 3600)
                        .hasFieldOrPropertyWithValue("rn", 10)
                        .hasFieldOrProperty("links")
                        .hasNoNullFieldsOrPropertiesExcept("grp", "project");

      assertThat(devCfg.getLinks()).isNotNull()
                                   .isNotEmpty()
                                   .extracting("href")
                                   .contains("/stepper")
                                   .hasSize(1);

      assertThat(devCfg.getLinks().get(0)).hasFieldOrPropertyWithValue("href", "/stepper")
                                          .hasNoNullFieldsOrPropertiesExcept("grp",
                                                                             "dl",
                                                                             "props",
                                                                             "project");

      List<ResProperty> properties = resProRepo.findAllByDevId(deviceId);
      properties.forEach(p -> System.out.println("--> " + p));
      devCfg = DeviceConfig.from(Optional.ofNullable(device),
                                 Optional.of(resources),
                                 Optional.of(properties),
                                 Optional.ofNullable(null),
                                 Optional.ofNullable(null));
      System.out.println("--> " + devCfg);

      devCfgAsJsonString = devCfg.toJsonString();
      System.out.println("--> " + devCfgAsJsonString);
      assertThat(devCfgAsJsonString).isNotNull()
                                    .isNotEmpty()
                                    .contains("di")
                                    .contains("dl")
                                    .contains("rn")
                                    .contains("attr")
                                    .contains("cfg")
                                    .contains("links")
                                    .doesNotContain("\"grp\"");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testCheckAndBuild() {
    try {
      handler.generateDevConfiguration("cardio");
      List<Config> config = cfgRepo.findAll();
      assertThat(config).isNotNull().isNotEmpty().hasSize(1);

      String configJson = config.get(0).getConfig();
      DeviceConfig devCfg = new ObjectMapper().readValue(configJson, DeviceConfig.class);
      assertThat(devCfg).isNotNull()
                        .hasFieldOrProperty("links")
                        .hasFieldOrPropertyWithValue("di", "cardio");

      assertThat(devCfg.getLinks()).isNotNull()
                                   .isNotEmpty()
                                   .hasSize(1)
                                   .element(0)
                                   .hasFieldOrPropertyWithValue("href", "/stepper")
                                   .hasFieldOrProperty("attr")
                                   .hasFieldOrProperty("cfg");

      assertThat(devCfg.getLinks().get(0).getProps()).isNotNull().isNotEmpty().hasSize(3);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
