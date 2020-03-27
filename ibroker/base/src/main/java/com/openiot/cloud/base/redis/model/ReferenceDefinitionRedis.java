/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.redis.model;

import com.openiot.cloud.base.help.ConstDef;
import org.springframework.data.redis.core.ZSetOperations;

public class ReferenceDefinitionRedis implements ZSetOperations.TypedTuple<Object> {
  private static final String prefixOfKey = ConstDef.REDIS_PREFIX_REFERENCE_DEFINITION;

  // form a key
  private String groupName;
  private String dataSourceName;
  private String key;

  // members
  private String devId;
  private String resUrl;
  private String propName;
  private long from;
  private long to;

  public ReferenceDefinitionRedis() {}

  public ReferenceDefinitionRedis(String groupName, String dataSourceName, String devId,
      String resUrl, String propName, long from, long to) {
    this.groupName = groupName;
    this.dataSourceName = dataSourceName;
    this.devId = devId;
    this.resUrl = resUrl;
    this.propName = propName;
    this.from = from;
    this.to = to;

    this.key = generateKey(groupName, dataSourceName, from);
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

  public String getDevId() {
    return devId;
  }

  public void setDevId(String devId) {
    this.devId = devId;
  }

  public String getResUrl() {
    return resUrl;
  }

  public void setResUrl(String resUrl) {
    this.resUrl = resUrl;
  }

  public String getPropName() {
    return propName;
  }

  public void setPropName(String propName) {
    this.propName = propName;
  }

  public long getFrom() {
    return from;
  }

  public void setFrom(long from) {
    this.from = from;
  }

  public long getTo() {
    return to;
  }

  public void setTo(long to) {
    this.to = to;
  }

  public static String generateKey(String groupName, String dataSourceName, long from) {
    // normal key
    return String.format("%s:%s:%s:%s", prefixOfKey, groupName, dataSourceName, from);
    // hash key
    // return Objects.hash(prefixOfKey, groupName, dataSourceName, from);
  }

  @Override
  public String toString() {
    return "ReferenceDefinitionRedis{" + "groupName='" + groupName + '\'' + ", dataSourceName='"
        + dataSourceName + '\'' + ", key='" + key + '\'' + ", devId='" + devId + '\'' + ", resUrl='"
        + resUrl + '\'' + ", propName='" + propName + '\'' + ", from=" + from + ", to=" + to + '}';
  }

  @Override
  public String getValue() {
    return this.key;
  }

  @Override
  public Double getScore() {
    return Double.valueOf(from);
  }

  @Override
  public int compareTo(ZSetOperations.TypedTuple<Object> o) {
    return getScore().compareTo(o.getScore());
  }
}
