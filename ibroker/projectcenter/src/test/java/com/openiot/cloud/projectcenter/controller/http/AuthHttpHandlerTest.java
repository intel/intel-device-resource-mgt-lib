/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.common.model.TokenContent;
import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.mongo.model.help.UserRole;
import com.openiot.cloud.projectcenter.Application;
import com.openiot.cloud.projectcenter.controller.ao.AuthenticationAO;
import com.openiot.cloud.projectcenter.controller.ao.AuthorizationAO;
import com.openiot.cloud.projectcenter.repository.ProjectRepository;
import com.openiot.cloud.projectcenter.repository.UserRepository;
import com.openiot.cloud.projectcenter.repository.document.Project;
import com.openiot.cloud.projectcenter.repository.document.User;
import com.openiot.cloud.projectcenter.service.UserService;
import com.openiot.cloud.projectcenter.utils.ApiJwtTokenUtil;
import com.openiot.cloud.base.service.model.UserAndRole;
import lombok.extern.slf4j.Slf4j;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthHttpHandlerTest {
  @Autowired
  private ProjectRepository projectRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private ApiJwtTokenUtil jwtTokenUtil;
  @Autowired
  private TestRestTemplate restTemplate;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private UserService userService;

  private Project project1 = null;
  private User admin1 = null;
  private User sysAdmin = null;
  private String tokenAdmin = null;
  private String tokenSysAdmin = null;
  private String baseUrl = null;
  private UriComponentsBuilder builder = null;

  @Value("${jwt.header}")
  private String tokenHeader;
  @Value("${jwt.tokenHead}")
  private String tokenHead;
  @LocalServerPort
  private int port;

  @Before
  public void init() throws Exception {
    userRepository.deleteAll();
    projectRepository.deleteAll();

    log.info("============AuthHttpTemplateApiTest INIT START===============");

    // 1. Create a user
    admin1 = new User();
    admin1.setName("admin1");
    admin1.setPassword("intel@123");
    admin1.setNickname("test_nickname");
    admin1.setEmail("test@intel.com");
    admin1.setPassword(userService.encryptPassword(admin1.getPassword()));
    // user1.setTime_reg(BaseUtil.getNowAsEpochMillis());
    admin1.setTime_reg(BaseUtil.getNow().getTime());
    userRepository.save(admin1);

    // 2. Create a project
    project1 = new Project();
    project1.setName("test_project");
    UserAndRole role = new UserAndRole();
    role.setName(admin1.getName());
    role.setRole(UserRole.ADMIN);
    List<UserAndRole> userList = new LinkedList<>();
    userList.add(role);
    project1.setUser(userList);
    project1 = projectRepository.save(project1);

    sysAdmin = new User();
    sysAdmin.setName("strawberry");
    sysAdmin.setRole(UserRole.SYS_ADMIN);
    userRepository.save(sysAdmin);

    Thread.sleep(1000);

    // 3. Create a token
    tokenAdmin = jwtTokenUtil.generateToken(admin1.getName(), project1.getId());
    tokenSysAdmin = jwtTokenUtil.generateToken(sysAdmin.getName(), null);

    // 4. Create Base URL
    baseUrl = String.format("http://localhost:%d/", port);

    builder = UriComponentsBuilder.newInstance().scheme("http").host("localhost").port(port);
  }

  @Test
  public void testLogin1() throws Exception {
    builder = builder.path("api/user/login");

    AuthenticationAO request = new AuthenticationAO();
    request.setUsername(admin1.getName());
    request.setPassword("intel@123");

    ResponseEntity<byte[]> response =
        restTemplate.postForEntity(builder.build().toString(), request, byte[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    String token = objectMapper.readValue(response.getBody(), AuthorizationAO.class).getToken();
    assertThat(jwtTokenUtil.validateToken(token)).isTrue();
    assertThat(jwtTokenUtil.getPidFromToken(token)).isEqualTo(project1.getId());


    request.setUsername(admin1.getName());
    request.setPassword("not_a_right_pasword");
    response = restTemplate.postForEntity(builder.build().toString(), request, byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  public void testRefreshToken1() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(tokenAdmin);

    HttpEntity<String> entity = new HttpEntity<>(tokenAdmin, headers);

    ResponseEntity<AuthorizationAO> response = null;
    response = restTemplate.exchange(baseUrl + "api/user/refresh",
                                     HttpMethod.POST,
                                     entity,
                                     AuthorizationAO.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    tokenAdmin = response.getBody().getToken();
    assertThat(jwtTokenUtil.validateToken(tokenAdmin)).isTrue();
  }

  @Test
  public void testSelectProject1() throws Exception {
    builder = builder.path("api/user/selectproject");

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(tokenAdmin);
    headers.setContentType(MediaType.APPLICATION_JSON);

    // select a valid project
    AuthorizationAO request = new AuthorizationAO();
    request.setToken(tokenAdmin);
    request.setPrj(project1.getId());
    HttpEntity<AuthorizationAO> entity = new HttpEntity<>(request, headers);
    ResponseEntity<AuthorizationAO> response = restTemplate.exchange(builder.build().toString(),
                                                                     HttpMethod.POST,
                                                                     entity,
                                                                     AuthorizationAO.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    tokenAdmin = response.getBody().getToken();
    assertThat(jwtTokenUtil.validateToken(tokenAdmin)).isTrue();
    assertThat(response.getBody().getPrj()).isEqualTo(project1.getId());


    // select a null project
    request = new AuthorizationAO();
    request.setToken(tokenAdmin);
    entity = new HttpEntity<>(request, headers);
    response = restTemplate.exchange(builder.build().toString(),
                                     HttpMethod.POST,
                                     entity,
                                     AuthorizationAO.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    tokenAdmin = response.getBody().getToken();
    assertThat(jwtTokenUtil.validateToken(tokenAdmin)).isTrue();
    assertThat(response.getBody().getPrj()).isNull();


    // select an invalid project
    request.setPrj("not_existed_project");
    entity = new HttpEntity<>(request, headers);
    ResponseEntity<String> response1 =
        restTemplate.exchange(builder.build().toString(), HttpMethod.POST, entity, String.class);
    assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  public void testTokenValidation() throws Exception {
    // get token by login
    AuthenticationAO request = new AuthenticationAO();
    request.setUsername(admin1.getName());
    request.setPassword("intel@123");
    ResponseEntity<AuthorizationAO> response =
        restTemplate.postForEntity(baseUrl + "api/user/login", request, AuthorizationAO.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    String token = response.getBody().getToken();
    assertThat(jwtTokenUtil.validateToken(token)).isTrue();

    // validate token by validation
    AuthorizationAO payload = new AuthorizationAO();
    payload.setToken(token);
    ResponseEntity<TokenContent> respValidation =
        restTemplate.postForEntity(baseUrl + "api/user/validation", payload, TokenContent.class);
    assertThat(respValidation.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(respValidation.getBody()).isNotNull();
    assertThat(respValidation.getBody().getUser()).isNotNull().isEqualTo(admin1.getName());
    assertThat(respValidation.getBody().getProject()).isNotNull().isEqualTo(project1.getId());
    assertThat(respValidation.getBody().getRole()).isEqualTo(UserRole.ADMIN);
  }
}
