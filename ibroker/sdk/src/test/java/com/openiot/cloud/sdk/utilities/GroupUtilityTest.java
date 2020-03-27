/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.utilities;

import com.openiot.cloud.base.mongo.dao.GroupRepository;
import com.openiot.cloud.base.mongo.dao.GroupTypeRepository;
import com.openiot.cloud.base.mongo.model.Group;
import com.openiot.cloud.sdk.Application;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class}, properties = {"mongo.db = test_openiot"})
public class GroupUtilityTest {
  @Autowired
  private GroupUtility gu;

  @Autowired
  private GroupTypeRepository grptRepo;
  @Autowired
  private GroupRepository grpRepo;

  @Before
  public void setUp() {
    grpRepo.deleteAll();
    grptRepo.deleteAll();
  }

  @After
  public void tearDown() {}

  @Test
  public void addGroupTest() {
    Group group = gu.addGroup("p1", "work", "from home", null);
    assertThat(group).isNotNull()
                     .hasFieldOrPropertyWithValue("n", "work")
                     .hasFieldOrPropertyWithValue("prj", "p1")
                     .hasFieldOrPropertyWithValue("dpn", "from home");

    group = gu.addGroup("p2", "work", null, null);
    assertThat(group).isNotNull()
                     .hasFieldOrPropertyWithValue("n", "work")
                     .hasFieldOrPropertyWithValue("prj", "p2");

    group.setDpn("from office");
    group = gu.addOrUpdateGroup(group);
    assertThat(group).isNotNull()
                     .hasFieldOrPropertyWithValue("n", "work")
                     .hasFieldOrPropertyWithValue("dpn", "from office");
  }

  @Test
  public void updateGroupCfgTest() {
    Group group = gu.addGroup("p1", "work", "from home", null);

    Map<String, String> cfgs = Stream
                                     .of(new AbstractMap.SimpleEntry<>("a", "1"),
                                         new AbstractMap.SimpleEntry<>("b", "2"),
                                         new AbstractMap.SimpleEntry<>("c", "3"))
                                     .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

    gu.updateGroupCfg(group, cfgs);
    gu.updateGroupCfg("work", cfgs);

    group = gu.getGroupByName("work");
    assertThat(group.getCs()).isNotNull().isNotEmpty().hasSize(3).extracting("cn").contains("a",
                                                                                            "b",
                                                                                            "c");
  }
}
