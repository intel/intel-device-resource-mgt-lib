/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.alarm;

public class ConstDef {

  public static final String F_ID = "_id";
  public static final String F_IID = "id";

  public static final String F_TAGDEVID = "di";
  public static final String P_TAGDEVID = "tagid.di";
  public static final String F_TAGRES = "res";
  public static final String P_TAGDRES = "tagid.res";
  public static final String F_TAGPROP = "pt";
  public static final String P_TAGPROP = "tagid.pt";
  public static final String P_TAGGRP = "tagid.grp";
  public static final String F_TAGDSN = "dsn";
  public static final String P_TAGDSN = "tagid.dsn";
  public static final String F_UNIT = "unit";
  public static final String F_PROJECT = "project";

  public static final String F_UNITDAY = "d";
  public static final String Q_PAGE = "page";
  public static final String Q_LIMIT = "limit";
  public static final String Q_BEGIN = "begin";
  public static final String Q_END = "end";
  public static final String Q_ID = "id";
  public static final String Q_FROM = "from";
  public static final String Q_TO = "to";
  public static final String Q_NAME = "name";
  public static final String Q_UNIT = "unit";

  // default page
  public static final int DFLT_PAGE = com.openiot.cloud.base.help.ConstDef.DFLT_PAGE;
  // default page size
  public static final int DFLT_SIZE = com.openiot.cloud.base.help.ConstDef.DFLT_SIZE;
  public static final String URL_ALARM = "/api/alarm";
  public static final String URL_DSS_TOTAL_STATS = "/api/statsdss";
  public static final String URL_ALARM_CNT = "/api/alarm/count";
}
