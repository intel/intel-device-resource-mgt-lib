/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao.custom;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.Device;
import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class DeviceRepositoryImpl implements DeviceRepositoryCustom {
  private static final Logger logger = LoggerFactory.getLogger(DeviceRepositoryImpl.class);
  @Autowired
  MongoOperations monOp;
  @Autowired
  MongoTemplate mTemplate;

  @Override
  // TODO: remove the parameter enabled
  public List<Device> filter(Optional<String> projectId, Optional<String> id, Optional<String> name,
                             Optional<String> standard, Optional<String> devType,
                             Optional<String> grpName, Optional<String> agentId,
                             Optional<String> folderId, Optional<Boolean> connected,
                             Boolean enabled, Optional<String> searchString,
                             Optional<AttributeEntity> attribute, Pageable pageable) {
    Query q = new Query();

    if (!id.isPresent()) {
      projectId.ifPresent(iD -> q.addCriteria("null".equals(iD) ? where(ConstDef.F_PROJECT).is(null)
          : where(ConstDef.F_PROJECT).is(iD)));
    }

    id.ifPresent(i -> q.addCriteria(where(ConstDef.F_ID).is(i)));

    name.ifPresent(n -> q.addCriteria(where(ConstDef.F_NAME).is(n)));

    searchString.ifPresent(n -> q.addCriteria(where(ConstDef.F_NAME).regex(n + ".*", "si")));

    grpName.ifPresent(gn -> q.addCriteria(where(ConstDef.F_GRPS).is(gn)));

    standard.ifPresent(st -> q.addCriteria(where(ConstDef.F_STAND).is(st)));

    devType.ifPresent(dt -> q.addCriteria(where(ConstDef.F_DEVTYPE).is(dt)));

    agentId.ifPresent(ai -> q.addCriteria(where(ConstDef.F_IAGENT).is(ai)));

    folderId.ifPresent(fi -> q.addCriteria(where(ConstDef.F_FOLDER).is(fi)));

    connected.ifPresent(c -> q.addCriteria(where(ConstDef.F_CONNED).is(c)));

    q.addCriteria(where(ConstDef.F_ENABLED).is(enabled));

    attribute.ifPresent(attr -> q.addCriteria(where(String.format("%s.%s",
                                                                  ConstDef.F_CONFIGS,
                                                                  ConstDef.F_ATTRS)).elemMatch(Criteria.where(ConstDef.F_ATTRNAME)
                                                                                                       .is(attr.getAn())
                                                                                                       .and(ConstDef.F_ATTRVALUE)
                                                                                                       .is(attr.getAv()))));

    q.with(pageable);

    logger.info("query " + q);

    return monOp.find(q, Device.class);
  }

}
