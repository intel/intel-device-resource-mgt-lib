/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao.custom;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.ResProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import org.springframework.data.mongodb.core.query.Query;
import java.util.List;
import java.util.Optional;

public class ResProRepositoryImpl implements ResProRepositoryCustom {
  @Autowired
  private MongoOperations monOp;

  @Override
  public List<ResProperty> filter(String devId, String resUrl, String propName, Boolean implemented,
                                  Pageable pageable) {
    Query q = new Query();

    Optional.ofNullable(devId).ifPresent(di -> q.addCriteria(where(ConstDef.F_DEVID).is(di)));

    Optional.ofNullable(resUrl).ifPresent(url -> q.addCriteria(where(ConstDef.F_RES).is(url)));

    Optional.ofNullable(propName).ifPresent(name -> q.addCriteria(where(ConstDef.F_NAME).is(name)));

    Optional.ofNullable(implemented)
            .ifPresent(impl -> q.addCriteria(where(ConstDef.F_IMPLED).is(impl)));

    pageable = pageable == null ? PageRequest.of(0, ConstDef.DFLT_SIZE) : pageable;
    q.with(pageable);

    return monOp.find(q, ResProperty.class);
  }
}
