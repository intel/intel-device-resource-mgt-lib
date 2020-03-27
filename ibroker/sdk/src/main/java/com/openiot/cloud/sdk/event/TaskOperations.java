/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.event;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.dao.TaskNewRepository;
import com.openiot.cloud.base.mongo.model.TaskNew;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.List;

@Component
public class TaskOperations {

  @Autowired
  private TaskNewRepository taskRepo;

  public boolean createTask(String monitorName, String eventType, String desc, String targetType,
                            String targetId, int lifeTime, byte[] data, String fmt) {

    return createTask(monitorName,
                      eventType,
                      desc,
                      targetType,
                      targetId,
                      lifeTime,
                      data,
                      null,
                      ConstDef.EVENT_TASK_OPTION_APPEND);
  }

  public boolean createTask(String monitorName, String eventType, String desc, String targetType,
                            String targetId, int lifeTime, byte[] data, String fmt, String option) {

    if ((ConstDef.EVENT_TASK_OPTION_IGNORE).equals(option)) {
      List<TaskNew> tasks =
          taskRepo.findByMonitorNameAndEventTypeAndTargetTypeAndTargetId(monitorName,
                                                                         eventType,
                                                                         targetType,
                                                                         targetId);
      if (tasks != null && tasks.size() > 0) {
        return true;
      }
    } else if ((ConstDef.EVENT_TASK_OPTION_OVERWRITE).equals(option)) {
      taskRepo.removeByMonitorNameAndEventTypeAndTargetTypeAndTargetId(monitorName,
                                                                       eventType,
                                                                       targetType,
                                                                       targetId);
    }

    Date createTime = new Date();
    Date deadline = new Date(createTime.getTime() + lifeTime);

    TaskNew task = new TaskNew();
    task.setMonitorName(monitorName);
    task.setEventType(eventType);
    task.setDesc(desc);
    task.setTargetType(targetType);
    task.setTargetId(targetId);
    task.setCreateTime(createTime);
    task.setDeadline(deadline);
    task.setData(data);
    task.setDataFmt(fmt);

    taskRepo.insert(task);
    return true;
  }

  public TaskNew getTask(String monitorName) {

    return taskRepo.findTopByMonitorNameOrderByCreateTimeAsc(monitorName);
  }

  public TaskNew getTaskById(String id) {
    return taskRepo.findOneById(id);
  }

  public void deleteTask(String id) {
    // TODO ???? taskRepo.delete(id);
    taskRepo.delete(taskRepo.findOneById(id));
  }
}
