/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.repository;

import com.openiot.cloud.projectcenter.repository.document.FactoryKey;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(properties = {"spring.data.mongodb.database=test_openiot"})
@RunWith(SpringRunner.class)
public class FactoryKeyRepositoryTest {
  @Autowired
  private FactoryKeyRepository factoryKeyRepository;

  @Before
  public void setup() throws Exception {
    factoryKeyRepository.deleteAll();

    FactoryKey factoryKey = new FactoryKey();
    factoryKeyRepository.save(factoryKey);
  }

  @Test
  public void testBasic() throws Exception {
    assertThat(factoryKeyRepository.count()).isNotNegative();
    assertThat(factoryKeyRepository.findAll().get(0).getId()).isNotEmpty();
  }
}
