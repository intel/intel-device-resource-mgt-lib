/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.utilities;

import com.openiot.cloud.base.common.model.StatsType;
import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.mongo.dao.GroupRepository;
import com.openiot.cloud.base.mongo.model.Group;
import com.openiot.cloud.base.mongo.model.help.DataSourceEntity;
import com.openiot.cloud.sdk.service.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The utility to access DSS data, to be defined .<br>
 * <br>
 *
 * @author saigon
 * @version 1.0
 * @since Oct 2017
 */
@Component(value = "dssUtility")
public class DssUtility {
  // @Autowired
  // StatsDataRepository statDataRepo;
  @Autowired
  GroupRepository grpRepo;

  private static final Logger logger = LoggerFactory.getLogger(DssUtility.class.getName());

  public static DssUtility getInstance() {
    return ApplicationContextProvider.getBean(DssUtility.class);
  }

  @Deprecated
  public String stdCondStat(String dsiid, long from, long to, StatsType stats, String cond) {
    // return statDataRepo.stdCondStatFromInflux(dsiid,
    // from,
    // to,
    // stats,
    // cond,
    // StatTargetType.DATASOURCE);
    return null;
  }

  @Deprecated
  public Long durationStat(String dsiid, long from, long to, String cond, StatsType statType) {
    // RawAggregationData result = statDataRepo.durDssStats(dsiid, from, to, cond);
    // if (result != null) {
    // switch (statType) {
    // case DURATION:
    // return result.getDur();
    // case DURMAX:
    // return result.getDurMax();
    // case DURMIN:
    // return result.getDurMin();
    // case DURAVG:
    // return result.getAvg() != null ? result.getAvg().longValue() : 0l;
    // default:
    // break;
    // }
    // }
    return 0l;
  }

  public DataSourceEntity createDssRef(Group grp, DataSourceEntity ds, String deviceID,
                                       String resourceUrl, String propertyName) {
    Optional<DataSourceEntity> dsInDBOpt = Optional.ofNullable(grp)
                                                   .map(g -> g.getDss())
                                                   .filter(dss -> !dss.isEmpty())
                                                   .map(dss -> dss.stream()
                                                                  .filter(dsDB -> dsDB.equals(ds))
                                                                  .collect(Collectors.toList()))
                                                   .filter(dssDB -> dssDB.size() == 1)
                                                   .map(dssDB -> dssDB.get(0));
    if (!dsInDBOpt.isPresent()) {
      // can't find such a ds in grp or there are more than one ds in grp
      return ds;
    }

    // insert one ref in group collection
    DataSourceEntity.Reference dsDef =
        new DataSourceEntity.Reference(deviceID,
                                       resourceUrl,
                                       propertyName,
                                       BaseUtil.getNowAsEpochMillis(),
                                       0);
    dsInDBOpt.get().setDsdefItem(dsDef);
    grpRepo.save(grp);
    return ds;
  }

  public String getFu(DataSourceEntity ds) {
    return Optional.ofNullable(ds)
                   .map(datasource -> datasource.getDsdefs())
                   .filter(dsDefs -> !dsDefs.isEmpty())
                   .map(dsDefs -> dsDefs.get(dsDefs.size() - 1))
                   .map(dsDef -> dsDef.getDsrurl())
                   .orElse(null);
  }

  public void stopDssRef(Group group, Set<String> keySet) {
    if (group == null || group.getDss() == null || group.getDss().size() == 0)
      return;
    long nowTime = BaseUtil.getNowAsEpochMillis();
    group.getDss()
         .stream()
         .filter(dssEntiry -> keySet.contains(dssEntiry.getDsn()) && dssEntiry.getDsdefs() != null
             && dssEntiry.getDsdefs().size() > 0)
         .forEach(dssEntiry -> {
           dssEntiry.getDsdefs().stream().filter(dsDef -> dsDef.getDsrt() == 0).forEach(dsDef -> {
             dsDef.setDsrt(nowTime);
           });
         });
    grpRepo.save(group);
  }

  public void stopDssRef(Group group, String dssName) {
    if (group == null || dssName == null || group.getDss() == null || group.getDss().size() == 0)
      return;
    long nowTime = BaseUtil.getNowAsEpochMillis();
    group.getDss()
         .stream()
         .filter(dssEntiry -> dssName.equals(dssEntiry.getDsn()) && dssEntiry.getDsdefs() != null
             && dssEntiry.getDsdefs().size() > 0)
         .forEach(dssEntiry -> {
           dssEntiry.getDsdefs().stream().filter(dsDef -> dsDef.getDsrt() == 0).forEach(dsDef -> {
             dsDef.setDsrt(nowTime);
           });
         });
    grpRepo.save(group);
  }
  // dsEntity->Optional.ofNullable(dsEntity.getDsdefs())
  // .map(dsDefs->dsDefs.stream()
  // .map(dsDef->dsDef.setDsrt(nowTime)))
}
