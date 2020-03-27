/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao;

import com.openiot.cloud.base.Application;
import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.mongo.model.Alarm;
import com.openiot.cloud.base.mongo.model.AlarmStats;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoField.MICRO_OF_SECOND;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class}, properties = {"mongo.db = test_openiot"})
public class AlarmRepositoryTest {
  @Autowired
  private AlarmRepository alarmRepo;

  @Before
  public void setup() throws Exception {
    alarmRepo.deleteAll();
  }

  @After
  public void tearDown() throws Exception {
    // alarmRepo.deleteAll();
  }

  @Test
  public void testBasic() throws Exception {
    // 0 prepare alarms in DB
    String[] aids = new String[] {"apple", "mongo"};
    LocalDateTime now = LocalDateTime.now();
    long from = now.minus(1, HOURS).toInstant(UTC).toEpochMilli();
    long truncaedFrom =
        DateUtils.truncate(new Date(from), Calendar.HOUR).toInstant().toEpochMilli();
    long to = now.plus(1, HOURS).toInstant(UTC).toEpochMilli();
    long truncaedTo = DateUtils.truncate(new Date(to), Calendar.HOUR).toInstant().toEpochMilli();

    Alarm alarm = new Alarm();
    alarm.setProject("fruit");
    alarm.setAlarmid(aids[0]);
    alarm.setSettime(from);
    alarm.setStatus(Alarm.Status.ACTIVE);
    alarmRepo.save(alarm);

    alarm.setId(null);
    alarm.setSettime(truncaedFrom > from - 1 ? from + 1 : from - 1);
    alarmRepo.save(alarm);

    alarm.setId(null);
    alarm.setSettime(truncaedFrom > from - 2 ? from + 2 : from - 2);
    alarmRepo.save(alarm);

    alarm.setId(null);
    alarm.setAlarmid(aids[1]);
    alarm.setSettime(truncaedTo > to - 1 ? to + 1 : to - 1);
    alarmRepo.save(alarm);

    // 1 verify raw alarms
    List<Alarm> alarms = alarmRepo.findAll();
    assertThat(alarms).isNotNull().asList().hasSize(4);
    System.out.println(alarms);

    // 2 verify alarms stats
    List<?> task = alarmRepo.filter("fruit", aids, null, null, "h", null, from, to, null, null);
    assertThat(task).isNotNull().asList().hasSize(3);
    Map<String, Object> data0 = (Map<String, Object>) task.get(0);
    Map<String, Object> data1 = (Map<String, Object>) task.get(1);
    Map<String, Object> data2 = (Map<String, Object>) task.get(2);
    assertThat(data0.get("data")).isNotNull();
    assertThat(data1.get("data")).isNotNull();
    assertThat(data2.get("data")).isNotNull();
    assertThat((AlarmStats) data0.get("data")).hasFieldOrPropertyWithValue("active_num", 3);
    assertThat((AlarmStats) data1.get("data")).hasFieldOrPropertyWithValue("active_num", 0);
    assertThat((AlarmStats) data2.get("data")).hasFieldOrPropertyWithValue("active_num", 1);
    assertThat((AlarmStats) data0.get("data")).hasFieldOrPropertyWithValue("clear_num", 0);
    assertThat((AlarmStats) data1.get("data")).hasFieldOrPropertyWithValue("clear_num", 0);
    assertThat((AlarmStats) data2.get("data")).hasFieldOrPropertyWithValue("clear_num", 0);

    // 3 verify alarms stats with only one alarmID
    aids = new String[] {"apple"};
    task = alarmRepo.filter("fruit", aids, null, null, "h", null, from, to, null, null);
    assertThat(task).isNotNull().asList().hasSize(3);
    data0 = (Map<String, Object>) task.get(0);
    data1 = (Map<String, Object>) task.get(1);
    data2 = (Map<String, Object>) task.get(2);
    assertThat(data0.get("data")).isNotNull();
    assertThat(data1.get("data")).isNotNull();
    assertThat(data2.get("data")).isNotNull();
    assertThat((AlarmStats) data0.get("data")).hasFieldOrPropertyWithValue("active_num", 3);
    assertThat((AlarmStats) data1.get("data")).hasFieldOrPropertyWithValue("active_num", 0);
    assertThat((AlarmStats) data2.get("data")).hasFieldOrPropertyWithValue("active_num", 0);
    assertThat((AlarmStats) data0.get("data")).hasFieldOrPropertyWithValue("clear_num", 0);
    assertThat((AlarmStats) data1.get("data")).hasFieldOrPropertyWithValue("clear_num", 0);
    assertThat((AlarmStats) data2.get("data")).hasFieldOrPropertyWithValue("clear_num", 0);

    // 4 verify alarms stats with only one alarmID
    aids = new String[] {"mongo"};
    task = alarmRepo.filter("fruit", aids, null, null, "h", null, from, to, null, null);
    assertThat(task).isNotNull().asList().hasSize(3);
    data0 = (Map<String, Object>) task.get(0);
    data1 = (Map<String, Object>) task.get(1);
    data2 = (Map<String, Object>) task.get(2);
    assertThat(data0.get("data")).isNotNull();
    assertThat(data1.get("data")).isNotNull();
    assertThat(data2.get("data")).isNotNull();
    assertThat((AlarmStats) data0.get("data")).hasFieldOrPropertyWithValue("active_num", 0);
    assertThat((AlarmStats) data1.get("data")).hasFieldOrPropertyWithValue("active_num", 0);
    assertThat((AlarmStats) data2.get("data")).hasFieldOrPropertyWithValue("active_num", 1);
    assertThat((AlarmStats) data0.get("data")).hasFieldOrPropertyWithValue("clear_num", 0);
    assertThat((AlarmStats) data1.get("data")).hasFieldOrPropertyWithValue("clear_num", 0);
    assertThat((AlarmStats) data2.get("data")).hasFieldOrPropertyWithValue("clear_num", 0);
  }

  @Test
  public void testPerformance() throws Exception {
    // 0 prepare alarms in DB
    String[] aids = new String[] {"apple", "mongo"};
    LocalDateTime now = LocalDateTime.now();
    long from = now.minus(50, HOURS).toInstant(UTC).toEpochMilli();
    long truncaedFrom =
        DateUtils.truncate(new Date(from), Calendar.HOUR).toInstant().toEpochMilli();
    long to = now.plus(50, HOURS).toInstant(UTC).toEpochMilli();
    long truncaedTo = DateUtils.truncate(new Date(to), Calendar.HOUR).toInstant().toEpochMilli();

    Alarm alarm = new Alarm();
    alarm.setProject("fruit");
    alarm.setAlarmid(aids[0]);
    alarm.setSettime(from);
    alarm.setStatus(Alarm.Status.ACTIVE);
    alarmRepo.save(alarm);

    alarm.setId(null);
    alarm.setSettime(truncaedFrom > from - 1 ? from + 1 : from - 1);
    alarmRepo.save(alarm);

    alarm.setId(null);
    alarm.setSettime(truncaedFrom > from - 2 ? from + 2 : from - 2);
    alarmRepo.save(alarm);

    alarm.setId(null);
    alarm.setAlarmid(aids[1]);
    alarm.setSettime(truncaedTo > to - 1 ? to + 1 : to - 1);
    alarmRepo.save(alarm);

    // 1 verify raw alarms
    List<Alarm> alarms = alarmRepo.findAll();
    assertThat(alarms).isNotNull().asList().hasSize(4);
    System.out.println(alarms);

    // 2 verify alarms stats
    List<?> task = alarmRepo.filter("fruit", aids, null, null, "h", null, from, to, null, null);
    assertThat(task).isNotNull().asList().hasSize(101);
    LocalDateTime end = LocalDateTime.now();
    assertThat(end.toInstant(UTC).toEpochMilli()
        - now.toInstant(UTC).toEpochMilli()).isLessThan(1000);
  }

}
