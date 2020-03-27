/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */


package com.openiot.cloud.event;

import com.openiot.cloud.sdk.service.IConnect;
import com.openiot.cloud.sdk.service.IConnectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TaskEngine {

  public static final Logger logger = LoggerFactory.getLogger(TaskEngine.class);

  @Autowired
  private IConnect iConnect;

  @Autowired
  private IConnectService iConnectService;

  @Autowired
  private EventMonitorRequestHandler eventMonitorRequestHandler;

  @Autowired
  private TaskRequestHandler taskRequestHandler;

  @EventListener
  public void onApplicationReady(final ApplicationReadyEvent event) {

    /** Thread 1: start the service for task service registration */
    iConnectService.addHandler("/event-monitor", eventMonitorRequestHandler);
    iConnectService.addHandler("/task", taskRequestHandler);
    iConnect.startService(iConnectService);

    // /** Thread 2: init the event monitor list cache */
    // eventOp.initEventMonitorCache();

    // new Timer().schedule(new TimerTask() {
    // @Override
    // public synchronized void run() {// use lock to guarantee tasks
    // // executed one by one
    // try {
    // List<Task> tasks = taskRepo.findByIsFailAndIsSentOrderByTaskTimeAsc(false, false);
    // if (tasks != null) {
    // for (Task task : tasks) {
    // logger.debug("====start handle task " + task.getName() + "====");
    // TaskClient.sendTaskMessage(task, new TaskResponseHanlder(task));
    // task.setSent(true);
    // taskRepo.save(task);
    // logger.debug("====finish handle task " + task.getName() + "====");
    // }
    // }
    // } catch (Exception e) {
    // logger.error("error happens");
    // e.printStackTrace();
    // }
    // }
    // }, 0, period);

    /** Thread 3: fetch failed task from DB and send it to Task service every 500ms */
    /**
     * new Timer().schedule(new TimerTask() { @Override public synchronized void run() {// use lock
     * to guarantee tasks // executed one by one try { List<Task> tasks =
     * taskRepo.findByIsFail(true); if (tasks != null) { for (Task task : tasks) {
     * logger.debug("====start handle failed task " + task.getName() + "====");
     *
     * <p>FailedHandler param = task.getfHandler(); if (param != null) { int count =
     * param.getCount(); int total = param.getTotal(); long current = new Date().getTime(); long
     * last = param.getLast().getTime(); if (count < total && current >= (last +
     * param.getInterval())) { TaskClient.sendTaskMessage(task, new TaskResponseHanlder()); } } else
     * { //TODO: failed task must have default handler param taskRepo.delete(task); }
     * logger.debug("====finish handle failed task " + task.getName() + "===="); } } } catch
     * (Exception e) { logger.error("error happens"); e.printStackTrace(); } } }, 0, period);
     */

    /** Thread 4: Query the live task services from the Ping service */
    /**
     * TODO: currently ping service is not available new Timer().schedule(new TimerTask()
     * { @Override public synchronized void run() { try { pingJmsClient.sendPingMessage(); } catch
     * (Exception e) { logger.error("error happens"); e.printStackTrace(); } } }, 0, period);
     */
  }
}
