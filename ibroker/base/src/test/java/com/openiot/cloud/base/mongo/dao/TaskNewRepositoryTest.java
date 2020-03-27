/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao;

import com.openiot.cloud.base.Application;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.TaskNew;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class}, properties = {"mongo.db = test_openiot"})
public class TaskNewRepositoryTest {
  @Autowired
  private TaskNewRepository taskRepo;

  @Before
  public void setup() throws Exception {
    taskRepo.deleteAll();
  }

  @After
  public void tearDown() throws Exception {
    // taskRepo.deleteAll();
  }

  @Test
  public void testBasic() throws Exception {
    // generate a task to sync projectID to ams
    createTask("amsClientProjectIdChangeMonitor",
               ConstDef.EVENT_TYPE_CFG_SYNC,
               "nothing",
               ConstDef.EVENT_TARGET_TYPE_DEVICE,
               "mongo",
               ConstDef.DAY_SECONDS,
               "fruit".getBytes(),
               null);

    createTask("CFG_MONITOR",
               ConstDef.EVENT_TYPE_CFG_SYNC,
               null,
               ConstDef.EVENT_TARGET_TYPE_GROUP,
               "group_lab",
               ConstDef.DAY_SECONDS,
               " ".getBytes(),
               null);

    TaskNew task =
        taskRepo.findTopByMonitorNameOrderByCreateTimeAsc("amsClientProjectIdChangeMonitor");

    assertThat(task).isNotNull()
                    .hasFieldOrPropertyWithValue("targetId", "mongo")
                    .hasFieldOrPropertyWithValue("monitorName", "amsClientProjectIdChangeMonitor");
  }

  private boolean createTask(String monitorName, String eventType, String desc, String targetType,
                             String targetId, int lifeTime, byte[] data, String fmt) {

    if ((ConstDef.EVENT_TASK_OPTION_IGNORE).equals(ConstDef.EVENT_TASK_OPTION_APPEND)) {
      List<TaskNew> tasks =
          taskRepo.findByMonitorNameAndEventTypeAndTargetTypeAndTargetId(monitorName,
                                                                         eventType,
                                                                         targetType,
                                                                         targetId);
      if (tasks != null && tasks.size() > 0) {
        return true;
      }
    } else if ((ConstDef.EVENT_TASK_OPTION_OVERWRITE).equals(ConstDef.EVENT_TASK_OPTION_APPEND)) {
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
}
