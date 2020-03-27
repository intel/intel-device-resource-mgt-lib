/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao;

import com.openiot.cloud.base.mongo.dao.custom.AlarmRepositoryCustom;
import com.openiot.cloud.base.mongo.model.Alarm;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlarmRepository extends MongoRepository<Alarm, String>, AlarmRepositoryCustom {

  @Query("{'project':?0, 'alarmid':?1, 'targettype':?2, 'targetid':?3, 'settime':?4, 'group':?5}")
  Alarm findOneByAlarmidAndTargettypeAndTargetidAndSet_t(String project, String aid, String tt,
                                                         String tid, Long bt, String group);

  // @DeleteQuery("{'project':?0}")
  List<Alarm> deleteByProject(String project);

  List<Alarm> findAllByProject(String project);

  Alarm findOneById(String id);
}
