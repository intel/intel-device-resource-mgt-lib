/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.httpproxy;

import com.openiot.cloud.base.profiling.DurationCounter;
import com.openiot.cloud.base.profiling.DurationCounterManage;
import com.openiot.cloud.httpproxy.utils.HttpProxyCustomLogging;
import lombok.extern.slf4j.Slf4j;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class ApplicationTest {
  @Autowired
  private HttpProxyCustomLogging httpProxyCustomLogging;
  @Autowired
  private DurationCounterManage counterManage;

  private final static Logger counterLogger = LoggerFactory.getLogger("COUNTERLOGGER");

  @Test
  public void contextLoad() throws Exception {
    counterLogger.info("info from a counter logger");
  }

  @Test
  public void testCounter() throws Exception {
    String[] urls = new String[] {"/fc/meta/restype", "/fc/controllog/events", "/fc/dp/raw",
        "/fc/meta/grptype", "/fc/opt/iagent/dad395b3dce1/sysinfo", "/fc/api/alarm",
        "/fc/api/project", "/fc/stats/hstr/cfg", "/fc/dp/prodata"};

    for (String url : urls) {
      ReflectionTestUtils.invokeMethod(httpProxyCustomLogging, "addCount", url, 100l);
    }

    DurationCounter counter = counterManage.getCounter("/fc/meta/restype");
    Map<Long, Long> executionTimeStatistic =
        (Map<Long, Long>) ReflectionTestUtils.getField(counter, "executionTimeStatistic");
    assertThat(executionTimeStatistic).isNotNull();
    assertThat(executionTimeStatistic.get(0l)).isEqualTo(1l);

    counter = counterManage.getCounter("/fc/meta/grptype");
    executionTimeStatistic =
        (Map<Long, Long>) ReflectionTestUtils.getField(counter, "executionTimeStatistic");
    assertThat(executionTimeStatistic).isNotNull();
    assertThat(executionTimeStatistic.get(0l)).isEqualTo(1l);

    counter = counterManage.getCounter("/fc/dp/raw");
    executionTimeStatistic =
        (Map<Long, Long>) ReflectionTestUtils.getField(counter, "executionTimeStatistic");
    assertThat(executionTimeStatistic).isNotNull();
    assertThat(executionTimeStatistic.get(0l)).isEqualTo(1l);

    counter = counterManage.getCounter("/fc/opt");
    executionTimeStatistic =
        (Map<Long, Long>) ReflectionTestUtils.getField(counter, "executionTimeStatistic");
    assertThat(executionTimeStatistic).isNotNull();
    assertThat(executionTimeStatistic.get(0l)).isEqualTo(1l);

    counter = counterManage.getCounter("/fc/api/alarm");
    executionTimeStatistic =
        (Map<Long, Long>) ReflectionTestUtils.getField(counter, "executionTimeStatistic");
    assertThat(executionTimeStatistic).isNotNull();
    assertThat(executionTimeStatistic.get(0l)).isEqualTo(1l);

    counter = counterManage.getCounter("/fc/stats");
    executionTimeStatistic =
        (Map<Long, Long>) ReflectionTestUtils.getField(counter, "executionTimeStatistic");
    assertThat(executionTimeStatistic).isNotNull();
    assertThat(executionTimeStatistic.get(0l)).isEqualTo(1l);

    counter = counterManage.getCounter("/fc/dp/raw");
    executionTimeStatistic =
        (Map<Long, Long>) ReflectionTestUtils.getField(counter, "executionTimeStatistic");
    assertThat(executionTimeStatistic).isNotNull();
    assertThat(executionTimeStatistic.get(0l)).isEqualTo(1l);

    counter = counterManage.getCounter("/fc/api/project");
    executionTimeStatistic =
        (Map<Long, Long>) ReflectionTestUtils.getField(counter, "executionTimeStatistic");
    assertThat(executionTimeStatistic).isNotNull();
    assertThat(executionTimeStatistic.get(0l)).isEqualTo(1l);

    counter = counterManage.getCounter("/fc/dp/prodata");
    executionTimeStatistic =
        (Map<Long, Long>) ReflectionTestUtils.getField(counter, "executionTimeStatistic");
    assertThat(executionTimeStatistic).isNotNull();
    assertThat(executionTimeStatistic.get(0l)).isEqualTo(1l);

    log.info("beforecheckAndDump");
    ReflectionTestUtils.invokeGetterMethod(counterManage, "checkAndDump");
    log.info("after checkAndDump");
  }
}
