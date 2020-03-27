/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.service;

public interface IConnectFailureHandler<T> {
  public void onFailure(T request);
}
