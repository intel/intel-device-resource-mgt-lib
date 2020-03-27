/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.cfg.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Content {
  @JsonProperty("id")
  private String id;

  @JsonProperty("tt")
  private String tt;

  @JsonProperty("ti")
  private String ti;

  @JsonProperty("cs")
  private String cs;

  public Content() {}
}
