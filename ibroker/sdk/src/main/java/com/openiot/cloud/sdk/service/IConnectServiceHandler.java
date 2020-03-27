/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.service;

import org.springframework.stereotype.Component;

@Component
public interface IConnectServiceHandler {
  public void onRequest(IConnectRequest request);
}
