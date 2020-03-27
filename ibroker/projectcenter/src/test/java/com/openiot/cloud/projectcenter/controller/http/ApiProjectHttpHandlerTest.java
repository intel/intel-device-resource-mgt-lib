/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.mongo.model.help.UserRole;
import com.openiot.cloud.projectcenter.Application;
import com.openiot.cloud.projectcenter.controller.ao.AuthenticationAO;
import com.openiot.cloud.projectcenter.controller.ao.AuthorizationAO;
import com.openiot.cloud.projectcenter.repository.ProjectRepository;
import com.openiot.cloud.projectcenter.repository.UserRepository;
import com.openiot.cloud.projectcenter.repository.document.Project;
import com.openiot.cloud.projectcenter.repository.document.User;
import com.openiot.cloud.projectcenter.service.UserService;
import com.openiot.cloud.base.service.model.UserAndRole;
import lombok.extern.slf4j.Slf4j;
import static org.assertj.core.api.Assertions.assertThat;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
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
public class ApiProjectHttpHandlerTest {
  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private ProjectRepository projectRepository;
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
    userRepository.deleteAll();
    projectRepository.deleteAll();

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

    userRepository.saveAll(Arrays.asList(adminCherry, userGrape, sysAdmin));

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

    projectRepository.saveAll(Arrays.asList(projectHoneydew, projectBoysenberry));

    builder =
        UriComponentsBuilder.newInstance().scheme("http").host("localhost").port(localServerPort);
  }

  @Test
  public void testProjectApiBasic() throws Exception {
    // GET a ADMIN token
    AuthenticationAO apiJwtAuthenticationRequest = new AuthenticationAO(sysAdmin.getName(), "sys");
    ResponseEntity<byte[]> response = testRestTemplate.postForEntity(
                                                                     builder.cloneBuilder()
                                                                            .path("api/user/login")
                                                                            .build()
                                                                            .toString(),
                                                                     apiJwtAuthenticationRequest,
                                                                     byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    String token = objectMapper.readValue(response.getBody(), AuthorizationAO.class).getToken();
    assertThat(token).isNotEmpty();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);

    HttpEntity<byte[]> requestEntity = new HttpEntity<>(headers);
    response =
        testRestTemplate.exchange(builder.cloneBuilder().path("api/project").build().toString(),
                                  HttpMethod.GET,
                                  requestEntity,
                                  byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // a admin w a project get the project
    AuthorizationAO apiJwtAuthenticationResponse =
        new AuthorizationAO(token, projectHoneydew.getId());
    requestEntity =
        new HttpEntity<>(objectMapper.writeValueAsBytes(apiJwtAuthenticationResponse), headers);
    response =
        testRestTemplate.exchange(builder.cloneBuilder()
                                         .path("api/user/selectproject")
                                         .build()
                                         .toString(),
                                  HttpMethod.POST,
                                  requestEntity,
                                  byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    token = objectMapper.readValue(response.getBody(), AuthorizationAO.class).getToken();
    assertThat(token).isNotEmpty();


    headers.setBearerAuth(token);
    requestEntity = new HttpEntity<>(headers);
    response =
        testRestTemplate.exchange(builder.cloneBuilder()
                                         .path("api/project")
                                         .queryParam("id", projectBoysenberry.getId())
                                         .build()
                                         .toString(),
                                  HttpMethod.GET,
                                  requestEntity,
                                  byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // POST api/user
    JSONObject jsonObject =
        new JSONObject().put("name", "apricot")
                        .put("location", "street")
                        .put("group_title", "cherry")
                        .put("user",
                             new JSONArray().put(new JSONObject().put("name", "fig").put("role",
                                                                                         "USER")));
    requestEntity = new HttpEntity<>(jsonObject.toString().getBytes(), headers);
    response =
        testRestTemplate.exchange(builder.cloneBuilder().path("api/project").build().toString(),
                                  HttpMethod.POST,
                                  requestEntity,
                                  byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // PUT api/user
    projectBoysenberry.setGroup_title("cat and dog");
    requestEntity = new HttpEntity<>(objectMapper.writeValueAsBytes(userGrape), headers);
    response =
        testRestTemplate.exchange(builder.cloneBuilder()
                                         .path("api/project")
                                         .queryParam("id", projectBoysenberry.getId())
                                         .build()
                                         .toString(),
                                  HttpMethod.PUT,
                                  requestEntity,
                                  byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // DELETE api/user
    requestEntity = new HttpEntity<>(headers);
    response = testRestTemplate.exchange(
                                         builder.cloneBuilder()
                                                .path("api/project")
                                                .queryParam("id", projectBoysenberry.getId())
                                                .build()
                                                .toString(),
                                         HttpMethod.DELETE,
                                         requestEntity,
                                         byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void testProjectApiQueryAsUser() throws Exception {
    // a user w/o any project get nothing since missing the role
    AuthenticationAO apiJwtAuthenticationRequest =
        new AuthenticationAO(userGrape.getName(), "grape");
    ResponseEntity<byte[]> response = testRestTemplate.postForEntity(
                                                                     builder.cloneBuilder()
                                                                            .path("api/user/login")
                                                                            .build()
                                                                            .toString(),
                                                                     apiJwtAuthenticationRequest,
                                                                     byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    String token = objectMapper.readValue(response.getBody(), AuthorizationAO.class).getToken();
    assertThat(token).isNotEmpty();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);

    HttpEntity<byte[]> requestEntity = new HttpEntity<>(headers);
    response =
        testRestTemplate.exchange(builder.cloneBuilder().path("api/project").build().toString(),
                                  HttpMethod.GET,
                                  requestEntity,
                                  byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);


    // a user w a project get the project
    AuthorizationAO apiJwtAuthenticationResponse =
        new AuthorizationAO(token, projectHoneydew.getId());
    requestEntity =
        new HttpEntity<>(objectMapper.writeValueAsBytes(apiJwtAuthenticationResponse), headers);
    response =
        testRestTemplate.exchange(builder.cloneBuilder()
                                         .path("api/user/selectproject")
                                         .build()
                                         .toString(),
                                  HttpMethod.POST,
                                  requestEntity,
                                  byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    token = objectMapper.readValue(response.getBody(), AuthorizationAO.class).getToken();
    assertThat(token).isNotEmpty();

    headers.setBearerAuth(token);
    requestEntity = new HttpEntity<>(headers);
    response =
        testRestTemplate.exchange(builder.cloneBuilder().path("api/project").build().toString(),
                                  HttpMethod.GET,
                                  requestEntity,
                                  byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Project[] projects = objectMapper.readValue(response.getBody(), Project[].class);
    assertThat(projects).isNotEmpty().extracting("name").containsOnly(projectHoneydew.getName(),
                                                                      projectBoysenberry.getName());
  }

  @Test
  public void testProjectApiQueryAsAdmin() throws Exception {
    // a admin w/o any project get nothing since missing the role
    AuthenticationAO apiJwtAuthenticationRequest =
        new AuthenticationAO(adminCherry.getName(), "cherry");
    ResponseEntity<byte[]> response = testRestTemplate.postForEntity(
                                                                     builder.cloneBuilder()
                                                                            .path("api/user/login")
                                                                            .build()
                                                                            .toString(),
                                                                     apiJwtAuthenticationRequest,
                                                                     byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    String token = objectMapper.readValue(response.getBody(), AuthorizationAO.class).getToken();
    assertThat(token).isNotEmpty();

    // every one can get a project list
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);

    HttpEntity<byte[]> requestEntity = new HttpEntity<>(headers);
    response =
        testRestTemplate.exchange(builder.cloneBuilder().path("api/project").build().toString(),
                                  HttpMethod.GET,
                                  requestEntity,
                                  byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // a admin w a project get the project
    AuthorizationAO apiJwtAuthenticationResponse =
        new AuthorizationAO(token, projectHoneydew.getId());
    requestEntity =
        new HttpEntity<>(objectMapper.writeValueAsBytes(apiJwtAuthenticationResponse), headers);
    response =
        testRestTemplate.exchange(builder.cloneBuilder()
                                         .path("api/user/selectproject")
                                         .build()
                                         .toString(),
                                  HttpMethod.POST,
                                  requestEntity,
                                  byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    token = objectMapper.readValue(response.getBody(), AuthorizationAO.class).getToken();
    assertThat(token).isNotEmpty();

    headers.setBearerAuth(token);
    requestEntity = new HttpEntity<>(headers);
    response =
        testRestTemplate.exchange(builder.cloneBuilder().path("api/project").build().toString(),
                                  HttpMethod.GET,
                                  requestEntity,
                                  byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Project[] projects = objectMapper.readValue(response.getBody(), Project[].class);
    assertThat(projects).isNotEmpty().extracting("name").containsOnly(projectHoneydew.getName(),
                                                                      projectBoysenberry.getName());
  }

  @Test
  public void testProjectApiQueryAsSysAdmin() throws Exception {
    // a sys_admin w/o any project get all projects
    AuthenticationAO apiJwtAuthenticationRequest = new AuthenticationAO(sysAdmin.getName(), "sys");
    ResponseEntity<byte[]> response = testRestTemplate.postForEntity(
                                                                     builder.cloneBuilder()
                                                                            .path("api/user/login")
                                                                            .build()
                                                                            .toString(),
                                                                     apiJwtAuthenticationRequest,
                                                                     byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    String token = objectMapper.readValue(response.getBody(), AuthorizationAO.class).getToken();
    assertThat(token).isNotEmpty();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);

    HttpEntity<byte[]> requestEntity = new HttpEntity<>(headers);
    response =
        testRestTemplate.exchange(builder.cloneBuilder().path("api/project").build().toString(),
                                  HttpMethod.GET,
                                  requestEntity,
                                  byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Project[] projects = objectMapper.readValue(response.getBody(), Project[].class);
    assertThat(projects).isNotEmpty().extracting("name").containsOnly(projectHoneydew.getName(),
                                                                      projectBoysenberry.getName());

    // a sys_admin w a project get the project
    AuthorizationAO apiJwtAuthenticationResponse =
        new AuthorizationAO(token, projectHoneydew.getId());
    requestEntity =
        new HttpEntity<>(objectMapper.writeValueAsBytes(apiJwtAuthenticationResponse), headers);
    response =
        testRestTemplate.exchange(builder.cloneBuilder()
                                         .path("api/user/selectproject")
                                         .build()
                                         .toString(),
                                  HttpMethod.POST,
                                  requestEntity,
                                  byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    token = objectMapper.readValue(response.getBody(), AuthorizationAO.class).getToken();
    assertThat(token).isNotEmpty();

    headers.setBearerAuth(token);
    requestEntity = new HttpEntity<>(headers);
    response =
        testRestTemplate.exchange(builder.cloneBuilder().path("api/project").build().toString(),
                                  HttpMethod.GET,
                                  requestEntity,
                                  byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    projects = objectMapper.readValue(response.getBody(), Project[].class);
    assertThat(projects).isNotEmpty().extracting("name").containsOnly(projectHoneydew.getName(),
                                                                      projectBoysenberry.getName());
  }
}
