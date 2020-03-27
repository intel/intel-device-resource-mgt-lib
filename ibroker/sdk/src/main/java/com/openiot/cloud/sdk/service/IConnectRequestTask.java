/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

// package com.openiot.cloud.sdk.service;
//
// import java.io.IOException;
// import java.util.Arrays;
// import org.springframework.http.HttpMethod;
// import com.fasterxml.jackson.core.JsonParseException;
// import com.fasterxml.jackson.databind.JsonMappingException;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.openiot.cloud.base.mongo.model.Task;
//
// public class IConnectRequestTask extends IConnectRequest {
// private Task task;
//
// public static IConnectRequestTask create(HttpMethod action, String messageId, String urlSrc,
// String uriPath, String format, byte[] payload) {
// IConnectRequestTask req = new IConnectRequestTask();
// req.setMessageID(messageId);
// req.setAction(action);
// req.setUrl(uriPath);
// req.setFormat(format);
// req.setPayload(payload);
//
// Task tmpTask;
// try {
// tmpTask = new ObjectMapper().readValue(payload, Task.class);
// req.setTask(tmpTask);
// } catch (JsonParseException e) {
// e.printStackTrace();
// } catch (JsonMappingException e) {
// e.printStackTrace();
// } catch (IOException e) {
// e.printStackTrace();
// }
//
// return req;
// }
//
// public Task getTask() {
// return task;
// }
//
// public void setTask(Task task) {
// this.task = task;
// }
//
// @Override
// public String toString() {
// return "IConnectRequestTask [task=" + task + ", messageID=" + messageID + ", action=" + action
// + ", destPath=" + url + ", format=" + format + ", payload=" + Arrays.toString(payload)
// + "]";
// }
// }
