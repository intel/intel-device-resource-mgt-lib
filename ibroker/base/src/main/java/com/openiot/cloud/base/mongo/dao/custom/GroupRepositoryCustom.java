/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao.custom;

import com.openiot.cloud.base.mongo.model.Group;
import com.openiot.cloud.base.mongo.model.help.DataSourceEntity;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GroupRepositoryCustom {
  // TODO: collections should not use Optional
  List<Group> filter(Optional<String> projectId, Optional<List<String>> names,
                     Optional<List<String>> gtNames, Optional<List<String>> dpNames,
                     Optional<List<String>> dsInternalIds, Optional<String> dsrUrl,
                     Optional<List<String>> devIds, Optional<Map<String, String>> resMap,
                     Optional<List<String>> withAttr, Optional<List<String>> withCfg,
                     Optional<List<String>> withDss, Optional<Map<String, String>> attrMap,
                     Optional<Map<String, String>> cfgMap, Optional<List<String>> fields,
                     Pageable pageRequest);

  // ------ RETURN GROUP
  Group findOneByName(String name);

  List<Group> findAllGroupByDevId(String devId);

  List<Group> findAllGroupByRes(String di, String uri);

  Group createOrUpdateOne(Group newGroup);

  // ------ RETURN DATASOURCEENTITY
  /**
   * Get the one data source with the given the group name + the data source name
   *
   * @param gName
   * @param dsName
   * @return a data source or null.
   */
  List<DataSourceEntity> findDssByGroupNameAndDsName(String gName, String dsName);

  /**
   * Get the one data source with the given internal id
   *
   * @param InternalId
   * @return a data source or null.
   */
  DataSourceEntity findOneDsByInternalId(String InternalId);

  Group findOneByDssInternalId(String internalId);

  // ------ RETURN RESPROPERTY

}
