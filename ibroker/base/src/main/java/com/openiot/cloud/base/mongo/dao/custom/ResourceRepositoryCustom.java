/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao.custom;

import com.openiot.cloud.base.mongo.model.Group;
import com.openiot.cloud.base.mongo.model.Resource;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ResourceRepositoryCustom {

  List<Resource> filter(String devId, String resName, String resurl, String resType, String group,
                        Pageable pageable);

  /**
   * Get all resouce in the given group
   *
   * @param group
   * @return
   */
  public List<Resource> findAllResourcesByGroup(Group group);
}
