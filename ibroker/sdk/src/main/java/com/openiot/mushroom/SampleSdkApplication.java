/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.openiot.cloud.base.mongo.model.Task;
// import com.openiot.cloud.base.mongo.model.TaskService;
// import com.openiot.cloud.sdk.service.*;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.boot.context.event.ApplicationReadyEvent;
// import org.springframework.context.event.EventListener;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.MediaType;
// import java.io.IOException;
//
//// IT IS a sample applicaiton using cloud sdk package com.openiot.mushroom;
//
//// WARNING: do not modify this line
// @SpringBootApplication(scanBasePackages = {"com.openiot.cloud", "com.openiot.mushroom"})
// public class SampleSdkApplication {
//
// @Autowired
// private IConnect iConnect;
//
// @Autowired
// private IConnectService iConnectService;
//
// // WARNING: do not modify this function
// public static void main(String[] args) {
// SpringApplication.run(SampleSdkApplication.class, args);
// }
//
// // WARNING: do not modify the annotation
// @EventListener
// public void onApplicationReady(final ApplicationReadyEvent event) {
// iConnectService.addHandler(
// "/mushroom/checkin",
// new IConnectServiceHandler() {
// @Override
// public void onRequest(IConnectRequest request) {
// // more business logic code here
// IConnectResponse resp =
// IConnectResponse.createFromRequest(
// request, HttpStatus.OK, MediaType.APPLICATION_JSON, null);
// resp.send();
// }
// });
// // conn.startService(service, 5682); // IConnectCoap supported in future
// iConnect.startService(iConnectService);
//
// IConnectTaskService taskService = IConnectTaskService.create();
// TaskService regInfo = new TaskService();
// regInfo.setName("Stats Service");
// taskService.addHandler(
// regInfo,
// new IConnectServiceHandler() {
// @Override
// public void onRequest(IConnectRequest request) {
// byte[] payload = request.getPayload();
//
// try {
// Task task = new ObjectMapper().readValue(payload, Task.class);
// // more business logic code here
// IConnectResponse resp =
// IConnectResponse.createFromRequest(
// request, HttpStatus.OK, MediaType.APPLICATION_JSON, task.getP());
// resp.send();
// } catch (IOException e) {
// e.printStackTrace();
// }
// }
// });
// iConnect.startService(taskService);
// }
// }
