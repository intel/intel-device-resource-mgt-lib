/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.redis.dao;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.redis.model.DataSourceReferenceRedis;
import com.openiot.cloud.base.redis.model.ReferenceDefinitionRedis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DataSourceRedisRepository {
  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  /**
   * create a definition item and bind it to group-data_source-definition set
   *
   * @param definition
   */
  public void save(ReferenceDefinitionRedis definition) {
    Map<String, Object> value = new HashMap<>();
    value.put(ConstDef.F_DEVID, definition.getDevId());
    value.put(ConstDef.F_RESURI, definition.getResUrl());
    value.put(ConstDef.F_PROPNAME, definition.getPropName());
    value.put(ConstDef.F_FROM, definition.getFrom());
    value.put(ConstDef.F_TO, definition.getTo());

    redisTemplate.opsForHash().putAll(definition.getKey(), value);

    redisTemplate.opsForZSet()
                 .add(DataSourceReferenceRedis.generateKey(definition.getGroupName(),
                                                           definition.getDataSourceName()),
                      definition.getValue(),
                      definition.getScore());
  }

  /**
   * create all definitions and a group-data_source_definition set
   *
   * @param dataSource
   */
  public void save(DataSourceReferenceRedis dataSource) {
    for (ZSetOperations.TypedTuple<Object> item : dataSource.getDefinitionSet()) {
      save((ReferenceDefinitionRedis) item);
    }
  }

  public ReferenceDefinitionRedis findLatestDefinition(String groupName, String dataSourceName) {
    String key = DataSourceReferenceRedis.generateKey(groupName, dataSourceName);
    if (!redisTemplate.hasKey(key)) {
      return null;
    }

    Set<Object> definitionSet = redisTemplate.opsForZSet().range(key, -1, -1);
    if (definitionSet == null || definitionSet.isEmpty()) {
      return null;
    }

    Iterator<Object> iterator = definitionSet.iterator();
    String latestDefinitionKey = (String) (iterator.next());
    if (!redisTemplate.hasKey(latestDefinitionKey)) {
      return null;
    }

    Map<Object, Object> value = redisTemplate.opsForHash().entries(latestDefinitionKey);
    return new ReferenceDefinitionRedis(groupName,
                                        dataSourceName,
                                        (String) value.get(ConstDef.F_DEVID),
                                        (String) value.get(ConstDef.F_RES),
                                        (String) value.get(ConstDef.F_PROPNAME),
                                        ((Long) value.get(ConstDef.F_FROM)).longValue(),
                                        ((Long) value.get(ConstDef.F_TO)).longValue());
  }

  public List<ReferenceDefinitionRedis>
      findDefinitionByTimeBetween(String groupName, String dataSourceName, long from, long to) {
    String key = DataSourceReferenceRedis.generateKey(groupName, dataSourceName);
    if (!redisTemplate.hasKey(key)) {
      return Collections.emptyList();
    }

    Set<Object> definitionSet = redisTemplate.opsForZSet().rangeByScore(key, from, to);
    if (definitionSet == null || definitionSet.isEmpty()) {
      return Collections.emptyList();
    }

    return definitionSet.stream()
                        .map(obj -> (String) obj)
                        .map(definitionKey -> redisTemplate.opsForHash().entries(definitionKey))
                        .map(value -> {
                          return new ReferenceDefinitionRedis(groupName,
                                                              dataSourceName,
                                                              (String) value.get(ConstDef.F_DEVID),
                                                              (String) value.get(ConstDef.F_RES),
                                                              (String) value.get(ConstDef.F_PROPNAME),
                                                              ((Long) value.get(ConstDef.F_FROM)).longValue(),
                                                              ((Long) value.get(ConstDef.F_TO)).longValue());
                        })
                        .sorted()
                        .collect(Collectors.toList());
  }
}
