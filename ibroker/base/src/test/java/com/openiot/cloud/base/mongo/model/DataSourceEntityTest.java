/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model;

import com.openiot.cloud.base.Application;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.help.DataSourceEntity;
import com.openiot.cloud.base.mongo.model.help.ResAndResProID;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class}, properties = {"mongo.db = test_openiot"})
public class DataSourceEntityTest {

  @Test
  public void testGetDsRefByTimeBwtween() throws Exception {
    DataSourceEntity ds =
        new DataSourceEntity("ds_1",
                             ConstDef.F_DATASOURCEREF,
                             Stream.of(new DataSourceEntity.Reference("74e182bd2873-m",
                                                                      "/1/0",
                                                                      "0",
                                                                      LocalDateTime.of(2000,
                                                                                       1,
                                                                                       1,
                                                                                       0,
                                                                                       0,
                                                                                       0)
                                                                                   .toInstant(ZoneOffset.UTC)
                                                                                   .toEpochMilli(),
                                                                      LocalDateTime.of(2000,
                                                                                       1,
                                                                                       1,
                                                                                       0,
                                                                                       9,
                                                                                       14)
                                                                                   .toInstant(ZoneOffset.UTC)
                                                                                   .toEpochMilli()),
                                       new DataSourceEntity.Reference("74e182bd2873-m",
                                                                      "/1/0",
                                                                      "2",
                                                                      LocalDateTime.of(2000,
                                                                                       1,
                                                                                       1,
                                                                                       0,
                                                                                       10,
                                                                                       0)
                                                                                   .toInstant(ZoneOffset.UTC)
                                                                                   .toEpochMilli(),
                                                                      LocalDateTime.of(2000,
                                                                                       1,
                                                                                       1,
                                                                                       0,
                                                                                       13,
                                                                                       52)
                                                                                   .toInstant(ZoneOffset.UTC)
                                                                                   .toEpochMilli()),
                                       new DataSourceEntity.Reference("74e182bd2873-m",
                                                                      "/1/0",
                                                                      "4",
                                                                      LocalDateTime.of(2000,
                                                                                       1,
                                                                                       1,
                                                                                       0,
                                                                                       15,
                                                                                       0)
                                                                                   .toInstant(ZoneOffset.UTC)
                                                                                   .toEpochMilli(),
                                                                      0))
                                   .collect(Collectors.toList()));

    List<DataSourceEntity.Reference> referenceList = ds.getReferenceByTimeBetween(0, 0);
    assertThat(referenceList).hasSize(1)
                             .extracting("dsri")
                             .containsOnly(new ResAndResProID("74e182bd2873-m", "/1/0", "4"));

    referenceList = ds.getReferenceByTimeBetween(
                                                 LocalDateTime.of(2000, 1, 1, 0, 0, 0)
                                                              .toInstant(ZoneOffset.UTC)
                                                              .toEpochMilli(),
                                                 LocalDateTime.of(2000, 1, 1, 0, 12, 0)
                                                              .toInstant(ZoneOffset.UTC)
                                                              .toEpochMilli());
    assertThat(referenceList).hasSize(2)
                             .extracting("dsri")
                             .containsOnly(new ResAndResProID("74e182bd2873-m", "/1/0", "0"),
                                           new ResAndResProID("74e182bd2873-m", "/1/0", "2"));

    referenceList = ds.getReferenceByTimeBetween(
                                                 LocalDateTime.of(1999, 12, 31, 23, 00, 0)
                                                              .toInstant(ZoneOffset.UTC)
                                                              .toEpochMilli(),
                                                 LocalDateTime.of(2000, 1, 1, 0, 0, 0)
                                                              .toInstant(ZoneOffset.UTC)
                                                              .toEpochMilli());
    assertThat(referenceList).hasSize(1)
                             .extracting("dsri")
                             .containsOnly(new ResAndResProID("74e182bd2873-m", "/1/0", "0"));

    referenceList = ds.getReferenceByTimeBetween(
                                                 LocalDateTime.of(1989, 1, 1, 0, 0, 0)
                                                              .toInstant(ZoneOffset.UTC)
                                                              .toEpochMilli(),
                                                 LocalDateTime.of(1989, 1, 2, 0, 0, 0)
                                                              .toInstant(ZoneOffset.UTC)
                                                              .toEpochMilli());
    assertThat(referenceList).isEmpty();

    ds = new DataSourceEntity("ds_2", ConstDef.F_DATASOURCETABLE);
    assertThat(ds.getReferenceByTimeBetween(0, 0)).isEmpty();
  }
}
