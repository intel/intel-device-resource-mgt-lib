/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.help.UserRole;
import com.openiot.cloud.projectcenter.controller.ao.AuthenticationAO;
import com.openiot.cloud.projectcenter.controller.ao.AuthorizationAO;
import com.openiot.cloud.projectcenter.repository.ProjectRepository;
import com.openiot.cloud.projectcenter.repository.UserRepository;
import com.openiot.cloud.projectcenter.repository.document.Project;
import com.openiot.cloud.projectcenter.repository.document.User;
import com.openiot.cloud.projectcenter.service.UserService;
import com.openiot.cloud.projectcenter.utils.ApiJwtTokenUtil;
import com.openiot.cloud.base.service.model.UserAndRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"mongo.db = test_openiot"})
public class ApplicationSecurityConfigTest {
  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private ProjectRepository projectRepository;
  @Autowired
  private ApiJwtTokenUtil jwtTokenUtil;
  @LocalServerPort
  private int localServerPort;
  @Autowired
  private UserService userService;

  // in the project honeydew
  private User adminCherry;
  private User userGrape;

  // in the project boysenberry
  private User adminPear;
  private User userKiwi;

  private User sysAdmin;
  private Project projectHoneydew;
  private Project projectBoysenberry;

  private HttpHeaders defaultHeader = new HttpHeaders();

  @Before
  public void setup() throws Exception {
    userRepository.deleteAll();
    projectRepository.deleteAll();

    adminCherry = new User();
    adminCherry.setName("cherry");
    adminCherry.setPassword(userService.encryptPassword("cherry"));

    adminPear = new User();
    adminPear.setName("pear");
    adminPear.setPassword(userService.encryptPassword("pear"));

    userGrape = new User();
    userGrape.setName("grape");
    userGrape.setPassword(userService.encryptPassword("grape"));

    userKiwi = new User();
    userKiwi.setName("kiwi");
    userKiwi.setPassword(userService.encryptPassword("kiwi"));

    sysAdmin = new User();
    sysAdmin.setName("sys");
    sysAdmin.setPassword(userService.encryptPassword("sys"));
    sysAdmin.setRole(UserRole.SYS_ADMIN);

    userRepository.saveAll(Arrays.asList(adminCherry, adminPear, userGrape, userKiwi, sysAdmin));

    projectHoneydew = new Project();
    projectHoneydew.setName("honeydew");

    UserAndRole uar1 = new UserAndRole();
    uar1.setName(adminCherry.getName());
    uar1.setRole(UserRole.ADMIN);

    UserAndRole uar2 = new UserAndRole();
    uar2.setName(userGrape.getName());
    uar2.setRole(UserRole.USER);

    projectHoneydew.setUser(Stream.of(uar1, uar2).collect(Collectors.toList()));

    projectBoysenberry = new Project();
    projectBoysenberry.setName("boysenberry");

    UserAndRole uar3 = new UserAndRole();
    uar3.setName(adminPear.getName());
    uar3.setRole(UserRole.ADMIN);

    UserAndRole uar4 = new UserAndRole();
    uar4.setName(userKiwi.getName());
    uar4.setRole(UserRole.USER);

    projectBoysenberry.setUser(Stream.of(uar3, uar4).collect(Collectors.toList()));

    projectRepository.saveAll(Arrays.asList(projectHoneydew, projectBoysenberry));

    defaultHeader.setContentType(MediaType.APPLICATION_JSON);
  }

  @Test
  public void contextLoad() throws Exception {}

  @Test
  public void testSecurityBasicWithUser() throws Exception {
    UriComponentsBuilder builder =
        UriComponentsBuilder.newInstance().scheme("http").host("localhost").port(localServerPort);

    AuthenticationAO request = new AuthenticationAO();
    request.setUsername(userGrape.getName());
    request.setPassword("grape");
    ResponseEntity<byte[]> response =
        testRestTemplate.postForEntity(builder.cloneBuilder()
                                              .path("api/user/login")
                                              .build()
                                              .toString(),
                                       request,
                                       byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    String token = objectMapper.readValue(response.getBody(), AuthorizationAO.class).getToken();
    assertThat(token).isNotEmpty();

    // GET api/user
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);

    HttpEntity<byte[]> requestEntity = new HttpEntity<>(headers);
    response = testRestTemplate.exchange(builder.cloneBuilder().path("api/user").build().toString(),
                                         HttpMethod.GET,
                                         requestEntity,
                                         byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // POST api/user
    JSONObject jsonObject = new JSONObject().put("name", "apricot").put("password", "apricot");
    requestEntity = new HttpEntity<>(jsonObject.toString().getBytes(), headers);
    response = testRestTemplate.exchange(builder.cloneBuilder().path("api/user").build().toString(),
                                         HttpMethod.POST,
                                         requestEntity,
                                         byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // PUT api/user
    userGrape.setLocation("street");
    requestEntity = new HttpEntity<>(objectMapper.writeValueAsBytes(userGrape), headers);
    response =
        testRestTemplate.exchange(builder.cloneBuilder()
                                         .path("api/user")
                                         .queryParam("name", userGrape.getName())
                                         .build()
                                         .toString(),
                                  HttpMethod.PUT,
                                  requestEntity,
                                  byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // DELETE api/user
    requestEntity = new HttpEntity<>(headers);
    response = testRestTemplate.exchange(
                                         builder.cloneBuilder()
                                                .path("api/user")
                                                .queryParam("name", userGrape.getName())
                                                .build()
                                                .toString(),
                                         HttpMethod.DELETE,
                                         requestEntity,
                                         byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  public void testSecurityBasicWithAdmin() throws Exception {
    UriComponentsBuilder builder =
        UriComponentsBuilder.newInstance().scheme("http").host("localhost").port(localServerPort);

    AuthenticationAO apiJwtAuthenticationRequest = new AuthenticationAO();
    apiJwtAuthenticationRequest.setUsername(adminPear.getName());
    apiJwtAuthenticationRequest.setPassword("pear");
    HttpEntity<byte[]> requestEntity =
        new HttpEntity<>(objectMapper.writeValueAsBytes(apiJwtAuthenticationRequest),
                         defaultHeader);
    ResponseEntity<byte[]> response =
        testRestTemplate.exchange(builder.cloneBuilder().path("api/user/login").build().toString(),
                                  HttpMethod.POST,
                                  requestEntity,
                                  byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    String token = objectMapper.readValue(response.getBody(), AuthorizationAO.class).getToken();
    assertThat(token).isNotEmpty();

    AuthorizationAO apiJwtAuthenticationResponse = new AuthorizationAO();
    apiJwtAuthenticationResponse.setToken(token);
    apiJwtAuthenticationResponse.setPrj(projectBoysenberry.getId());

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
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

    // GET api/user
    headers.setBearerAuth(token);
    requestEntity = new HttpEntity<>(headers);
    response = testRestTemplate.exchange(builder.cloneBuilder().path("api/user").build().toString(),
                                         HttpMethod.GET,
                                         requestEntity,
                                         byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // POST api/user
    JSONObject jsonObject = new JSONObject().put("name", "apricot").put("password", "apricot");
    requestEntity = new HttpEntity<>(jsonObject.toString().getBytes(), headers);
    response = testRestTemplate.exchange(builder.cloneBuilder().path("api/user").build().toString(),
                                         HttpMethod.POST,
                                         requestEntity,
                                         byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // PUT api/user
    jsonObject = new JSONObject().put("location", "street");
    requestEntity = new HttpEntity<>(jsonObject.toString().getBytes(), headers);
    response =
        testRestTemplate.exchange(builder.cloneBuilder()
                                         .path("api/user")
                                         .queryParam("name", userGrape.getName())
                                         .build()
                                         .toString(),
                                  HttpMethod.PUT,
                                  requestEntity,
                                  byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // DELETE api/user
    requestEntity = new HttpEntity<>(headers);
    response = testRestTemplate.exchange(
                                         builder.cloneBuilder()
                                                .path("api/user")
                                                .queryParam("name", userGrape.getName())
                                                .build()
                                                .toString(),
                                         HttpMethod.DELETE,
                                         requestEntity,
                                         byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  public void testSecurityBasicWithSysAdmin() throws Exception {
    UriComponentsBuilder builder =
        UriComponentsBuilder.newInstance().scheme("http").host("localhost").port(localServerPort);

    AuthenticationAO apiJwtAuthenticationRequest = new AuthenticationAO();
    apiJwtAuthenticationRequest.setUsername(sysAdmin.getName());
    apiJwtAuthenticationRequest.setPassword("sys");
    HttpEntity<byte[]> requestEntity =
        new HttpEntity<>(objectMapper.writeValueAsBytes(apiJwtAuthenticationRequest),
                         defaultHeader);
    ResponseEntity<byte[]> response =
        testRestTemplate.exchange(builder.cloneBuilder().path("api/user/login").build().toString(),
                                  HttpMethod.POST,
                                  requestEntity,
                                  byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    String token = objectMapper.readValue(response.getBody(), AuthorizationAO.class).getToken();
    assertThat(token).isNotEmpty();

    AuthorizationAO apiJwtAuthenticationResponse = new AuthorizationAO();
    apiJwtAuthenticationResponse.setToken(token);
    apiJwtAuthenticationResponse.setPrj(projectBoysenberry.getId());

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);
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

    // GET api/user
    headers.setBearerAuth(token);

    requestEntity = new HttpEntity<>(headers);
    response = testRestTemplate.exchange(builder.cloneBuilder().path("api/user").build().toString(),
                                         HttpMethod.GET,
                                         requestEntity,
                                         byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // POST api/user
    JSONObject jsonObject = new JSONObject().put("name", "apricot").put("password", "apricot");
    requestEntity = new HttpEntity<>(jsonObject.toString().getBytes(), headers);
    response = testRestTemplate.exchange(builder.cloneBuilder().path("api/user").build().toString(),
                                         HttpMethod.POST,
                                         requestEntity,
                                         byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // PUT api/user
    jsonObject = new JSONObject().put("location", "street");
    requestEntity = new HttpEntity<>(jsonObject.toString().getBytes(), headers);
    response =
        testRestTemplate.exchange(builder.cloneBuilder()
                                         .path("api/user")
                                         .queryParam("name", userGrape.getName())
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
                                                .path("api/user")
                                                .queryParam("name", userGrape.getName())
                                                .build()
                                                .toString(),
                                         HttpMethod.DELETE,
                                         requestEntity,
                                         byte[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void testToken() throws Exception {
    User user = new User();
    user.setName("plum");
    user.setPassword("pear");
    user.setEmail("peach@jujube.com");
    user.setRecentProject("apple");
    Instant now = Instant.now(Clock.systemUTC());
    user.setTime_reg(now.toEpochMilli());
    userRepository.save(user);

    assertThat(userRepository.findByName(user.getName())
                             .get(0)).hasFieldOrPropertyWithValue("time_reg", user.getTime_reg());

    String token = jwtTokenUtil.generateToken(user.getName(), null);
    assertThat(token).isNotNull().isNotEmpty();
    assertThat(jwtTokenUtil.getUsernameFromToken(token)).isEqualTo(user.getName());
    assertThat((Boolean) ReflectionTestUtils.invokeMethod(jwtTokenUtil,
                                                          "isTokenExpired",
                                                          token)).isFalse();
    assertThat(jwtTokenUtil.canTokenBeRefreshed(token, null)).isTrue();
    assertThat(jwtTokenUtil.canTokenBeRefreshed(token, Date.from(now))).isTrue();
    assertThat(jwtTokenUtil.canTokenBeRefreshed(token, Date.from(now.minusSeconds(10)))).isTrue();
    assertThat(jwtTokenUtil.canTokenBeRefreshed(token, Date.from(now.plusSeconds(60)))).isFalse();
    assertThat(jwtTokenUtil.validateToken(token)).isTrue();
  }

  @Test
  public void testDateAndInstant() throws Exception {
    Instant now = Instant.now(Clock.systemUTC());
    Date nowOfDate = Date.from(now);

    assertThat(Date.from(now).equals(nowOfDate)).isTrue();
    assertThat(Date.from(now).before(nowOfDate)).isFalse();
    assertThat(Date.from(now).after(nowOfDate)).isFalse();
  }

  @Test
  // jwt time bases on second
  public void testJwts() throws Exception {
    Instant now = Instant.now(Clock.systemUTC());
    Date nowAsDate = Date.from(now);

    Map<String, Object> claims = new HashMap<>();
    claims.put(ConstDef.KEY_PID, "123");

    String token = Jwts.builder()
                       .setClaims(claims)
                       .setSubject("abc")
                       .setIssuedAt(nowAsDate)
                       .setExpiration(Date.from(now.plusSeconds(3600)))
                       .signWith(SignatureAlgorithm.HS512, "iconnect")
                       .compact();

    Date creationTimeOfToken = jwtTokenUtil.getIssuedAtDateFromToken(token);
    log.info("ct in token {}({}), should be {}({}",
             creationTimeOfToken,
             creationTimeOfToken.toInstant(),
             nowAsDate,
             nowAsDate.toInstant());
    assertThat(creationTimeOfToken.equals(nowAsDate)).isFalse();
  }
}
