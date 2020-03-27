/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.cfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.dao.ConfigRepository;
import com.openiot.cloud.base.mongo.model.Config;
import com.openiot.cloud.base.service.model.ProjectDTO;
import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import com.openiot.cloud.cfg.model.ProjectCfg;
import com.openiot.cloud.sdk.service.IConnectRequest;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ProjectConfigTest {
  @Autowired
  private ConfigRepository cfgRepo;
  @Autowired
  private ConfigTaskHandler handler;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private ConfigUploader configUploader;

  @Before
  public void setup() throws Exception {
    cfgRepo.deleteAll();

  }

  @Test
  public void loadContext() throws Exception {}

  // TBD: need a new case which doesn't depend on other module
  // @Test
  public synchronized void testCheckAndBuild() throws Exception {
    configUploader.setStop(true);

    // 1. prepare
    ProjectDTO projectDTO = new ProjectDTO();
    projectDTO.setId("test_project_id");
    projectDTO.setName("test_name");
    projectDTO.setGroup_title("test_title");

    CompletableFuture<Boolean> result0 = new CompletableFuture<>();
    IConnectRequest.create(HttpMethod.DELETE,
                           "/api/project?id=" + projectDTO.getId(),
                           MediaType.APPLICATION_JSON,
                           null)
                   .send(response -> {
                     // assertThat(response.getStatus().is2xxSuccessful()).isTrue();
                     result0.complete(true);
                   }, 5, TimeUnit.SECONDS);
    assertThat(result0.get(5, TimeUnit.SECONDS)).isTrue();

    List<ConfigurationEntity> cs = new ArrayList<ConfigurationEntity>();
    cs.add(new ConfigurationEntity("cn_1", "cv_1"));
    cs.add(new ConfigurationEntity("cn_2", "cv_2"));
    projectDTO.setCs(cs);

    List<AttributeEntity> as = new ArrayList<AttributeEntity>();
    as.add(new AttributeEntity("an_1", "av_1"));
    as.add(new AttributeEntity("an_2", "av_2"));
    projectDTO.setAs(as);

    CompletableFuture<Boolean> result1 = new CompletableFuture<>();
    IConnectRequest.create(HttpMethod.POST,
                           ConstDef.U_PROJECT,
                           MediaType.APPLICATION_JSON,
                           objectMapper.writeValueAsBytes(projectDTO))
                   .send(response -> {
                     result1.complete(response.getStatus().is2xxSuccessful());
                   }, 5, TimeUnit.SECONDS);
    assertThat(result1.get(5, TimeUnit.SECONDS)).isTrue();


    // 2. confirm full cfg
    handler.generateProjectConfiguration("test_project_id");
    Thread.sleep(3000);
    List<Config> config = cfgRepo.findAll();
    assertThat(config).isNotNull().isNotEmpty().hasSize(1);
    assertThat(config.get(0)).isNotNull()
                             .hasFieldOrPropertyWithValue("targetType", ConstDef.CFG_TT_PRJ)
                             .hasFieldOrPropertyWithValue("targetId", "test_project_id");

    String configJson = config.get(0).getConfig();
    assertThat(configJson).isNotNull()
                          .contains("id")
                          .contains("name")
                          .contains("group_title")
                          .contains("cfg")
                          .contains("attr")
                          .doesNotContain("location")
                          .doesNotContain("null")
                          .doesNotContain("description");

    ProjectCfg prjCfg = new ObjectMapper().readValue(configJson, ProjectCfg.class);
    assertThat(prjCfg).isNotNull()
                      .hasFieldOrPropertyWithValue("id", "test_project_id")
                      .hasFieldOrPropertyWithValue("name", "test_name")
                      .hasFieldOrPropertyWithValue("group_title", "test_title");

    // assertThat(prjCfg.getAttr()).isNull();

    assertThat(prjCfg.getCfg()).isNotNull()
                               .hasSize(2)
                               .containsEntry("cn_1", "cv_1")
                               .containsEntry("cn_2", "cv_2");
    cfgRepo.deleteAll();

    // 3. confirm partial cfg
    projectDTO.setCs(Collections.emptyList());
    projectDTO.setGroup_title("");

    CompletableFuture<Boolean> result2 = new CompletableFuture<>();
    IConnectRequest.create(HttpMethod.PUT,
                           "/api/project?id=" + projectDTO.getId(),
                           MediaType.APPLICATION_JSON,
                           objectMapper.writeValueAsBytes(projectDTO))
                   .send(response -> {
                     assertThat(response.getStatus().is2xxSuccessful()).isTrue();
                     result2.complete(true);
                   }, 5, TimeUnit.SECONDS);
    assertThat(result2.get(5, TimeUnit.SECONDS)).isTrue();

    Thread.sleep(5000);
    handler.generateProjectConfiguration("test_project_id");
    config = cfgRepo.findAll();
    assertThat(config).isNotNull().isNotEmpty().hasSize(1);
    assertThat(config.get(0)).isNotNull()
                             .hasFieldOrPropertyWithValue("targetType", ConstDef.CFG_TT_PRJ)
                             .hasFieldOrPropertyWithValue("targetId", "test_project_id");
    configJson = config.get(0).getConfig();
    assertThat(configJson).isNotNull()
                          .contains("id")
                          .contains("name")
                          .contains("attr")
                          .doesNotContain("cfg")
                          .doesNotContain("group_title")
                          .doesNotContain("location")
                          .doesNotContain("null")
                          .doesNotContain("description");

    cfgRepo.deleteAll();

    CompletableFuture<Boolean> result3 = new CompletableFuture<>();
    IConnectRequest.create(HttpMethod.DELETE,
                           "/api/project?id=" + projectDTO.getId(),
                           MediaType.APPLICATION_JSON,
                           null)
                   .send(response -> {
                     result3.complete(response.getStatus().is2xxSuccessful());
                   }, 5, TimeUnit.SECONDS);
    assertThat(result3.get(5, TimeUnit.SECONDS)).isTrue();

    configUploader.setStop(false);
  }
}
