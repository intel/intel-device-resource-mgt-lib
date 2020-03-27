/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao;

import com.openiot.cloud.base.mongo.dao.custom.GroupTypeRepositoryCustom;
import com.openiot.cloud.base.mongo.model.GroupType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupTypeRepository
    extends MongoRepository<GroupType, String>, GroupTypeRepositoryCustom {
  GroupType findOneByN(String n);
}
