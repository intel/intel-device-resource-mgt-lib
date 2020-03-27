/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao;

import com.openiot.cloud.base.mongo.model.TaskNew;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

public interface TaskNewRepository extends MongoRepository<TaskNew, String> {

  public TaskNew findTopByMonitorNameOrderByCreateTimeAsc(String monitorName);

  public List<TaskNew>
      findByMonitorNameAndEventTypeAndTargetTypeAndTargetId(String monitorName, String eventType,
                                                            String targetType, String targetId);

  public List<TaskNew>
      removeByMonitorNameAndEventTypeAndTargetTypeAndTargetId(String monitorName, String eventType,
                                                              String targetType, String targetId);

  public TaskNew findOneById(String id);
}
