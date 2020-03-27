/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.httpproxy.utils;

import org.iotivity.cloud.base.protocols.enums.RequestMethod;
import org.springframework.http.HttpMethod;

public class HttpMethodToCoapMethod {
  public static RequestMethod transfer(HttpMethod hmethod) {
    switch (hmethod) {
      case GET:
        return RequestMethod.GET;
      case PUT:
        return RequestMethod.PUT;
      case POST:
        return RequestMethod.POST;
      case DELETE:
        return RequestMethod.DELETE;
      case HEAD:
        return RequestMethod.HEAD;
      case OPTIONS:
        return RequestMethod.OPTIONS;
      case TRACE:
        return RequestMethod.TRACE;
      case PATCH:
        return RequestMethod.PATCH;
      default:
        break;
    }

    return null;
  }
}
