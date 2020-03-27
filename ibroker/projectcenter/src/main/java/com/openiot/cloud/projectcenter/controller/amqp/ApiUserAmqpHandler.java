/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.projectcenter.controller.ErrorMessage;
import com.openiot.cloud.projectcenter.controller.ao.UserAO;
import com.openiot.cloud.projectcenter.service.UserService;
import com.openiot.cloud.projectcenter.service.dto.UserDTO;
import com.openiot.cloud.sdk.service.IConnectRequest;
import com.openiot.cloud.sdk.service.IConnectResponse;
import com.openiot.cloud.sdk.service.IConnectServiceHandler;
import com.openiot.cloud.sdk.service.TokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.BeanUtils;
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

@Component
@Slf4j
public class ApiUserAmqpHandler implements IConnectServiceHandler {
  @Autowired
  private UserService userService;
  @Autowired
  private ObjectMapper objectMapper;

  @Override
  public void onRequest(IConnectRequest request) {
    Objects.requireNonNull(request);

    HttpMethod action = request.getAction();
    try {
      if (HttpMethod.GET.equals(action)) {
        queryUser(request);
      } else if (HttpMethod.POST.equals(action)) {
        createUser(request);
      } else if (HttpMethod.PUT.equals(action)) {
        updateUser(request);
      } else if (HttpMethod.DELETE.equals(action)) {
        removeUser(request);
      } else {
        IConnectResponse.createFromRequest(request,
                                           HttpStatus.METHOD_NOT_ALLOWED,
                                           MediaType.APPLICATION_JSON,
                                           objectMapper.writeValueAsBytes(new ErrorMessage("not support "
                                               + action)))
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

  private void createUser(IConnectRequest request) throws IOException {
    if (request.getPayload() == null || request.getPayload().length == 0) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need a not empty payload")))
                      .send();
      return;
    }

    UserAO userAO = objectMapper.readValue(request.getPayload(), UserAO.class);
    UserDTO userDTO = new UserDTO();
    BeanUtils.copyProperties(userAO, userDTO);

    if (userService.createUser(userDTO, TokenUtil.formTokenContent(request))) {
      IConnectResponse.createFromRequest(request, HttpStatus.OK, MediaType.APPLICATION_JSON, null)
                      .send();
      return;
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         null)
                      .send();
      return;
    }
  }

  private void updateUser(IConnectRequest request) throws IOException {
    Map<String, String> queryParams = UriComponentsBuilder.fromUriString(request.getUrl())
                                                          .build()
                                                          .getQueryParams()
                                                          .toSingleValueMap();
    if (!queryParams.containsKey(ConstDef.Q_NAME)) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need \"name\"")))
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

    UserAO userAO = objectMapper.readValue(request.getPayload(), UserAO.class);
    UserDTO userDTO = new UserDTO();
    BeanUtils.copyProperties(userAO, userDTO);
    userDTO.setName(queryParams.get(ConstDef.Q_NAME));

    if (userService.updateUser(userDTO,
                               userAO.getPassword(),
                               TokenUtil.formTokenContent(request))) {
      IConnectResponse.createFromRequest(request, HttpStatus.OK, MediaType.APPLICATION_JSON, null)
                      .send();
      return;
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         null)
                      .send();
      return;
    }
  }

  private void queryUser(IConnectRequest request) throws IOException {
    Map<String, String> queryParams = UriComponentsBuilder.fromUriString(request.getUrl())
                                                          .build()
                                                          .getQueryParams()
                                                          .toSingleValueMap();

    List<UserAO> userAOS = userService
                                      .queryUser(queryParams.get(ConstDef.Q_NAME),
                                                 queryParams.get(ConstDef.Q_PROJECTID_II),
                                                 TokenUtil.formTokenContent(request))
                                      .stream()
                                      .map(userDTO -> {
                                        UserAO userAO = new UserAO();
                                        BeanUtils.copyProperties(userDTO, userAO);
                                        return userAO;
                                      })
                                      .collect(Collectors.toList());
    IConnectResponse.createFromRequest(request,
                                       HttpStatus.OK,
                                       MediaType.APPLICATION_JSON,
                                       objectMapper.writeValueAsBytes(userAOS))
                    .send();
    return;
  }

  private void removeUser(IConnectRequest request) throws IOException {
    Map<String, String> queryParams = UriComponentsBuilder.fromUriString(request.getUrl())
                                                          .build()
                                                          .getQueryParams()
                                                          .toSingleValueMap();

    if (!queryParams.containsKey(ConstDef.Q_NAME)) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need \"name\"")))
                      .send();
      return;
    }

    if (userService.removeUser(queryParams.get(ConstDef.Q_NAME),
                               TokenUtil.formTokenContent(request))) {
      IConnectResponse.createFromRequest(request, HttpStatus.OK, MediaType.APPLICATION_JSON, null)
                      .send();
      return;
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         null)
                      .send();
      return;
    }
  }
}
