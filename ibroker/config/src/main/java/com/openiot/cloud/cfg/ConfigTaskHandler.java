/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.cfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.dao.*;
import com.openiot.cloud.base.mongo.model.*;
import com.openiot.cloud.base.service.model.ProjectDTO;
import com.openiot.cloud.cfg.model.DeviceConfig;
import com.openiot.cloud.cfg.model.GroupConfig;
import com.openiot.cloud.cfg.model.ProjectCfg;
import com.openiot.cloud.sdk.service.IConnectRequest;
import com.openiot.cloud.sdk.service.IConnectResponse;
import com.openiot.cloud.sdk.service.IConnectResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Component
public class ConfigTaskHandler implements IConnectResponseHandler {
  private static Logger logger = LoggerFactory.getLogger(ConfigTaskHandler.class);

  @Autowired
  private ConfigRepository cfgRepo;
  @Autowired
  private GroupRepository grpRepo;
  @Autowired
  private DeviceRepository devRepo;
  @Autowired
  private ResourceRepository resRepo;
  @Autowired
  private ResProRepository resProRepo;

  private boolean isTaskHandleSuccess = false;
  private Object taskLock = new Object();
  private String currentTaskId = null;

  @Override
  public void onResponse(IConnectResponse response) {
    synchronized (taskLock) {
      try {
        if (response.getStatus().equals(HttpStatus.OK) && response.getPayload() != null) {

          TaskNew task = null;
          try {
            task = new ObjectMapper().readValue(response.getPayload(), TaskNew.class);
          } catch (IOException e) {
            logger.warn(BaseUtil.getStackTrace(e));
          }

          if (task != null) {
            switch (task.getTargetType()) {
              // device config update
              case ConstDef.EVENT_TARGET_TYPE_DEVICE:
                generateDevConfiguration(task.getTargetId());
                break;
              // group config update
              case ConstDef.EVENT_TARGET_TYPE_GROUP:
                generateGroupConfiguration(task.getTargetId());
                break;
              // project config update
              case ConstDef.EVENT_TARGET_TYPE_PROJECT:
                generateProjectConfiguration(task.getTargetId());
                break;
              default:
                break;
            }

            currentTaskId = task.getId();
            // Delete the task after handling
            String url = "/task?id=" + currentTaskId;
            IConnectRequest request = IConnectRequest.create(HttpMethod.DELETE, url, null, null);
            request.send((resp) -> {
              synchronized (taskLock) {
                if (resp.getStatus().equals(HttpStatus.OK)) {
                  isTaskHandleSuccess = true;
                  logger.info("Config service handle task: {} succeed!", currentTaskId);
                }
                taskLock.notifyAll(); // Task handle succeed, notify the sender thread
              }
            }, 10, TimeUnit.SECONDS);
          } else {
            logger.warn("Config service pull task with wrong payload!");
            taskLock.notifyAll(); // GET task payload is not correct, handling failed, notify the
            // sender thread
          }
        } else {
          if (response.getStatus().equals(HttpStatus.BAD_REQUEST)) {
            logger.warn("Config service pull task with bad request!");
          } else if (response.getStatus().equals(HttpStatus.NOT_FOUND)) {
            logger.warn("Config service pull task with not found!");
          } else {
            logger.warn("Config service pull task with unknown issue!");
          }

          taskLock.notifyAll(); // GET task failed, notify the sender thread
        }
      } catch (Exception e) {
        logger.warn(BaseUtil.getStackTrace(e));
      }
    }
    return;
  }

  public Object getTaskLock() {
    return this.taskLock;
  }

  public boolean getTaskHandleStatus() {
    return this.isTaskHandleSuccess;
  }

  public void setTaskHandleStatus(boolean status) {
    this.isTaskHandleSuccess = status;
  }

  public void generateDevConfiguration(String devId) {
    logger.debug("going to generateDevConfiguration for {}", devId);
    Optional<String> deviceId = Optional.ofNullable(devId);
    Optional<Device> device = deviceId.map(did -> devRepo.findOneById(did));
    Optional<List<Resource>> resources = deviceId.map(did -> resRepo.findAllByDevId(did));
    Optional<List<ResProperty>> properties = deviceId.map(did -> resProRepo.findAllByDevId(did));
    Optional<List<Group>> deviceGroups =
        device.map(dev -> grpRepo.findAllGroupByDevId(dev.getId()));
    Optional<List<Group>> resourceGroups =
        resources.map(reses -> reses.stream()
                                    .map(res -> grpRepo.findAllGroupByRes(res.getDevId(),
                                                                          res.getUrl()))
                                    .filter(gllist -> !gllist.isEmpty())
                                    .flatMap(List::stream)
                                    .collect(Collectors.toList()));

    Optional<DeviceConfig> devCfg = Optional.ofNullable(DeviceConfig.from(device,
                                                                          resources,
                                                                          properties,
                                                                          deviceGroups,
                                                                          resourceGroups));
    devCfg.map(dc -> dc.toJsonString())
          .filter(dcJson -> !dcJson.isEmpty())
          .ifPresent(dcJson -> addConfig(ConstDef.CFG_TT_DEVONGW, deviceId.get(), dcJson));
  }

  public void generateGroupConfiguration(String groupName) {
    logger.debug("going to generateGroupConfiguration for {} ", groupName);
    Optional<Group> group = Optional.ofNullable(grpRepo.findOneByName(groupName));
    Optional<GroupConfig> grpCfg = Optional.ofNullable(GroupConfig.from(group));
    grpCfg.map(gc -> gc.toJsonString())
          .filter(gcJson -> !gcJson.isEmpty())
          .ifPresent(gcJson -> addConfig(ConstDef.CFG_TT_GRP, groupName, gcJson));
  }

  public void addConfig(String targetType, String targetId, String config) {
    Config cfg = cfgRepo.findOneByTargetTypeAndTargetId(targetType, targetId);
    if (cfg == null) {
      cfg = new Config(targetType, targetId, config);
    } else {
      cfg.setConfig(config);
    }
    cfgRepo.save(cfg);

    logger.info("generate a new config: {} ", cfg);
  }

  public void generateProjectConfiguration(String projectID) {
    IConnectRequest.create(HttpMethod.GET,
                           String.format("/api/project?%s=%s", ConstDef.Q_ID, projectID),
                           MediaType.APPLICATION_JSON,
                           null)
                   .send(response -> {
                     if (response.getStatus().is2xxSuccessful()) {
                       try {
                         ProjectDTO[] projectDTO =
                             new ObjectMapper().readValue(response.getPayload(),
                                                          ProjectDTO[].class);
                         if (projectDTO.length == 0) {
                           logger.warn("empty project information about {}", projectID);
                         } else {
                           ProjectCfg projectCfg = ProjectCfg.from(projectDTO[0]);
                           addConfig(ConstDef.CFG_TT_PRJ, projectID, projectCfg.toJsonString());
                         }
                       } catch (IOException e) {
                         logger.error("can not parse the payload from /api/project");
                       }
                     } else {
                       logger.error("GET /api/project failed {}", response);
                     }
                   }, 5, TimeUnit.SECONDS);
  }
}
