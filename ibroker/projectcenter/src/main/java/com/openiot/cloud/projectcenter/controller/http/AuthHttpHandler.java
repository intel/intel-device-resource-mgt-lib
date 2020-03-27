/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.http;

import com.openiot.cloud.base.common.model.TokenContent;
import com.openiot.cloud.projectcenter.controller.ao.AuthenticationAO;
import com.openiot.cloud.projectcenter.controller.ao.AuthorizationAO;
import com.openiot.cloud.projectcenter.service.AuthenticationService;
import com.openiot.cloud.projectcenter.service.dto.AuthenticationDTO;
import com.openiot.cloud.projectcenter.service.dto.AuthorizationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class AuthHttpHandler {
  @Autowired
  private AuthenticationService authenticationService;

  @PostMapping(value = "/api/user/login", consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> login(@RequestBody AuthenticationAO request) {
    AuthenticationDTO authenticationDTO = new AuthenticationDTO();
    BeanUtils.copyProperties(request, authenticationDTO);

    AuthorizationDTO authorizationDTO = authenticationService.login(authenticationDTO);
    if (authorizationDTO == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    } else {
      AuthorizationAO response = new AuthorizationAO();
      BeanUtils.copyProperties(authorizationDTO, response);
      return ResponseEntity.ok(response);
    }
  }

  @PostMapping(value = "/api/user/refresh", consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> refreshToken(@RequestBody String oldToken) {
    AuthorizationDTO authorizationDTO = authenticationService.refreshToken(oldToken);
    if (authorizationDTO == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    } else {
      AuthorizationAO response = new AuthorizationAO();
      BeanUtils.copyProperties(authorizationDTO, response);
      return ResponseEntity.ok(response);
    }

  }

  @PostMapping(value = "/api/user/selectproject", consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> selectProject(@RequestBody AuthorizationAO request) {
    AuthorizationDTO authorizationDTO = new AuthorizationDTO();
    BeanUtils.copyProperties(request, authorizationDTO);

    authorizationDTO = authenticationService.selectProject(authorizationDTO);
    if (authorizationDTO == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    } else {
      AuthorizationAO response = new AuthorizationAO();
      BeanUtils.copyProperties(authorizationDTO, response);
      return ResponseEntity.ok(response);
    }
  }

  @PostMapping(value = "/api/user/validation", consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> validateToken(@RequestBody AuthorizationAO request) {
    AuthorizationDTO authorizationDTO = new AuthorizationDTO();
    BeanUtils.copyProperties(request, authorizationDTO);
    TokenContent tokenContent = authenticationService.validateToken(authorizationDTO);
    if (tokenContent == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    } else {
      return ResponseEntity.ok(tokenContent);
    }
  }
}
