/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao;

import com.openiot.cloud.base.mongo.model.Folder;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FolderRepository extends MongoRepository<Folder, String> {

  List<Folder> findAllByParent(String parent);

  Folder findOneById(String id);

  Folder findByName(String name);

  List<Folder> findAllByOrderByDepthAsc();

  List<Folder> findByDepthGreaterThanOrderByDepthAsc(int depGT);
}
