/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao.custom;

import com.openiot.cloud.base.mongo.model.Device;
import com.openiot.cloud.base.mongo.model.Group;
import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface DeviceRepositoryCustom {
  public List<Device> filter(Optional<String> projectId, Optional<String> id, Optional<String> name,
                             Optional<String> standard, Optional<String> devType,
                             Optional<String> grpName, Optional<String> agentId,
                             Optional<String> folder, Optional<Boolean> connected, Boolean enabled,
                             Optional<String> searchString, Optional<AttributeEntity> attribute,
                             Pageable pageable);
}
