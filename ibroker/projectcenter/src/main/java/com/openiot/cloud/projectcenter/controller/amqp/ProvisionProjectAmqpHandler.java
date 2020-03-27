/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.projectcenter.controller.ErrorMessage;
import com.openiot.cloud.projectcenter.service.GatewayService;
import com.openiot.cloud.projectcenter.service.ProjectService;
import com.openiot.cloud.projectcenter.service.dto.GatewayDTO;
import com.openiot.cloud.projectcenter.service.dto.ProjectDTO;
import com.openiot.cloud.sdk.service.IConnectRequest;
import com.openiot.cloud.sdk.service.IConnectResponse;
import com.openiot.cloud.sdk.service.IConnectServiceHandler;
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
public class ProvisionProjectAmqpHandler implements IConnectServiceHandler {
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private GatewayService gatewayService;
  @Autowired
  private ProjectService projectService;

  @Override
  public void onRequest(IConnectRequest request) {
    Objects.requireNonNull(request);
    try {
      if (Objects.equals(HttpMethod.GET, request.getAction())) {
        queryProject(request);
      } else if (Objects.equals(HttpMethod.POST, request.getAction())) {
        updateProject(request);
      } else if (Objects.equals(HttpMethod.DELETE, request.getAction())) {
        removeProject(request);
      } else {
        IConnectResponse.createFromRequest(request,
                                           HttpStatus.METHOD_NOT_ALLOWED,
                                           MediaType.APPLICATION_JSON,
                                           objectMapper.writeValueAsBytes(new ErrorMessage("only support GET/POST/DELETE")))
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

  void queryProject(IConnectRequest request) throws IOException {
    Map<String, String> queryParams = UriComponentsBuilder.fromUriString(request.getUrl())
                                                          .build()
                                                          .getQueryParams()
                                                          .toSingleValueMap();
    if (!queryParams.containsKey(ConstDef.Q_DEVID)
        && !queryParams.containsKey(ConstDef.Q_PROJECTID)) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need either \"di\" or \"pi\"")))
                      .send();
      return;
    }

    List<GatewayDTO> gatewayList = null;
    if (queryParams.containsKey(ConstDef.Q_DEVID)
        && queryParams.containsKey(ConstDef.Q_PROJECTID)) {
      // di + pi
      gatewayList =
          Stream.of(gatewayService.findByIAgentIdAndProjectId(queryParams.get(ConstDef.Q_DEVID),
                                                              queryParams.get(ConstDef.Q_PROJECTID)))
                .collect(Collectors.toList());
    } else if (queryParams.containsKey(ConstDef.Q_DEVID)) {
      // di only
      gatewayList = Stream.of(gatewayService.findByIAgentId(queryParams.get(ConstDef.Q_DEVID)))
                          .collect(Collectors.toList());
    } else {
      // pi only
      if ("null".equals(queryParams.get(ConstDef.Q_PROJECTID))) {
        gatewayList = gatewayService.findUnassigned();
      } else {
        gatewayList = gatewayService.findByProjectId(queryParams.get(ConstDef.Q_PROJECTID));
      }
    }
    IConnectResponse.createFromRequest(request,
                                       HttpStatus.OK,
                                       MediaType.APPLICATION_JSON,
                                       objectMapper.writeValueAsBytes(gatewayList))
                    .send();
  }

  void updateProject(IConnectRequest request) throws IOException {
    Map<String, String> queryParams = UriComponentsBuilder.fromUriString(request.getUrl())
                                                          .build()
                                                          .getQueryParams()
                                                          .toSingleValueMap();
    if (!queryParams.containsKey(ConstDef.Q_PROJECTID)) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need \"pi\"")))
                      .send();
      return;
    }

    ProjectDTO projectDTO = projectService.findByProjectId(queryParams.get(ConstDef.Q_PROJECTID));
    if (projectDTO == null) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage(String.format("%s is not an existed project",
                                                                                                       queryParams.get(ConstDef.Q_PROJECTID)))))
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

    String[] iAgentIds = objectMapper.readValue(request.getPayload(), String[].class);
    gatewayService.setProject(iAgentIds, projectDTO);
    IConnectResponse.createFromRequest(request, HttpStatus.OK, MediaType.APPLICATION_JSON, null)
                    .send();
  }

  void removeProject(IConnectRequest request) throws IOException {
    // we actually do not care about the pi parameter. we just remove the project information
    if (request.getPayload() == null || request.getPayload().length == 0) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need a not empty payload")))
                      .send();
      return;
    }

    String[] iAgentIds = objectMapper.readValue(request.getPayload(), String[].class);
    gatewayService.setProject(iAgentIds, null);
    IConnectResponse.createFromRequest(request, HttpStatus.OK, MediaType.APPLICATION_JSON, null)
                    .send();
  }
}
