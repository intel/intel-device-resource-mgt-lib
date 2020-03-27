/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.service;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.dao.GroupRepository;
import com.openiot.cloud.base.mongo.dao.ResProRepository;
import com.openiot.cloud.base.mongo.model.Group;
import com.openiot.cloud.base.mongo.model.ResProperty;
import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import com.openiot.cloud.base.mongo.model.help.DataSourceEntity;
import com.openiot.cloud.base.mongo.model.help.ResAndResProID;
import com.openiot.cloud.base.redis.dao.DataSourceRedisRepository;
import com.openiot.cloud.base.redis.model.DataSourceReferenceRedis;
import com.openiot.cloud.base.redis.model.ReferenceDefinitionRedis;
import com.openiot.cloud.base.service.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class DataSourceService {
  private static final Logger logger = LoggerFactory.getLogger(DataSourceService.class);
  @Autowired
  private DataSourceRedisRepository dataSourceRedisRepository;
  @Autowired
  private GroupRepository groupRepository;
  @Autowired
  private ResProRepository propertyRepository;

  // TODO: a service model for group
  // a temporary method until we have a service model for group
  // for now, has to retrieve data source information from data base
  // only use this method to update cache
  public void save(String groupName) {
    // Database
    Group group = groupRepository.findOneByName(groupName);
    if (group == null) {
      return;
    }

    if (group.getDss() == null) {
      return;
    }

    for (DataSourceEntity dataSourceMongo : group.getDss()) {
      // Cache
      DataSourceReferenceRedis dataSourceReferenceRedis =
          new DataSourceReferenceRedis(groupName, dataSourceMongo.getDsn());
      if (dataSourceMongo.getDsdefs() == null) {
        continue;
      }

      for (DataSourceEntity.Reference definition : dataSourceMongo.getDsdefs()) {
        ResAndResProID resProID = definition.getDsri();
        if (resProID != null) {
          dataSourceReferenceRedis.addDefinitionItem(new ReferenceDefinitionRedis(groupName,
                                                                                  dataSourceMongo.getDsn(),
                                                                                  resProID.getDi(),
                                                                                  resProID.getResUri(),
                                                                                  resProID.getPt(),
                                                                                  definition.getDsrf(),
                                                                                  definition.getDsrt()));
        }
      }
      dataSourceRedisRepository.save(dataSourceReferenceRedis);
    }
  }

  /**
   * will create a new one or update the existed one, it depends on data source dataSourceName is
   * exist or not.
   *
   * <p>if to create, will totally use dataSource
   *
   * <p>if to update, will use all non-null or non-empty value of dataSource
   *
   * @param dataSource, a new data source content
   * @return
   */
  public void save(String groupName, DataSource dataSource) {
    // Database
    Group group = groupRepository.findOneByName(groupName);
    if (group == null) {
      return;
    }

    dataSource.setReferenceList(verifyAndModifyRefernces(dataSource.getReferenceList()));

    // create or replace
    DataSourceEntity dataSourceMongo = create(dataSource);
    group.insertOrUpdateDss(dataSourceMongo);
    group = groupRepository.save(group);

    if (dataSource.getReferenceList() == null) {
      return;
    }

    // Cache
    DataSourceReferenceRedis dataSourceReferenceRedis =
        new DataSourceReferenceRedis(groupName, dataSource.getName());
    for (ReferenceDefinition definition : dataSource.getReferenceList()) {
      dataSourceReferenceRedis.addDefinitionItem(new ReferenceDefinitionRedis(groupName,
                                                                              dataSource.getName(),
                                                                              definition.getDevId(),
                                                                              definition.getResUrl(),
                                                                              definition.getPropName(),
                                                                              definition.getFrom(),
                                                                              definition.getTo()));
    }
    dataSourceRedisRepository.save(dataSourceReferenceRedis);
    // TODO: start event
  }

  /*
   * validate reference table as below from to last_reference valid_reference ======== ========
   * =============== ================ 0 0 true valid by setting from to now 0 0 false invalid 0 >0 *
   * invalid > 0 true valid > 0 false invalid >0 >0 * valid if from <= to >0 >0 * invalid if from >
   * to
   */
  private List<ReferenceDefinition>
      verifyAndModifyRefernces(List<ReferenceDefinition> referenceList) {
    if (referenceList == null || referenceList.isEmpty())
      return referenceList;

    List<ReferenceDefinition> newReferences = new ArrayList<ReferenceDefinition>();

    for (int i = 0; i < referenceList.size(); i++) {
      ReferenceDefinition ref = referenceList.get(i);

      if (ref.getFrom() == 0 && ref.getTo() == 0 && i == (referenceList.size() - 1)) {
        ref.setFrom(BaseUtil.getNowAsEpochMillis());
        newReferences.add(ref);
      } else if (ref.getFrom() > 0 && ref.getTo() == 0 && i == (referenceList.size() - 1)) {
        newReferences.add(ref);
      } else if (ref.getFrom() > 0 && ref.getTo() > 0 && ref.getFrom() <= ref.getTo()) {
        newReferences.add(ref);
      } else {
        logger.warn("Dss reference is ignored: " + ref);
        continue;
      }
    }

    return newReferences;
  }

  public DataSource findDataSourceByGroupNameAndDataSourceName(String groupName,
                                                               String dataSourceName) {
    List<DataSourceEntity> dataSourceMongoList =
        groupRepository.findDssByGroupNameAndDsName(groupName, dataSourceName);
    if (dataSourceMongoList == null || dataSourceMongoList.isEmpty()) {
      return null;
    }

    DataSourceEntity dataSourceMongo = dataSourceMongoList.get(0);

    return create(dataSourceMongo);
  }

  /**
   * will include: - all reference definitions start in the range of [from, to] no matter they are
   * ended or not - all reference definitions end in the range of [from, to] no matter when they
   * start
   *
   * @param groupName
   * @param dataSourceName
   * @param from, in epoch milliseconds
   * @param to, in epoch milliseconds
   * @return
   */
  public List<ReferenceDefinition>
      findReferenceByGroupNameAndDataSourceNameAndTimeBetween(String groupName,
                                                              String dataSourceName, long from,
                                                              long to) {
    List<ReferenceDefinitionRedis> definitionRedisList =
        dataSourceRedisRepository.findDefinitionByTimeBetween(groupName, dataSourceName, from, to);

    if (definitionRedisList == null || definitionRedisList.isEmpty()) {
      return Collections.emptyList();
    }

    return definitionRedisList.stream()
                              .map(definitionRedis -> new ReferenceDefinition(definitionRedis.getDevId(),
                                                                              definitionRedis.getResUrl(),
                                                                              definitionRedis.getPropName(),
                                                                              definitionRedis.getFrom(),
                                                                              definitionRedis.getTo()))
                              .collect(Collectors.toList());
  }

  /**
   * just return the latest started reference definition
   *
   * @param groupName
   * @param dataSourceName
   * @return
   */
  public ReferenceDefinition
      findLatestReferenceByGroupNameAndDataSourceName(String groupName, String dataSourceName) {
    ReferenceDefinitionRedis definitionRedis =
        dataSourceRedisRepository.findLatestDefinition(groupName, dataSourceName);
    if (definitionRedis == null) {
      return null;
    }
    return new ReferenceDefinition(definitionRedis.getDevId(),
                                   definitionRedis.getResUrl(),
                                   definitionRedis.getPropName(),
                                   definitionRedis.getFrom(),
                                   definitionRedis.getTo());
  }

  public List<String> findAllDataSourcesWithRef() {
    List<String> allDss = new ArrayList<String>();

    Pageable pageRequest = PageRequest.of(0, ConstDef.DFLT_SIZE);
    Page<Group> groupPage = groupRepository.findAll(pageRequest);
    while (groupPage.hasContent()) {
      List<String> pageDss =
          groupPage.stream()
                   .filter(grp -> grp.getDss() != null && !grp.getDss().isEmpty())
                   .flatMap(grp -> grp.getDss().stream())
                   .filter(ds -> ds.getLatestReference() != null)
                   .map(DataSourceEntity::getDsintId)
                   .collect(Collectors.toList());
      allDss.addAll(pageDss);
      pageRequest = pageRequest.next();
      groupPage = groupRepository.findAll(pageRequest);
    }

    return allDss;
  }

  /**
   * to warm up all below caches - the data source reference definition cache - ...
   *
   * @param
   * @return nothing
   */
  public void warmUpCache() {
    Pageable pageable = PageRequest.of(0, ConstDef.DFLT_SIZE);
    Page<Group> groupPage = groupRepository.findAll(pageable);
    while (groupPage.hasContent()) {
      for (Group group : groupPage) {
        if (group.getDss() == null) {
          continue;
        }

        for (DataSourceEntity dataSource : group.getDss()) {
          DataSourceReferenceRedis referenceRedis =
              new DataSourceReferenceRedis(group.getN(), dataSource.getDsn());
          if (dataSource.getDsdefs() == null) {
            continue;
          }

          for (DataSourceEntity.Reference definition : dataSource.getDsdefs()) {
            if (definition.getDsri() == null) {
              continue;
            }

            dataSourceRedisRepository.save(new ReferenceDefinitionRedis(group.getN(),
                                                                        dataSource.getDsn(),
                                                                        definition.getDsri()
                                                                                  .getDi(),
                                                                        definition.getDsri()
                                                                                  .getResUri(),
                                                                        definition.getDsri()
                                                                                  .getPt(),
                                                                        definition.getDsrf(),
                                                                        definition.getDsrt()));
          }
        }
      }

      pageable = pageable.next();
      groupPage = groupRepository.findAll(pageable);
    }
  }

  // mongo model -> service layer model
  DataSource create(DataSourceEntity dataSourceMongo) {
    DataSource dataSource = new DataSource();
    dataSource.setName(dataSourceMongo.getDsn());
    dataSource.setType(DataSourceType.fromString(dataSourceMongo.getDst()));
    dataSource.setClassInfo(dataSourceMongo.getClassInfo());
    dataSource.setDescription(dataSourceMongo.getDescription());
    dataSource.setThreshHigh(dataSourceMongo.getThreshHigh());
    dataSource.setThreshLow(dataSourceMongo.getThreshLow());
    dataSource.setInterval(dataSourceMongo.getInterval());
    dataSource.setTitle(dataSourceMongo.getTitle());
    dataSource.setUnit(dataSourceMongo.getUnit());

    Optional.ofNullable(dataSourceMongo.getAttributeList())
            .map(attributeList -> attributeList.stream()
                                               .map(attribute -> new GeneralKeyValuePair(attribute.getAn(),
                                                                                         attribute.getAv()))
                                               .collect(Collectors.toList()))
            .ifPresent(keyValuePairList -> dataSource.setAttributeList(keyValuePairList));
    Optional.ofNullable(dataSourceMongo.getDsdefs())
            .map(referenceList -> referenceList.stream().map(reference -> {
              ResAndResProID resProId = reference.getDsri();
              return new ReferenceDefinition(resProId.getDi(),
                                             resProId.getResUri(),
                                             resProId.getPt(),
                                             reference.getDsrf(),
                                             reference.getDsrt());
            }).collect(Collectors.toList()))
            .ifPresent(definitionList -> dataSource.setReferenceList(definitionList));
    Optional.ofNullable(dataSourceMongo.getOperate()).ifPresent(op -> {
      dataSource.setOperate(new Operate(op.getType(),
                                        op.getBackground_state(),
                                        op.getDi(),
                                        op.getUrl(),
                                        op.getPn(),
                                        op.getSched(),
                                        op.getState_cmds(),
                                        op.getRepeat()));
    });

    return dataSource;
  }

  // server layer model -> mongo model
  DataSourceEntity create(DataSource dataSource) {
    DataSourceEntity dataSourceMongo = new DataSourceEntity();
    dataSourceMongo.setDsn(dataSource.getName());
    dataSourceMongo.setDst(dataSource.getType().toString());
    dataSourceMongo.setClassInfo(dataSource.getClassInfo());
    dataSourceMongo.setDescription(dataSource.getDescription());
    dataSourceMongo.setThreshHigh(dataSource.getThreshHigh());
    dataSourceMongo.setThreshLow(dataSource.getThreshLow());
    dataSourceMongo.setInterval(dataSource.getInterval());
    dataSourceMongo.setTitle(dataSource.getTitle());
    dataSourceMongo.setUnit(dataSource.getUnit());

    Optional.ofNullable(dataSource.getAttributeList())
            .map(attributeList -> attributeList.stream()
                                               .map(attribute -> new AttributeEntity(attribute.getKey(),
                                                                                     attribute.getValue()
                                                                                              .toString()))
                                               .collect(Collectors.toList()))
            .ifPresent(attributeEntityList -> dataSourceMongo.setAttributeList(attributeEntityList));

    Optional.ofNullable(dataSource.getReferenceList())
            .map(referenceList -> referenceList.stream()
                                               .map(reference -> new DataSourceEntity.Reference(reference.getDevId(),
                                                                                                reference.getResUrl(),
                                                                                                reference.getPropName(),
                                                                                                reference.getFrom(),
                                                                                                reference.getTo()))
                                               .collect(Collectors.toList()))
            .ifPresent(referenceList -> dataSourceMongo.setDsdefs(referenceList));
    Optional.ofNullable(dataSource.getOperate())
            .ifPresent(op -> dataSourceMongo.setOperate(new DataSourceEntity.OperateEntity(op.getType(),
                                                                                           op.getBackground_state(),
                                                                                           op.getDi(),
                                                                                           op.getUrl(),
                                                                                           op.getPn(),
                                                                                           op.getSched(),
                                                                                           op.getState_cmds(),
                                                                                           op.getRepeat())));

    return dataSourceMongo;
  }
}
