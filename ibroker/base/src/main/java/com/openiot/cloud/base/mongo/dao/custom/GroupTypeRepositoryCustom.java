/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao.custom;

import com.openiot.cloud.base.mongo.model.GroupType;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GroupTypeRepositoryCustom {

  List<GroupType> filter(Optional<String> project, Optional<String> name,
                         Optional<String> displayName, List<String> withAttr, List<String> withCfg,
                         List<String> withDs, Map<String, String> attrMap,
                         Map<String, String> cfgMap, List<String> fields, Pageable pageRequest);

  GroupType createOrUpdateOne(GroupType newGroupType);
}
