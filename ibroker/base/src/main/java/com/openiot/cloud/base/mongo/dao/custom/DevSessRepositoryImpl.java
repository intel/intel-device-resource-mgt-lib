/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao.custom;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.DevSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import org.springframework.data.mongodb.core.query.Query;
import java.util.List;
import java.util.Optional;

public class DevSessRepositoryImpl implements DevSessRepositoryCustom {
  @Autowired
  private MongoOperations monOp;

  @Override
  public List<DevSession> filter(String id, String devId, Boolean ended, long from, long to,
                                 Pageable pageable) {
    Query q = new Query();

    Optional.ofNullable(id).ifPresent(i -> q.addCriteria(where(ConstDef.F_ID).is(i)));

    Optional.ofNullable(devId).ifPresent(di -> q.addCriteria(where(ConstDef.F_DEV).is(di)));

    // from and to have default values
    // ended parameter adjust to
    from = from == 0 ? BaseUtil.getStartOfTodayAsEpochMillis() : from;
    to = to == 0 ? BaseUtil.getNowAsEpochMillis() : to;

    // ended parameter adjust to
    if (ended != null && ended.booleanValue()) {
      to = BaseUtil.getNowAsEpochMillis();
    }

    // from <= "b" <= to or from <= "e" <= to
    q.addCriteria(new Criteria().orOperator(where(ConstDef.F_BEGEIN).gte(from)
                                                                    .andOperator(where(ConstDef.F_BEGEIN).lte(to)),
                                            where(ConstDef.F_END).gte(from)
                                                                 .andOperator(where(ConstDef.F_END).lte(to))));

    pageable = pageable == null ? PageRequest.of(0, ConstDef.DFLT_SIZE) : pageable;
    q.with(pageable);

    return monOp.find(q, DevSession.class);
  }
}
