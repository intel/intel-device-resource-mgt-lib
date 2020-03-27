/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import com.openiot.cloud.projectcenter.controller.ErrorMessage;
import com.openiot.cloud.projectcenter.service.ProjectService;
import com.openiot.cloud.projectcenter.service.dto.ProjectDTO;
import com.openiot.cloud.sdk.service.IConnectRequest;
import com.openiot.cloud.sdk.service.IConnectResponse;
import com.openiot.cloud.sdk.service.IConnectServiceHandler;
import com.openiot.cloud.sdk.service.TokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class ApiProjectAmqpHandler implements IConnectServiceHandler {
  @Autowired
  private ProjectService projectService;
  @Autowired
  private ObjectMapper objectMapper;

  @Override
  public void onRequest(IConnectRequest request) {
    Objects.requireNonNull(request);

    String path = UriComponentsBuilder.fromUriString(request.getUrl()).build().getPath();
    log.debug("a incoming request {} ", request);
    try {
      if (path.startsWith(ConstDef.U_PROJECT_ATTR)) {
        projectAttributeHandler(request);
      } else if (path.startsWith(ConstDef.U_PROJECT_CFG)) {
        projectConfiguraitonHandler(request);
      } else if (path.startsWith(ConstDef.U_PROJECT_MEMBER)) {
        projectMemberHandler(request);
      } else if (path.startsWith(ConstDef.U_PROJECT)) {
        projectHandler(request);
      } else {
        IConnectResponse.createFromRequest(request,
                                           HttpStatus.NOT_IMPLEMENTED,
                                           MediaType.APPLICATION_JSON,
                                           objectMapper.writeValueAsBytes(new ErrorMessage("not support "
                                               + path)))
                        .send();
      }
    } catch (IOException e) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.INTERNAL_SERVER_ERROR,
                                         MediaType.APPLICATION_JSON,
                                         new JSONObject().append("error",
                                                                 "failed to serialize/deserialize with JSON")
                                                         .toString()
                                                         .getBytes())
                      .send();
    }
  }

  private void projectHandler(IConnectRequest request) throws IOException {
    HttpMethod action = request.getAction();
    if (HttpMethod.GET.equals(action)) {
      queryProject(request);
    } else if (HttpMethod.POST.equals(action)) {
      createProject(request);
    } else if (HttpMethod.PUT.equals(action)) {
      updateProject(request);
    } else if (HttpMethod.DELETE.equals(action)) {
      removeProject(request);
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.METHOD_NOT_ALLOWED,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("not support "
                                             + action)))
                      .send();
    }
  }

  private void queryProject(IConnectRequest request) throws IOException {
    Map<String, String> queryParams = UriComponentsBuilder.fromUriString(request.getUrl())
                                                          .build()
                                                          .getQueryParams()
                                                          .toSingleValueMap();

    List<ProjectDTO> projectDTOS = null;
    if (queryParams.containsKey(ConstDef.Q_ID)) {
      projectDTOS = Stream.of(projectService.findByProjectId(queryParams.get(ConstDef.Q_ID)))
                          .collect(Collectors.toList());
    } else if (queryParams.containsKey(ConstDef.Q_USER)) {
      projectDTOS = projectService.findByUserName(queryParams.get(ConstDef.Q_USER));
    } else {
      projectDTOS = projectService.findAll(TokenUtil.formTokenContent(request));
    }

    IConnectResponse.createFromRequest(request,
                                       HttpStatus.OK,
                                       MediaType.APPLICATION_JSON,
                                       objectMapper.writeValueAsBytes(projectDTOS))
                    .send();
  }

  private void createProject(IConnectRequest request) throws IOException {
    if (Objects.isNull(request.getPayload()) || request.getPayload().length == 0) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need a not empty payload")))
                      .send();
      return;
    }

    ProjectDTO payload = objectMapper.readValue(request.getPayload(), ProjectDTO.class);
    payload = projectService.createProject(payload, TokenUtil.formTokenContent(request));
    if (Objects.nonNull(payload)) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.OK,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(payload))
                      .send();
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         null)
                      .send();
    }
  }

  private void updateProject(IConnectRequest request) throws IOException {
    Map<String, String> queryParams = UriComponentsBuilder.fromUriString(request.getUrl())
                                                          .build()
                                                          .getQueryParams()
                                                          .toSingleValueMap();
    if (!queryParams.containsKey(ConstDef.Q_ID)) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need \"id\"")))
                      .send();
      return;
    }

    if (request.getPayload() == null || request.getPayload().length == 0) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need a not empty payload")))
                      .send();
      return;
    }

    ProjectDTO payload = objectMapper.readValue(request.getPayload(), ProjectDTO.class);
    payload.setId(queryParams.get(ConstDef.Q_ID));

    if (projectService.updateProject(payload, TokenUtil.formTokenContent(request))) {
      IConnectResponse.createFromRequest(request, HttpStatus.OK, MediaType.APPLICATION_JSON, null)
                      .send();
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         null)
                      .send();
    }
  }

  private void removeProject(IConnectRequest request) throws IOException {
    Map<String, String> queryParams = UriComponentsBuilder.fromUriString(request.getUrl())
                                                          .build()
                                                          .getQueryParams()
                                                          .toSingleValueMap();
    if (!queryParams.containsKey(ConstDef.Q_ID)) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need \"id\"")))
                      .send();
      return;
    }

    if (projectService.removeProject(queryParams.get(ConstDef.Q_ID))) {
      IConnectResponse.createFromRequest(request, HttpStatus.OK, MediaType.APPLICATION_JSON, null)
                      .send();
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         null)
                      .send();
    }
  }

  private void projectAttributeHandler(IConnectRequest request) throws IOException {
    HttpMethod action = request.getAction();
    if (HttpMethod.POST.equals(action)) {
      updateProjectAttr(request);
    } else if (HttpMethod.DELETE.equals(action)) {
      removeProjectAttr(request);
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.METHOD_NOT_ALLOWED,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("not support "
                                             + action)))
                      .send();
    }
  }

  private void updateProjectAttr(IConnectRequest request) throws IOException {
    Map<String, String> queryParams = UriComponentsBuilder.fromUriString(request.getUrl())
                                                          .build()
                                                          .getQueryParams()
                                                          .toSingleValueMap();
    if (!queryParams.containsKey(ConstDef.Q_PROJECT)) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need \"project\"")))
                      .send();
      return;
    }

    if (request.getPayload() == null || request.getPayload().length == 0) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need a not empty payload")))
                      .send();
      return;
    }

    AttributeEntity[] attributes =
        objectMapper.readValue(request.getPayload(), AttributeEntity[].class);
    if (projectService.updateOrInsertProjectAttribute(queryParams.get(ConstDef.Q_PROJECT),
                                                      attributes,
                                                      TokenUtil.formTokenContent(request))) {
      IConnectResponse.createFromRequest(request, HttpStatus.OK, MediaType.APPLICATION_JSON, null)
                      .send();
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         null)
                      .send();
    }
  }

  private void removeProjectAttr(IConnectRequest request) throws IOException {
    Map<String, String> queryParams = UriComponentsBuilder.fromUriString(request.getUrl())
                                                          .build()
                                                          .getQueryParams()
                                                          .toSingleValueMap();
    if (!queryParams.containsKey(ConstDef.Q_PROJECT)
        || !queryParams.containsKey(ConstDef.Q_ATTRS)) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need \"project\" and \"attrs\"")))
                      .send();
      return;
    }

    if (projectService.removeProjectAttribute(queryParams.get(ConstDef.Q_PROJECT),
                                              queryParams.get(ConstDef.Q_ATTRS),
                                              TokenUtil.formTokenContent(request))) {
      IConnectResponse.createFromRequest(request, HttpStatus.OK, MediaType.APPLICATION_JSON, null)
                      .send();
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         null)
                      .send();
    }
  }

  private void projectConfiguraitonHandler(IConnectRequest request) throws IOException {
    HttpMethod action = request.getAction();
    if (HttpMethod.POST.equals(action)) {
      updateProjectCfg(request);
    } else if (HttpMethod.DELETE.equals(action)) {
      removeProjectCfg(request);
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.METHOD_NOT_ALLOWED,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("not support "
                                             + action)))
                      .send();
    }
  }

  private void updateProjectCfg(IConnectRequest request) throws IOException {
    Map<String, String> queryParams = UriComponentsBuilder.fromUriString(request.getUrl())
                                                          .build()
                                                          .getQueryParams()
                                                          .toSingleValueMap();
    if (!queryParams.containsKey(ConstDef.Q_PROJECT)) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need \"project\"")))
                      .send();
      return;
    }

    if (request.getPayload() == null || request.getPayload().length == 0) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need a not empty payload")))
                      .send();
      return;
    }

    ConfigurationEntity[] configurations =
        objectMapper.readValue(request.getPayload(), ConfigurationEntity[].class);
    if (projectService.updateOrInsertProjectConfiguration(queryParams.get(ConstDef.Q_PROJECT),
                                                          configurations,
                                                          TokenUtil.formTokenContent(request))) {
      IConnectResponse.createFromRequest(request, HttpStatus.OK, MediaType.APPLICATION_JSON, null)
                      .send();
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         null)
                      .send();
    }
  }

  private void removeProjectCfg(IConnectRequest request) throws IOException {
    Map<String, String> queryParams = UriComponentsBuilder.fromUriString(request.getUrl())
                                                          .build()
                                                          .getQueryParams()
                                                          .toSingleValueMap();
    if (!queryParams.containsKey(ConstDef.Q_PROJECT) || !queryParams.containsKey(ConstDef.Q_CFGS)) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need \"project\" and \"cfs\"")))
                      .send();
      return;
    }

    if (projectService.removeProjectConfiguration(queryParams.get(ConstDef.Q_PROJECT),
                                                  queryParams.get(ConstDef.Q_CFGS),
                                                  TokenUtil.formTokenContent(request))) {
      IConnectResponse.createFromRequest(request, HttpStatus.OK, MediaType.APPLICATION_JSON, null)
                      .send();
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         null)
                      .send();
    }
  }

  private void projectMemberHandler(IConnectRequest request) throws IOException {
    HttpMethod action = request.getAction();
    if (HttpMethod.POST.equals(action)) {
      updateProjectMember(request);
    } else if (HttpMethod.DELETE.equals(action)) {
      removeProjectMember(request);
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.METHOD_NOT_ALLOWED,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("not support "
                                             + action)))
                      .send();
    }
  }

  private void updateProjectMember(IConnectRequest request) throws IOException {
    Map<String, String> queryParams = UriComponentsBuilder.fromUriString(request.getUrl())
                                                          .build()
                                                          .getQueryParams()
                                                          .toSingleValueMap();
    if (!queryParams.containsKey(ConstDef.Q_ID)) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need \"id\"")))
                      .send();
      return;
    }

    if (request.getPayload() == null || request.getPayload().length == 0) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need a not empty payload")))
                      .send();
      return;
    }

    ProjectDTO body = objectMapper.readValue(request.getPayload(), ProjectDTO.class);
    if (projectService.updateOrInsertProjectMember(queryParams.get(ConstDef.Q_ID),
                                                   body,
                                                   TokenUtil.formTokenContent(request))) {
      IConnectResponse.createFromRequest(request, HttpStatus.OK, MediaType.APPLICATION_JSON, null)
                      .send();
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         null)
                      .send();
    }
  }

  private void removeProjectMember(IConnectRequest request) throws IOException {
    Map<String, String> queryParams = UriComponentsBuilder.fromUriString(request.getUrl())
                                                          .build()
                                                          .getQueryParams()
                                                          .toSingleValueMap();
    if (!queryParams.containsKey(ConstDef.Q_ID) || !queryParams.containsKey(ConstDef.Q_USER)) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need \"id\" and \"user\"")))
                      .send();
      return;
    }

    if (projectService.removeProjectMember(queryParams.get(ConstDef.Q_ID),
                                           queryParams.get(ConstDef.Q_USER),
                                           TokenUtil.formTokenContent(request))) {
      IConnectResponse.createFromRequest(request, HttpStatus.OK, MediaType.APPLICATION_JSON, null)
                      .send();
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         null)
                      .send();
    }
  }
}
