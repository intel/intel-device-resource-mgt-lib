/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.httpproxy;

import com.openiot.cloud.base.help.ConfigurableRoutingTable;
import com.openiot.cloud.base.profiling.DurationCounter;
import com.openiot.cloud.base.profiling.DurationCounterConfiguration;
import com.openiot.cloud.base.profiling.DurationCounterManage;
import com.openiot.cloud.base.profiling.DurationCounterOfUrlBuilder.CounterOfUrl;
import com.openiot.cloud.base.profiling.SimpleAlarmHandler;
import com.openiot.cloud.httpproxy.security.TokenClient;
import org.iotivity.cloud.base.connector.ConnectorPool;
import org.iotivity.cloud.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jms.annotation.EnableJms;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@EnableJms
@EnableCaching
@SpringBootApplication(exclude = {ErrorMvcAutoConfiguration.class},
    scanBasePackages = {"com.openiot.cloud.sdk.service", "com.openiot.cloud.httpproxy"})
public class Application extends SpringBootServletInitializer {
  private static final Logger logger = LoggerFactory.getLogger(Application.class);

  @Autowired
  private DurationCounterManage counterManage;

  @Autowired
  private List<CounterOfUrl> counterOfUrls;

  @Autowired
  private DurationCounterConfiguration urlConfiguration;

  @Autowired
  private DurationCounterConfiguration methodConfiguration;

  @Autowired
  TokenClient cacheService;

  @EventListener
  public void onApplicationReady(final ApplicationReadyEvent event) {
    coapTcpConnections();
    initCountersForUrls();
    logger.info("HTTPPROXY is online");
  }

  @EventListener
  public void handleContextClose(ContextClosedEvent event) {
    ConnectorPool.disconnectAll();
    ConnectorPool.shutdown();
    logger.info("HTTPPROXY is offline");
  }

  Map<String, InetSocketAddress> coapTcpConnections() {
    Map<String, InetSocketAddress> routingTable = null;
    try {
      Log.Init();

      Resource resource = new ClassPathResource("routing_table.json");
      if (resource.exists()) {
        routingTable = ConfigurableRoutingTable.readRoutingTable(resource.getInputStream());
      } else {
        logger.error("resource/routing_table.json doesn't exist ");
        return null;
      }

      if (routingTable == null || routingTable.isEmpty()) {
        logger.error("routing table is empty or invalid, either way there is no usable routing item");
        return null;
      }

      logger.info("routingTable is {}", routingTable);

      for (Map.Entry<String, InetSocketAddress> item : routingTable.entrySet()) {
        try {
          ConnectorPool.addConnection(item.getKey(), item.getValue(), false);
        } catch (Exception e) {
          logger.error("meet an exception when trying to make COAP connections", e);
        }
      }

      logger.info("set up connections with remote services "
          + ConfigurableRoutingTable.dump(routingTable));
    } catch (Exception e) {
      logger.error("meet an exception when trying to make COAP connections", e);
    }

    return routingTable;
  }

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(Application.class);
  }

  void initCountersForUrls() {
    Map<String, DurationCounterConfiguration> configurationMap =
        Stream.of(urlConfiguration, methodConfiguration)
              .collect(Collectors.toMap(config -> config.getTag(), config -> config));

    counterOfUrls.stream().map(counterOfUrl -> {
      String configurationName = counterOfUrl.getConfigName();
      if (configurationMap.containsKey(configurationName)) {
        DurationCounterConfiguration configuration = configurationMap.get(configurationName);
        return new DurationCounter(counterOfUrl.getUrl(),
                                   configuration,
                                   new SimpleAlarmHandler(configuration.getAlarmOutputPath()));
      } else {
        return null;
      }
    }).filter(counter -> counter != null).forEach(counter -> {
      logger.debug("add " + counter.getName() + " into counter manage");
      counterManage.putCounter(counter.getName(), counter);
    });

    counterManage.initialize();
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
