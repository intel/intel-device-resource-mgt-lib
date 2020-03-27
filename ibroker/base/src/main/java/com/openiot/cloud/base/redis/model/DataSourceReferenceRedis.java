/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.redis.model;

import com.openiot.cloud.base.help.ConstDef;
import org.springframework.data.redis.core.ZSetOperations;
import java.util.HashSet;
import java.util.Set;

public class DataSourceReferenceRedis {

  private static final String prefixOfKey = ConstDef.REDIS_PREFIX_REFERENCE;

  // form a key
  private String groupName;
  private String dataSourceName;
  private String key;

  // members
  private Set<ZSetOperations.TypedTuple<Object>> definitionSet;

  public DataSourceReferenceRedis(String groupName, String dataSourceName) {
    this.groupName = groupName;
    this.dataSourceName = dataSourceName;
    this.key = generateKey(groupName, dataSourceName);
    this.definitionSet = new HashSet<>();
  }

  public static String generateKey(String groupName, String dataSourceName) {
    // normal key
    return String.format("%s:%s:%s", prefixOfKey, groupName, dataSourceName);
    // hash key
    // return Objects.hash(prefixOfKey, groupName, dataSourceName);
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public String getDataSourceName() {
    return dataSourceName;
  }

  public void setDataSourceName(String dataSourceName) {
    this.dataSourceName = dataSourceName;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public Set<ZSetOperations.TypedTuple<Object>> getDefinitionSet() {
    return definitionSet;
  }

  public void setDefinitionSet(Set<ZSetOperations.TypedTuple<Object>> definitionSet) {
    this.definitionSet.clear();
    this.definitionSet.addAll(definitionSet);
  }

  public void addDefinitionItem(ZSetOperations.TypedTuple<Object> definition) {
    this.definitionSet.add(definition);
  }
}
