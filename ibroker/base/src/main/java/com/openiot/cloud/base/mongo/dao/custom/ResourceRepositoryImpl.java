/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao.custom;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.Group;
import com.openiot.cloud.base.mongo.model.Group.MemberResRef;
import com.openiot.cloud.base.mongo.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ResourceRepositoryImpl implements ResourceRepositoryCustom {
  static class ResouceSortHelper implements Comparable<ResouceSortHelper> {
    int matchedCharacterCount;
    Resource res;

    public ResouceSortHelper(Resource res, int matchedCharacterCount) {
      this.res = res;
      this.matchedCharacterCount = matchedCharacterCount;
    }

    @Override
    public int compareTo(ResouceSortHelper rsh) {
      if (rsh.matchedCharacterCount > this.matchedCharacterCount) {
        return -1;
      } else if (rsh.matchedCharacterCount == this.matchedCharacterCount) {
        return 0;
      } else {
        return 1;
      }
    }
  }

  @Autowired
  MongoOperations monOp;

  @Override
  public List<Resource> filter(String devId, String resName, String resUrl, String resType,
                               String group, Pageable pageable) {
    Query q = new Query();

    Optional.ofNullable(devId).ifPresent(di -> q.addCriteria(where(ConstDef.F_DEVID).is(di)));

    Optional.ofNullable(resName).ifPresent(n -> q.addCriteria(where(ConstDef.F_NAME).is(n)));

    Optional.ofNullable(resUrl).ifPresent(url -> q.addCriteria(where(ConstDef.F_URL).is(url)));

    Optional.ofNullable(resType).ifPresent(rt -> q.addCriteria(where(ConstDef.F_RESTYPE).is(rt)));

    Optional.ofNullable(group).ifPresent(grp -> q.addCriteria(where(ConstDef.F_GRPS).is(grp)));

    Optional.ofNullable(pageable).ifPresent(p -> q.with(p));

    return monOp.find(q, Resource.class);
  }

  @Override
  public List<Resource> findAllResourcesByGroup(Group group) {
    Optional<List<MemberResRef>> risOpt = Optional.ofNullable(group.getMr());

    return risOpt.map(ris -> {
      List<String> devIds = ris.stream().map(ri -> ri.getDi()).collect(Collectors.toList());
      List<String> resUrls = ris.stream().map(ri -> ri.getUri()).collect(Collectors.toList());

      return monOp.find(Query.query(Criteria.where(ConstDef.F_DEVID)
                                            .in(devIds)
                                            .and(ConstDef.F_URL)
                                            .in(resUrls)),
                        Resource.class,
                        ConstDef.C_RES);
    }).orElse(Collections.emptyList());
  }
}
