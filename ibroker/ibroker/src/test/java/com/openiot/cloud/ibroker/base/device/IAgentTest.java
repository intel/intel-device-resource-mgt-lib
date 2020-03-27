/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.ibroker.base.device;

import com.openiot.cloud.base.mongo.model.help.ShortSession;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class IAgentTest {

  @Autowired
  private IAgentCache ac;

  @Test
  public void TestCase1() throws Exception {
    IAgent agent = new IAgent(null);

    ShortSession[] ssList =
        new ShortSession[] {new ShortSession("1234", "1234"), new ShortSession("2345", "2345"),
            new ShortSession("3456", "3456"), new ShortSession("4567", "4567"),};

    agent.cacheConnectedDevice(ssList);

    System.out.println(agent);

    agent.removeConnectedDevice("1234");
    assertThat(agent.containsDi("1234")).isFalse();

    agent.removeConnectedDevice("3456");
    assertThat(agent.containsDi("3456")).isFalse();
  }

  @Test
  public void TestCase2() {
    IAgent a1 = new IAgent(null);
    a1.setAgentId("abf41a4d-a901-4c7d-98e3-20fc95e79056");
    ac.addAgent("abf41a4d-a901-4c7d-98e3-20fc95e79056", a1);

    IAgent a2 = new IAgent(null);
    a2.setAgentId("f66dcfa0-492f-4a23-91cb-d55d193d1729");
    ac.addAgent("f66dcfa0-492f-4a23-91cb-d55d193d1729", a2);
    System.out.println(ac);

    ac.removeAgent("f66dcfa0-492f-4a23-91cb-d55d193d1729", false);
    assertThat(ac.containsKey("f66dcfa0-492f-4a23-91cb-d55d193d1729")).isFalse();
    assertThat(ac.containsKey("abf41a4d-a901-4c7d-98e3-20fc95e79056")).isTrue();
  }
}
