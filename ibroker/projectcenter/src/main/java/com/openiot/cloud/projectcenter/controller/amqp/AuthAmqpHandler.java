/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.controller.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.common.model.TokenContent;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.projectcenter.controller.ErrorMessage;
import com.openiot.cloud.projectcenter.controller.ao.AuthenticationAO;
import com.openiot.cloud.projectcenter.controller.ao.AuthorizationAO;
import com.openiot.cloud.projectcenter.service.AuthenticationService;
import com.openiot.cloud.projectcenter.service.dto.AuthenticationDTO;
import com.openiot.cloud.projectcenter.service.dto.AuthorizationDTO;
import com.openiot.cloud.sdk.service.IConnectRequest;
import com.openiot.cloud.sdk.service.IConnectResponse;
import com.openiot.cloud.sdk.service.IConnectServiceHandler;
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
import java.util.Objects;

@Component
@Slf4j
public class AuthAmqpHandler implements IConnectServiceHandler {
  @Autowired
  private AuthenticationService authenticationService;
  @Autowired
  private ObjectMapper objectMapper;

  @Override
  public void onRequest(IConnectRequest request) {
    Objects.requireNonNull(request);

    String path = UriComponentsBuilder.fromUriString(request.getUrl()).build().getPath();
    try {
      if (ConstDef.U_USER_LOGIN.equals(path)) {
        loginHandler(request);
      } else if (ConstDef.U_USER_SELECTPROJECT.equals(path)) {
        selectProjectHandler(request);
      } else if (ConstDef.U_USER_REFRESH.equals(path)) {
        refreshHandler(request);
      } else if (ConstDef.U_USER_VALIDATION.equals(path)) {
        validationHandler(request);
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

  private void validationHandler(IConnectRequest request) throws IOException {
    if (Objects.equals(HttpMethod.POST, request.getAction())) {
      doValidation(request);
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.METHOD_NOT_ALLOWED,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("only support POST")))
                      .send();
    }
  }

  private void doValidation(IConnectRequest request) throws IOException {
    if (request.getPayload() == null || request.getPayload().length == 0) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need a not empty payload")))
                      .send();
      return;
    }

    AuthorizationAO authorizationAO =
        objectMapper.readValue(request.getPayload(), AuthorizationAO.class);
    if (authorizationAO.getToken() == null || authorizationAO.getToken().isEmpty()) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need a not empty token. might need to login firstly")))
                      .send();
      return;
    }

    AuthorizationDTO authorizationDTO = new AuthorizationDTO();
    BeanUtils.copyProperties(authorizationAO, authorizationDTO);

    TokenContent tokenContent = authenticationService.validateToken(authorizationDTO);
    if (tokenContent == null) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.UNAUTHORIZED,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("authentication failed")))
                      .send();
      return;
    }

    IConnectResponse.createFromRequest(request,
                                       HttpStatus.OK,
                                       MediaType.APPLICATION_JSON,
                                       objectMapper.writeValueAsBytes(tokenContent))
                    .send();
  }

  private void refreshHandler(IConnectRequest request) throws IOException {
    if (Objects.equals(HttpMethod.POST, request.getAction())) {
      doRefresh(request);
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.METHOD_NOT_ALLOWED,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("only support POST")))
                      .send();
    }
  }

  private void doRefresh(IConnectRequest request) throws IOException {
    if (request.getPayload() == null || request.getPayload().length == 0) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need a not empty payload")))
                      .send();
      return;
    }

    String oldToken = objectMapper.readValue(request.getPayload(), String.class);
    AuthorizationDTO authorizationDTO = authenticationService.refreshToken(oldToken);
    if (authorizationDTO == null) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.UNAUTHORIZED,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("authentication failed")))
                      .send();
      return;
    }

    AuthorizationAO authenticationResponse = new AuthorizationAO();
    BeanUtils.copyProperties(authorizationDTO, authenticationResponse);
    IConnectResponse.createFromRequest(request,
                                       HttpStatus.OK,
                                       MediaType.APPLICATION_JSON,
                                       objectMapper.writeValueAsBytes(authenticationResponse))
                    .send();
  }

  private void selectProjectHandler(IConnectRequest request) throws IOException {
    if (Objects.equals(HttpMethod.POST, request.getAction())) {
      doSelectProject(request);
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.METHOD_NOT_ALLOWED,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("only support POST")))
                      .send();
    }
  }

  private void doSelectProject(IConnectRequest request) throws IOException {
    if (request.getPayload() == null || request.getPayload().length == 0) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need a not empty payload")))
                      .send();
      return;
    }

    AuthorizationAO authorizationAO =
        objectMapper.readValue(request.getPayload(), AuthorizationAO.class);
    if (authorizationAO.getToken() == null || authorizationAO.getToken().isEmpty()) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need a not empty token. might need to login firstly")))
                      .send();
      return;
    }

    AuthorizationDTO authorizationDTO = new AuthorizationDTO();
    BeanUtils.copyProperties(authorizationAO, authorizationDTO);

    authorizationDTO = authenticationService.selectProject(authorizationDTO);
    if (authorizationDTO == null) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.UNAUTHORIZED,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("authentication failed")))
                      .send();
      return;
    }

    AuthorizationAO authenticationResponse = new AuthorizationAO();
    BeanUtils.copyProperties(authorizationDTO, authenticationResponse);
    IConnectResponse.createFromRequest(request,
                                       HttpStatus.OK,
                                       MediaType.APPLICATION_JSON,
                                       objectMapper.writeValueAsBytes(authenticationResponse))
                    .send();
  }

  private void loginHandler(IConnectRequest request) throws IOException {
    if (Objects.equals(HttpMethod.POST, request.getAction())) {
      doLogin(request);
    } else {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.METHOD_NOT_ALLOWED,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("only support POST")))
                      .send();
    }
  }

  private void doLogin(IConnectRequest request) throws IOException {
    if (request.getPayload() == null || request.getPayload().length == 0) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("need a not empty payload")))
                      .send();
      return;
    }

    AuthenticationAO authenticationRequest =
        objectMapper.readValue(request.getPayload(), AuthenticationAO.class);
    if (authenticationRequest.getPassword() == null || authenticationRequest.getPassword().isEmpty()
        || authenticationRequest.getUsername() == null
            | authenticationRequest.getUsername().isEmpty()) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.BAD_REQUEST,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("a username and a password are required")))
                      .send();
      return;
    }

    AuthenticationDTO authenticationDTO = new AuthenticationDTO();
    BeanUtils.copyProperties(authenticationRequest, authenticationDTO);

    AuthorizationDTO authorizationDTO = authenticationService.login(authenticationDTO);
    if (authorizationDTO == null) {
      IConnectResponse.createFromRequest(request,
                                         HttpStatus.UNAUTHORIZED,
                                         MediaType.APPLICATION_JSON,
                                         objectMapper.writeValueAsBytes(new ErrorMessage("authentication failed")))
                      .send();
      return;
    }

    AuthorizationAO authenticationResponse = new AuthorizationAO();
    BeanUtils.copyProperties(authorizationDTO, authenticationResponse);
    IConnectResponse.createFromRequest(request,
                                       HttpStatus.OK,
                                       MediaType.APPLICATION_JSON,
                                       objectMapper.writeValueAsBytes(authenticationResponse))
                    .send();
  }
}
