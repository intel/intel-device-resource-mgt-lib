/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao;

import com.openiot.cloud.base.mongo.dao.custom.ResProRepositoryCustom;
import com.openiot.cloud.base.mongo.model.ResProperty;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ResProRepository
    extends MongoRepository<ResProperty, String>, ResProRepositoryCustom {
  ResProperty findOneById(String id);

  List<ResProperty> findAllByDevId(String devId);

  ResProperty findOneByDevIdAndResAndName(String devId, String res, String name);

  @Deprecated
  ResProperty findOneByFullUrl(String fullurl);

  List<ResProperty> findByDevIdAndRes(String devId, String res);

  List<ResProperty> findByResAndName(String res, String name);

  List<ResProperty> findByDevIdAndResAndName(String devId, String res, String name);
}
