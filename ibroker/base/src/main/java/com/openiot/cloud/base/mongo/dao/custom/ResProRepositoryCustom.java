/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao.custom;

import com.openiot.cloud.base.mongo.model.ResProperty;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ResProRepositoryCustom {
  List<ResProperty> filter(String devId, String resUrl, String propName, Boolean implemented,
                           Pageable pageable);
}
