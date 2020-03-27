/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao.custom;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.dao.GroupTypeRepository;
import com.openiot.cloud.base.mongo.model.Group;
import com.openiot.cloud.base.mongo.model.GroupType;
import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import com.openiot.cloud.base.mongo.model.help.DataSourceEntity;
import com.openiot.cloud.base.mongo.model.validator.CreateValidator;
import com.openiot.cloud.base.mongo.model.validator.UpdateValidator;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import javax.validation.Validator;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GroupRepositoryImpl implements GroupRepositoryCustom {
  private static final Logger logger = LoggerFactory.getLogger(GroupRepositoryImpl.class.getName());
  @Autowired
  GroupTypeRepository gtRepo;

  @Autowired
  MongoTemplate mtemplate;

  @Autowired
  Validator validator;

  // if grouptype and group have the same cs, ignore it
  // if only grouptype has such cs, copy it
  // if only group has such cs, ignore it
  public static List<Group> mergeCfgsAndDss(List<Group> groups) {
    groups.forEach(g -> {
      Optional.ofNullable(g.getGtdtl())
              .map(gtdl -> gtdl.getCs())
              .filter(gtcs -> !gtcs.isEmpty())
              .ifPresent(gtcs -> {
                List<ConfigurationEntity> configurationListGroup = g.getCs();
                gtcs.stream()
                    .filter(configurationGroupType -> !configurationListGroup.contains(configurationGroupType))
                    .forEach(g::insertOrUpdateCs);
              });

      // merge attributes in the data source and the data source template
      Optional.ofNullable(g.getGtdtl())
              .map(gtdl -> gtdl.getDss())
              .filter(gtDss -> !gtDss.isEmpty())
              .ifPresent(gtDss -> {
                // different attributes in same name data sources
                List<DataSourceEntity> gDss = g.getDss();
                if (gDss == null) {
                  System.out.println("get null dss !");
                  return;
                }
                for (DataSourceEntity dataSourceGroup : gDss) {
                  System.out.println("merging dss " + dataSourceGroup.getDsn());
                  for (DataSourceEntity dataSourceGroupType : gtDss) {
                    if (dataSourceGroup.getDsn().equals(dataSourceGroupType.getDsn())) {
                      mergeDss(dataSourceGroup, dataSourceGroupType);
                      break;
                    }
                  }
                }
              });

      Optional.ofNullable(g.getGtdtl())
              .map(gtdl -> gtdl.getDss())
              .filter(gtdss -> !gtdss.isEmpty())
              .ifPresent(gtDss -> {
                List<DataSourceEntity> dataSourceListGroup = g.getDss();
                if (dataSourceListGroup == null)
                  return;
                gtDss.stream()
                     .filter(dataSourceGroupType -> !dataSourceListGroup.contains(dataSourceGroupType))
                     .forEach(g::insertOrUpdateDss);
              });
    });

    return groups;
  }

  private static <T> List<T> merge(Optional<List<T>> gItemList, List<T> gtItemList) {
    return gItemList.map(gItems -> {
      List<T> inherent = gtItemList.stream()
                                   .filter(gtItem -> !gItems.contains(gtItem))
                                   .collect(Collectors.toList());
      gItems.addAll(inherent);
      return gItems;
    }).orElse(gtItemList);
  }

  /**
   * every item in parentList but not in childList will be added into the return every item in
   * childList will be added into the return
   *
   * @param childList
   * @param parentList
   * @param <T>
   * @return
   */
  private static <T> List<T> assembleList(List<T> childList, List<T> parentList) {
    if (childList == null && parentList == null) {
      return Collections.emptyList();
    }

    if (childList == null || parentList == null) {
      return childList == null ? new LinkedList<>(parentList) : new LinkedList<>(childList);
    }

    if (childList.isEmpty() && parentList.isEmpty()) {
      return Collections.emptyList();
    }

    if (childList.isEmpty() || parentList.isEmpty()) {
      return childList.isEmpty() ? new LinkedList<>(parentList) : new LinkedList<>(childList);
    }

    List<T> result = new LinkedList<>(childList);
    parentList.stream().filter(i -> !childList.contains(i)).forEach(result::add);
    return result;
  }

  private List<Group> filterWithFields(List<Group> full, Optional<List<String>> fields) {
    full.forEach(g -> {
      // clean unnecessary fields
      if (!(fields.map(f -> f.contains(ConstDef.F_DESCRIPTION)).orElse(false))) {
        g.setD(null);
      }

      if (!(fields.map(f -> f.contains(ConstDef.U_MBR)).orElse(false))) {
        g.setMr(null);
        g.setMd(null);
      }

      if (!(fields.map(f -> f.contains(ConstDef.F_ATTRS)).orElse(false))) {
        g.setAs(null);
      }

      if (!(fields.map(f -> f.contains(ConstDef.F_CONFIGS)).orElse(false))) {
        g.setCs(null);
      }

      if (!(fields.map(f -> f.contains(ConstDef.F_DATASOURCES)).orElse(false))) {
        g.setDss(null);
      }

      g.setGtdtl(null);
    });

    return full;
  }

  @Override
  public List<Group> filter(Optional<String> projectId, Optional<List<String>> names,
                            Optional<List<String>> gtNames, Optional<List<String>> dpNames,
                            Optional<List<String>> dsInternalIds, Optional<String> dsrUrl,
                            Optional<List<String>> devIds, Optional<Map<String, String>> resMap,
                            Optional<List<String>> withAttr, Optional<List<String>> withCfg,
                            Optional<List<String>> withDs, Optional<Map<String, String>> attrMap,
                            Optional<Map<String, String>> cfgMap, Optional<List<String>> fields,
                            Pageable pageRequest) {
    log.debug("in filter");
    List<AggregationOperation> operations = new ArrayList<>();

    // $match : {"project" : {$is : projectId}}
    projectId.ifPresent(proj -> operations.add(Aggregation.match(where(ConstDef.F_PROJECT).is(proj))));

    // $match : {"n" : {$in : names}}
    names.filter(ns -> !ns.isEmpty())
         .ifPresent(ns -> operations.add(Aggregation.match(where(ConstDef.F_ID).in(ns))));

    // $match : {"gt" : {$in : gtNames}}
    gtNames.filter(gtns -> !gtns.isEmpty())
           .ifPresent(gtns -> operations.add(Aggregation.match(where(ConstDef.F_GRPTYPE).in(gtns))));

    // $match : {"dpn" : {$in : dpNames}}
    dpNames.filter(dpns -> !dpns.isEmpty())
           .ifPresent(dpns -> operations.add(Aggregation.match(where(ConstDef.F_DISPLAYNAME).in(dpns))));

    // $lookup : {from : "GroupType", localField : "gt", foreignField :
    // "_id", as : "gtdtl"
    operations.add(Aggregation.lookup(ConstDef.C_GRPTYPE,
                                      ConstDef.F_GRPTYPE,
                                      ConstDef.F_ID,
                                      ConstDef.F_GTDETAIL));

    // $unwind : "$gtdtl"
    operations.add(Aggregation.unwind(ConstDef.F_GTDETAIL, true));

    // match operations
    List<Criteria> mpara = new ArrayList<>();

    // $match : { $or : [{"dss.dsintId" : {$in : dsintids},
    // {"gtdtl.dss.dsintId" : {$in :
    // dsintids}}]}
    dsInternalIds.filter(dsintids -> !dsintids.isEmpty())
                 .ifPresent(dsintids -> mpara.add(new Criteria().orOperator(where(String.format("%s.%s",
                                                                                                ConstDef.F_DATASOURCES,
                                                                                                ConstDef.F_DATASOURCEINTLID)).in(dsintids),
                                                                            where(String.format("%s.%s.%s",
                                                                                                ConstDef.F_GTDETAIL,
                                                                                                ConstDef.F_DATASOURCES,
                                                                                                ConstDef.F_DATASOURCEINTLID)).in(dsintids))));

    // $match : {"dss.dsdefs.dsrurl" : url}
    dsrUrl.ifPresent(url -> mpara.add(where(String.format("%s.%s.%s",
                                                          ConstDef.F_DATASOURCES,
                                                          ConstDef.F_DATASOURCEDEFS,
                                                          ConstDef.F_DATASOURCEREFNAME)).is(url)));

    // $match : {"md" : {$in : devIds}}
    devIds.filter(devs -> !devs.isEmpty())
          .ifPresent(devs -> operations.add(Aggregation.match(where(ConstDef.F_MD).in(devs))));

    // $match : {"mr" : { $elematch : {"di" : k, "uri" : v} } }
    resMap.filter(reses -> !reses.isEmpty()).ifPresent(map -> {
      map.forEach((k, v) -> {
        mpara.add(where(ConstDef.F_MR).elemMatch(where(ConstDef.F_DEVID).is(k)
                                                                        .and(ConstDef.F_RESURI)
                                                                        .is(v)));
      });
    });

    // $match : { $or : [{"as.an" : {$in : attrs}, {"gtdtl.as.an" : {$in :
    // attrs}}]}
    withAttr.filter(attrs -> !attrs.isEmpty())
            .ifPresent(attrs -> mpara.add(new Criteria().orOperator(where(String.format("%s.%s",
                                                                                        ConstDef.F_ATTRS,
                                                                                        ConstDef.F_ATTRNAME)).in(attrs),
                                                                    where(String.format("%s.%s.%s",
                                                                                        ConstDef.F_GTDETAIL,
                                                                                        ConstDef.F_ATTRS,
                                                                                        ConstDef.F_ATTRNAME)).in(attrs))));

    // $match : { $or : [{"cs.cn" : {$in : cfgs}, {"gtdtl.cs.cn" : {$in :
    // cfgs}}]}
    withCfg.filter(cfgs -> !cfgs.isEmpty())
           .ifPresent(cfgs -> mpara.add(new Criteria().orOperator(where(String.format("%s.%s",
                                                                                      ConstDef.F_CONFIGS,
                                                                                      ConstDef.F_CONFIGNAME)).in(cfgs),
                                                                  where(String.format("%s.%s.%s",
                                                                                      ConstDef.F_GTDETAIL,
                                                                                      ConstDef.F_CONFIGS,
                                                                                      ConstDef.F_CONFIGNAME)).in(cfgs))));

    // $match : { $or : [{"dss.dsn" : {$in : dss}, {"gtdtl.dss.dsn" : {$in :
    // dss}}]}
    withDs.filter(dss -> !dss.isEmpty())
          .ifPresent(dss -> mpara.add(new Criteria().orOperator(where(String.format("%s.%s",
                                                                                    ConstDef.F_DATASOURCES,
                                                                                    ConstDef.F_DATASOURCENAME)).in(dss),
                                                                where(String.format("%s.%s.%s",
                                                                                    ConstDef.F_GTDETAIL,
                                                                                    ConstDef.F_DATASOURCES,
                                                                                    ConstDef.F_DATASOURCENAME)).in(dss))));

    // for entry in attrMap.entrySet()
    // $match : { $or : [
    // { "as": { $elemMetch : {"an" : key, "av" : value} } },
    // { "gtdtl.as": { $elemMetch : {"an" : key, "av" : value} } }
    /// ]}
    attrMap.filter(map -> !map.isEmpty()).ifPresent(map -> {
      map.forEach((k, v) -> {
        if (!BaseUtil.isNonequivalentQuery(k, v)) {
          mpara.add(new Criteria().orOperator(where(ConstDef.F_ATTRS).elemMatch(where(ConstDef.F_ATTRNAME).is(k)
                                                                                                          .and(ConstDef.F_ATTRVALUE)
                                                                                                          .is(v)),
                                              where(String.format("%s.%s",
                                                                  ConstDef.F_GTDETAIL,
                                                                  ConstDef.F_ATTRS)).elemMatch(where(ConstDef.F_ATTRNAME).is(k)
                                                                                                                         .and(ConstDef.F_ATTRVALUE)
                                                                                                                         .is(v))));
        } else {
          mpara.add(getNonequivalentQueryForAttrAndCfg(ConstDef.F_ATTRS,
                                                       String.format("%s.%s",
                                                                     ConstDef.F_GTDETAIL,
                                                                     ConstDef.F_ATTRS),
                                                       ConstDef.F_ATTRNAME,
                                                       ConstDef.F_ATTRVALUE,
                                                       k,
                                                       v));
        }
      });
    });

    // $match : { $or : [
    // { "cs": { $elemMetch : {"cn" : key, "cv" : value} } },
    // { "gtdtl.cs": { $elemMetch : {"cn" : key, "cv" : value} } }
    /// ]}
    cfgMap.filter(map -> !map.isEmpty()).ifPresent(map -> {
      map.forEach((k, v) -> {
        if (!BaseUtil.isNonequivalentQuery(k, v)) {
          mpara.add(new Criteria().orOperator(where(ConstDef.F_CONFIGS).elemMatch(where(ConstDef.F_CONFIGNAME).is(k)
                                                                                                              .and(ConstDef.F_CONFIGVALUE)
                                                                                                              .is(v)),
                                              where(String.format("%s.%s",
                                                                  ConstDef.F_GTDETAIL,
                                                                  ConstDef.F_CONFIGS)).elemMatch(where(ConstDef.F_CONFIGNAME).is(k)
                                                                                                                             .and(ConstDef.F_CONFIGVALUE)
                                                                                                                             .is(v))));
        } else {
          mpara.add(getNonequivalentQueryForAttrAndCfg(ConstDef.F_CONFIGS,
                                                       String.format("%s.%s",
                                                                     ConstDef.F_GTDETAIL,
                                                                     ConstDef.F_CONFIGS),
                                                       ConstDef.F_CONFIGNAME,
                                                       ConstDef.F_CONFIGVALUE,
                                                       k,
                                                       v));
        }
      });
    });
    // $match
    if (!mpara.isEmpty()) {
      operations.add(Aggregation.match(new Criteria().andOperator(mpara.toArray(new Criteria[mpara.size()]))));
    }

    // $skip and $limit
    Optional.ofNullable(pageRequest).ifPresent(p -> {
      operations.add(Aggregation.skip(p.getOffset()));
      operations.add(Aggregation.limit(p.getPageSize()));
    });

    // the aggregate includes $lookup $unwind $match
    Aggregation agg =
        Aggregation.newAggregation(operations.toArray(new AggregationOperation[operations.size()]));

    log.debug("--> agg " + agg);

    AggregationResults<Group> result = mtemplate.aggregate(agg, ConstDef.C_GRP, Group.class);

    List<Group> full = result.getMappedResults();
    log.debug("--> full " + full);

    // merge grouptype cs with group cs, and dss
    for (Group group : full) {
      group = assembleGroup(group);
    }
    // full = mergeCfgsAndDss(full);
    log.debug("--> aft merge full " + full);

    // $project in another way
    full = filterWithFields(full, fields);
    log.debug("--> aft filter full " + full);

    return full;
  }

  @Override
  public List<Group> findAllGroupByDevId(String devId) {
    // TODO: pageable ?
    Query query = Query.query(where(ConstDef.F_MD).is(devId));
    List<Group> groupList = mtemplate.find(query, Group.class, ConstDef.C_GRP);

    for (Group group : groupList) {
      group = assembleGroup(group);
    }

    return groupList;
  }

  @Override
  public List<Group> findAllGroupByRes(String di, String uri) {
    // TODO: pageable ?
    Query query =
        Query.query(where(ConstDef.F_MR).elemMatch(where(ConstDef.F_DEVID).is(di)
                                                                          .and(ConstDef.F_RESURI)
                                                                          .is(uri)));
    List<Group> groupList = mtemplate.find(query, Group.class, ConstDef.C_GRP);

    for (Group group : groupList) {
      group = assembleGroup(group);
    }

    return groupList;
  }

  @Override
  public Group findOneByName(String name) {
    // TODO: pageable ?
    Query query = Query.query(where(ConstDef.F_ID).is(name));
    List<Group> groupList = mtemplate.find(query, Group.class, ConstDef.C_GRP);
    if (groupList.isEmpty()) {
      return null;
    }

    Group group = groupList.get(0);
    group = assembleGroup(group);

    return group;
  }

  @Override
  public List<DataSourceEntity> findDssByGroupNameAndDsName(String gName, String dsName) {
    if (gName == null) {
      return Collections.emptyList();
    }

    // TODO: pageable ?
    List<Group> groupList =
        mtemplate.find(Query.query(where(ConstDef.F_ID).is(gName)), Group.class, ConstDef.C_GRP);
    if (groupList == null || groupList.isEmpty()) {
      return Collections.emptyList();
    }

    List<DataSourceEntity> dataSourceList = new LinkedList<>();

    Group group = groupList.get(0);
    group = assembleGroup(group);
    if (dsName != null) {
      DataSourceEntity fromGroup = group.getDsByName(dsName);
      if (fromGroup != null) {
        dataSourceList.add(fromGroup);
      } else {
        // from group type
        if (group.getGt() != null) {
          List<GroupType> groupTypeList =
              mtemplate.find(Query.query(where(ConstDef.F_ID).is(group.getGt())),
                             GroupType.class,
                             ConstDef.C_GRPTYPE);
          if (groupTypeList != null) {
            GroupType groupType = groupTypeList.get(0);
            DataSourceEntity fromGroupType = groupType.getDsByName(dsName);
            if (fromGroupType != null) {
              dataSourceList.add(fromGroupType);
            }
          }
        }
      }
    } else {
      dataSourceList = group.getDss();
    }

    return dataSourceList;
  }

  @Override
  public Group createOrUpdateOne(Group newGroup) {
    Optional<Map<String, String>> valResult;
    Optional<Group> existingGroup = Optional.ofNullable(findOneByName(newGroup.getN()));
    Optional<Group> group = Optional.ofNullable(newGroup);

    if (existingGroup.isPresent()) {
      valResult = group.map(g -> BaseUtil.validate(validator, g, UpdateValidator.class))
                       .filter(result -> !result.isEmpty());
    } else {
      valResult = group.map(g -> BaseUtil.validate(validator, g, CreateValidator.class))
                       .filter(result -> !result.isEmpty());
    }

    return group.map(g -> {
      if (valResult.isPresent()) {
        logger.error(valResult.get().toString());
        return null;
      }

      mtemplate.save(g, ConstDef.C_GRP);
      return g;
    }).orElse(null);
  }

  @Override
  public DataSourceEntity findOneDsByInternalId(String internalId) {
    // TODO: pageable ?
    Query query = Query.query(where(String.format("%s.%s",
                                                  ConstDef.F_DATASOURCES,
                                                  ConstDef.F_DATASOURCEINTLID)).is(internalId));
    List<Group> groupList = mtemplate.find(query, Group.class, ConstDef.C_GRP);
    if (groupList.isEmpty()) {
      return null;
    }

    Group group = assembleGroup(groupList.get(0));
    return group.getDss()
                .stream()
                .filter(dataSource -> dataSource.getDsintId().equals(internalId))
                .findAny()
                .orElse(null);
  }

  @Override
  public Group findOneByDssInternalId(String internalId) {
    // TODO: pageable ?
    Query query = Query.query(where(String.format("%s.%s",
                                                  ConstDef.F_DATASOURCES,
                                                  ConstDef.F_DATASOURCEINTLID)).is(internalId));
    List<Group> groupList = mtemplate.find(query, Group.class, ConstDef.C_GRP);
    if (groupList == null || groupList.isEmpty()) {
      return null;
    }
    return assembleGroup(groupList.get(0));
  }

  private Criteria getNonequivalentQueryForAttrAndCfg(String itemInGrp, String itemInGrpTyp,
                                                      String keyInItem, String valueInItem,
                                                      String queryKey, String queryValue) {

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

    return new Criteria().orOperator(where(itemInGrp).elemMatch(new Criteria().andOperator(where(keyInItem).is(queryKey),
                                                                                           query.apply(valueInItem)
                                                                                                .apply(queryValue))),
                                     where(itemInGrpTyp).elemMatch(new Criteria().andOperator(where(keyInItem).is(queryKey),
                                                                                              query.apply(valueInItem)
                                                                                                   .apply(queryValue))));
  }

  /**
   * get the group type of the group and merge - configurations of the group type and the group -
   * data source attributes of the group type and the group - data sources of the group type and the
   * group
   *
   * @param group
   * @return
   */
  private Group assembleGroup(Group group) {
    GroupType groupType = gtRepo.findOneByN(group.getGt());
    if (groupType == null) {
      return group;
    }

    // configurations
    List<ConfigurationEntity> configurationListGroup = group.getCs();
    List<ConfigurationEntity> configurationListGroupType = groupType.getCs();
    if (configurationListGroupType != null) {
      configurationListGroupType.stream()
                                .filter(configurationGroupType -> configurationListGroup == null
                                    || !configurationListGroup.contains(configurationGroupType))
                                .forEach(group::insertOrUpdateCs);
    }

    // data sources attributes, will copy data source templates later
    List<DataSourceEntity> dataSourceListGroupType = groupType.getDss();
    List<DataSourceEntity> dataSourceListGroup = group.getDss();
    if (dataSourceListGroupType != null && dataSourceListGroup != null) {
      for (DataSourceEntity dataSourceTemplate : dataSourceListGroupType) {
        for (DataSourceEntity dataSource : dataSourceListGroup) {
          if (dataSource.getDsn().equals(dataSourceTemplate.getDsn())) {
            mergeDss(dataSource, dataSourceTemplate);
            break;
          }
        }
      }
    }

    if (dataSourceListGroupType != null) {
      dataSourceListGroupType.stream()
                             .filter(dataSourceTempalte -> dataSourceListGroup == null
                                 || !dataSourceListGroup.contains(dataSourceTempalte))
                             .forEach(group::insertOrUpdateDss);
    }

    return group;
  }

  public static void mergeDss(DataSourceEntity dataSource, DataSourceEntity dataSourceTemplate) {
    List<AttributeEntity> attributeListDataSourceTemplate = dataSourceTemplate.getAttributeList();
    List<AttributeEntity> attributeListDataSource = dataSource.getAttributeList();
    if (attributeListDataSourceTemplate != null) {
      attributeListDataSourceTemplate.stream()
                                     .filter(attributeDataSourceTemplate -> attributeListDataSource == null
                                         || !attributeListDataSource.contains(attributeDataSourceTemplate))
                                     .forEach(dataSource::addDsAttrItem);
    }

    // values of a data source template are default values of the relative data source
    if (dataSource.getClassInfo() == null || dataSource.getClassInfo().isEmpty())
      dataSource.setClassInfo(dataSourceTemplate.getClassInfo());
    if (dataSource.getDescription() == null || dataSource.getDescription().isEmpty())
      dataSource.setDescription(dataSourceTemplate.getDescription());
    if (dataSource.getTitle() == null || dataSource.getTitle().isEmpty())
      dataSource.setTitle(dataSourceTemplate.getTitle());
    if (dataSource.getUnit() == null || dataSource.getUnit().isEmpty())
      dataSource.setUnit(dataSourceTemplate.getUnit());
    if (dataSource.getThreshHigh() == null)
      dataSource.setThreshHigh(dataSourceTemplate.getThreshHigh());
    if (dataSource.getThreshLow() == null)
      dataSource.setThreshLow(dataSourceTemplate.getThreshLow());
    if (dataSource.getInterval() == null)
      dataSource.setInterval(dataSourceTemplate.getInterval());
  }
}
