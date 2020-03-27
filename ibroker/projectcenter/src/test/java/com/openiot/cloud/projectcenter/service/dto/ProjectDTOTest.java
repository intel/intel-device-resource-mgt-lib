/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.service.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.mongo.model.help.UserRole;
import com.openiot.cloud.projectcenter.service.ProjectService;
import com.openiot.cloud.base.service.model.UserAndRole;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import static org.assertj.core.api.Assertions.assertThat;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ProjectDTOTest {
  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ProjectService projectService;
  private ProjectDTO target;

  @Before
  public void setup() throws Exception {
    target = new ProjectDTO();
    target.setId("1");
    target.setName("apple");
    target.setTime_created(Long.valueOf(1234l));
    target.setUser(Stream.of(new UserAndRole("mango", UserRole.USER)).collect(Collectors.toList()));
  }

  @Test
  public void testNull() throws Exception {
    // a collection field is null
    JSONObject jsonObject = new JSONObject().put("id", "apple");
    ProjectDTO projectDTO =
        objectMapper.readValue(jsonObject.toString().getBytes(), ProjectDTO.class);
    assertThat(projectDTO).hasFieldOrPropertyWithValue("id", "apple")
                          .hasFieldOrPropertyWithValue("name", null)
                          .hasFieldOrPropertyWithValue("cs", null)
                          .hasFieldOrPropertyWithValue("as", null);

    log.debug("b4 copy properties source={}, target={}", projectDTO, target);
    BaseUtil.copyPropertiesIgnoreCollectionNull(projectDTO, target);
    log.debug("after copy properties target={}", projectDTO, target);

    assertThat(target).hasFieldOrPropertyWithValue("id", "apple")
                      .hasFieldOrPropertyWithValue("name", null)
                      .hasFieldOrPropertyWithValue("time_created", null)
                      .hasFieldOrPropertyWithValue("cs", null)
                      .hasFieldOrPropertyWithValue("as", null);
    assertThat(target.getUser()).extracting("name").containsOnly("mango");
  }

  @Test
  public void testEmpty() throws Exception {
    // a collection field is empty
    JSONObject jsonObject = new JSONObject().put("user", new JSONArray())
                                            .put("as", new JSONArray())
                                            .put("cs", new JSONArray());
    ProjectDTO projectDTO =
        objectMapper.readValue(jsonObject.toString().getBytes(), ProjectDTO.class);
    assertThat(projectDTO).hasFieldOrPropertyWithValue("id", null)
                          .hasFieldOrPropertyWithValue("name", null)
                          .hasFieldOrPropertyWithValue("user", Collections.emptyList())
                          .hasFieldOrPropertyWithValue("cs", Collections.emptyList())
                          .hasFieldOrPropertyWithValue("as", Collections.emptyList());

    BaseUtil.copyPropertiesIgnoreCollectionNull(projectDTO, target);
    assertThat(target).hasFieldOrPropertyWithValue("id", null)
                      .hasFieldOrPropertyWithValue("name", null)
                      .hasFieldOrPropertyWithValue("time_created", null);
    assertThat(target.getUser()).isNull();
    assertThat(target.getAs()).isNull();
    assertThat(target.getCs()).isNull();
  }

  @Test
  public void testNormal() throws Exception {
    // a collection field is not null and not empty
    JSONObject jsonObject =
        new JSONObject().put("id", "1")
                        .put("name", "apple")
                        .put("time_created", 56789)
                        .put("user",
                             new JSONArray().appendElement(new JSONObject().put("name", "kiwi")
                                                                           .put("role", "ADMIN")));
    ProjectDTO projectDTO =
        objectMapper.readValue(jsonObject.toString().getBytes(), ProjectDTO.class);
    assertThat(projectDTO.getUser()).hasSize(1).extracting("name").containsOnly("kiwi");

    BaseUtil.copyPropertiesIgnoreCollectionNull(projectDTO, target);
    assertThat(target).hasFieldOrPropertyWithValue("id", "1")
                      .hasFieldOrPropertyWithValue("name", "apple")
                      .hasFieldOrPropertyWithValue("time_created", 56789L);
    assertThat(target.getUser()).extracting("name").containsOnly("kiwi");
    assertThat(target.getAs()).isNull();
    assertThat(target.getCs()).isNull();
  }
}
