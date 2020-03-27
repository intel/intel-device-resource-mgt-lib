/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */


package com.openiot.cloud.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.mongo.dao.TaskNewRepository;
import com.openiot.cloud.base.mongo.model.TaskNew;
import com.openiot.cloud.sdk.service.IConnectRequest;
import com.openiot.cloud.sdk.service.IConnectResponse;
import com.openiot.cloud.sdk.service.IConnectServiceHandler;
import com.openiot.cloud.sdk.utilities.UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class TaskRequestHandler implements IConnectServiceHandler {

  public static final Logger logger = LoggerFactory.getLogger(TaskRequestHandler.class);

  @Autowired
  private TaskNewRepository taskRepo;

  @Override
  public void onRequest(IConnectRequest request) {
    if (request.getAction() == HttpMethod.DELETE) {
      handleDelete(request);
    } else if (request.getAction() == HttpMethod.GET) {
      handleGet(request);
    } else {
      IConnectResponse.createFromRequest(request, HttpStatus.BAD_REQUEST, null, null).send();
    }
  }

  private void handleDelete(IConnectRequest request) {
    HttpStatus respCode = HttpStatus.OK;
    String respPayload = null;
    MediaType respDataType = MediaType.TEXT_PLAIN;
    IConnectResponse resp = null;

    String url = request.getUrl();
    Map<String, String> params = UrlUtil.getAllQueryParam(url);
    if (params == null || params.get("id") == null) {
      respPayload = "Query params 'id' is required!";
      respCode = HttpStatus.BAD_REQUEST;
      respDataType = MediaType.TEXT_PLAIN;
      resp = IConnectResponse.createFromRequest(request,
                                                respCode,
                                                respDataType,
                                                respPayload.getBytes());
      resp.send();
      return;
    }
    String id = params.get("id");
    taskRepo.deleteById(id);

    resp = IConnectResponse.createFromRequest(request, respCode, null, null);
    resp.send();
  }

  private void handleGet(IConnectRequest request) {
    String respPayload = null;
    MediaType respDataType = MediaType.APPLICATION_JSON;
    IConnectResponse resp = null;

    String url = request.getUrl();
    Map<String, String> params = UrlUtil.getAllQueryParam(url);
    if (params == null || params.get("monitor") == null) {
      respPayload = "Query params 'monitor' is required!";
      resp = IConnectResponse.createFromRequest(request,
                                                HttpStatus.BAD_REQUEST,
                                                MediaType.TEXT_PLAIN,
                                                respPayload.getBytes());
      resp.send();
      return;
    }

    String monitorName = params.get("monitor");
    TaskNew task = taskRepo.findTopByMonitorNameOrderByCreateTimeAsc(monitorName);
    if (task != null) {
      try {
        resp = IConnectResponse.createFromRequest(request,
                                                  HttpStatus.OK,
                                                  respDataType,
                                                  new ObjectMapper().writeValueAsBytes(task));
        resp.send();
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
      return;
    } else {
      resp = IConnectResponse.createFromRequest(request, HttpStatus.NOT_FOUND, null, null);
      resp.send();
      return;
    }
  }
}
