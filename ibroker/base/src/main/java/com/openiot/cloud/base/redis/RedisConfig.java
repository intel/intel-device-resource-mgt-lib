/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisKeyValueAdapter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.repository.query.QueryLookupStrategy;

@Configuration
@EnableRedisRepositories(basePackages = "com.openiot.cloud.base.redis.dao",
    enableKeyspaceEvents = RedisKeyValueAdapter.EnableKeyspaceEvents.ON_STARTUP,
    queryLookupStrategy = QueryLookupStrategy.Key.USE_DECLARED_QUERY)
public class RedisConfig {
  private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

  @Value(value = "${redis.addr:127.0.0.1}")
  private String address;

  @Value(value = "${redis.port:6379}")
  private int port;

  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(address, port);
    if (configuration != null) {
      return new LettuceConnectionFactory(configuration);
    }
    return null;
  }

  @Bean
  public RedisTemplate<String, Object>
      redisTemplate(RedisConnectionFactory redisConnectionFactory) {
    if (redisConnectionFactory == null) {
      return null;
    }

    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setDefaultSerializer(new StringRedisSerializer());
    // TODO: need to find out a language independent serializer
    template.setHashValueSerializer(new JdkSerializationRedisSerializer());
    template.setConnectionFactory(redisConnectionFactory);
    template.afterPropertiesSet();
    return template;
  }
}
