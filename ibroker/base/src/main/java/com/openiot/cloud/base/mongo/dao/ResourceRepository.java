/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao;

import com.openiot.cloud.base.mongo.dao.custom.ResourceRepositoryCustom;
import com.openiot.cloud.base.mongo.model.Resource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResourceRepository
    extends MongoRepository<Resource, String>, ResourceRepositoryCustom {

  Resource findOneById(String id);

  Resource findOneByName(String name);

  Resource findOneByUrl(String url);

  Resource findOneByFullUrl(String fullurl);

  Resource findOneByDevIdAndUrl(String devId, String url);

  List<Resource> findAllByDevId(String devId);

  @Query("{'rt': ?0}")
  List<Resource> findAllByResType(String resourceType);
}
