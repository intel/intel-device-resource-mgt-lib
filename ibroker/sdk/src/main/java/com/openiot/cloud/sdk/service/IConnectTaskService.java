/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

// package com.openiot.cloud.sdk.service;
//
// import java.util.HashMap;
// import java.util.Set;
// import java.util.concurrent.TimeUnit;
// import org.slf4j.Logger;
// import org.springframework.beans.factory.annotation.Qualifier;
// import org.springframework.context.annotation.Scope;
// import org.springframework.http.HttpMethod;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.MediaType;
// import org.springframework.stereotype.Component;
// import com.fasterxml.jackson.core.JsonProcessingException;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.openiot.cloud.base.help.MessageIdMaker;
// import com.openiot.cloud.base.mongo.model.TaskService;
// import com.openiot.cloud.sdk.Constants;
//
/// **
// * The abstract task service based on JMS protocol.<br> <br>
// *
// * @author saigon
// * @version 1.0
// * @since Oct 2017
// */
// @Component
// @Scope("prototype")
// @Qualifier("IConnectTaskService")
// public class IConnectTaskService extends IConnectService {
// private static final Logger logger =
// LoggerFactory.getLogger(IConnectTaskService.class.getName());
// private HashMap<String, TaskService> serviceRegInfos = new HashMap<String, TaskService>();
//
// public static IConnectTaskService create() {
// return ApplicationContextProvider.getBean("IConnectTaskService", IConnectTaskService.class);
// }
//
// public void addHandler(TaskService regInfo, IConnectServiceHandler handler) {
// if (regInfo == null) {
// logger.warn("null registry info for taskserver");
// return;
// }
// String existingID = serviceRegInfos.keySet()
// .stream()
// .filter((key) -> isRegInfoEqual(serviceRegInfos.get(key),
// regInfo))
// .findAny()
// .orElse(null);
// if (existingID == null) {
// Integer id = MessageIdMaker.getMessageIdAsInteger();
// regInfo.setId(null);
// serviceRegInfos.put("" + id, regInfo);
// String sp = regInfo.getSrvProvider();
// addHandler(sp == null ? regInfo.getName() : sp, handler);
// if (sp == null)
// regInfo.setSrvProvider("jms://" + regInfo.getName());
// } else { // update existing reginfo during runtime
// logger.warn("re-register task service!");
// addHandler(regInfo.getName(), handler);
// reRegisterTaskService(existingID, regInfo);
// }
// }
//
// private void reRegisterTaskService(String existingRegID, TaskService regInfo) {
// serviceRegInfos.put(existingRegID, regInfo);
// registerTask(existingRegID);
// }
//
// private boolean isRegInfoEqual(TaskService taskService, TaskService taskService2) {
// if (taskService == null || taskService2 == null)
// return false;
//
// return taskService.getName()
// .equals(taskService2.getName())
// && taskService.getType()
// .equals(taskService2.getType())
// && taskService.getSrvProvider()
// .equals(taskService2.getSrvProvider());
// }
//
// @Override
// public void onInit() {
// registerTask(null);
// }
//
// private void registerTask(String regID) {
// if (serviceRegInfos.size() == 0)
// return;
//
// if (regID == null) {
// final Set<String> ids = serviceRegInfos.keySet();
// for (String id : ids) {
// if (serviceRegInfos.get(id)
// .getId() == null)
// registerTask(id);
// }
// } else {
// TaskService regInfo = serviceRegInfos.get(regID);
// logger.info("registerTask " + regInfo);
// if (regInfo != null) {
// try {
// IConnectRequest regRequest = IConnectRequest.create(HttpMethod.POST,
// Constants.MQ_DST_TASK_ENGINE, MediaType.APPLICATION_JSON.toString(),
// new ObjectMapper().writeValueAsBytes(regInfo));
// regRequest.setMessageID(regID);
// regRequest.send(response -> onRegResponse(response), 60, TimeUnit.SECONDS);
// } catch (JsonProcessingException e) {
// e.printStackTrace();
// }
// } else {
// logger.warn("No reg info for service ID: " + regID);
// }
// }
// }
//
// public void onRegResponse(IConnectResponse response) {
// String id = response.getMessageID();
// if (response.getStatus()
// .equals(HttpStatus.OK)) {
// if (serviceRegInfos.containsKey(id)) {
// serviceRegInfos.get(id)
// .setId(id);
// } else {
// logger.error("Wrong register response ID received: " + id);
// }
// } else if (response.getStatus()
// .equals(HttpStatus.REQUEST_TIMEOUT)) {
// logger.error("register timeout response(60s) received, register again: " + id);
// registerTask(id);
// } else {
// logger.error("register fail response: " + id);
// }
// }
// }
