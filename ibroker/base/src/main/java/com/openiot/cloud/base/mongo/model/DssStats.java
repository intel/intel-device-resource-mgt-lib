/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DssStats {
  public DssStats() {
    pass_ratio = 0;
    num_over = 0;
    num_room = 0;
    td_total = (long) 0;
    td_ava = (Double) 0.0;
    td_min = (long) 0;
    td_max = (long) 0;
  }

  public Date ts;
  public Date te;
  public String name;
  public Integer pass_ratio;
  public Integer num_over;
  public Integer num_room;
  public Long td_total;
  public Double td_ava;
  public Long td_min;
  public Long td_max;

  @Override
  public String toString() {
    return "DssStats [num_over=" + num_over + ", num_room=" + num_room + ", td_total=" + td_total
        + ", td_ava=" + td_ava + ", td_min=" + td_min + ", td_max=" + td_max + "]";
  }
}
