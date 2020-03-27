/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.httpproxy.utils;

import org.iotivity.cloud.base.protocols.enums.ResponseStatus;
import org.springframework.http.HttpStatus;

public class CoapStatusToHttpStatus {
  public static HttpStatus transfer(ResponseStatus status) {
    switch (status) {
      case CREATED:
        return HttpStatus.CREATED;
      case DELETED:
        return HttpStatus.OK;
      case VALID:
        return HttpStatus.OK;
      case CHANGED:
        return HttpStatus.OK;
      case CONTENT:
        return HttpStatus.OK;
      case BAD_REQUEST:
        return HttpStatus.BAD_REQUEST;
      case UNAUTHORIZED:
        return HttpStatus.UNAUTHORIZED;
      case BAD_OPTION:
        return HttpStatus.NOT_ACCEPTABLE;
      case FORBIDDEN:
        return HttpStatus.FORBIDDEN;
      case NOT_FOUND:
        return HttpStatus.NOT_FOUND;
      case METHOD_NOT_ALLOWED:
        return HttpStatus.METHOD_NOT_ALLOWED;
      case NOT_ACCEPTABLE:
        return HttpStatus.NOT_ACCEPTABLE;
      case PRECONDITION_FAILED:
        return HttpStatus.PRECONDITION_FAILED;
      case REQUEST_ENTITY_TOO_LARGE:
        return HttpStatus.PAYLOAD_TOO_LARGE;
      case UNSUPPORTED_CONTENT_FORMAT:
        return HttpStatus.UNSUPPORTED_MEDIA_TYPE;
      case INTERNAL_SERVER_ERROR:
        return HttpStatus.INTERNAL_SERVER_ERROR;
      case NOT_IMPLEMENTED:
        return HttpStatus.NOT_IMPLEMENTED;
      case BAD_GATEWAY:
        return HttpStatus.BAD_GATEWAY;
      case SERVICE_UNAVAILABLE:
        return HttpStatus.SERVICE_UNAVAILABLE;
      case GATEWAY_TIMEOUT:
        return HttpStatus.GATEWAY_TIMEOUT;
      case PROXY_NOT_SUPPORTED:
        return HttpStatus.SERVICE_UNAVAILABLE;
      default:
        break;
    }

    return HttpStatus.NOT_ACCEPTABLE;
  }
}
