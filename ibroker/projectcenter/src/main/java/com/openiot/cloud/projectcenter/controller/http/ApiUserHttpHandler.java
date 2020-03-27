/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.common.model.TokenContent;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.projectcenter.controller.ao.UserAO;
import com.openiot.cloud.projectcenter.service.UserService;
import com.openiot.cloud.projectcenter.service.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class ApiUserHttpHandler {
  @Autowired
  private UserService userService;
  @Autowired
  private ObjectMapper objectMapper;

  @PostMapping(value = "/api/user", consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> createUser(@RequestBody UserAO request) {
    TokenContent tokenContent =
        Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication.getDetails() instanceof TokenContent)
                .map(authentication -> (TokenContent) authentication.getDetails())
                .orElse(null);

    UserDTO userDTO = new UserDTO();
    BeanUtils.copyProperties(request, userDTO);

    if (userService.createUser(userDTO, tokenContent)) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @PutMapping(value = "/api/user", consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> updateUser(@RequestParam(ConstDef.Q_NAME) String uesrName,
                                      @RequestBody UserAO userAO) {
    TokenContent tokenContent =
        Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication.getDetails() instanceof TokenContent)
                .map(authentication -> (TokenContent) authentication.getDetails())
                .orElse(null);

    UserDTO userDTO = new UserDTO();
    BeanUtils.copyProperties(userAO, userDTO);
    userDTO.setName(uesrName);

    if (userService.updateUser(userDTO, userAO.getPassword(), tokenContent)) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @GetMapping(value = "/api/user", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?>
      queryUser(@RequestParam(value = ConstDef.Q_NAME, required = false) String userName,
                @RequestParam(value = ConstDef.Q_PROJECTID_II, required = false) String projectId)
          throws JsonProcessingException {
    TokenContent tokenContent =
        Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication.getDetails() instanceof TokenContent)
                .map(authentication -> (TokenContent) authentication.getDetails())
                .orElse(null);

    List<UserAO> userAOS =
        userService.queryUser(userName, projectId, tokenContent).stream().map(userDTO -> {
          UserAO userAO = new UserAO();
          BeanUtils.copyProperties(userDTO, userAO);
          return userAO;
        }).collect(Collectors.toList());
    return ResponseEntity.ok(objectMapper.writeValueAsBytes(userAOS));
  }

  @DeleteMapping(value = "/api/user", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> removeUser(@RequestParam(ConstDef.Q_NAME) String userName) {
    TokenContent tokenContent =
        Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication.getDetails() instanceof TokenContent)
                .map(authentication -> (TokenContent) authentication.getDetails())
                .orElse(null);

    if (userService.removeUser(userName, tokenContent)) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }
}
