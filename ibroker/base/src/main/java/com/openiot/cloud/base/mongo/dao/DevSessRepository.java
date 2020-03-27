/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao;

import com.openiot.cloud.base.mongo.dao.custom.DevSessRepositoryCustom;
import com.openiot.cloud.base.mongo.model.DevSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

public interface DevSessRepository
    extends MongoRepository<DevSession, String>, DevSessRepositoryCustom {

  DevSession findTopByDevIdOrderByIdDesc(String devId);

  DevSession findOneById(String id);

  List<DevSession> findAllByDevId(String devId);
}
