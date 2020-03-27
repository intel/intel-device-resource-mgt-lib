/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.utilities;

import com.openiot.cloud.base.help.BaseUtil;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

public class UrlUtil {
  public static String getPotocol(String url) {
    if (url == null)
      return null;
    if (url.startsWith("jms:") || url.startsWith("JMS:"))
      return url.substring(0, 3);

    int indexFlag = url.indexOf("://");

    if (indexFlag < 0)
      return "jms"; // jms as default if no protocol set

    return url.substring(0, indexFlag);
  }

  public static String getPath(String url) {
    if (url == null)
      return null;

    String protoFlag;
    boolean isJms = true;
    if (url.startsWith("jms://") || url.startsWith("JMS://")) {
      protoFlag = "://";
    } else if (url.startsWith("jms:") || url.startsWith("JMS:")) {
      protoFlag = ":";
    } else {
      protoFlag = "://";
      isJms = false;
    }

    int indexFlagProto = url.indexOf(protoFlag);
    if (indexFlagProto >= 0) {
      url = url.substring(indexFlagProto + protoFlag.length());
    } else {
      isJms = true; // set jms as default, if there is no protocol flag
    }

    if (!isJms) { // remove host:port for protocols except JMS
      int indexPath = url.indexOf("/");
      if (indexPath >= 0)
        url = url.substring(indexPath);
    }

    int indexFlagQuery = url.indexOf("?");
    if (indexFlagQuery >= 0)
      url = url.substring(0, indexFlagQuery);

    url = BaseUtil.removeTrailingSlash(url);
    return url;
  }

  public static String getHost(String url) {
    if (url == null)
      return null;
    if (url.startsWith("jms://") || url.startsWith("JMS://")) {
      return null;
    }

    if (url.startsWith("jms:") || url.startsWith("JMS:")) {
      return null;
    }

    String protoFlag = "://";

    int indexFlagProto = url.indexOf(protoFlag);
    if (indexFlagProto < 0)
      return null;

    url = url.substring(indexFlagProto + protoFlag.length());

    int indexPath = url.indexOf("/");
    if (indexPath < 0)
      return null;
    url = url.substring(0, indexPath);

    if (url == null)
      return null;

    String[] hostPort = url.split(":");
    return hostPort[0];
  }

  public static String getPort(String url) {
    if (url == null)
      return null;
    if (url.startsWith("jms://") || url.startsWith("JMS://")) {
      return null;
    }

    if (url.startsWith("jms:") || url.startsWith("JMS:")) {
      return null;
    }

    String protoFlag = "://";

    int indexFlagProto = url.indexOf(protoFlag);
    if (indexFlagProto < 0)
      return null;

    url = url.substring(indexFlagProto + protoFlag.length());

    int indexPath = url.indexOf("/");
    if (indexPath < 0)
      return null;
    url = url.substring(0, indexPath);

    if (url == null)
      return null;

    String[] hostPort = url.split(":");

    return hostPort.length < 2 ? null : hostPort[1];
  }

  public static String getQueryParam(String url, String key) {
    Map<String, String> allParams = getAllQueryParam(url);

    return allParams == null ? null : allParams.get(key);
  }

  // TODO:
  public static Map<String, String> getAllQueryParam(String url) {
    // return UriComponentsBuilder.fromUriString(url).build().getQueryParams();

    Map<String, String> query_pairs = new LinkedHashMap<String, String>();
    if (url == null)
      return query_pairs;

    int indexFlag = url.indexOf("?");
    if (indexFlag < 0)
      return query_pairs;

    url = url.substring(indexFlag + 1);
    if (url == null)
      return query_pairs;

    String[] pairs = url.indexOf("&") > 0 ? url.split("&") : url.split(";");
    for (String pair : pairs) {
      if (pair == null || pair.length() == 0)
        continue;

      int idx = pair.indexOf("=");
      try {
        if ((idx < 0)) {
          query_pairs.put(URLDecoder.decode(pair, "UTF-8"), null);
        } else {
          query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                          URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
    }
    return query_pairs;
  }
}
