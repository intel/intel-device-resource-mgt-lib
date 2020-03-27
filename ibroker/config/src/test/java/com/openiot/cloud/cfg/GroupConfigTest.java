/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.cfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.Group;
import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import com.openiot.cloud.base.mongo.model.help.DataSourceEntity;
import com.openiot.cloud.cfg.model.GroupConfig;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class}, properties = {"mongo.db = test_openiot"})
public class GroupConfigTest {
  @Test
  public void testTransfer() throws Exception {
    GroupConfig config = GroupConfig.from(Optional.empty());
    assertThat(config).isNull();

    Group group = new Group();
    group.setN("nectarine");
    group.setD("guava");
    group.setDpn("kiwi");
    group.setPrj("boom");
    group.setGt("peach");

    group.insertOrUpdateCs(new ConfigurationEntity("date", "13"));
    group.insertOrUpdateCs(new ConfigurationEntity("honeydew", "23"));

    group.insertOrUpdateMds("device_1023");
    group.insertOrUpdateMds("device_1025");

    group.insertOrUpdateMrs(new Group.MemberResRef("device_1020", "resource_1"));
    group.insertOrUpdateMrs(new Group.MemberResRef("device_1020", "resource_2"));

    DataSourceEntity.OperateEntity operate = new DataSourceEntity.OperateEntity();
    operate.setType("DO");
    operate.setSched("apple");

    DataSourceEntity dataSource = new DataSourceEntity();
    dataSource.addDsAttrItem(new AttributeEntity("lemon", "25"));
    dataSource.addDsAttrItem(new AttributeEntity("grape", "38"));
    dataSource.setClassInfo("raspberry");
    dataSource.setDsdefItem(new DataSourceEntity.Reference("device_pineapple",
                                                           "/resource_1/23",
                                                           "kumquat",
                                                           BaseUtil.getNowAsEpochMillis(),
                                                           0));
    dataSource.setDescription("cantaloupe");
    dataSource.setDsn("apricot");
    dataSource.setDst("ref");
    dataSource.setTitle("title");
    dataSource.setThreshHigh(100l);
    dataSource.setThreshLow(13l);
    dataSource.setUnit("c");
    dataSource.setOperate(operate);
    group.insertOrUpdateDss(dataSource);

    config = GroupConfig.from(Optional.of(group));

    String json = new ObjectMapper().writeValueAsString(config);
    System.out.println("--> " + json);
    config = new ObjectMapper().readValue(json, GroupConfig.class);

    assertThat(config).hasFieldOrProperty("ct")
                      .hasFieldOrPropertyWithValue("d", group.getD())
                      .hasFieldOrPropertyWithValue("dpn", group.getDpn())
                      .hasFieldOrPropertyWithValue("gt", group.getGt())
                      .hasFieldOrPropertyWithValue("n", group.getN())
                      .hasFieldOrPropertyWithValue("project", group.getPrj());

    assertThat(config.getCs()).extracting("n").containsOnly("date", "honeydew");
    assertThat(config.getMd()).containsOnly("device_1023", "device_1025");
    assertThat(config.getMr()).extracting("resUri").containsOnly("resource_1", "resource_2");

    assertThat(config.getDss()).hasSize(1);
    assertThat(config.getDss().get(0))
                                      .hasFieldOrPropertyWithValue("classInfo",
                                                                   dataSource.getClassInfo())
                                      .hasFieldOrPropertyWithValue("description",
                                                                   dataSource.getDescription())
                                      .hasFieldOrPropertyWithValue("n", dataSource.getDsn())
                                      .hasFieldOrPropertyWithValue("t", dataSource.getDst())
                                      .hasFieldOrPropertyWithValue("threshLow",
                                                                   dataSource.getThreshLow())
                                      .hasFieldOrPropertyWithValue("threshHigh",
                                                                   dataSource.getThreshHigh())
                                      .hasFieldOrProperty("operate");
    assertThat(config.getDss().get(0).getOperate()).isNotNull()
                                                   .hasFieldOrPropertyWithValue("type", "DO")
                                                   .hasFieldOrPropertyWithValue("sched", "apple");
    assertThat(config.getDss().get(0).getOperate().getBackground_state()).isNull();
    assertThat(config.getDss().get(0).getOperate().getDi()).isNull();
    assertThat(config.getDss().get(0).getOperate().getUrl()).isNull();
    assertThat(config.getDss().get(0).getOperate().getPn()).isNull();
    assertThat(config.getDss().get(0).getOperate().getState_cmds()).isNull();

    assertThat(config.getDss().get(0).getAttributeList()).extracting("an").containsOnly("lemon",
                                                                                        "grape");
    assertThat(config.getDss()
                     .get(0)
                     .getD()).hasFieldOrPropertyWithValue("d",
                                                          dataSource.getLatestReference()
                                                                    .getDsri()
                                                                    .getDi())
                             .hasFieldOrPropertyWithValue("url",
                                                          dataSource.getLatestReference()
                                                                    .getDsri()
                                                                    .getResUri())
                             .hasFieldOrPropertyWithValue("pn",
                                                          dataSource.getLatestReference()
                                                                    .getDsri()
                                                                    .getPt())
                             .hasFieldOrPropertyWithValue("s",
                                                          dataSource.getLatestReference()
                                                                    .getDsrf());

    DataSourceEntity dataSourceApple = new DataSourceEntity();
    dataSourceApple.setDsn("apple");
    dataSourceApple.setDst(ConstDef.F_DATASOURCEREF);
    group.insertOrUpdateDss(dataSourceApple);

    config = GroupConfig.from(Optional.of(group));
    json = new ObjectMapper().writeValueAsString(config);
    config = new ObjectMapper().readValue(json, GroupConfig.class);

    assertThat(config.getDss()
                     .stream()
                     .filter(dataSource1 -> dataSource1.getN().equals(dataSourceApple.getDsn()))
                     .findFirst()
                     .orElse(null)).isNotNull()
                                   .hasFieldOrPropertyWithValue("threshHigh", null)
                                   .hasFieldOrPropertyWithValue("classInfo", null);
  }
}
