/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker;

import com.openiot.cloud.base.mq.MessageQueue;
import com.openiot.cloud.ibroker.proxy.dp.GenericCoapTCPServiceClient;
import com.openiot.cloud.ibroker.proxy.rd.RDProxy;
import org.iotivity.cloud.base.protocols.IRequest;
import org.iotivity.cloud.base.resource.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.UUID;
import java.util.concurrent.Executors;

@SpringBootApplication(scanBasePackages = {"com.openiot.cloud.sdk.service",
    "com.openiot.cloud.ibroker", "com.openiot.cloud.base"})
@EnableScheduling
public class IBrokerMain {
  private static final Logger logger = LoggerFactory.getLogger(IBrokerMain.class);

  @Value("${openiot.performance.message_queue_capacity:36864}")
  private int MESSAGE_QUEUE_CAPACITY;

  @Bean(name = "dataPointResource")
  public GenericCoapTCPServiceClient dataPointResource() {
    return new GenericCoapTCPServiceClient("dp", "/dp");
  }

  @Bean(name = "metaResource")
  public GenericCoapTCPServiceClient metaResource() {
    return new GenericCoapTCPServiceClient("meta", "/meat");
  }

  @Bean
  public ResourceManager resourcesForCoapOverTcp(RDProxy resourceDirectoryResource,
                                                 GenericCoapTCPServiceClient dataPointResource,
                                                 GenericCoapTCPServiceClient metaResource) {
    ResourceManager rm = new ResourceManager();
    rm.addResource(resourceDirectoryResource);
    rm.addResource(metaResource);
    rm.addResource(dataPointResource);
    return rm;
  }

  @Bean(name = "ibrokerId")
  public String defaultResponseDestination() {
    return "ibroker_" + UUID.randomUUID().toString();
  }

  @Bean(name = "messageQueue")
  public MessageQueue<IRequest> createRequestBuffer() {
    MessageQueue<IRequest> buffer =
        new MessageQueue<>(MESSAGE_QUEUE_CAPACITY, Executors.newSingleThreadExecutor());
    return buffer;
  }

  public static void main(String[] args) {
    SpringApplication.run(IBrokerMain.class, args);
  }
}
