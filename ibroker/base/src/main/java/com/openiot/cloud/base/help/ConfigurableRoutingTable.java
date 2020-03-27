/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.help;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.mongo.model.help.RoutingTableItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigurableRoutingTable {
  private static final Logger logger = LoggerFactory.getLogger(ConfigurableRoutingTable.class);
  // support localhost, IP, and domain name
  private static final String PORT_PATTERN = "\\d{1,5}";
  private static final String LOCALHOST_PATTERN = "(localhost)" + ":(" + PORT_PATTERN + ")";
  private static final String IP_ADDRESS_PATTERN =
      "((\\d{1,3}\\.){3}\\d{1,3})" + ":(" + PORT_PATTERN + ")";
  private static final String DOMAIN_NAME_PATTERN =
      "((\\S{1,63}\\.)+[A-Za-z]{2,6})" + ":(" + PORT_PATTERN + ")";

  public static Map<String, InetSocketAddress> readRoutingTable(InputStream in) {
    if (in == null) {
      logger.warn("can not read a null input stream");
      return null;
    }

    logger.debug("the input stream " + in.toString());

    // read configuration file which is formed as JSON
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, InetSocketAddress> routingTable = new HashMap<String, InetSocketAddress>();

    Pattern localhost = Pattern.compile(LOCALHOST_PATTERN);
    Pattern ipAddress = Pattern.compile(IP_ADDRESS_PATTERN);
    Pattern domainName = Pattern.compile(DOMAIN_NAME_PATTERN);
    try {
      RoutingTableItem[] allItems = objectMapper.readValue(in, RoutingTableItem[].class);
      if (allItems == null || allItems.length == 0) {
        logger.warn("it is an empty routing table");
        return null;
      }

      logger.debug("RoutingTableItem[].length " + allItems.length);
      logger.debug("localhost pattern " + localhost);
      logger.debug("ip address  pattern " + ipAddress);
      logger.debug("domain name pattern " + domainName);

      for (RoutingTableItem item : allItems) {
        Matcher matcherLocalhost = localhost.matcher(item.getInetAddr());
        if (matcherLocalhost.matches()) {
          String hostname = matcherLocalhost.group(1);
          int port = Integer.parseInt(matcherLocalhost.group(2));
          InetSocketAddress inetAddr = new InetSocketAddress(hostname, port);
          logger.debug(item + " matches localhost pattern " + inetAddr);
          routingTable.put(item.getUriPath(), inetAddr);
          continue;
        }

        Matcher matcherIpaddress = ipAddress.matcher(item.getInetAddr());
        if (matcherIpaddress.matches()) {
          String hostname = matcherIpaddress.group(1);
          int port = Integer.parseInt(matcherIpaddress.group(3));
          InetSocketAddress inetAddr = new InetSocketAddress(hostname, port);
          routingTable.put(item.getUriPath(), inetAddr);
          logger.debug(item + " matches ipaddress pattern " + inetAddr);
          continue;
        }

        Matcher matcherDomainName = domainName.matcher(item.getInetAddr());
        if (matcherDomainName.matches()) {
          String hostname = matcherDomainName.group(1);
          int port = Integer.parseInt(matcherDomainName.group(3));
          InetSocketAddress inetAddr = new InetSocketAddress(hostname, port);
          routingTable.put(item.getUriPath(), inetAddr);
          logger.debug(item + " matches domainName pattern " + inetAddr);
          continue;
        }

        logger.warn("item " + item + " is a unknown pattern");
      }
    } catch (Exception e) {
      logger.warn(BaseUtil.getStackTrace(e));
      return null;
    }

    if (routingTable.isEmpty()) {
      logger.warn("doesn't get any valid information");
      return null;
    }

    return routingTable;
  }

  public static String dump(Map<String, InetSocketAddress> routingTable) {
    StringBuilder sb = new StringBuilder();

    for (Map.Entry<String, InetSocketAddress> entry : routingTable.entrySet()) {
      String k = entry.getKey();
      InetSocketAddress v = entry.getValue();

      sb.append(String.format("%s -> %s:%d  ", k, v.getHostString(), v.getPort()));
    }

    return sb.toString();
  }
}
