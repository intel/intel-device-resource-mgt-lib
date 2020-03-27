/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.testbase;

public class TestConstDef {
  public static final double DELTA = 1e-15;

  // resources files
  // for imported collections
  public static final String R_IM_PREFIX = "/importdb/";
  public static final String R_IM_SUFFIX = ".jsons";
  // end resources files

  // ids
  public static final String I_DEV1 = "00000000-0000-0000-0000-000000000001"; // device 1
  public static final String I_DEV2 = "00000000-0000-0000-0000-000000000002"; // device 2
  public static final String I_DEV6 = "00000000-0000-0000-0000-000000000006"; // device 6 for iagent
  public static final String I_RT1 = "_id_resourceType_1"; // ResourceType 1
  public static final String I_RT2 = "_id_resourceType_2"; // ResourceType 2
  // end ids

  // names
  public static final String N_GRPTYPE1 = "n_groupType_1";
  public static final String N_GRPTYPE2 = "n_groupType_2";
  public static final String N_GT1ATTR1 = "n_gt1-attr1";
  public static final String N_GT1ATTR2 = "n_gt1-attr2";
  public static final String N_GT2ATTR1 = "n_gt2-attr1";
  public static final String N_GT2ATTR2 = "n_gt2-attr2";
  public static final String N_DEVTYPE1 = "n_deviceType_1";
  public static final String N_DEVTYPE2 = "n_deviceType_2";
  public static final String N_RESTYPE1 = "n_resourceType_1";
  public static final String N_RESTYPE2 = "n_resourceType_2";
  public static final String N_GRP1 = "n_group_1"; // group 1
  public static final String N_GRP2 = "n_group_2"; // group 2
  public static final String N_RT1PRO1 = "n_rt1-property_1";
  public static final String N_RT2PRO1 = "n_rt2-property_1";
  public static final String N_RT2PRO2 = "n_rt2-property_2";
  // end names

  // resource url
  public static final String U_RES1 = "/d1/xxx/x1";
  public static final String U_RES2 = "/d2/xxx/x1";
  public static final String U_RES3 = "/d2/xxx/x2";
  // end resource url

  // attribute value
  public static final String V_GT1ATTR2 = "n_gt1-attr2 default value";
  public static final String V_GT2ATTR2 = "n_gt2-attr2 default value";
  // end attribute value

  // resource type property definition
  // end resource type property definition

  // response fields
  public static final String F_DEVID = "di"; // device id
  // end response fields

  // mongodb objectid
  public static final String F_OBJID = "$oid"; // device id
}
