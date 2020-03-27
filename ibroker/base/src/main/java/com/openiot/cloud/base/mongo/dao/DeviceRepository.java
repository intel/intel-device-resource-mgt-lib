/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao;

import com.openiot.cloud.base.mongo.dao.custom.DeviceRepositoryCustom;
import com.openiot.cloud.base.mongo.model.Device;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DeviceRepository extends MongoRepository<Device, String>, DeviceRepositoryCustom {
  Device findOneById(String id);

  Device findOneByName(String name);

  List<Device> findByFolderId(String folderd);

  List<Device> findByNameAllIgnoreCase(String name);

  List<Device> findByFolderIdAndNameAllIgnoreCase(String folderId, String name);

  List<Device> findByNameContainingAllIgnoreCase(String nameFraction);

  List<Device> findByIAgentId(String iagentID);

  List<Device> findByPrj(String projectID);

  List<Device> findByStandard(String standard);

  List<Device> findByConnected(boolean connectedStatus);
}
