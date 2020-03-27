/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.mongo.model.help.UserRole;
import com.openiot.cloud.projectcenter.Application;
import com.openiot.cloud.projectcenter.repository.ProjectRepository;
import com.openiot.cloud.projectcenter.repository.UserRepository;
import com.openiot.cloud.projectcenter.repository.document.Project;
import com.openiot.cloud.projectcenter.repository.document.User;
import com.openiot.cloud.projectcenter.service.UserService;
import com.openiot.cloud.base.service.model.UserAndRole;
import lombok.extern.slf4j.Slf4j;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"mongo.db = test_openiot"})
public class ApiUserHttpHandlerTest {
  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private UserRepository repositoryUsers;
  @Autowired
  private ProjectRepository repositoryProject;
  @LocalServerPort
  private int localServerPort;
  @Autowired
  private UserService userService;

  // in the project honeydew
  // in the project boysenberry
  private User adminCherry;
  private User userGrape;

  private User sysAdmin;
  private Project projectHoneydew;
  private Project projectBoysenberry;

  private UriComponentsBuilder builder;

  @Before
  public void setup() throws Exception {
    repositoryUsers.deleteAll();
    repositoryProject.deleteAll();

    adminCherry = new User();
    adminCherry.setName("cherry");
    adminCherry.setPassword(userService.encryptPassword("cherry"));

    userGrape = new User();
    userGrape.setName("grape");
    userGrape.setPassword(userService.encryptPassword("grape"));

    sysAdmin = new User();
    sysAdmin.setName("sys");
    sysAdmin.setPassword(userService.encryptPassword("sys"));
    sysAdmin.setRole(UserRole.SYS_ADMIN);

    repositoryUsers.saveAll(Arrays.asList(adminCherry, userGrape, sysAdmin));

    projectHoneydew = new Project();
    projectHoneydew.setId("honeydew");
    projectHoneydew.setName("honeydew");
    projectHoneydew.setUser(Stream.of(new UserAndRole(adminCherry.getName(), UserRole.ADMIN),
                                      new UserAndRole(userGrape.getName(), UserRole.USER))
                                  .collect(Collectors.toList()));

    projectBoysenberry = new Project();
    projectBoysenberry.setId("boysenberry");
    projectBoysenberry.setName("boysenberry");
    projectBoysenberry.setUser(Stream.of(new UserAndRole(adminCherry.getName(), UserRole.ADMIN),
                                         new UserAndRole(userGrape.getName(), UserRole.USER))
                                     .collect(Collectors.toList()));

    repositoryProject.saveAll(Arrays.asList(projectHoneydew, projectBoysenberry));

    builder =
        UriComponentsBuilder.newInstance().scheme("http").host("localhost").port(localServerPort);
  }

  @Test
  public void testUserApiBasic() throws Exception {}

  @Test
  public void testEnum() throws Exception {
    UserRole sysAdmin = UserRole.valueOf("SYS_ADMIN");
    System.out.println("1 sysAdmin " + sysAdmin);

    sysAdmin = Enum.valueOf(UserRole.class, "SYS_ADMIN");
    System.out.println("2 sysAdmin " + sysAdmin);

    User user = new User();
    user.setName("name");
    user.setRole(UserRole.SYS_ADMIN);

    assertThat(new ObjectMapper().readValue(new ObjectMapper().writeValueAsString(user),
                                            User.class)).isEqualTo(user);
  }

  @Test
  public void testPasswordEncoder() throws Exception {
    log.info("intel@123 -> {}", userService.encryptPassword("intel@123"));
    log.info("intel@123 -> {}", userService.encryptPassword("intel@123"));
    log.info("intel@123 -> {}", userService.encryptPassword("intel@123"));
    assertThat(userService.encryptPassword("watermelon")).isEqualTo(userService.encryptPassword("watermelon"));

  }
}
