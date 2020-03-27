/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.redis.dao;

import com.openiot.cloud.base.Application;
import com.openiot.cloud.base.redis.model.DataSourceReferenceRedis;
import com.openiot.cloud.base.redis.model.ReferenceDefinitionRedis;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.junit4.SpringRunner;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class})
public class DataSourceRedisRepositoryTest {
  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @Autowired
  private DataSourceRedisRepository dataSourceRedisRepository;

  @Before
  public void setUp() throws Exception {
    redisTemplate.execute(connection -> {
      connection.flushDb();
      return null;
    }, false);
  }

  @Test
  public void testBasic() throws Exception {
    ReferenceDefinitionRedis definition =
        new ReferenceDefinitionRedis("group_1",
                                     "data_source_1",
                                     "E-43165433",
                                     "/3303/0",
                                     "5601",
                                     LocalDateTime.of(2000, 1, 2, 3, 4, 5)
                                                  .toInstant(ZoneOffset.UTC)
                                                  .toEpochMilli(),
                                     LocalDateTime.of(2000, 1, 2, 3, 5, 5)
                                                  .toInstant(ZoneOffset.UTC)
                                                  .toEpochMilli());
    dataSourceRedisRepository.save(definition);
    assertThat(redisTemplate.hasKey(definition.getKey())).isTrue();
    assertThat(redisTemplate.hasKey(DataSourceReferenceRedis.generateKey("group_1",
                                                                         "data_source_1"))).isTrue();

    long baseTime = LocalDateTime.of(2000, 1, 2, 3, 4).toInstant(ZoneOffset.UTC).toEpochMilli();
    DataSourceReferenceRedis dataSourceReferenceRedis =
        new DataSourceReferenceRedis("group_1", "data_source_1");
    dataSourceReferenceRedis.addDefinitionItem(new ReferenceDefinitionRedis("group_1",
                                                                            "data_source_1",
                                                                            "E-43165433",
                                                                            "/3303/0",
                                                                            "5602",
                                                                            baseTime
                                                                                + TimeUnit.MINUTES.toMillis(5),
                                                                            baseTime
                                                                                + TimeUnit.MINUTES.toMillis(10)));
    dataSourceReferenceRedis.addDefinitionItem(new ReferenceDefinitionRedis("group_1",
                                                                            "data_source_1",
                                                                            "E-43165433",
                                                                            "/3303/0",
                                                                            "5603",
                                                                            baseTime
                                                                                + TimeUnit.MINUTES.toMillis(15),
                                                                            baseTime
                                                                                + TimeUnit.MINUTES.toMillis(20)));
    dataSourceReferenceRedis.addDefinitionItem(new ReferenceDefinitionRedis("group_1",
                                                                            "data_source_1",
                                                                            "E-43165433",
                                                                            "/3303/0",
                                                                            "5604",
                                                                            baseTime
                                                                                + TimeUnit.MINUTES.toMillis(25),
                                                                            baseTime
                                                                                + TimeUnit.MINUTES.toMillis(30)));
    dataSourceRedisRepository.save(dataSourceReferenceRedis);

    assertThat(redisTemplate.hasKey(dataSourceReferenceRedis.getKey())).isTrue();
    for (ZSetOperations.TypedTuple<Object> item : dataSourceReferenceRedis.getDefinitionSet()) {
      assertThat(redisTemplate.hasKey(((ReferenceDefinitionRedis) item).getKey())).isTrue();
    }
    assertThat(redisTemplate.opsForZSet().zCard(dataSourceReferenceRedis.getKey())).isEqualTo(4);

    assertThat(dataSourceRedisRepository.findLatestDefinition("group_1",
                                                              "data_source_1")).hasFieldOrPropertyWithValue("propName",
                                                                                                            "5604");

    List<ReferenceDefinitionRedis> definitionRedisList =
        dataSourceRedisRepository.findDefinitionByTimeBetween("group_1",
                                                              "data_source_1",
                                                              baseTime
                                                                  + TimeUnit.MINUTES.toMillis(10),
                                                              baseTime
                                                                  + TimeUnit.MINUTES.toMillis(25));
    assertThat(definitionRedisList).hasSize(2).extracting("propName").containsOnly("5603", "5604");
  }
}
