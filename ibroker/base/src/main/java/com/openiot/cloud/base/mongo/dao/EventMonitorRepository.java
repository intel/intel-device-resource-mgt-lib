/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao;

import com.openiot.cloud.base.mongo.dao.custom.EventMonitorRepositoryCustom;
import com.openiot.cloud.base.mongo.model.EventMonitor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

public interface EventMonitorRepository
    extends MongoRepository<EventMonitor, String>, EventMonitorRepositoryCustom {
  EventMonitor findOneByName(String name);
}
