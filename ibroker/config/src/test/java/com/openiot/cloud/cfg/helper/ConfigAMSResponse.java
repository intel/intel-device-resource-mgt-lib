/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.cfg.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ConfigAMSResponse {
  @JsonProperty("cfg_uuid")
  private String cfgUUID;

  @JsonProperty("content")
  // it is a json string, not a json object
  private String content;

  @JsonProperty("path_name")
  private String pathName;

  @JsonProperty("product_name")
  private String productName;

  @JsonProperty("target_id")
  private String targetID;

  @JsonProperty("target_type")
  private String targetType;

  public ConfigAMSResponse() {}
}
