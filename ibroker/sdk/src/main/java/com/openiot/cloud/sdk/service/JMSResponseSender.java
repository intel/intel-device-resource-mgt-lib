/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.service;

import javax.jms.Destination;

public class JMSResponseSender implements IConnectResponseSender {
  private Destination dest;

  public JMSResponseSender(Destination dest) {
    this.dest = dest;
  }

  public void send(IConnectResponse response) {
    IConnect.getInstance().send(dest, response);
  }
}
