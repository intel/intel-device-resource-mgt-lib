/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao;

import com.openiot.cloud.base.Application;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.GroupType;
import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import com.openiot.cloud.base.mongo.model.help.DataSourceEntity;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class}, properties = {"mongo.db = test_openiot"})
public class GroupTypeRepositoryTest {
  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private GroupTypeRepository groupTypeRepository;

  @Before
  public void setUp() throws Exception {
    groupTypeRepository.deleteAll();
  }

  @Test
  public void testFields() throws Exception {
    GroupType groupType = new GroupType();
    groupType.setN("group_type_1");
    groupType.setD("a simple description of group_type_1");
    groupType.insertOrUpdateAs(new AttributeEntity("huckleberry", "13"));
    groupType.insertOrUpdateCs(new ConfigurationEntity("dragonfruit", "17"));
    groupType.insertOrUpdateDss(new DataSourceEntity("nectarine", ConstDef.F_DATASOURCEREF));
    groupTypeRepository.save(groupType);

    List<GroupType> groupTypeList =
        groupTypeRepository.filter(Optional.empty(),
                                   Optional.of("group_type_1"),
                                   Optional.empty(),
                                   Collections.emptyList(),
                                   Collections.emptyList(),
                                   Collections.emptyList(),
                                   Collections.emptyMap(),
                                   Collections.emptyMap(),
                                   Stream.of(ConstDef.F_DESCRIPTION).collect(Collectors.toList()),
                                   PageRequest.of(0, 10));
    System.out.println(groupTypeList);
    assertThat(groupTypeList).hasSize(1)
                             .element(0)
                             .hasFieldOrPropertyWithValue("d", groupType.getD())
                             .hasFieldOrPropertyWithValue("as", null)
                             .hasFieldOrPropertyWithValue("cs", null)
                             .hasFieldOrPropertyWithValue("dss", null);

    groupTypeList = groupTypeRepository.filter(Optional.empty(),
                                               Optional.of("group_type_1"),
                                               Optional.empty(),
                                               Collections.emptyList(),
                                               Collections.emptyList(),
                                               Collections.emptyList(),
                                               Collections.emptyMap(),
                                               Collections.emptyMap(),
                                               Stream.of(ConstDef.F_DESCRIPTION, ConstDef.F_ATTRS)
                                                     .collect(Collectors.toList()),
                                               PageRequest.of(0, 10));
    System.out.println(groupTypeList);
    assertThat(groupTypeList).hasSize(1)
                             .element(0)
                             .hasFieldOrPropertyWithValue("d", groupType.getD())
                             .hasFieldOrPropertyWithValue("as", groupType.getAs())
                             .hasFieldOrPropertyWithValue("cs", null)
                             .hasFieldOrPropertyWithValue("dss", null);

    groupTypeList =
        groupTypeRepository.filter(Optional.empty(),
                                   Optional.of("group_type_1"),
                                   Optional.empty(),
                                   Collections.emptyList(),
                                   Collections.emptyList(),
                                   Collections.emptyList(),
                                   Collections.emptyMap(),
                                   Collections.emptyMap(),
                                   Stream.of(ConstDef.F_DESCRIPTION,
                                             ConstDef.F_ATTRS,
                                             ConstDef.F_CONFIGS)
                                         .collect(Collectors.toList()),
                                   PageRequest.of(0, 10));
    System.out.println(groupTypeList);
    assertThat(groupTypeList).hasSize(1)
                             .element(0)
                             .hasFieldOrPropertyWithValue("d", groupType.getD())
                             .hasFieldOrPropertyWithValue("as", groupType.getAs())
                             .hasFieldOrPropertyWithValue("cs", groupType.getCs())
                             .hasFieldOrPropertyWithValue("dss", null);

    groupTypeList =
        groupTypeRepository.filter(Optional.empty(),
                                   Optional.of("group_type_1"),
                                   Optional.empty(),
                                   Collections.emptyList(),
                                   Collections.emptyList(),
                                   Collections.emptyList(),
                                   Collections.emptyMap(),
                                   Collections.emptyMap(),
                                   Stream.of(ConstDef.F_DESCRIPTION,
                                             ConstDef.F_ATTRS,
                                             ConstDef.F_CONFIGS,
                                             ConstDef.F_DATASOURCES)
                                         .collect(Collectors.toList()),
                                   PageRequest.of(0, 10));
    System.out.println(groupTypeList);
    assertThat(groupTypeList).hasSize(1)
                             .element(0)
                             .hasFieldOrPropertyWithValue("d", groupType.getD())
                             .hasFieldOrPropertyWithValue("as", groupType.getAs())
                             .hasFieldOrPropertyWithValue("cs", groupType.getCs())
                             .hasFieldOrPropertyWithValue("dss", groupType.getDss());
  }
}
