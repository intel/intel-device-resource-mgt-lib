/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.help;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ConfigurableRoutingTableTest {

  @Test
  public void testCase1() {
    Resource resource = new ClassPathResource("ConfigrableRoutingTableTest1.json");
    assertThat(resource.exists()).isTrue();

    Map<String, InetSocketAddress> routingTable = null;
    try {
      routingTable = ConfigurableRoutingTable.readRoutingTable(resource.getInputStream());
    } catch (IOException e) {
      assertThat("should not reach").isNull();
    }

    assertThat(routingTable).isNotNull();
    assertThat(routingTable.size()).isEqualTo(2);
    assertThat(routingTable.containsKey("/brewed")).isTrue();
    assertThat(routingTable.containsKey("/frenchpress")).isTrue();
    assertThat(routingTable.get("/brewed").getHostString()).isEqualTo("10.0.0.1");
    assertThat(routingTable.get("/brewed").getPort()).isEqualTo(12);
    assertThat(routingTable.get("/frenchpress").getHostString()).isEqualTo("10.0.0.1");
    assertThat(routingTable.get("/frenchpress").getPort()).isEqualTo(13);
  }

  @Test
  public void testCase2() {
    Resource resource = new ClassPathResource("ConfigrableRoutingTableTest2.json");
    Map<String, InetSocketAddress> routingTable = null;
    try {
      routingTable = ConfigurableRoutingTable.readRoutingTable(resource.getInputStream());
    } catch (IOException e) {
      assertThat("should not reach").isNull();
    }

    assertThat(routingTable).isNull();
  }

  @Test
  public void testCase3() {
    Resource resource = new ClassPathResource("ConfigrableRoutingTableTest3.json");
    Map<String, InetSocketAddress> routingTable = null;
    try {
      routingTable = ConfigurableRoutingTable.readRoutingTable(resource.getInputStream());
    } catch (IOException e) {
      assertThat("should not reach").isNull();
    }

    assertThat(routingTable).isNull();
  }

  @Test
  public void testCase4() {
    Resource resource = new ClassPathResource("ConfigrableRoutingTableTest4.json");
    Map<String, InetSocketAddress> routingTable = null;
    try {
      routingTable = ConfigurableRoutingTable.readRoutingTable(resource.getInputStream());
    } catch (IOException e) {
      assertThat("should not reach").isNull();
    }

    assertThat(routingTable).isNotNull();
    assertThat(routingTable.size()).isEqualTo(1);
    assertThat(routingTable.containsKey("/turkish/coffee")).isFalse();
    assertThat(routingTable.containsKey("/moka")).isTrue();
    assertThat(routingTable.get("/moka").getHostString()).isEqualTo("10.0.0.2");
    assertThat(routingTable.get("/moka").getPort()).isEqualTo(14);
  }

  @Test
  public void testRoutingTable() throws Exception {
    Resource resource = new ClassPathResource("test_routing_table.json");
    assertThat(resource.exists()).isTrue();

    Map<String, InetSocketAddress> routingTable = null;
    routingTable = ConfigurableRoutingTable.readRoutingTable(resource.getInputStream());

    System.out.println("rourtingTable " + routingTable);

    assertThat(routingTable).isNotNull().isNotEmpty();
    assertThat(routingTable.size()).isEqualTo(3);
    assertThat(routingTable.get("/dp").getHostString()).isEqualTo("127.0.0.1");
    assertThat(routingTable.get("/dp").getPort()).isEqualTo(5685);

    assertThat(routingTable.get("/rd")
                           .getHostString()).isEqualTo("openiot-server-service-1.bj.intel.com");
    assertThat(routingTable.get("/rd").getPort()).isEqualTo(5684);

    assertThat(routingTable.get("/meta").getHostString()).isEqualTo("localhost");
    assertThat(routingTable.get("/meta").getPort()).isEqualTo(5684);

    assertThat(routingTable.containsKey("/unreachable")).isFalse();
  }
}
