/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.alarm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.mongo.dao.AlarmDefinitionRepository;
import com.openiot.cloud.base.mongo.dao.AlarmRepository;
import com.openiot.cloud.base.mongo.model.Alarm;
import com.openiot.cloud.base.mongo.model.Alarm.Status;
import com.openiot.cloud.base.mongo.model.AlarmDefinition;
import com.openiot.cloud.base.mongo.model.DssStats;
import com.openiot.cloud.sdk.event.EventOperations;
import com.openiot.cloud.sdk.service.IConnectRequest;
import com.openiot.cloud.sdk.service.IConnectResponse;
import com.openiot.cloud.sdk.service.IConnectServiceHandler;
import com.openiot.cloud.sdk.utilities.UrlUtil;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class AlarmRequestHandler implements IConnectServiceHandler {

  public static final Logger logger = LoggerFactory.getLogger(AlarmRequestHandler.class);

  @Autowired
  AlarmRepository alarmRepo;
  @Autowired
  AlarmDefinitionRepository alarmDefRepo;
  @Autowired
  private EventOperations eventOp;

  private ExecutorService executorService = Executors.newFixedThreadPool(2);

  @Override
  public void onRequest(IConnectRequest request) {
    executorService.submit(() -> {
      requestProcess(request);
    });
  }

  private void requestProcess(IConnectRequest request) {
    String url = UrlUtil.getPath(request.getUrl());
    if (ConstDef.URL_ALARM.equals(url)) {
      try {
        if (request.action == HttpMethod.GET)
          handleGet(request);
        else if (request.action == HttpMethod.PUT)
          handlePut(request);
        else if (request.action == HttpMethod.POST)
          handlePost(request);
        else if (request.action == HttpMethod.DELETE)
          handleDelete(request);
        else {
          String error =
              "URL: " + ConstDef.URL_ALARM + " does NOT supported method: " + request.action;
          IConnectResponse resp = IConnectResponse.createFromRequest(request,
                                                                     HttpStatus.BAD_REQUEST,
                                                                     MediaType.TEXT_PLAIN,
                                                                     error.getBytes());
          resp.send();
          logger.warn(error);
        }
      } catch (Exception e) {
        logger.warn(BaseUtil.getStackTrace(e));
        IConnectResponse resp =
            IConnectResponse.createFromRequest(request,
                                               HttpStatus.BAD_REQUEST,
                                               MediaType.TEXT_PLAIN,
                                               (BaseUtil.getStackTrace(e)).getBytes());
        resp.send();
      }
    } else if (ConstDef.URL_DSS_TOTAL_STATS.equals(url)) {
      try {
        if (request.action == HttpMethod.GET)
          handleGetDssStats(request);
        else {
          String error = "URL: " + ConstDef.URL_DSS_TOTAL_STATS + " does NOT supported method: "
              + request.action;
          IConnectResponse resp = IConnectResponse.createFromRequest(request,
                                                                     HttpStatus.BAD_REQUEST,
                                                                     MediaType.TEXT_PLAIN,
                                                                     error.getBytes());
          resp.send();
          logger.warn(error);
        }
      } catch (Exception e) {
        logger.warn(BaseUtil.getStackTrace(e));
      }
    } else if (ConstDef.URL_ALARM_CNT.equals(url)) {
      try {
        if (request.action == HttpMethod.GET)
          handleGetCnt(request);
        else {
          String error =
              "URL: " + ConstDef.URL_ALARM + " does NOT supported method: " + request.action;
          IConnectResponse resp = IConnectResponse.createFromRequest(request,
                                                                     HttpStatus.BAD_REQUEST,
                                                                     MediaType.TEXT_PLAIN,
                                                                     error.getBytes());
          resp.send();
          logger.warn(error);
        }
      } catch (Exception e) {
        logger.warn(BaseUtil.getStackTrace(e));
        IConnectResponse resp =
            IConnectResponse.createFromRequest(request,
                                               HttpStatus.BAD_REQUEST,
                                               MediaType.TEXT_PLAIN,
                                               (BaseUtil.getStackTrace(e)).getBytes());
        resp.send();
      }
    } else {
      logger.warn("URL: " + UrlUtil.getPath(request.getUrl()) + " is NOT supported currently!");
      IConnectResponse resp =
          IConnectResponse.createFromRequest(request,
                                             HttpStatus.BAD_REQUEST,
                                             MediaType.TEXT_PLAIN,
                                             ("URL: " + UrlUtil.getPath(request.getUrl())
                                                 + " is NOT supported currently!").getBytes());
      resp.send();
      return;
    }
  }

  private void handleGetCnt(IConnectRequest request) {
    Map<String, String> params = UrlUtil.getAllQueryParam(request.getUrl());

    String project = request.getTokenInfo(com.openiot.cloud.base.help.ConstDef.MSG_KEY_PRJ);
    if (project == null)
      project = params.get(com.openiot.cloud.base.help.ConstDef.Q_PROJECT);

    // 1. check the request validation
    if (project == null) {
      IConnectResponse resp =
          IConnectResponse.createFromRequest(request,
                                             HttpStatus.BAD_REQUEST,
                                             MediaType.TEXT_PLAIN,
                                             "missing query parameter 'project' ".getBytes());
      resp.send();
      return;
    }
    // status
    String status = params.get(com.openiot.cloud.base.help.ConstDef.F_ALARMSTATUS);
    if (noEmptyStr(status) != null) {
      try {
        if (status.startsWith("!"))
          status = status.substring(1);
        Status enumStatus = Status.valueOf(status.toUpperCase());
      } catch (Exception e1) {
        logger.error(BaseUtil.getStackTrace(e1));
        IConnectResponse resp =
            IConnectResponse.createFromRequest(request,
                                               HttpStatus.BAD_REQUEST,
                                               MediaType.TEXT_PLAIN,
                                               "status is illegal, valid status: ACTIVE, CLEARED, SOLVED or !ACTIVE".getBytes());
        resp.send();
        return;
      }
    }
    // targetType
    String tt = null;
    // targetid
    String te = null;
    // begin and end
    Optional<Long> begin =
        Optional.ofNullable(noEmptyStr(params.get(ConstDef.Q_BEGIN))).map(b -> Long.parseLong(b));
    Optional<Long> end =
        Optional.ofNullable(noEmptyStr(params.get(ConstDef.Q_END))).map(e -> Long.parseLong(e));
    // group
    String grp = noEmptyStr(params.get(com.openiot.cloud.base.help.ConstDef.F_TAGGRP));
    if (grp == null) {
      // targetType
      tt = params.get(com.openiot.cloud.base.help.ConstDef.F_ALARMTAGTYPE);
      te = params.get(com.openiot.cloud.base.help.ConstDef.F_ALARMTAGID);
    }
    String strAlarms = noEmptyStr(params.get(com.openiot.cloud.base.help.ConstDef.F_ALARMID));
    List<String> alarmIds = null;
    if (strAlarms != null) {
      alarmIds = BaseUtil.parseArray(strAlarms);
    }
    Long cnt =
        alarmRepo.filterCnt(project,
                            alarmIds == null ? null : alarmIds.toArray(new String[alarmIds.size()]),
                            tt,
                            te,
                            grp,
                            begin.orElse(null),
                            end.orElse(null),
                            params.get(com.openiot.cloud.base.help.ConstDef.F_ALARMSTATUS));
    IConnectResponse resp = null;
    resp = IConnectResponse.createFromRequest(request,
                                              HttpStatus.OK,
                                              MediaType.TEXT_PLAIN,
                                              (cnt == null ? "0" : String.valueOf(cnt)).getBytes());
    resp.send();
  }

  private void handleGetDssStats(IConnectRequest request) {
    Map<String, String> queryParams = UrlUtil.getAllQueryParam(request.getUrl());
    String project = request.getTokenInfo(com.openiot.cloud.base.help.ConstDef.MSG_KEY_PRJ);
    if (project == null)
      project = queryParams.get(com.openiot.cloud.base.help.ConstDef.Q_PROJECT);

    // 1. validate the request
    String validationResult = isStatsOfDssTotalValid(queryParams);
    if (validationResult != null || project == null) {
      IConnectResponse resp = IConnectResponse.createFromRequest(request,
                                                                 HttpStatus.BAD_REQUEST,
                                                                 MediaType.TEXT_PLAIN,
                                                                 validationResult.getBytes());
      resp.send();
      logger.warn("1. validation result: " + validationResult);
      return;
    }

    String dssName = queryParams.get(ConstDef.Q_NAME);
    String unit = queryParams.get(ConstDef.Q_UNIT);
    String page = queryParams.get(ConstDef.Q_PAGE);
    String limit = queryParams.get(ConstDef.Q_LIMIT);
    Date now = BaseUtil.getNow();
    String strFrom = queryParams.get(ConstDef.Q_FROM);
    String strTo = queryParams.get(ConstDef.Q_TO);
    Long from = (strFrom == null || strFrom.length() == 0)
        ? (DateUtils.truncate(now, Calendar.DATE).getTime())
        : Long.valueOf(strFrom);
    Long to = (strTo == null || strTo.length() == 0) ? (now.getTime()) : (Long.valueOf(strTo));

    List<DssStats> result = alarmRepo.getDssStats(project, dssName, unit, from, to, page, limit);
    if (result != null && !result.isEmpty()) {
      try {
        IConnectResponse resp =
            IConnectResponse.createFromRequest(request,
                                               HttpStatus.OK,
                                               MediaType.APPLICATION_JSON,
                                               new ObjectMapper().writeValueAsBytes(result.get(0)));
        resp.send();
      } catch (JsonProcessingException e) {
        logger.error(BaseUtil.getStackTrace(e));
        IConnectResponse resp = IConnectResponse.createFromRequest(request,
                                                                   HttpStatus.INTERNAL_SERVER_ERROR,
                                                                   MediaType.TEXT_PLAIN,
                                                                   null);
        resp.send();
      }
    } else if (result != null) {
      IConnectResponse resp = IConnectResponse.createFromRequest(request,
                                                                 HttpStatus.OK,
                                                                 MediaType.TEXT_PLAIN,
                                                                 "{}".getBytes());
      resp.send();
    } else {
      IConnectResponse resp = IConnectResponse.createFromRequest(request,
                                                                 HttpStatus.INTERNAL_SERVER_ERROR,
                                                                 MediaType.TEXT_PLAIN,
                                                                 null);
      resp.send();
    }
  }

  private String isStatsOfDssTotalValid(Map<String, String> queryParams) {
    if (queryParams == null)
      return "NO query parameters";
    // if (queryParams.get(com.openiot.cloud.base.help.ConstDef.Q_PROJECT) == null)
    // return "missing param 'project' ";
    if (queryParams.get(ConstDef.Q_NAME) == null)
      return "missing param 'name' ";
    return null;
  }

  private void handleDelete(IConnectRequest request) {
    Map<String, String> params = UrlUtil.getAllQueryParam(request.getUrl());
    String project = request.getTokenInfo(com.openiot.cloud.base.help.ConstDef.MSG_KEY_PRJ);
    if (project == null)
      project = params.get(com.openiot.cloud.base.help.ConstDef.Q_PROJECT);

    // 1. check the request validation
    if (project == null) {
      IConnectResponse resp =
          IConnectResponse.createFromRequest(request,
                                             HttpStatus.BAD_REQUEST,
                                             MediaType.TEXT_PLAIN,
                                             "missing query parameter 'project' ".getBytes());
      resp.send();
      return;
    }

    // 2. delete alarms
    if (params.get(ConstDef.Q_ID) != null) {
      alarmRepo.deleteById(params.get(ConstDef.Q_ID));
    } else {
      alarmRepo.deleteAll(alarmRepo.findAllByProject(project));
    }
    IConnectResponse resp =
        IConnectResponse.createFromRequest(request,
                                           HttpStatus.OK,
                                           MediaType.TEXT_PLAIN,
                                           ("Delete alarms in project " + project).getBytes());
    resp.send();
  }

  private void handleGet(IConnectRequest request) {
    Map<String, String> params = UrlUtil.getAllQueryParam(request.getUrl());

    String project = request.getTokenInfo(com.openiot.cloud.base.help.ConstDef.MSG_KEY_PRJ);
    if (project == null)
      project = params.get(com.openiot.cloud.base.help.ConstDef.Q_PROJECT);

    // 1. check the request validation
    if (project == null) {
      IConnectResponse resp =
          IConnectResponse.createFromRequest(request,
                                             HttpStatus.BAD_REQUEST,
                                             MediaType.TEXT_PLAIN,
                                             "missing query parameter 'project' ".getBytes());
      resp.send();
      return;
    }
    // status
    String status = params.get(com.openiot.cloud.base.help.ConstDef.F_ALARMSTATUS);
    if (noEmptyStr(status) != null) {
      try {
        if (status.startsWith("!"))
          status = status.substring(1);
        Status enumStatus = Status.valueOf(status.toUpperCase());
      } catch (Exception e1) {
        logger.error(BaseUtil.getStackTrace(e1));
        IConnectResponse resp =
            IConnectResponse.createFromRequest(request,
                                               HttpStatus.BAD_REQUEST,
                                               MediaType.TEXT_PLAIN,
                                               "status is illegal, valid status: ACTIVE, CLEARED, SOLVED or !ACTIVE".getBytes());
        resp.send();
        return;
      }
    }
    // targetType
    String tt = null;
    // targetid
    String te = null;
    // begin and end
    Optional<Long> begin =
        Optional.ofNullable(noEmptyStr(params.get(ConstDef.Q_BEGIN))).map(b -> Long.parseLong(b));
    Optional<Long> end =
        Optional.ofNullable(noEmptyStr(params.get(ConstDef.Q_END))).map(e -> Long.parseLong(e));
    // group
    String grp = noEmptyStr(params.get(com.openiot.cloud.base.help.ConstDef.F_TAGGRP));
    if (grp == null) {
      // targetType
      tt = params.get(com.openiot.cloud.base.help.ConstDef.F_ALARMTAGTYPE);
      // if (noEmptyStr(params.get(ConstDef.F_ALARMTAGTYPE)) != null) {
      // try {
      //// tt = TargetType.valueOf(params.get(ConstDef.F_ALARMTAGTYPE).toUpperCase());
      // tt = params.get(ConstDef.F_ALARMTAGTYPE);
      // } catch (Exception e1) {
      // e1.printStackTrace();
      // IConnectResponse resp = IConnectResponse.createFromRequest(request,
      // HttpStatus.BAD_REQUEST, MediaType.TEXT_PLAIN, "targetType is illegal".getBytes());
      // resp.send();
      // return;
      // }
      // }
      // targetid
      te = params.get(com.openiot.cloud.base.help.ConstDef.F_ALARMTAGID);
      // if (noEmptyStr(params.get(ConstDef.F_ALARMTAGID)) != null) {
      // try {
      // JSONObject jsonObj = new JSONObject(params.get(ConstDef.F_ALARMTAGID));
      // te = new TargetEntity(
      // jsonObj.has(ConstDef.F_TAGDEVID) ? jsonObj.getString(ConstDef.F_TAGDEVID) : null,
      // jsonObj.has(ConstDef.F_TAGRES) ? jsonObj.getString(ConstDef.F_TAGRES) : null,
      // jsonObj.has(ConstDef.F_TAGPROP) ? jsonObj.getString(ConstDef.F_TAGPROP) : null,
      // jsonObj.has(ConstDef.F_TAGGRP) ? jsonObj.getString(ConstDef.F_TAGGRP) : null,
      // jsonObj.has(ConstDef.F_TAGDSN) ? jsonObj.getString(ConstDef.F_TAGDSN) : null);
      // } catch (Exception e1) {
      // e1.printStackTrace();
      // IConnectResponse resp = IConnectResponse.createFromRequest(request,
      // HttpStatus.BAD_REQUEST, MediaType.TEXT_PLAIN, "targetId is illegal".getBytes());
      // resp.send();
      // return;
      // }
      // }
    }
    // unit
    String unit = noEmptyStr(params.get(ConstDef.F_UNIT));
    // unit
    String strAlarms = noEmptyStr(params.get(com.openiot.cloud.base.help.ConstDef.F_ALARMID));
    List<String> alarmIds = null;
    if (strAlarms != null) {
      alarmIds = BaseUtil.parseArray(strAlarms);
    }
    int page = params.get(ConstDef.Q_PAGE) == null ? ConstDef.DFLT_PAGE
        : Integer.valueOf(params.get(ConstDef.Q_PAGE));
    int limit = params.get(ConstDef.Q_LIMIT) == null ? ConstDef.DFLT_SIZE
        : Integer.valueOf(params.get(ConstDef.Q_LIMIT));
    List<?> results =
        alarmRepo.filter(project,
                         alarmIds == null ? null : alarmIds.toArray(new String[alarmIds.size()]),
                         tt,
                         te,
                         unit,
                         grp,
                         begin.orElse(null),
                         end.orElse(null),
                         params.get(com.openiot.cloud.base.help.ConstDef.F_ALARMSTATUS),
                         PageRequest.of(page, limit));
    if (results.size() > 0) {
      if (unit == null) {
        for (Object queryAlarm : results) {
          Alarm alarm = (Alarm) queryAlarm;
          AlarmDefinition alaDef = alarmDefRepo.findOneByAid(alarm.getAid());
          alarm.setSev(alaDef == null ? null : alaDef.getSev());
          // alarm.setTitle(alaDef == null ? null : alaDef.getDesc());
        }
        results.stream().forEach(alarm -> {
          if (((Alarm) alarm).getStatus().equals(Status.ACTIVE))
            ((Alarm) alarm).setCleartime(BaseUtil.getNowAsEpochMillis());
        });
      }
      IConnectResponse resp = null;
      try {
        resp = IConnectResponse.createFromRequest(request,
                                                  HttpStatus.OK,
                                                  MediaType.APPLICATION_JSON,
                                                  new ObjectMapper().writeValueAsBytes(results));
      } catch (JsonProcessingException e) {
        logger.error(BaseUtil.getStackTrace(e));
        resp = IConnectResponse.createFromRequest(request,
                                                  HttpStatus.INTERNAL_SERVER_ERROR,
                                                  MediaType.TEXT_PLAIN,
                                                  null);
      }
      resp.send();
    } else {
      IConnectResponse resp = IConnectResponse.createFromRequest(request,
                                                                 HttpStatus.OK,
                                                                 MediaType.TEXT_PLAIN,
                                                                 "[]".getBytes());
      resp.send();
    }

    // tag id and tag type
    // Tag tag = new Tag();
    // tag.setTid(params.get(ConstDef.F_ALARMTAGID));
    // tag.setTt(params.get(ConstDef.F_ALARMTAGTYPE));
    // // begin and end
    // Optional<Date> begin = Optional.ofNullable(
    // noEmptyStr(params.get(ConstDef.F_ALARMBEGINNINGTIME)))
    // .map(b -> Date.from(Instant.ofEpochMilli(Long.parseLong(b))));
    // Optional<Date> end = Optional
    // .ofNullable(noEmptyStr(params.get(ConstDef.F_ALARMENDTIME)))
    // .map(e -> Date.from(Instant.ofEpochMilli(Long.parseLong(e))));
    // // page and limit
    // int page = Integer.parseInt(
    // Optional.ofNullable(params.get(ConstDef.Q_PAGE)).orElse("0"));
    // int size = Integer.parseInt(Optional
    // .ofNullable(params.get(ConstDef.Q_LIMIT)).orElse("100"));
    // size = Math.max(size, 100);
    // PageRequest pageReq = new PageRequest(page, size);
    // // find
    // List<Alarm> results =
    // alarmRepo.filter(noEmptyStr(params.get(ConstDef.F_IID)),
    // noEmptyStr(params.get(ConstDef.F_ALARMSEVERITY)), tag,
    // begin, end, status, pageReq);
    // // no results
    // if (results == null || results.size() == 0) {
    // IConnectResponse resp = IConnectResponse.createFromRequest(request,
    // HttpStatus.NOT_FOUND,
    // MediaType.APPLICATION_JSON, null);
    // resp.send();
    // return;
    // }
    // // success
    // IConnectResponse resp = null;
    // try {
    // resp = IConnectResponse.createFromRequest(request, HttpStatus.OK,
    // MediaType.APPLICATION_JSON,
    // new ObjectMapper().writeValueAsBytes(results));
    // resp.send();
    // } catch (JsonProcessingException e) {
    // e.printStackTrace();
    // resp = IConnectResponse.createFromRequest(request,
    // HttpStatus.INTERNAL_SERVER_ERROR, MediaType.TEXT_PLAIN,
    // null);
    // resp.send();
    // }
  }

  private void handlePut(IConnectRequest request) {
    String url = request.getUrl();
    Map<String, String> params = UrlUtil.getAllQueryParam(url);

    // 1. check request payload and query parameters
    Alarm alarm;
    try {
      alarm = new ObjectMapper().readValue(request.getPayload(), Alarm.class);
    } catch (Exception e1) {
      logger.error(BaseUtil.getStackTrace(e1));
      IConnectResponse resp = IConnectResponse.createFromRequest(request,
                                                                 HttpStatus.BAD_REQUEST,
                                                                 MediaType.TEXT_PLAIN,
                                                                 "readValue error".getBytes());
      resp.send();
      return;
    }
    if (alarm == null || alarm.getStatus() == null) {
      IConnectResponse resp =
          IConnectResponse.createFromRequest(request,
                                             HttpStatus.BAD_REQUEST,
                                             MediaType.TEXT_PLAIN,
                                             "missing 'status' in payload! ".getBytes());
      resp.send();
      return;
    }

    String alarmIdInDB = params.get(ConstDef.F_IID);
    if (noEmptyStr(alarmIdInDB) == null) {
      IConnectResponse resp =
          IConnectResponse.createFromRequest(request,
                                             HttpStatus.BAD_REQUEST,
                                             MediaType.TEXT_PLAIN,
                                             "missing query parameter: id ".getBytes());
      resp.send();
      return;
    }

    // 2. update existing alarm
    Alarm existingAlarm = alarmRepo.findOneById(alarmIdInDB);
    if (existingAlarm != null) {
      existingAlarm.setStatus(alarm.getStatus());
      if (alarm.getCleartime() != null)
        existingAlarm.setCleartime(alarm.getCleartime());
      if (alarm.getContent() != null)
        existingAlarm.setContent(alarm.getContent());
      alarmRepo.save(existingAlarm);
      IConnectResponse resp =
          IConnectResponse.createFromRequest(request,
                                             HttpStatus.OK,
                                             MediaType.TEXT_PLAIN,
                                             ("alarm [" + alarmIdInDB
                                                 + "] updated successfully!").getBytes());
      resp.send();
    } else {
      IConnectResponse resp =
          IConnectResponse.createFromRequest(request,
                                             HttpStatus.BAD_REQUEST,
                                             MediaType.TEXT_PLAIN,
                                             ("alarm [" + alarmIdInDB + "] not exists").getBytes());
      resp.send();
    }
  }

  private void handlePost(IConnectRequest request) {
    Alarm alarm;

    // 1. check request payload
    try {
      alarm = new ObjectMapper().readValue(request.getPayload(), Alarm.class);
      if (alarm.getSettime() != null) {
        alarm.setSettime(alarm.getSettime() == 0 ? BaseUtil.getNow().getTime()
            : alarm.getSettime() * 1000);
        logger.info("settime in alarm is updated to: " + alarm.getSettime());
      }
    } catch (Exception e1) {
      logger.error(BaseUtil.getStackTrace(e1));
      IConnectResponse resp = IConnectResponse.createFromRequest(request,
                                                                 HttpStatus.INTERNAL_SERVER_ERROR,
                                                                 MediaType.TEXT_PLAIN,
                                                                 "readValue error".getBytes());
      resp.send();
      return;
    }

    String validation = isValidAlarmRequest(alarm);
    if (validation != null) {
      IConnectResponse resp =
          IConnectResponse.createFromRequest(request,
                                             HttpStatus.BAD_REQUEST,
                                             MediaType.TEXT_PLAIN,
                                             ("request payload error:" + validation).getBytes());
      resp.send();
      return;
    }

    // 2.1 find existing alarm
    Alarm existingAlarm;
    if (alarm.getId() != null) {
      /* request from web to change the status */
      existingAlarm = alarmRepo.findOneById(alarm.getId());
    } else {
      /* request from gateway */
      existingAlarm =
          alarmRepo.findOneByAlarmidAndTargettypeAndTargetidAndSet_t(alarm.getProject(),
                                                                     alarm.getAid(),
                                                                     alarm.getTargettype(),
                                                                     alarm.getTargetid(),
                                                                     alarm.getSettime(),
                                                                     alarm.getGroup());
    }

    // 2.2 no existing alarm, just insert
    if (existingAlarm == null) {
      logger.info("2.1 create a new alarm " + alarm);
      if (alarm.getCleartime() != null)
        alarm.setCleartime(alarm.getCleartime() * 1000);

      try {
        alarmRepo.save(alarm);
        IConnectResponse resp =
            IConnectResponse.createFromRequest(request,
                                               HttpStatus.OK,
                                               MediaType.APPLICATION_JSON,
                                               new ObjectMapper().writeValueAsBytes(alarm));
        resp.send();
      } catch (JsonProcessingException e) {
        logger.error(BaseUtil.getStackTrace(e));
      }
    } else {
      logger.info("2.1 update the existing alarm " + existingAlarm);
      // 2.3 there is existing alarm, just update or discard
      // 2.3.1 existing ACTIVE
      // 2.3.1.1 new is ACTIVE, just discard
      // 2.3.1.1 new is CLEARED just update
      if (alarm.getCleartime() != null)
        alarm.setCleartime(alarm.getCleartime() == 0 ? BaseUtil.getNow().getTime()
            : alarm.getCleartime() * 1000);
      if (alarm.getStatus().compare(existingAlarm.getStatus()) > 0) {
        Optional.ofNullable(alarm.getStatus()).ifPresent(status -> existingAlarm.setStatus(status));
        Optional.ofNullable(alarm.getCleartime()).ifPresent(end -> existingAlarm.setCleartime(end));
        Optional.ofNullable(alarm.getContent())
                .ifPresent(details -> existingAlarm.setContent(details));
        Optional.ofNullable(alarm.getTitle()).ifPresent(title -> existingAlarm.setTitle(title));
        alarmRepo.save(existingAlarm);
        try {
          IConnectResponse resp =
              IConnectResponse.createFromRequest(request,
                                                 HttpStatus.OK,
                                                 MediaType.APPLICATION_JSON,
                                                 new ObjectMapper().writeValueAsBytes(existingAlarm));
          resp.send();
        } catch (JsonProcessingException e) {
          logger.error(BaseUtil.getStackTrace(e));
        }
      } else {
        // 2.3.2 existing SOLVED
        // 2.3.2.1 new is ACTIVE, just discard
        // 2.3.2.1 new is CLEARED just discard
        IConnectResponse resp = IConnectResponse.createFromRequest(request,
                                                                   HttpStatus.BAD_REQUEST,
                                                                   MediaType.TEXT_PLAIN,
                                                                   ("Existing Alarm "
                                                                       + existingAlarm
                                                                       + " status conflict with new alarm "
                                                                       + alarm).getBytes());
        resp.send();
      }
    }

    // 3. generate an event
    try {
      boolean isSuccess = eventOp.eventFilter("ALARM",
                                              alarm.getProject(),
                                              alarm.getTargettype(),
                                              alarm.getTargetid(),
                                              new ObjectMapper().writeValueAsBytes(alarm),
                                              "JSON");
      logger.info("3.1 create event for alarm: " + (isSuccess ? "sucess" : "fail"));
    } catch (JsonProcessingException e) {
      logger.error("3.1 create event fail for alarm format error: " + BaseUtil.getStackTrace(e));
    }
  }

  private String isValidAlarmRequest(Alarm alarm) {
    if (alarm == null)
      return "null payload in request!";
    if (alarm.getStatus() == null)
      return "no status in payload";
    if (alarm.getId() != null)
      return null;
    if (alarm.getProject() == null)
      return "no project in payload";
    if (alarm.getAid() == null)
      return "no alarmid in payload";
    if (alarm.getTargetid() == null)
      return "no targetid in payload";
    if (alarm.getTargettype() == null)
      return "no targettype in payload";
    if (alarm.getSettime() == null)
      return "no set_t in payload";
    if (alarm.getStatus() == Status.CLEARED && alarm.getCleartime() == null)
      return "status is cleared but there is no clear_t";
    return null;
  }

  private String noEmptyStr(String in) {
    return in != null ? in.trim().length() > 0 ? in.trim() : null : null;
  }

  // private boolean islegal(String tt, String te) {
  // if (te == null) {
  // return false;
  // }
  // if (tt == TargetType.DEVICE) {
  // if (te.getDi() != null)
  // return true;
  // } else if (tt == TargetType.PROPERTY) {
  // if (te.getDi() != null && te.getRes() != null && te.getPt() != null)
  // return true;
  // } else if (tt == TargetType.RESOURCE) {
  // if (te.getDi() != null && te.getRes() != null)
  // return true;
  // } else if (tt == TargetType.DATASOURCE) {
  // if (te.getGrp() != null && te.getDsn() != null)
  // return true;
  // } else if (tt == TargetType.GROUP) {
  // if (te.getGrp() != null)
  // return true;
  // }
  // return false;
  // }

}
