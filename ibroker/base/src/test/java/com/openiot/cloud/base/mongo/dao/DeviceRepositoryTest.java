/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao;

import com.openiot.cloud.base.Application;
import com.openiot.cloud.base.TestUtilBase;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.Device;
import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.List;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class}, properties = {"mongo.db = test_openiot"})
public class DeviceRepositoryTest {
  @Autowired
  TestUtilBase testUtil;

  @Autowired
  DeviceRepository devRepo;

  @Before
  public void beforeTestCase() throws Exception {
    devRepo.deleteAll();
    testUtil.importTestDb(ConstDef.C_DEV);
  }

  @After
  public void afterTestCase() throws Exception {
    devRepo.deleteAll();
  }

  @AfterClass
  public static void tearDown() throws Exception {}

  @Test
  public void testFilterCase1() throws Exception {
    AttributeEntity attr = new AttributeEntity("iced_tea", "summer_sunrise");
    PageRequest pageRequest = new PageRequest(0, ConstDef.MAX_SIZE);
    List<Device> devices = devRepo.filter(Optional.empty(),
                                          Optional.empty(),
                                          Optional.empty(),
                                          Optional.empty(),
                                          Optional.empty(),
                                          Optional.empty(),
                                          Optional.empty(),
                                          Optional.empty(),
                                          Optional.empty(),
                                          Boolean.TRUE,
                                          Optional.empty(),
                                          Optional.of(attr),
                                          pageRequest);
    assertThat(devices).isNotEmpty()
                       .hasSize(2)
                       .extracting("id")
                       .contains("00000000-0000-0000-0000-000000000004",
                                 "00000000-0000-0000-0000-000000000005");

    attr = new AttributeEntity("iced_tea", "summer_sunset");
    devices = devRepo.filter(Optional.empty(),
                             Optional.empty(),
                             Optional.empty(),
                             Optional.empty(),
                             Optional.empty(),
                             Optional.empty(),
                             Optional.empty(),
                             Optional.empty(),
                             Optional.empty(),
                             Boolean.TRUE,
                             Optional.empty(),
                             Optional.of(attr),
                             pageRequest);
    assertThat(devices).isEmpty();

    attr = new AttributeEntity("iced_tea", "summer_sunrise");
    devices = devRepo.filter(Optional.empty(),
                             Optional.empty(),
                             Optional.empty(),
                             Optional.empty(),
                             Optional.empty(),
                             Optional.empty(),
                             Optional.empty(),
                             Optional.empty(),
                             Optional.empty(),
                             Boolean.TRUE,
                             Optional.empty(),
                             Optional.of(attr),
                             pageRequest);
    assertThat(devices).isNotEmpty()
                       .hasSize(2)
                       .extracting("id")
                       .contains("00000000-0000-0000-0000-000000000004",
                                 "00000000-0000-0000-0000-000000000005");
  }

  @Test
  public void testFilterCase2() throws Exception {
    String devType = "host";
    PageRequest pageRequest = new PageRequest(0, ConstDef.MAX_SIZE);
    List<Device> devices = devRepo.filter(Optional.empty(),
                                          Optional.empty(),
                                          Optional.empty(),
                                          Optional.empty(),
                                          Optional.of(devType),
                                          Optional.empty(),
                                          Optional.empty(),
                                          Optional.empty(),
                                          Optional.empty(),
                                          Boolean.TRUE,
                                          Optional.empty(),
                                          Optional.empty(),
                                          pageRequest);

    assertThat(devices).isNotEmpty()
                       .hasSize(2)
                       .extracting("id")
                       .contains("00000000-0000-0000-0000-000000000007",
                                 "00000000-0000-0000-0000-000000000008");
  }
}
