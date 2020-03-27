/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao;

import com.openiot.cloud.base.mongo.dao.custom.GroupRepositoryCustom;
import com.openiot.cloud.base.mongo.model.Group;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface GroupRepository extends MongoRepository<Group, String>, GroupRepositoryCustom {
  // be careful, it will not return all extended content
  List<Group> findByGt(String groupTypeName);

  // be careful, it will not return all extended content
  List<Group> findByPrj(String projectId);
}
