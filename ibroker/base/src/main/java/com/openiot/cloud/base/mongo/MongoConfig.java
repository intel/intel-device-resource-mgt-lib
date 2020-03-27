/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo;

import com.mongodb.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.WriteConcernResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import javax.validation.Validator;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableMongoRepositories(basePackages = "com.openiot.cloud.base.mongo.dao")
public class MongoConfig {
  @Value(value = "#{'${mongo.addr:localhost}'.split(',')}")
  private List<String> mongoAddr;

  @Value(value = "${mongo.db:openiot}")
  private String openiotDb;

  @Value(value = "${mongo.user:beihai}")
  private String mongoUser;

  @Value(value = "${mongo.passwd:intel@123}")
  private String mongoPasswd;

  @Bean
  public MongoDbFactory mongoDbFactory() {
    MongoClient mongo;

    // remove empty members
    mongoAddr = mongoAddr.stream()
                         .map(item -> item.replace(" ", ""))
                         .filter(addr -> !addr.isEmpty())
                         .collect(Collectors.toList());

    MongoClientOptions options =
        MongoClientOptions.builder().readConcern(ReadConcern.LOCAL).connectionsPerHost(64).build();
    MongoCredential credential =
        MongoCredential.createCredential(mongoUser, openiotDb, mongoPasswd.toCharArray());
    if (mongoAddr.size() > 1) {
      List<ServerAddress> replSetAddr =
          mongoAddr.stream().map(addr -> new ServerAddress(addr)).collect(Collectors.toList());
      mongo = new MongoClient(replSetAddr, credential, options);
    } else {
      mongo = new MongoClient(new ServerAddress(mongoAddr.get(0)), credential, options);
    }
    SimpleMongoDbFactory simpleMongoDbFactory = new SimpleMongoDbFactory(mongo, openiotDb);
    simpleMongoDbFactory.setWriteConcern(WriteConcern.JOURNALED.withJournal(Boolean.TRUE));

    return simpleMongoDbFactory;
  }

  // @EnableMongoRepositories declares a monoOperations already
  @Bean
  public MongoTemplate mongoTemplate() {
    // remove "_class" field
    MappingMongoConverter converter =
        new MappingMongoConverter(new DefaultDbRefResolver(mongoDbFactory()),
                                  new MongoMappingContext());
    converter.setTypeMapper(new DefaultMongoTypeMapper(null));

    return new MongoTemplate(mongoDbFactory(), converter);
  }

  @Bean
  public WriteConcernResolver writeConcernResolver() {
    return action -> {
      return WriteConcern.JOURNALED.withJournal(Boolean.TRUE);
    };
  }

  @Bean
  public Validator localValidatorFactoryBean() {
    return new LocalValidatorFactoryBean();
  }
}
