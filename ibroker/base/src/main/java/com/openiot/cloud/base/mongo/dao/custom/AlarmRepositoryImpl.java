/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao.custom;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.dao.AlarmDefinitionRepository;
import com.openiot.cloud.base.mongo.dao.AlarmRepository;
import com.openiot.cloud.base.mongo.model.Alarm;
import com.openiot.cloud.base.mongo.model.Alarm.Status;
import com.openiot.cloud.base.mongo.model.AlarmStats;
import com.openiot.cloud.base.mongo.model.DssStats;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

@Component
public class AlarmRepositoryImpl implements AlarmRepositoryCustom {
  public static final Logger logger = LoggerFactory.getLogger(AlarmRepositoryImpl.class);
  private static ExecutorService executor = Executors.newFixedThreadPool(4);

  @Autowired
  AlarmRepository alarmRepo;
  @Autowired
  AlarmDefinitionRepository alarmDefRepo;
  @Autowired
  MongoOperations monOp;

  @Override
  public List<?> filter(String project, String[] aid, String tt, String tid, String unit,
                        String grp, Long from, Long to, String status, PageRequest pageReq) {

    logger.info(String.format("query param: project=%s aid=%s tt=%s tid=%s unit=%s grp=%s from=%s to=%s status=%s ",
                              project,
                              aid,
                              tt,
                              tid,
                              unit,
                              grp,
                              from,
                              to,
                              status));

    // 1. for history stats query unit
    if (unit != null) {

      Map<Date, FutureTask<AlarmStats>> futureTasks = new LinkedHashMap<>();
      ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

      // begin and end
      Date Begin = from != null ? new Date(from) : BaseUtil.getNow();
      Date End = to != null ? new Date(to) : BaseUtil.getNow();

      while (End.after(Begin)) {
        Date IntervalStart;
        if (ConstDef.F_UNITMONTH.equals(unit)) {
          IntervalStart = DateUtils.truncate(Begin, Calendar.MONTH);
          Begin = DateUtils.addMonths(IntervalStart, 1);
        } else if (ConstDef.F_UNITWEEK.equals(unit)) {
          IntervalStart = DateUtils.truncate(Begin, Calendar.MONDAY);
          Begin = DateUtils.addWeeks(IntervalStart, 1);
        } else if (ConstDef.F_UNITHOUR.equals(unit)) {
          IntervalStart = DateUtils.truncate(Begin, Calendar.HOUR);
          Begin = DateUtils.addHours(IntervalStart, 1);
        } else {
          IntervalStart = DateUtils.truncate(Begin, Calendar.DATE);
          Begin = DateUtils.addDays(IntervalStart, 1);
        }

        final Date fBegin = (Date) Begin.clone();
        futureTasks.put(IntervalStart, new FutureTask<AlarmStats>(() -> {
          return getAlarmStats(project, tt, tid, aid, IntervalStart, fBegin);
        }));
      }

      for (FutureTask futureTask : futureTasks.values()) {
        executor.submit(futureTask);
      }
      for (Date IntervalStart : futureTasks.keySet()) {
        try {
          AlarmStats alarmStats = (AlarmStats) futureTasks.get(IntervalStart).get();
          if (alarmStats != null) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("t", IntervalStart);
            map.put("data", alarmStats);
            list.add(map);
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (ExecutionException e) {
          e.printStackTrace();
        }
      }

      return new PageImpl<Map<String, Object>>(list).getContent();
    }
    // 2. for general query unit
    else {
      Query q = new Query(Criteria.where(ConstDef.Q_PROJECT).is(project));
      // aid
      Optional.ofNullable(aid).ifPresent(idStr -> q.addCriteria(where(ConstDef.F_ALARMID).in(aid)));

      // targettype
      Optional.ofNullable(tt)
              .map(t -> t.toString())
              .ifPresent(ttStr -> q.addCriteria(where(ConstDef.F_ALARMTAGTYPE).is(ttStr)));

      Optional.ofNullable(tid)
              .ifPresent(tDi -> q.addCriteria(where(ConstDef.F_ALARMTAGID).is(tid)));

      if (grp != null) {
        q.addCriteria(where(ConstDef.F_TAGGRP).is(grp));
      }
      // if (tid != null) {
      // // tagid.di
      // Optional.ofNullable(tid.getDi()).ifPresent(tDi -> q.addCriteria(
      // where(String.format("%s.%s", ConstDef.F_ALARMTAGID, ConstDef.F_TAGDEVID)).is(tt)));
      //
      // // tagid.res
      // Optional.ofNullable(tid.getRes()).ifPresent(tRes -> q.addCriteria(
      // where(String.format("%s.%s", ConstDef.F_ALARMTAGID, ConstDef.F_TAGRES)).is(tRes)));
      //
      // // tagid.pt
      // Optional.ofNullable(tid.getPt()).ifPresent(tPt -> q.addCriteria(
      // where(String.format("%s.%s", ConstDef.F_ALARMTAGID, ConstDef.F_TAGPROP)).is(tPt)));
      //
      // // tagid.grp
      // Optional.ofNullable(tid.getGrp()).ifPresent(tGrp -> q.addCriteria(
      // where(String.format("%s.%s", ConstDef.F_ALARMTAGID, ConstDef.F_TAGGRP)).is(tGrp)));
      // }
      // if (tid == null && grp != null) {
      // // grp
      // Optional.ofNullable(grp)
      // .ifPresent(g -> q.addCriteria(
      // where(String.format("%s.%s", ConstDef.F_ALARMTAGID, ConstDef.F_TAGGRP)).is(g)));
      // }
      // begin and end
      if (from != null && to != null) {
        q.addCriteria(new Criteria().andOperator(where(ConstDef.F_ALARMBEGINNINGTIME).gte(from),
                                                 where(ConstDef.F_ALARMBEGINNINGTIME).lte(to)));
      } else {
        if (from != null)
          q.addCriteria(where(ConstDef.F_ALARMBEGINNINGTIME).gte(from));
        if (to != null)
          q.addCriteria(where(ConstDef.F_ALARMBEGINNINGTIME).lte(to));
      }
      // status support !ACTIVE ACTIVE RESOLVED CLEARED
      Optional.ofNullable(status).ifPresent(s -> {
        if (status.startsWith("!")) {
          q.addCriteria(where(ConstDef.F_ALARMSTATUS).ne(s.substring(1).toUpperCase()));
        } else {
          q.addCriteria(where(ConstDef.F_ALARMSTATUS).is(s.toUpperCase()));
        }
      });

      Optional.ofNullable(pageReq).ifPresent(p -> q.with(p));

      logger.info("query: " + q.toString());

      return monOp.find(q.with(new Sort(Sort.Direction.DESC,
                                        ConstDef.F_ALARMBEGINNINGTIME,
                                        ConstDef.F_ID)),
                        Alarm.class);
    }
  }

  private AlarmStats getAlarmStats(String project, String tt, String tid, String[] aid,
                                   Date intervalStart, Date begin) {

    AlarmStats alarmStats = new AlarmStats();
    for (Status s : Status.values()) {

      Query q = new Query(Criteria.where(ConstDef.Q_PROJECT).is(project));
      // targettype
      Optional.ofNullable(tt)
              .map(t -> t.toString())
              .ifPresent(ttStr -> q.addCriteria(where(ConstDef.F_ALARMTAGTYPE).is(ttStr)));

      Optional.ofNullable(tid).ifPresent(tDi -> q.addCriteria(where(ConstDef.F_ALARMTAGID).is(tt)));
      // aid
      Optional.ofNullable(aid).ifPresent(idStr -> q.addCriteria(where(ConstDef.F_ALARMID).in(aid)));

      if (s == Status.ACTIVE) {
        q.addCriteria(new Criteria().andOperator(where(ConstDef.F_ALARMBEGINNINGTIME).gte(intervalStart.getTime()),
                                                 where(ConstDef.F_ALARMBEGINNINGTIME).lt(begin.getTime())));
        // q.addCriteria(where(ConstDef.F_ALARMSTATUS).is(Status.ACTIVE));
        alarmStats.active_num = (int) monOp.count(q, Alarm.class);
        logger.info("/api/alarm query: " + q);
      } else if (s == Status.CLEARED) {
        q.addCriteria(new Criteria().andOperator(where(ConstDef.F_ALARMENDTIME).gte(intervalStart.getTime()),
                                                 where(ConstDef.F_ALARMENDTIME).lt(begin.getTime())));
        q.addCriteria(where(ConstDef.F_ALARMSTATUS).is(Status.CLEARED));
        alarmStats.clear_num = (int) monOp.count(q, Alarm.class);
        logger.info("/api/alarm query: " + q);
      } else {
      }
    }

    return alarmStats;
  }

  @Override
  public List<DssStats> getDssStats(String project, String dssName, String unit, Long from, Long to,
                                    String page, String limit) {

    List<AggregationOperation> operations = new ArrayList<>();

    // 1. {$match:{"project" : "5b2b1bdc251d2c6798f22bd4", "targettype" : "dss",
    // "clear_t":{$ne:null}, "targetid" : {$regex:".*:temperature"}}}
    Criteria match = new Criteria();
    match.and(ConstDef.Q_PROJECT).is(project);
    match.and(ConstDef.F_ALARMTAGTYPE).is("dss");
    match.and(ConstDef.F_ALARMENDTIME).gt(0L);
    match.and(ConstDef.F_ALARMTAGID).regex(".*:" + dssName);
    match.andOperator(where(ConstDef.F_ALARMBEGINNINGTIME).gte(from),
                      where(ConstDef.F_ALARMBEGINNINGTIME).lte(to));
    operations.add(Aggregation.match(match));

    // 2. {$group:{_id:"$targetid", grp_count:{$sum:1},
    // grp_sum:{$sum:{$subtract:["$clear_t","$set_t"]}},
    // grp_max:{$max:{$subtract:["$clear_t","$set_t"]}},
    // grp_min:{$min:{$subtract:["$clear_t","$set_t"]}}} }
    GroupOperation groupByRoom = Aggregation.group(ConstDef.F_ALARMTAGID);
    groupByRoom = groupByRoom.count().as("grpcount");
    // AggregationExpression expr = ArithmeticOperators.Subtract.valueOf(ConstDef.F_ALARMENDTIME)
    // .subtract(
    // ConstDef.F_ALARMBEGINNINGTIME);
    AggregationExpression expr =
        ArithmeticOperators.Subtract.valueOf(ConstDef.F_ALARMENDTIME)
                                    .subtract(ConstDef.F_ALARMBEGINNINGTIME);
    groupByRoom = groupByRoom.sum(expr).as("grpsum");
    groupByRoom = groupByRoom.max(expr).as("grpmax");
    groupByRoom = groupByRoom.min(expr).as("grpmin");
    operations.add(groupByRoom);

    // 3. {$group: {_id:null, room_num:{$sum:1}, count:{$sum:"$grp_count"}, max:{$max:"$grp_max"},
    // min:{$min:"$grp_min"}, sum:{$sum:"$grp_sum"}}},
    GroupOperation groupByAll = Aggregation.group();
    groupByAll = groupByAll.count().as("num_room");
    groupByAll = groupByAll.sum("grpcount").as("num_over");
    groupByAll = groupByAll.sum("grpsum").as("td_total");
    groupByAll = groupByAll.max("grpmax").as("td_max");
    groupByAll = groupByAll.min("grpmin").as("td_min");
    operations.add(groupByAll);

    // 4. {$project: {room_num:1, count:1, max:1, min:1, sum:1, avg: {$divide:["$sum", "$count"]}}}
    ProjectionOperation projection =
        Aggregation.project("num_room", "num_over", "td_total", "td_max", "td_min");
    AggregationExpression expression =
        ArithmeticOperators.Divide.valueOf("td_total").divideBy("num_over");
    projection = projection.and(expression).as("td_ava");
    operations.add(projection);

    Aggregation agg = null;
    try {
      agg = Aggregation.newAggregation(operations);
      logger.info("/api/statsdss aggregation: " + agg);
      List<DssStats> result = monOp.aggregate(agg, Alarm.class, DssStats.class).getMappedResults();
      logger.info("aggregation result: " + result);
      return result;
    } catch (PropertyReferenceException e) {
      logger.error(BaseUtil.getStackTrace(e));
    }
    return null;
  }

  @Override
  public Long filterCnt(String project, String[] aid, String tt, String tid, String grp, Long from,
                        Long to, String status) {
    logger.info(String.format("query param: project=%s aid=%s tt=%s tid=%s grp=%s from=%s to=%s status=%s ",
                              project,
                              aid,
                              tt,
                              tid,
                              grp,
                              from,
                              to,
                              status));

    Query q = new Query(Criteria.where(ConstDef.Q_PROJECT).is(project));
    // aid
    Optional.ofNullable(aid).ifPresent(idStr -> q.addCriteria(where(ConstDef.F_ALARMID).in(aid)));

    // targettype
    Optional.ofNullable(tt)
            .map(t -> t.toString())
            .ifPresent(ttStr -> q.addCriteria(where(ConstDef.F_ALARMTAGTYPE).is(ttStr)));

    Optional.ofNullable(tid).ifPresent(tDi -> q.addCriteria(where(ConstDef.F_ALARMTAGID).is(tid)));

    if (grp != null) {
      q.addCriteria(where(ConstDef.F_TAGGRP).is(grp));
    }
    if (from != null && to != null) {
      q.addCriteria(new Criteria().andOperator(where(ConstDef.F_ALARMBEGINNINGTIME).gte(from),
                                               where(ConstDef.F_ALARMBEGINNINGTIME).lte(to)));
    } else {
      if (from != null)
        q.addCriteria(where(ConstDef.F_ALARMBEGINNINGTIME).gte(from));
      if (to != null)
        q.addCriteria(where(ConstDef.F_ALARMBEGINNINGTIME).lte(to));
    }
    // status support !ACTIVE ACTIVE RESOLVED CLEARED
    Optional.ofNullable(status).ifPresent(s -> {
      if (status.startsWith("!")) {
        q.addCriteria(where(ConstDef.F_ALARMSTATUS).ne(s.substring(1).toUpperCase()));
      } else {
        q.addCriteria(where(ConstDef.F_ALARMSTATUS).is(s.toUpperCase()));
      }
    });

    logger.info("query: " + q.toString());

    return monOp.count(q, Alarm.class);
  }

  /*
   * @Override public List<Map<String, Object>> stats(String[] aid, TargetType tt, TargetEntity tid,
   * String unit, String grp, String begin, String end, Status status) { ArrayList<Map<String,
   * Object>> list = new ArrayList<Map<String, Object>>();
   *
   * Query q = new Query(); // aid Optional.ofNullable(aid).ifPresent( idStr ->
   * q.addCriteria(where(ConstDef.F_ALARMID).in(aid)));
   *
   * // targettype Optional.ofNullable(tt).map(t -> t.toString().toUpperCase()) .ifPresent(ttStr ->
   * q .addCriteria( where(ConstDef.F_ALARMTAGTYPE).is(ttStr)));
   *
   * if (tid != null && grp == null) { // tagid.di Optional.ofNullable(tid.getDi()).ifPresent(tDi ->
   * q .addCriteria( where(String.format("%s.%s", ConstDef.F_ALARMTAGID, ConstDef.F_TAGDEVID))
   * .is(tt)));
   *
   * // tagid.res Optional.ofNullable(tid.getRes()).ifPresent(tRes -> q .addCriteria(
   * where(String.format("%s.%s", ConstDef.F_ALARMTAGID, ConstDef.F_TAGRES)) .is(tRes)));
   *
   * // tagid.pt Optional.ofNullable(tid.getPt()).ifPresent(tPt -> q .addCriteria(
   * where(String.format("%s.%s", ConstDef.F_ALARMTAGID, ConstDef.F_TAGPROP)) .is(tPt)));
   *
   * // tagid.grp Optional.ofNullable(tid.getGrp()).ifPresent(tGrp -> q .addCriteria(
   * where(String.format("%s.%s", ConstDef.F_ALARMTAGID, ConstDef.F_TAGGRP)).is(tGrp))); }
   *
   * if(tid == null && grp != null){ // grp Optional.ofNullable(grp).ifPresent(g -> q .addCriteria(
   * where(String.format("%s.%s", ConstDef.F_ALARMTAGID, ConstDef.F_TAGGRP)).is(g))); }
   *
   * Date Begin = begin == null ? new Date() : new Date(Long.valueOf(begin)); Date End = end == null
   * ? new Date() : new Date(Long.valueOf(end)); while(End.after(Begin)){ Date IntervalStart;
   * if(ConstDef.F_UNITMONTH.equals(unit)){ IntervalStart = DateUtils.truncate(Begin,
   * Calendar.MONTH); Begin = DateUtils.addMonths(IntervalStart, 1); }else
   * if(ConstDef.F_UNITWEEK.equals(unit)){ IntervalStart = DateUtils.truncate(Begin,
   * Calendar.MONDAY); Begin = DateUtils.addWeeks(IntervalStart, 1); }else
   * if(ConstDef.F_UNITHOUR.equals(unit)){ IntervalStart = DateUtils.truncate(Begin, Calendar.HOUR);
   * Begin = DateUtils.addHours(IntervalStart, 1); }else { IntervalStart = DateUtils.truncate(Begin,
   * Calendar.DATE); Begin = DateUtils.addDays(IntervalStart, 1); } q.addCriteria(new
   * Criteria().andOperator( where(ConstDef.F_ALARMBEGINNINGTIME).gte(IntervalStart),
   * where(ConstDef.F_ALARMENDTIME).lt(Begin)));
   * q.addCriteria(where(ConstDef.F_ALARMSTATUS).is(Status.ACTIVE)); Integer avtive_count =
   * (int)monOp.count(q, Alarm.class);
   * q.addCriteria(where(ConstDef.F_ALARMSTATUS).is(Status.CLEARED)); Integer clear_count =
   * (int)monOp.count(q, Alarm.class); AlarmStats alarmStats = new AlarmStats(avtive_count,
   * clear_count); if(alarmStats != null){ HashMap<String, Object> map = new HashMap<String,
   * Object>(); map.put("t", IntervalStart); map.put("data", alarmStats); list.add(map); } } return
   * new PageImpl<Map<String, Object>>(list).getContent(); }
   */
}
