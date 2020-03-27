/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */
package com.openiot.cloud.event;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.mongo.dao.EventMonitorRepository;
import com.openiot.cloud.base.mongo.model.EventMonitor;
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
import java.io.IOException;
import java.util.Date;
import java.util.Map;

@Component
public class EventMonitorRequestHandler implements IConnectServiceHandler {

  public static final Logger logger = LoggerFactory.getLogger(EventMonitorRequestHandler.class);

  @Autowired
  private EventMonitorRepository monitorRepo;

  @Override
  public void onRequest(IConnectRequest request) {
    if (request.getAction() == HttpMethod.DELETE) {
      handleDelete(request);
    } else if (request.getAction() == HttpMethod.POST) {
      handlePost(request);
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
    if (params == null || params.get("monitor") == null) {
      respPayload = "Query params 'monitor' is required!";
      respCode = HttpStatus.BAD_REQUEST;
      respDataType = MediaType.TEXT_PLAIN;
      resp = IConnectResponse.createFromRequest(request,
                                                respCode,
                                                respDataType,
                                                respPayload.getBytes());
      resp.send();
      return;
    }

    String monitorName = params.get("monitor");
    EventMonitor monitor = monitorRepo.findOneByName(monitorName);
    if (monitor == null) {
      respPayload = String.format("can not find %s", monitorName);
      respCode = HttpStatus.BAD_REQUEST;
      respDataType = MediaType.TEXT_PLAIN;
      resp = IConnectResponse.createFromRequest(request,
                                                respCode,
                                                respDataType,
                                                respPayload.getBytes());
      resp.send();
      return;
    }

    monitorRepo.delete(monitor);
    resp = IConnectResponse.createFromRequest(request, respCode, null, null);
    resp.send();
  }

  private void handlePost(IConnectRequest request) {
    HttpStatus respCode = HttpStatus.OK;
    String respPayload = null;
    MediaType respDataType = MediaType.TEXT_PLAIN;
    IConnectResponse resp = null;

    if (request.getPayload() == null || request.getPayload().length == 0) {
      respPayload = "Request payload is null!";
      respCode = HttpStatus.BAD_REQUEST;
      respDataType = MediaType.TEXT_PLAIN;
      resp = IConnectResponse.createFromRequest(request,
                                                respCode,
                                                respDataType,
                                                respPayload.getBytes());
      resp.send();
      return;
    }

    EventMonitor monitor = null;
    try {
      monitor = new ObjectMapper().readValue(request.getPayload(), EventMonitor.class);
    } catch (JsonParseException e) {
      e.printStackTrace();
    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (monitor != null) {
      monitor.setRegTime(new Date());
      monitorRepo.save(monitor);
      resp = IConnectResponse.createFromRequest(request, respCode, null, null);
      resp.send();
      return;
    } else {
      respPayload = "Request payload format is incorrect!";
      respCode = HttpStatus.BAD_REQUEST;
      respDataType = MediaType.TEXT_PLAIN;
      resp = IConnectResponse.createFromRequest(request,
                                                respCode,
                                                respDataType,
                                                respPayload.getBytes());
      resp.send();
      return;
    }
  }
}
