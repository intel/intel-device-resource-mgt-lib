/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.repository;

import com.openiot.cloud.projectcenter.repository.document.Project;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ProjectRepository extends MongoRepository<Project, String> {
  List<Project> findByName(String name);

  List<Project> findByUserName(String name);

  List<Project> findByIdAndUserName(String projectId, String userName);
}
