/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.service;

import com.openiot.cloud.base.Application;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.dao.GroupRepository;
import com.openiot.cloud.base.mongo.model.Group;
import com.openiot.cloud.base.mongo.model.help.DataSourceEntity;
import com.openiot.cloud.base.mongo.model.help.DataSourceEntity.Reference;
import com.openiot.cloud.base.redis.model.DataSourceReferenceRedis;
import com.openiot.cloud.base.redis.model.ReferenceDefinitionRedis;
import com.openiot.cloud.base.service.model.DataSource;
import com.openiot.cloud.base.service.model.DataSourceType;
import com.openiot.cloud.base.service.model.ReferenceDefinition;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class}, properties = {"mongo.db = test_openiot"})
public class DataSourceServiceTest {
  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private DataSourceService dataSourceService;

  @Autowired
  private GroupRepository groupRepository;

  @Before
  public void setUp() throws Exception {
    redisTemplate.execute(connection -> {
      connection.flushDb();
      return null;
    }, false);

    groupRepository.deleteAll();
  }

  @Test
  public void testWarmUp() throws Exception {
    Group group = new Group();
    group.setN("group_1");

    DataSourceEntity dataSourceTbl =
        new DataSourceEntity("data_source_1", DataSourceType.TABLE.name());
    group.insertOrUpdateDss(dataSourceTbl);

    DataSourceEntity dataSourceRef1 =
        new DataSourceEntity("data_source_2", DataSourceType.REFERENCE.name());
    group.insertOrUpdateDss(dataSourceRef1);

    DataSourceEntity dataSourceRef2 =
        new DataSourceEntity("data_source_3", DataSourceType.REFERENCE.name());
    dataSourceRef2.setDsdefItem(new DataSourceEntity.Reference("30d4b4700689-m",
                                                               "/3/0",
                                                               "3",
                                                               LocalDateTime.of(2000, 1, 2, 3, 4)
                                                                            .toInstant(ZoneOffset.UTC)
                                                                            .toEpochMilli(),
                                                               LocalDateTime.of(2000, 1, 2, 3, 10)
                                                                            .toInstant(ZoneOffset.UTC)
                                                                            .toEpochMilli()));
    group.insertOrUpdateDss(dataSourceRef2);

    mongoTemplate.save(group, ConstDef.C_GRP);

    dataSourceService.warmUpCache();

    String key = DataSourceReferenceRedis.generateKey("group_1", "data_source_1");
    assertThat(redisTemplate.hasKey(key)).isFalse();

    key = DataSourceReferenceRedis.generateKey("group_1", "data_source_2");
    assertThat(redisTemplate.hasKey(key)).isFalse();

    key = DataSourceReferenceRedis.generateKey("group_1", "data_source_3");
    assertThat(redisTemplate.hasKey(key)).isTrue();
    assertThat(redisTemplate.opsForZSet().zCard(key)).isEqualTo(1);

    key = ReferenceDefinitionRedis.generateKey("group_1",
                                               "data_source_3",
                                               LocalDateTime.of(2000, 1, 2, 3, 4)
                                                            .toInstant(ZoneOffset.UTC)
                                                            .toEpochMilli());
    assertThat(redisTemplate.hasKey(key)).isTrue();
    assertThat(redisTemplate.opsForHash().entries(key).get(ConstDef.F_PROPNAME)).isEqualTo("3");
  }

  @Test
  public void testBasic() throws Exception {
    Group group = new Group();
    group.setN("group_1");
    mongoTemplate.save(group, ConstDef.C_GRP);

    dataSourceService.save("group_1");

    DataSource dataSource = new DataSource();
    dataSource.setName("data_source_1");
    dataSource.setType(DataSourceType.REFERENCE);
    dataSourceService.save("group_1", dataSource);

    assertThat(redisTemplate.hasKey(DataSourceReferenceRedis.generateKey("group_1",
                                                                         "data_source_1"))).isFalse();

    dataSource.setName("data_source_2");
    dataSource.setType(DataSourceType.TABLE);
    dataSourceService.save("group_1", dataSource);
    assertThat(redisTemplate.hasKey(DataSourceReferenceRedis.generateKey("group_1",
                                                                         "data_source_1"))).isFalse();
    assertThat(mongoTemplate.find(query(where(ConstDef.F_ID).is("group_1")),
                                  Group.class,
                                  ConstDef.C_GRP)
                            .get(0)
                            .getDss()).hasSize(2).extracting("dsn").containsOnly("data_source_1",
                                                                                 "data_source_2");

    dataSource.setName("data_source_3");
    dataSource.setType(DataSourceType.REFERENCE);
    dataSource.addReference(new ReferenceDefinition("dad395b3dce1_g1-controller-IO_1201_sim",
                                                    "/d0/3",
                                                    "value",
                                                    LocalDateTime.of(2000, 1, 2, 3, 4)
                                                                 .toInstant(ZoneOffset.UTC)
                                                                 .toEpochMilli(),
                                                    LocalDateTime.of(2000, 1, 2, 3, 14)
                                                                 .toInstant(ZoneOffset.UTC)
                                                                 .toEpochMilli()));
    dataSourceService.save("group_1", dataSource);
    String key = DataSourceReferenceRedis.generateKey("group_1", "data_source_3");
    assertThat(redisTemplate.hasKey(key)).isTrue();
    assertThat(redisTemplate.opsForZSet().zCard(key)).isEqualTo(1);
    key = ReferenceDefinitionRedis.generateKey("group_1",
                                               "data_source_3",
                                               LocalDateTime.of(2000, 1, 2, 3, 4)
                                                            .toInstant(ZoneOffset.UTC)
                                                            .toEpochMilli());
    assertThat(redisTemplate.hasKey(key)).isTrue();
    assertThat(redisTemplate.opsForHash().entries(key).get("pt")).isEqualTo("value");
    assertThat(mongoTemplate.find(query(where(ConstDef.F_ID).is("group_1")),
                                  Group.class,
                                  ConstDef.C_GRP)
                            .get(0)
                            .getDss()).hasSize(3).extracting("dsn").containsOnly("data_source_1",
                                                                                 "data_source_2",
                                                                                 "data_source_3");

    dataSource.setName("data_source_4");
    dataSource.setType(DataSourceType.REFERENCE);
    dataSource.addReference(new ReferenceDefinition("74e182bd2873-m",
                                                    "/3/0",
                                                    "10",
                                                    LocalDateTime.of(2000, 1, 2, 3, 45)
                                                                 .toInstant(ZoneOffset.UTC)
                                                                 .toEpochMilli(),
                                                    0));
    dataSourceService.save("group_1", dataSource);
    group =
        mongoTemplate.find(query(where(ConstDef.F_ID).is("group_1")), Group.class, ConstDef.C_GRP)
                     .get(0);
    DataSourceEntity dataSourceMongo = group.getDsByName("data_source_4");
    assertThat(dataSourceMongo.getLatestReference().getDsri())
                                                              .hasFieldOrPropertyWithValue("resUri",
                                                                                           "/3/0")
                                                              .hasFieldOrPropertyWithValue("pt",
                                                                                           "10");
    assertThat(dataSourceMongo.getLatestReference()).hasFieldOrPropertyWithValue("dsrf",
                                                                                 LocalDateTime.of(2000,
                                                                                                  1,
                                                                                                  2,
                                                                                                  3,
                                                                                                  45)
                                                                                              .toInstant(ZoneOffset.UTC)
                                                                                              .toEpochMilli());
    assertThat(dataSourceMongo.getLatestReference()).hasFieldOrPropertyWithValue("dsrt", 0L);
    assertThat(dataSourceMongo.getThreshHigh()).isNull();
    assertThat(dataSourceMongo.getThreshLow()).isNull();

    // add a new one, should end previous one
    dataSource.addReference(new ReferenceDefinition("74e182bd2873-m",
                                                    "/3/0",
                                                    "10",
                                                    LocalDateTime.of(2000, 1, 2, 4, 45)
                                                                 .toInstant(ZoneOffset.UTC)
                                                                 .toEpochMilli(),
                                                    0));
    dataSourceService.save("group_1", dataSource);

    group =
        mongoTemplate.find(query(where(ConstDef.F_ID).is("group_1")), Group.class, ConstDef.C_GRP)
                     .get(0);
    dataSourceMongo = group.getDsByName("data_source_4");
    assertThat(dataSourceMongo.getLatestReference()).hasFieldOrPropertyWithValue("dsrf",
                                                                                 LocalDateTime.of(2000,
                                                                                                  1,
                                                                                                  2,
                                                                                                  4,
                                                                                                  45)
                                                                                              .toInstant(ZoneOffset.UTC)
                                                                                              .toEpochMilli())
                                                    .hasFieldOrPropertyWithValue("dsrt", 0L);

    List<DataSourceEntity.Reference> referenceList = dataSourceMongo.getDsdefs();
    DataSourceEntity.Reference lastSecondReference = referenceList.get(referenceList.size() - 2);
    assertThat(lastSecondReference.getDsri()).hasFieldOrPropertyWithValue("resUri", "/3/0")
                                             .hasFieldOrPropertyWithValue("pt", "10");
    assertThat(lastSecondReference).hasFieldOrPropertyWithValue("dsrf",
                                                                LocalDateTime.of(2000, 1, 2, 3, 45)
                                                                             .toInstant(ZoneOffset.UTC)
                                                                             .toEpochMilli())
                                   .hasFieldOrPropertyWithValue("dsrt",
                                                                LocalDateTime.of(2000, 1, 2, 4, 45)
                                                                             .toInstant(ZoneOffset.UTC)
                                                                             .toEpochMilli());
  }

  @Test
  public void testGetAllDataSource() throws Exception {
    Group group = new Group();
    group.setN("group_1");

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
    group.insertOrUpdateDss(new DataSourceEntity("datasource_3",
                                                 DataSourceType.REFERENCE.toString()));

    mongoTemplate.save(group, ConstDef.C_GRP);
    List<String> allDss = dataSourceService.findAllDataSourcesWithRef();

    assertThat(allDss).isNotEmpty().hasSize(2);
  }
}
