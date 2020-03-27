/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model.help;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.help.ConstDef;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RoutingTableItem {

  @JsonProperty(ConstDef.RT_ITEM_URI)
  String uripath; // service uri path

  @JsonProperty(ConstDef.RT_ITEM_INETADDR)
  String inetaddr; // service ipaddr + port

  @JsonCreator
  public RoutingTableItem(@JsonProperty(ConstDef.RT_ITEM_URI) String uripath,
      @JsonProperty(ConstDef.RT_ITEM_INETADDR) String inetaddr) {
    super();
    this.uripath = uripath;
    this.inetaddr = inetaddr;
  }

  public String getUriPath() {
    return uripath;
  }

  public String getInetAddr() {
    return inetaddr;
  }

  @Override
  public String toString() {
    return "RoutingTableItem{" + "uripath='" + uripath + '\'' + ", inetaddr='" + inetaddr + '\''
        + '}';
  }
}
