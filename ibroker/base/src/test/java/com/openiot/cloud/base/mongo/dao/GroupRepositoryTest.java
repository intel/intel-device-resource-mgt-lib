/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao;

import com.openiot.cloud.base.Application;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.Group;
import com.openiot.cloud.base.mongo.model.GroupType;
import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import com.openiot.cloud.base.mongo.model.help.DataSourceEntity;
import com.openiot.cloud.base.mongo.model.help.DataSourceEntity.Reference;
import com.openiot.cloud.base.service.model.DataSourceType;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class}, properties = {"mongo.db = test_openiot"})
public class GroupRepositoryTest {
  @Autowired
  private GroupRepository groupRepository;

  @Autowired
  private GroupTypeRepository groupTypeRepository;

  @Autowired
  private MongoTemplate template;

  @Before
  public void setUp() throws Exception {
    groupRepository.deleteAll();

    Group g1 = new Group();
    g1.setN("g1");
    g1.insertOrUpdateMds("dev_1");
    g1.insertOrUpdateMrs(new Group.MemberResRef("dev_1", "res_1"));
    g1.insertOrUpdateMds("dev_2");
    g1.insertOrUpdateMrs(new Group.MemberResRef("dev_2", "res_1"));
    g1.insertOrUpdateMds("dev_3");
    g1.insertOrUpdateMrs(new Group.MemberResRef("dev_3", "res_1"));
    groupRepository.save(g1);

    Group g2 = new Group();
    g2.setN("g2");
    g2.insertOrUpdateMds("dev_2");
    g2.insertOrUpdateMrs(new Group.MemberResRef("dev_2", "res_1"));
    g2.insertOrUpdateMds("dev_3");
    g2.insertOrUpdateMrs(new Group.MemberResRef("dev_3", "res_1"));
    g2.insertOrUpdateMds("dev_4");
    g2.insertOrUpdateMrs(new Group.MemberResRef("dev_4", "res_1"));

    DataSourceEntity dataSource = new DataSourceEntity("ds_1", ConstDef.F_DATASOURCEREF);
    g2.insertOrUpdateDss(dataSource);
    groupRepository.save(g2);

    Group g3 = new Group();
    g3.setN("g3");
    g3.insertOrUpdateMds("dev_3");
    g3.insertOrUpdateMrs(new Group.MemberResRef("dev_3", "res_1"));
    g3.insertOrUpdateMds("dev_4");
    g3.insertOrUpdateMrs(new Group.MemberResRef("dev_4", "res_1"));
    g3.insertOrUpdateMds("dev_5");
    g3.insertOrUpdateMrs(new Group.MemberResRef("dev_5", "res_1"));
    groupRepository.save(g3);
  }

  @Test
  public void testFindAllGroupByDevId() throws Exception {
    List<Group> groupList = groupRepository.findAllGroupByDevId("dev_2");
    assertThat(groupList).isNotEmpty().extracting("n").containsOnly("g1", "g2");

    groupList = groupRepository.findAllGroupByDevId("dev_3");
    assertThat(groupList).isNotEmpty().extracting("n").containsOnly("g1", "g2", "g3");
  }

  @Test
  public void testFindAllGroupByRes() throws Exception {
    List<Group> groupList = groupRepository.findAllGroupByRes("dev_2", "res_1");
    assertThat(groupList).isNotEmpty().extracting("n").containsOnly("g1", "g2");

    groupList = groupRepository.findAllGroupByRes("dev_3", "res_1");
    assertThat(groupList).isNotEmpty().extracting("n").containsOnly("g1", "g2", "g3");
  }

  @Test
  public void testFindOneByName() throws Exception {
    Group group = groupRepository.findOneByName("g2");
    assertThat(group).hasFieldOrPropertyWithValue("n", "g2");
  }

  @Test
  public void testFindOneDsByName() throws Exception {
    GroupType groupType = new GroupType();
    groupType.setN("group_type_1");
    DataSourceEntity dataSourceTemplate =
        new DataSourceEntity("data_source_1", DataSourceType.TABLE.name());
    dataSourceTemplate.addDsAttrItem(new AttributeEntity("attr_1", "from template"));
    groupType.insertOrUpdateDss(dataSourceTemplate);
    groupTypeRepository.save(groupType);

    Group group = new Group();
    group.setN("group_1");
    group.setGt("group_type_1");
    groupRepository.save(group);

    List<DataSourceEntity> dataSourceList =
        groupRepository.findDssByGroupNameAndDsName("group_1", "data_source_1");
    assertThat(dataSourceList).isNotEmpty();
    assertThat(dataSourceList.get(0)
                             .getAttributeList()
                             .get(0)).hasFieldOrPropertyWithValue("av", "from template");

    DataSourceEntity dataSource =
        new DataSourceEntity("data_source_1", DataSourceType.TABLE.name());
    dataSource.addDsAttrItem(new AttributeEntity("attr_1", "from data source"));
    group.insertOrUpdateDss(dataSource);
    groupRepository.save(group);

    dataSourceList = groupRepository.findDssByGroupNameAndDsName("group_1", "data_source_1");
    assertThat(dataSourceList.get(0)
                             .getAttributeList()
                             .get(0)).hasFieldOrPropertyWithValue("av", "from data source");
  }

  @Test
  public void testFindOneDsByInternalId() throws Exception {
    Group group = groupRepository.findOneByName("g2");

    DataSourceEntity dataSource =
        groupRepository.findOneDsByInternalId(group.getDss().get(0).getDsintId());
    assertThat(dataSource).hasFieldOrPropertyWithValue("dsn", "ds_1");
  }

  @Test
  public void testInherit() throws Exception {
    GroupType groupType = new GroupType();
    groupType.setN("group_type_1");

    groupType.insertOrUpdateCs(new ConfigurationEntity("cfg_1", "123"));

    DataSourceEntity dataSourceRef =
        new DataSourceEntity("data_source_1", DataSourceType.REFERENCE.name());
    dataSourceRef.addDsAttrItem(new AttributeEntity("attr_1", "3.14"));

    DataSourceEntity dataSourceTbl =
        new DataSourceEntity("data_source_2", DataSourceType.TABLE.name());

    groupType.insertOrUpdateDss(dataSourceRef);
    groupType.insertOrUpdateDss(dataSourceTbl);

    groupTypeRepository.save(groupType);

    Group group = new Group();
    group.setN("group_1");
    group.setGt("group_type_1");
    groupRepository.save(group);

    group = groupRepository.findOneByName("group_1");
    assertThat(group.getCs()).extracting("cn").containsOnly("cfg_1");
    assertThat(group.getDss()).extracting("dsn").containsOnly("data_source_1", "data_source_2");

    DataSourceEntity dataSource =
        groupRepository.findDssByGroupNameAndDsName("group_1", "data_source_1").get(0);
    assertThat(dataSource.getAttributeList()).extracting("av").containsOnly("3.14");

    group.insertOrUpdateCs(new ConfigurationEntity("cfg_1", "234"));
    groupRepository.save(group);

    group = groupRepository.findOneByName("group_1");
    assertThat(group.getCs()).extracting("cn").containsOnly("cfg_1");
    assertThat(group.getCs()).extracting("cv").containsOnly("234");
  }

  @Test
  public void testDataSourcePropertiesInheritFromTemplateInGroupType() throws Exception {
    // prepare group type
    GroupType gt = new GroupType();
    gt.setN("group_type_1");
    DataSourceEntity ds1 = new DataSourceEntity();
    ds1.setDsn("datasource_1");
    ds1.setClassInfo("sensor");
    ds1.setDescription("desc");
    ds1.setTitle("title_1");
    ds1.setUnit("unit");
    ds1.setThreshHigh(30l);
    ds1.setThreshLow(10l);
    gt.insertOrUpdateDss(ds1);
    DataSourceEntity ds2 = new DataSourceEntity();
    ds2.setDsn("datasource_2");
    ds2.setClassInfo("sensor");
    ds2.setDescription("desc2");
    gt.insertOrUpdateDss(ds2);
    groupTypeRepository.save(gt);

    // prepare group
    Group group = new Group();
    group.setN("group_1");
    group.setGt("group_type_1");
    DataSourceEntity dataSourceTable =
        new DataSourceEntity("datasource_1", DataSourceType.TABLE.toString());
    dataSourceTable.setDsdefItem(new Reference("", "", "", 1, 1));
    DataSourceEntity dataSourceReference =
        new DataSourceEntity("datasource_2", DataSourceType.REFERENCE.toString());
    dataSourceReference.setDsdefItem(new Reference("dummy_device",
                                                   "/switch",
                                                   "value",
                                                   LocalDateTime.of(2000, 1, 2, 3, 4, 5)
                                                                .toInstant(ZoneOffset.UTC)
                                                                .toEpochMilli(),
                                                   LocalDateTime.of(2000, 2, 3, 4, 5, 6)
                                                                .toInstant(ZoneOffset.UTC)
                                                                .toEpochMilli()));
    group.insertOrUpdateDss(dataSourceTable);
    group.insertOrUpdateDss(dataSourceReference);
    groupRepository.save(group);

    // check
    List<DataSourceEntity> datasource1 =
        groupRepository.findDssByGroupNameAndDsName("group_1", "datasource_1");
    assertThat(datasource1).isNotNull().hasSize(1);
    assertThat(datasource1.get(0)).hasFieldOrPropertyWithValue("classInfo", "sensor");
    assertThat(datasource1.get(0)).hasFieldOrPropertyWithValue("description", "desc");
    assertThat(datasource1.get(0)).hasFieldOrPropertyWithValue("title", "title_1");
    assertThat(datasource1.get(0)).hasFieldOrPropertyWithValue("unit", "unit");
    assertThat(datasource1.get(0)).hasFieldOrPropertyWithValue("threshHigh", 30l);
    assertThat(datasource1.get(0)).hasFieldOrPropertyWithValue("threshLow", 10l);

    List<DataSourceEntity> datasource2 =
        groupRepository.findDssByGroupNameAndDsName("group_1", "datasource_2");
    assertThat(datasource2).isNotNull().hasSize(1);
    assertThat(datasource2.get(0)).hasFieldOrPropertyWithValue("classInfo", "sensor");
    assertThat(datasource2.get(0)).hasFieldOrPropertyWithValue("description", "desc2");
    assertThat(datasource2.get(0).getTitle()).isNullOrEmpty();
    assertThat(datasource2.get(0).getUnit()).isNullOrEmpty();
    assertThat(datasource2.get(0).getThreshHigh()).isNull();
    assertThat(datasource2.get(0).getThreshLow()).isNull();
  }

  @Test
  public void testReplaceOrUpdate() throws Exception {
    Group group = new Group();
    group.setN("group_17");
    group.setGt("group_type_19");
    group = groupRepository.save(group);

    assertThat(group.getN()).isEqualTo("group_17");
    assertThat(group.getAs()).isNull();
    assertThat(group.getCs()).isNull();
    assertThat(group.getDss()).isNull();
    assertThat(group.getMd()).isNull();
    assertThat(group.getMr()).isNull();

    group.replaceOrClearAs(Stream.of(new AttributeEntity("grape", "13"),
                                     new AttributeEntity("lime", "3.14"))
                                 .collect(Collectors.toList()));
    assertThat(group.getAs()).isNotEmpty();
    group.replaceOrClearAs(null);
    assertThat(group.getAs()).isNotEmpty().extracting("av").containsOnly("13", "3.14");

    group.replaceOrClearCs(Stream.of(new ConfigurationEntity("apricot", "true"))
                                 .collect(Collectors.toList()));
    assertThat(group.getCs()).isNotEmpty();
    group.replaceOrClearCs(new LinkedList<>());
    assertThat(group.getCs()).isNull();
  }

  @Test
  public void testDelete() throws Exception {
    Group group = new Group();
    group.setN("guava");
    group = groupRepository.save(group);

    group.removeItemFromMd("not exist");
    group.removeItemFromMr(new Group.MemberResRef("not", "exist"));
    group.removeItemsFromAs(Collections.emptyList());
    group.removeItemsFromAs(Stream.of("not exist").collect(Collectors.toList()));
    group.removeItemsFromCs(Collections.emptyList());
    group.removeItemsFromCs(Stream.of("not exist").collect(Collectors.toList()));
    group.removeItemsFromMd(Collections.emptyList());
    group.removeItemsFromMd(Stream.of("not exist").collect(Collectors.toList()));
    group.removeItemsFromMr(Collections.emptyList());
    group.removeItemsFromMr(Stream.of(new Group.MemberResRef("not", "exist"))
                                  .collect(Collectors.toList()));
  }
}
