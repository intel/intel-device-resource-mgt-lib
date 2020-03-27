/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.profiling;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DurationCounterOfUrlBuilder {
  public static class CounterOfUrl {
    private String url;
    private String configName;

    @JsonCreator
    public CounterOfUrl(@JsonProperty("url") String url,
        @JsonProperty("config") String configName) {
      this.url = url;
      this.configName = configName;
    }

    public String getUrl() {
      return url;
    }

    public String getConfigName() {
      return configName;
    }

    @Override
    public String toString() {
      return "CounterOfUrl{" + "url='" + url + '\'' + ", configName='" + configName + '\'' + '}';
    }
  }

  public static List<CounterOfUrl> readCounterOfUrl(String pathName) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      CounterOfUrl[] counters =
          mapper.readValue(new ClassPathResource(pathName).getInputStream(), CounterOfUrl[].class);
      return Arrays.asList(counters);
    } catch (IOException e) {
      e.printStackTrace();
      return Collections.emptyList();
    }
  }
}
