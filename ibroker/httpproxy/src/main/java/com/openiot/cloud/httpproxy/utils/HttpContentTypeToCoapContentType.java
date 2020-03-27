/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.httpproxy.utils;

import org.iotivity.cloud.base.protocols.enums.ContentFormat;
import org.springframework.http.MediaType;

public class HttpContentTypeToCoapContentType {

  public static ContentFormat transfer(MediaType httpType) {
    if (httpType == null) {
      return ContentFormat.NO_CONTENT;
    }

    if (httpType.isCompatibleWith(MediaType.TEXT_PLAIN)) {
      return ContentFormat.APPLICATION_TEXTPLAIN;
    } else if (httpType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
      return ContentFormat.APPLICATION_JSON;
    } else if (httpType.isCompatibleWith(MediaType.APPLICATION_OCTET_STREAM)) {
      return ContentFormat.APPLICATION_OCTET_STREAM;
    } else {
      return ContentFormat.APPLICATION_TEXTPLAIN;
    }
  }

  public static MediaType transfer(ContentFormat cf) {
    if (cf == ContentFormat.APPLICATION_TEXTPLAIN) {
      return MediaType.TEXT_PLAIN;
    } else if (cf == ContentFormat.APPLICATION_JSON) {
      return MediaType.APPLICATION_JSON;
    } else if (cf == ContentFormat.APPLICATION_OCTET_STREAM) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }

    return MediaType.TEXT_PLAIN;
  }
}
