/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.event;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.dao.DeviceRepository;
import com.openiot.cloud.base.mongo.dao.EventMonitorRepository;
import com.openiot.cloud.base.mongo.dao.GroupRepository;
import com.openiot.cloud.base.mongo.dao.ResourceRepository;
import com.openiot.cloud.base.mongo.model.EventMonitor;
import com.openiot.cloud.base.mongo.model.EventMonitor.EventType;
import com.openiot.cloud.base.mongo.model.Group;
import com.openiot.cloud.base.mongo.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class EventOperations {

  @Autowired
  private EventMonitorRepository monitorRepo;

  @Autowired
  private ResourceRepository resRepo;

  @Autowired
  private DeviceRepository devRepo;

  @Autowired
  private GroupRepository groupRepository;

  @Autowired
  private TaskOperations taskOp;

  private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

  private EventMonitorCache cache = EventMonitorCache.getInstance();

  public void initEventMonitorCache() {
    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        syncEventMonitorCache();
      }
    }, 0, 1000 * 60);
  }

  public void syncEventMonitorCache() {
    rwLock.writeLock().lock();
    cache.setMonitors(monitorRepo.findAll());
    rwLock.writeLock().unlock();
  }

  public boolean eventFilter(String eventType, String project, String targetType, String targetId,
                             byte[] data, String fmt) {

    return eventFilter(eventType,
                       project,
                       targetType,
                       targetId,
                       data,
                       fmt,
                       ConstDef.EVENT_TASK_OPTION_APPEND);
  }

  public boolean eventFilter(String eventType, String project, String targetType, String targetId,
                             byte[] data, String fmt, String option) {

    List<String> groupIds = new ArrayList<String>();
    if (targetType.equals("RESOURCE")) {
      Resource resource = resRepo.findOneById(targetId);
      if (resource != null) {
        List<Group> groups =
            groupRepository.findAllGroupByRes(resource.getDevId(), resource.getUrl());
        if (groups != null && groups.size() > 0) {
          for (Group g : groups) {
            groupIds.add(g.getN());
          }
        }
      }
    } else if (targetType.equals("DEVICE")) {
      List<Group> groups = groupRepository.findAllGroupByDevId(targetId);
      if (groups != null && groups.size() > 0) {
        for (Group g : groups) {
          groupIds.add(g.getN());
        }
      }
    } else if (targetType.equals("GROUP")) {
      groupIds.add(targetId);
    } else { // TODO: Property
      return false;
    }

    rwLock.readLock().lock();
    if (cache.getMonitors() != null && cache.getMonitors().size() > 0) {
      for (EventMonitor monitor : cache.getMonitors()) {
        if ((project == null || project.equals(monitor.getProject()))
            && monitor.getEventTypes() != null) {
          for (EventType type : monitor.getEventTypes()) {
            if (eventType.equals(type.getEventType())) {

              if (type.getTargetType() == null && type.getTargetId() == null) {
                taskOp.createTask(monitor.getName(),
                                  eventType,
                                  "",
                                  null,
                                  null,
                                  type.getLifeTime(),
                                  data,
                                  fmt);
                rwLock.readLock().unlock();
                return true;
              }

              if (targetType.equals(type.getTargetType()) && targetId.equals(type.getTargetId())) {
                taskOp.createTask(monitor.getName(),
                                  eventType,
                                  "",
                                  targetType,
                                  targetId,
                                  type.getLifeTime(),
                                  data,
                                  fmt);
                rwLock.readLock().unlock();
                return true;
              }

              if (targetType.equals("DEVICE") && type.getTargetType().equals("GROUP")
                  && groupIds.contains(type.getTargetId())) {
                taskOp.createTask(monitor.getName(),
                                  eventType,
                                  "",
                                  targetType,
                                  targetId,
                                  type.getLifeTime(),
                                  data,
                                  fmt);
                rwLock.readLock().unlock();
                return true;
              }

              if (targetType.equals("RESOURCE") && type.getTargetType().equals("GROUP")
                  && groupIds.contains(type.getTargetId())) {
                taskOp.createTask(monitor.getName(),
                                  eventType,
                                  "",
                                  targetType,
                                  targetId,
                                  type.getLifeTime(),
                                  data,
                                  fmt);
                rwLock.readLock().unlock();
                return true;
              }
            }
          }
        }
      }
    }

    rwLock.readLock().unlock();
    return false;
  }
}
