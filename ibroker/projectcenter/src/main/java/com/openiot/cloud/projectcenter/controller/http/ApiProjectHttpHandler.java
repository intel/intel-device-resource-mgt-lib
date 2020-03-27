/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.common.model.TokenContent;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import com.openiot.cloud.projectcenter.service.ProjectService;
import com.openiot.cloud.projectcenter.service.dto.ProjectDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RestController
public class ApiProjectHttpHandler {
  @Autowired
  private ProjectService projectService;
  @Autowired
  private ObjectMapper objectMapper;

  //////////////////////////////////////////////////////////////////////
  @PostMapping(value = "/api/project", consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> createProject(@RequestBody ProjectDTO payload) {
    TokenContent tokenContent =
        Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication.getDetails() instanceof TokenContent)
                .map(authentication -> (TokenContent) authentication.getDetails())
                .orElse(null);
    payload = projectService.createProject(payload, tokenContent);
    if (Objects.nonNull(payload)) {
      return ResponseEntity.ok(payload);
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @PutMapping(value = "/api/project", consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> updateProject(@RequestParam(ConstDef.Q_ID) String projectId,
                                         @RequestBody ProjectDTO payload) {
    TokenContent tokenContent =
        Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication.getDetails() instanceof TokenContent)
                .map(authentication -> (TokenContent) authentication.getDetails())
                .orElse(null);

    payload.setId(projectId);
    if (projectService.updateProject(payload, tokenContent)) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @GetMapping(value = "/api/project", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?>
      queryProject(@RequestParam(value = ConstDef.Q_ID, required = false) String projectId,
                   @RequestParam(value = ConstDef.Q_USER, required = false) String userName)
          throws JsonProcessingException {
    TokenContent tokenContent =
        Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication.getDetails() instanceof TokenContent)
                .map(authentication -> (TokenContent) authentication.getDetails())
                .orElse(null);

    List<ProjectDTO> projectDTOS = null;
    if (Objects.nonNull(projectId)) {
      projectDTOS =
          Stream.of(projectService.findByProjectId(projectId)).collect(Collectors.toList());
    } else if (Objects.nonNull(userName)) {
      projectDTOS = projectService.findByUserName(userName);
    } else {
      projectDTOS = projectService.findAll(tokenContent);
    }

    return ResponseEntity.ok(objectMapper.writeValueAsBytes(projectDTOS));
  }

  @DeleteMapping(value = "/api/project", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> removeProject(@RequestParam(ConstDef.Q_ID) String projectId) {
    TokenContent tokenContent =
        Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication.getDetails() instanceof TokenContent)
                .map(authentication -> (TokenContent) authentication.getDetails())
                .orElse(null);

    if (projectService.removeProject(projectId)) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  //////////////////////////////////////////////////////////////////////
  @PostMapping(value = "/api/project/attr", consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> updateProjectAttr(@RequestParam(ConstDef.Q_PROJECT) String projectId,
                                             @RequestBody AttributeEntity[] attributes) {
    TokenContent tokenContent =
        Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication.getDetails() instanceof TokenContent)
                .map(authentication -> (TokenContent) authentication.getDetails())
                .orElse(null);

    if (projectService.updateOrInsertProjectAttribute(projectId, attributes, tokenContent)) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }


  @DeleteMapping(value = "/api/project/attr", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> removeProjectAttr(@RequestParam(ConstDef.Q_PROJECT) String projectId,
                                             @RequestParam(ConstDef.Q_ATTRS) String attributes) {
    TokenContent tokenContent =
        Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication.getDetails() instanceof TokenContent)
                .map(authentication -> (TokenContent) authentication.getDetails())
                .orElse(null);

    if (projectService.removeProjectAttribute(projectId, attributes, tokenContent)) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  //////////////////////////////////////////////////////////////////////
  @PostMapping(value = "/api/project/cfg", consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> updateProjectCfg(@RequestParam(ConstDef.Q_PROJECT) String projectId,
                                            @RequestBody ConfigurationEntity[] configurations) {
    TokenContent tokenContent =
        Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication.getDetails() instanceof TokenContent)
                .map(authentication -> (TokenContent) authentication.getDetails())
                .orElse(null);

    if (projectService.updateOrInsertProjectConfiguration(projectId,
                                                          configurations,
                                                          tokenContent)) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @DeleteMapping(value = "/api/project/cfg", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> removeProjectCfg(@RequestParam(ConstDef.Q_PROJECT) String projectId,
                                            @RequestParam(ConstDef.Q_CFGS) String configurations) {
    TokenContent tokenContent =
        Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication.getDetails() instanceof TokenContent)
                .map(authentication -> (TokenContent) authentication.getDetails())
                .orElse(null);

    if (projectService.removeProjectConfiguration(projectId, configurations, tokenContent)) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  //////////////////////////////////////////////////////////////////////
  @PostMapping(value = "/api/project/member", consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> updateProjectMember(@RequestParam(ConstDef.Q_ID) String projectId,
                                               @RequestBody ProjectDTO body) {
    TokenContent tokenContent =
        Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication.getDetails() instanceof TokenContent)
                .map(authentication -> (TokenContent) authentication.getDetails())
                .orElse(null);

    if (projectService.updateOrInsertProjectMember(projectId, body, tokenContent)) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @DeleteMapping(value = "/api/project/member", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> removeProjectMember(@RequestParam(ConstDef.Q_ID) String projectId,
                                               @RequestParam(ConstDef.Q_USER) String removedUser) {
    TokenContent tokenContent =
        Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication.getDetails() instanceof TokenContent)
                .map(authentication -> (TokenContent) authentication.getDetails())
                .orElse(null);

    if (projectService.removeProjectMember(projectId, removedUser, tokenContent)) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

}
