/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao.custom;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.GroupType;
import com.openiot.cloud.base.mongo.model.validator.CreateValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import javax.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class GroupTypeRepositoryImpl implements GroupTypeRepositoryCustom {

  private static Logger logger = LoggerFactory.getLogger(GroupTypeRepositoryImpl.class);

  @Autowired
  MongoTemplate mtemplate;

  @Autowired
  Validator validator;

  @Override
  public List<GroupType> filter(Optional<String> project, Optional<String> name,
                                Optional<String> displayName, List<String> withAttr,
                                List<String> withCfg, List<String> withDs,
                                Map<String, String> attrMap, Map<String, String> cfgMap,
                                List<String> fields, Pageable pageRequest) {
    List<AggregationOperation> operations = new ArrayList<>();

    // $match : {"prj" : project}
    if (!name.isPresent()) {
      project.ifPresent(prj -> operations.add(Aggregation.match("null".equals(prj)
          ? where(ConstDef.F_PROJECT).is(null)
          : new Criteria().orOperator(where(ConstDef.F_PROJECT).is(prj),
                                      where(ConstDef.F_PROJECT).is(null)))));
    }

    // $match : {"n" : name}
    name.ifPresent(n -> operations.add(Aggregation.match(where(ConstDef.F_NAME).is(n))));

    // $match : {"dpn" : displayName}
    displayName.ifPresent(dpn -> operations.add(Aggregation.match(where(ConstDef.F_DISPLAYNAME).is(dpn))));

    // Criteria for $match
    List<Criteria> mpara = new ArrayList<>();

    // $match : {"as.an" : { $in : attrs}}
    if (!withAttr.isEmpty()) {
      mpara.add(where(String.format("%s.%s", ConstDef.F_ATTRS, ConstDef.F_ATTRNAME)).in(withAttr));
    }

    // $match : {"cs.cn" : { $in : cfgs}}
    if (!withCfg.isEmpty()) {
      mpara.add(where(String.format("%s.%s",
                                    ConstDef.F_CONFIGS,
                                    ConstDef.F_CONFIGNAME)).in(withCfg));
    }
    // $match : {"dss.dsn" : { $in : dss}}
    if (!withDs.isEmpty()) {
      mpara.add(where(String.format("%s.%s",
                                    ConstDef.F_DATASOURCES,
                                    ConstDef.F_DATASOURCENAME)).in(withDs));
    }

    if (!attrMap.isEmpty()) {
      attrMap.forEach((k, v) -> {
        if (!BaseUtil.isNonequivalentQuery(k, v)) {
          // $match : { "as": { $elemMetch : {"an" : key, "av" :
          // value} } },
          mpara.add(where(ConstDef.F_ATTRS).elemMatch(where(ConstDef.F_ATTRNAME).is(k)
                                                                                .and(ConstDef.F_ATTRVALUE)
                                                                                .is(v)));
        } else {
          mpara.add(getNonequivalentQueryForAttrAndCfg(ConstDef.F_ATTRS,
                                                       ConstDef.F_ATTRNAME,
                                                       ConstDef.F_ATTRVALUE,
                                                       k,
                                                       v));
        }
      });

    }

    if (!cfgMap.isEmpty()) {
      cfgMap.forEach((k, v) -> {
        if (!BaseUtil.isNonequivalentQuery(k, v)) {
          // $match : { "cs": { $elemMetch : {"cn" : key, "cv" :
          // value} } },
          mpara.add(where(ConstDef.F_CONFIGS).elemMatch(where(ConstDef.F_CONFIGNAME).is(k)
                                                                                    .and(ConstDef.F_CONFIGVALUE)
                                                                                    .is(v)));
        } else {
          mpara.add(getNonequivalentQueryForAttrAndCfg(ConstDef.F_CONFIGS,
                                                       ConstDef.F_CONFIGNAME,
                                                       ConstDef.F_CONFIGVALUE,
                                                       k,
                                                       v));
        }
      });

    }

    // $match
    if (!mpara.isEmpty()) {
      operations.add(Aggregation.match(new Criteria().andOperator(mpara.toArray(new Criteria[mpara.size()]))));
    }

    // $skip and $limit
    Optional.ofNullable(pageRequest).ifPresent(p -> {
      operations.add(Aggregation.skip(p.getOffset()));
      operations.add(Aggregation.limit(p.getPageSize()));
    });

    Aggregation agg =
        Aggregation.newAggregation(operations.toArray(new AggregationOperation[operations.size()]));

    logger.debug("query in meta/grptype: " + agg);

    List<GroupType> full =
        mtemplate.aggregate(agg, GroupType.class, GroupType.class).getMappedResults();

    // $project
    full = filterFields(full, fields);

    // pageable
    return full;
  }

  // set some fileds of group to null
  List<GroupType> filterFields(List<GroupType> full, List<String> fields) {
    full.forEach(gt -> {
      if (!fields.contains(ConstDef.F_DESCRIPTION)) {
        gt.setD(null);
      }

      if (!fields.contains(ConstDef.F_ATTRS)) {
        gt.setAs(null);
      }

      if (!fields.contains(ConstDef.F_CONFIGS)) {
        gt.setCs(null);
      }

      if (!fields.contains(ConstDef.F_DATASOURCES)) {
        gt.setDss(null);
      }

      if (!fields.contains(ConstDef.F_DEVICE_PLAN)) {
        gt.setDevicePlan(null);
      }
    });

    return full;
  }

  private Criteria getNonequivalentQueryForAttrAndCfg(String item, String keyInItem,
                                                      String valueInItem, String queryKey,
                                                      String queryValue) {

    Function<String, Function<String, Criteria>> query = null;

    if (queryValue.startsWith("lt")) {
      query = key -> value -> where(key).lt(value);
      queryValue = queryValue.substring(2);
    }

    if (queryValue.startsWith("gt")) {
      query = key -> value -> where(key).gt(value);
      queryValue = queryValue.substring(2);
    }

    if (queryValue.startsWith("gte")) {
      query = key -> value -> where(key).gte(value);
      queryValue = queryValue.substring(3);
    }

    if (queryValue.startsWith("lte")) {
      query = key -> value -> where(key).lte(value);
      queryValue = queryValue.substring(3);
    }

    if (queryValue.startsWith("ne")) {
      query = key -> value -> where(key).ne(value);
      queryValue = queryValue.substring(2);
    }

    return where(item).elemMatch(new Criteria().andOperator(where(keyInItem).is(queryKey),
                                                            query.apply(valueInItem)
                                                                 .apply(queryValue)));
  }

  @Override
  public GroupType createOrUpdateOne(GroupType newGroupType) {
    Optional<GroupType> groupType = Optional.ofNullable(newGroupType);
    Optional<Map<String, String>> valResult =
        groupType.map(gt -> BaseUtil.validate(validator, gt, CreateValidator.class))
                 .filter(result -> !result.isEmpty());
    return groupType.map(gt -> {
      if (valResult.isPresent()) {
        return null;
      }
      mtemplate.save(gt, ConstDef.C_GRPTYPE);
      return gt;
    }).orElse(null);
  }
}
