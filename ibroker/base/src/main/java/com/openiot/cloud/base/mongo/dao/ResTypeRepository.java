/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao;

import com.openiot.cloud.base.mongo.model.ResourceType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResTypeRepository extends MongoRepository<ResourceType, String> {
  ResourceType findOneByName(String name);

  List<ResourceType> findByName(String name, Pageable pageRequest);
}
