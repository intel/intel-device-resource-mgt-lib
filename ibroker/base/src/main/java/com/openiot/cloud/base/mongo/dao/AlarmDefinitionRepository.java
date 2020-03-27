/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao;

import com.openiot.cloud.base.mongo.dao.custom.AlarmDefinitionRepositoryCustom;
import com.openiot.cloud.base.mongo.model.AlarmDefinition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlarmDefinitionRepository
    extends MongoRepository<AlarmDefinition, String>, AlarmDefinitionRepositoryCustom {

  AlarmDefinition findOneByAid(String aid);
}
