/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.dao.TaskNewRepository;
import com.openiot.cloud.base.mongo.model.TaskNew;
import com.openiot.cloud.base.mongo.model.help.UserRole;
import com.openiot.cloud.projectcenter.Application;
import com.openiot.cloud.projectcenter.repository.ProjectRepository;
import com.openiot.cloud.projectcenter.repository.document.Project;
import com.openiot.cloud.sdk.service.IConnect;
import com.openiot.cloud.sdk.service.IConnectRequest;
import com.openiot.cloud.sdk.service.IConnectService;
import lombok.extern.slf4j.Slf4j;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"mongo.db = test_openiot"})
public class ApiProjectAmqpHandlerTest {
  @Autowired
  private ProjectRepository projectRepository;
  @Autowired
  private TaskNewRepository taskRepo;
  @Autowired
  private ApiProjectAmqpHandler handler;
  @Autowired
  private IConnect conn;
  @Autowired
  private IConnectService service;

  @Before
  public void setup() throws Exception {
    projectRepository.deleteAll();
  }

  @Test
  public void testProjectCfg() throws Exception {
    Project project = new Project();
    project.setId("test_id");
    project.setName("test_name");
    project.setGroup_title("test_title");
    projectRepository.save(project);

    // 1. update by adding new
    CompletableFuture<HttpStatus> future = new CompletableFuture();
    IConnectRequest request =
        IConnectRequest.create(HttpMethod.POST,
                               ConstDef.U_PROJECT_CFG
                                   + String.format("?%s=%s", ConstDef.Q_PROJECT, project.getId()),
                               MediaType.APPLICATION_JSON,
                               "[{\"cn\":\"cn_1\",\"cv\":\"cv_1\"},{\"cn\":\"cn_2\",\"cv\":\"cv_2\"}]".getBytes());
    request.setTokenInfo(ConstDef.MSG_KEY_USR, "admin");
    request.setTokenInfo(ConstDef.MSG_KEY_PRJ, project.getId());
    request.setTokenInfo(ConstDef.MSG_KEY_ROLE, UserRole.ADMIN.getValue());
    request.send(response -> {
      future.complete(response.getStatus());
    }, 6, TimeUnit.SECONDS);
    assertThat(future.get()).isEqualTo(HttpStatus.OK);

    project = projectRepository.findById(project.getId()).get();
    assertThat(project).isNotNull();
    assertThat(project.getCs()).isNotEmpty().hasSize(2);

    // 2. update by add new and modify existing
    CompletableFuture<HttpStatus> future2 = new CompletableFuture();
    request =
        IConnectRequest.create(HttpMethod.POST,
                               ConstDef.U_PROJECT_CFG
                                   + String.format("?%s=%s", ConstDef.Q_PROJECT, project.getId()),
                               MediaType.APPLICATION_JSON,
                               "[{\"cn\":\"cn_1\",\"cv\":\"cv_1_new\"},{\"cn\":\"cn_3\",\"cv\":\"cv_3\"}]".getBytes());
    request.setTokenInfo(ConstDef.MSG_KEY_USR, "admin");
    request.setTokenInfo(ConstDef.MSG_KEY_PRJ, project.getId());
    request.setTokenInfo(ConstDef.MSG_KEY_ROLE, UserRole.ADMIN.getValue());
    request.send(response -> {
      future2.complete(response.getStatus());
    }, 5, TimeUnit.SECONDS);
    assertThat(future2.get()).isEqualTo(HttpStatus.OK);

    project = projectRepository.findById(project.getId()).get();

    assertThat(project).isNotNull();
    assertThat(project.getCs()).isNotEmpty().hasSize(3);
    assertThat(project.getCs().get(0)).isNotNull().hasFieldOrPropertyWithValue("cv", "cv_1_new");
    assertThat(project.getCs().get(2)).isNotNull().hasFieldOrPropertyWithValue("cv", "cv_3");

    // 3. delete by one
    CompletableFuture<HttpStatus> future3 = new CompletableFuture();
    request = IConnectRequest.create(HttpMethod.DELETE,
                                     ConstDef.U_PROJECT_CFG + String.format("?%s=%s&%s=%s",
                                                                            ConstDef.Q_PROJECT,
                                                                            project.getId(),
                                                                            ConstDef.Q_CFGS,
                                                                            "[cn_1]"),
                                     MediaType.APPLICATION_JSON,
                                     "".getBytes());
    request.setTokenInfo(ConstDef.MSG_KEY_USR, "admin");
    request.setTokenInfo(ConstDef.MSG_KEY_PRJ, project.getId());
    request.setTokenInfo(ConstDef.MSG_KEY_ROLE, UserRole.ADMIN.getValue());
    request.send(response -> {
      future3.complete(response.getStatus());
    }, 5, TimeUnit.SECONDS);
    assertThat(future3.get()).isEqualTo(HttpStatus.OK);

    project = projectRepository.findById(project.getId()).get();

    assertThat(project).isNotNull();
    assertThat(project.getCs()).isNotEmpty().hasSize(2);
    assertThat(project.getCs().get(0)).isNotNull().hasFieldOrPropertyWithValue("cv", "cv_2");

    // 4. delete by multiple
    CompletableFuture<HttpStatus> future4 = new CompletableFuture();
    request = IConnectRequest.create(HttpMethod.DELETE,
                                     ConstDef.U_PROJECT_CFG + String.format("?%s=%s&%s=%s",
                                                                            ConstDef.Q_PROJECT,
                                                                            project.getId(),
                                                                            ConstDef.Q_CFGS,
                                                                            "[    cn_2 , cn_3 ]"),
                                     MediaType.APPLICATION_JSON,
                                     "".getBytes());
    request.setTokenInfo(ConstDef.MSG_KEY_USR, "admin");
    request.setTokenInfo(ConstDef.MSG_KEY_PRJ, project.getId());
    request.setTokenInfo(ConstDef.MSG_KEY_ROLE, UserRole.ADMIN.getValue());
    request.send(response -> {
      future4.complete(response.getStatus());
    }, 5, TimeUnit.SECONDS);
    assertThat(future4.get()).isEqualTo(HttpStatus.OK);

    project = projectRepository.findById(project.getId()).get();

    assertThat(project).isNotNull();
    assertThat(project.getCs()).isEmpty();

    projectRepository.deleteById(project.getId());
  }

  @Test
  public void testProjectAttr() throws Exception {
    Project project = new Project();
    project.setId("test_id");
    project.setName("test_name");
    project.setGroup_title("test_title");
    projectRepository.save(project);

    // 1. update by adding new
    CompletableFuture<HttpStatus> future = new CompletableFuture();
    IConnectRequest request =
        IConnectRequest.create(HttpMethod.POST,
                               ConstDef.U_PROJECT_ATTR
                                   + String.format("?%s=%s", ConstDef.Q_PROJECT, project.getId()),
                               MediaType.APPLICATION_JSON,
                               "[{\"an\":\"an_1\",\"av\":\"av_1\"},{\"an\":\"an_2\",\"av\":\"av_2\"}]".getBytes());
    request.setTokenInfo(ConstDef.MSG_KEY_USR, "admin");
    request.setTokenInfo(ConstDef.MSG_KEY_PRJ, project.getId());
    request.setTokenInfo(ConstDef.MSG_KEY_ROLE, UserRole.ADMIN.getValue());
    request.send(response -> {
      future.complete(response.getStatus());
    }, 6, TimeUnit.SECONDS);
    assertThat(future.get()).isEqualTo(HttpStatus.OK);

    project = projectRepository.findById(project.getId()).get();

    assertThat(project).isNotNull();
    assertThat(project.getAs()).isNotEmpty().hasSize(2);
    assertThat(project.getAs().get(1)).isNotNull().hasFieldOrPropertyWithValue("av", "av_2");

    // 2. update by add new and modify existing
    CompletableFuture<HttpStatus> future2 = new CompletableFuture();
    request =
        IConnectRequest.create(HttpMethod.POST,
                               ConstDef.U_PROJECT_ATTR
                                   + String.format("?%s=%s", ConstDef.Q_PROJECT, project.getId()),
                               MediaType.APPLICATION_JSON,
                               "[{\"an\":\"an_1\",\"av\":\"av_1_new\"},{\"an\":\"an_3\",\"av\":\"av_3\"}]".getBytes());
    request.setTokenInfo(ConstDef.MSG_KEY_USR, "admin");
    request.setTokenInfo(ConstDef.MSG_KEY_PRJ, project.getId());
    request.setTokenInfo(ConstDef.MSG_KEY_ROLE, UserRole.ADMIN.getValue());
    request.send(response -> {
      future2.complete(response.getStatus());
    }, 6, TimeUnit.SECONDS);
    assertThat(future2.get()).isEqualTo(HttpStatus.OK);

    project = projectRepository.findById(project.getId()).get();

    assertThat(project).isNotNull();
    assertThat(project.getAs()).isNotEmpty().hasSize(3);
    assertThat(project.getAs().get(0)).isNotNull().hasFieldOrPropertyWithValue("av", "av_1_new");
    assertThat(project.getAs().get(2)).isNotNull().hasFieldOrPropertyWithValue("av", "av_3");

    // 3. delete by one
    CompletableFuture<HttpStatus> future3 = new CompletableFuture();
    request = IConnectRequest.create(HttpMethod.DELETE,
                                     ConstDef.U_PROJECT_ATTR + String.format("?%s=%s&%s=%s",
                                                                             ConstDef.Q_PROJECT,
                                                                             project.getId(),
                                                                             ConstDef.Q_ATTRS,
                                                                             "[an_1]"),
                                     MediaType.APPLICATION_JSON,
                                     "".getBytes());
    request.setTokenInfo(ConstDef.MSG_KEY_USR, "admin");
    request.setTokenInfo(ConstDef.MSG_KEY_PRJ, project.getId());
    request.setTokenInfo(ConstDef.MSG_KEY_ROLE, UserRole.ADMIN.getValue());
    request.send(response -> {
      future3.complete(response.getStatus());
    }, 6, TimeUnit.SECONDS);
    assertThat(future3.get()).isEqualTo(HttpStatus.OK);

    project = projectRepository.findById(project.getId()).get();

    assertThat(project).isNotNull();
    assertThat(project.getAs()).isNotEmpty().hasSize(2);
    assertThat(project.getAs().get(0)).isNotNull().hasFieldOrPropertyWithValue("av", "av_2");

    // 4. delete by multiple
    CompletableFuture<HttpStatus> future4 = new CompletableFuture();
    request = IConnectRequest.create(HttpMethod.DELETE,
                                     ConstDef.U_PROJECT_ATTR + String.format("?%s=%s&%s=%s",
                                                                             ConstDef.Q_PROJECT,
                                                                             project.getId(),
                                                                             ConstDef.Q_ATTRS,
                                                                             "[    an_2 , an_3 ]"),
                                     MediaType.APPLICATION_JSON,
                                     "".getBytes());
    request.setTokenInfo(ConstDef.MSG_KEY_USR, "admin");
    request.setTokenInfo(ConstDef.MSG_KEY_PRJ, project.getId());
    request.setTokenInfo(ConstDef.MSG_KEY_ROLE, UserRole.ADMIN.getValue());
    request.send(response -> {
      future4.complete(response.getStatus());
    }, 6, TimeUnit.SECONDS);
    assertThat(future4.get()).isEqualTo(HttpStatus.OK);

    project = projectRepository.findById(project.getId()).get();

    assertThat(project).isNotNull();
    assertThat(project.getAs()).isEmpty();

    projectRepository.deleteById(project.getId());
  }

  @Test
  public void testProjectUpdate() throws Exception {
    Project project = new Project();
    project.setId("test_id");
    project.setName("test_name");
    project.setGroup_title("test_title");
    projectRepository.save(project);

    // 1. initialize basic project
    CompletableFuture<HttpStatus> future = new CompletableFuture();
    IConnectRequest request =
        IConnectRequest.create(HttpMethod.POST,
                               ConstDef.U_PROJECT_CFG
                                   + String.format("?%s=%s", ConstDef.Q_PROJECT, project.getId()),
                               MediaType.APPLICATION_JSON,
                               "[{\"cn\":\"cn_1\",\"cv\":\"cv_1\"},{\"cn\":\"cn_2\",\"cv\":\"cv_2\"}]".getBytes());
    request.setTokenInfo(ConstDef.MSG_KEY_USR, "admin");
    request.setTokenInfo(ConstDef.MSG_KEY_PRJ, project.getId());
    request.setTokenInfo(ConstDef.MSG_KEY_ROLE, UserRole.ADMIN.getValue());
    request.send(response -> {
      future.complete(response.getStatus());
    }, 6, TimeUnit.SECONDS);
    assertThat(future.get()).isEqualTo(HttpStatus.OK);

    CompletableFuture<HttpStatus> future2 = new CompletableFuture();
    request =
        IConnectRequest.create(HttpMethod.POST,
                               ConstDef.U_PROJECT_ATTR
                                   + String.format("?%s=%s", ConstDef.Q_PROJECT, project.getId()),
                               MediaType.APPLICATION_JSON,
                               "[{\"an\":\"an_1\",\"av\":\"av_1\"},{\"an\":\"an_2\",\"av\":\"av_2\"}]".getBytes());
    request.setTokenInfo(ConstDef.MSG_KEY_USR, "admin");
    request.setTokenInfo(ConstDef.MSG_KEY_PRJ, project.getId());
    request.setTokenInfo(ConstDef.MSG_KEY_ROLE, UserRole.ADMIN.getValue());
    request.send(response -> {
      future2.complete(response.getStatus());
    }, 6, TimeUnit.SECONDS);
    assertThat(future2.get()).isEqualTo(HttpStatus.OK);

    project = projectRepository.findById(project.getId()).get();
    assertThat(project).isNotNull();
    assertThat(project.getCs()).isNotEmpty().hasSize(2);
    assertThat(project.getCs().get(1)).isNotNull().hasFieldOrPropertyWithValue("cv", "cv_2");
    assertThat(project.getAs()).isNotEmpty().hasSize(2);
    assertThat(project.getAs().get(1)).isNotNull().hasFieldOrPropertyWithValue("av", "av_2");

    // 2. update by overwriting
    CompletableFuture<HttpStatus> future3 = new CompletableFuture();
    project.getCs().remove(1);
    project.getCs().get(0).setCv("cv_1_new");
    project.getAs().remove(1);
    request = IConnectRequest.create(HttpMethod.PUT,
                                     ConstDef.U_PROJECT
                                         + String.format("?%s=%s", ConstDef.Q_ID, project.getId()),
                                     MediaType.APPLICATION_JSON,
                                     new ObjectMapper().writeValueAsBytes(project));
    request.setTokenInfo(ConstDef.MSG_KEY_USR, "admin");
    request.setTokenInfo(ConstDef.MSG_KEY_PRJ, project.getId());
    request.setTokenInfo(ConstDef.MSG_KEY_ROLE, UserRole.ADMIN.getValue());
    request.send(response -> {
      future3.complete(response.getStatus());
    }, 6, TimeUnit.SECONDS);
    assertThat(future3.get()).isEqualTo(HttpStatus.OK);

    project = projectRepository.findById(project.getId()).get();
    assertThat(project).isNotNull();
    assertThat(project.getCs()).isNotEmpty().hasSize(1);
    assertThat(project.getCs().get(0)).isNotNull().hasFieldOrPropertyWithValue("cv", "cv_1_new");
    assertThat(project.getAs()).isNotEmpty().hasSize(1);
    assertThat(project.getAs().get(0)).isNotNull().hasFieldOrPropertyWithValue("av", "av_1");

    // 3. delete by oeverwriting
    CompletableFuture<HttpStatus> future4 = new CompletableFuture();
    project.getCs().remove(0);
    project.getAs().remove(0);
    project.setGroup_title("test_title_new");;
    request = IConnectRequest.create(HttpMethod.PUT,
                                     ConstDef.U_PROJECT
                                         + String.format("?%s=%s", ConstDef.Q_ID, project.getId()),
                                     MediaType.APPLICATION_JSON,
                                     new ObjectMapper().writeValueAsBytes(project));
    request.setTokenInfo(ConstDef.MSG_KEY_USR, "admin");
    request.setTokenInfo(ConstDef.MSG_KEY_PRJ, project.getId());
    request.setTokenInfo(ConstDef.MSG_KEY_ROLE, UserRole.ADMIN.getValue());
    request.send(response -> {
      future4.complete(response.getStatus());
    }, 6, TimeUnit.SECONDS);
    assertThat(future4.get()).isEqualTo(HttpStatus.OK);

    project = projectRepository.findById(project.getId()).get();
    assertThat(project).isNotNull();
    assertThat(project.getCs()).isNull();;
    assertThat(project.getAs()).isNull();;
    assertThat(project.getGroup_title()).isNotNull().isEqualTo("test_title_new");

    List<TaskNew> tasks =
        taskRepo.findByMonitorNameAndEventTypeAndTargetTypeAndTargetId("CFG_MONITOR",
                                                                       ConstDef.EVENT_TYPE_CFG_SYNC,
                                                                       ConstDef.EVENT_TARGET_TYPE_PROJECT,
                                                                       "test_id");
    assertThat(tasks).isNotNull().hasSize(1);

    projectRepository.deleteById(project.getId());
    taskRepo.removeByMonitorNameAndEventTypeAndTargetTypeAndTargetId("CFG_MONITOR",
                                                                     ConstDef.EVENT_TYPE_CFG_SYNC,
                                                                     ConstDef.EVENT_TARGET_TYPE_PROJECT,
                                                                     "test_id");
  }
}
