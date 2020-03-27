/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AmsConfigIdendifier {
  public String cfg_uuid;
  public String product_name;
  public String path_name;
  public String target_type;
  public String default_content;
  public String target_id;
  public String content;
  public String content_name;
}
