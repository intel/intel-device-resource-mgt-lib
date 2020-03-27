/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.httpproxy.utils;

import com.openiot.cloud.base.help.MessageIdMaker;
import com.openiot.cloud.base.profiling.DurationCounter;
import com.openiot.cloud.base.profiling.DurationCounterManage;
import com.openiot.cloud.base.profiling.DurationCounterOfUrlBuilder.CounterOfUrl;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class HttpProxyCustomLogging extends HandlerInterceptorAdapter {
  private static Logger logger = LoggerFactory.getLogger(HttpProxyCustomLogging.class);

  private static final String ATTRIBUTE_ID = "X-REQUEST-UUID";
  private static final String ATTRIBUTE_TEIMSTAMP = "X-REQUEST-TIMESTAMP";

  @Autowired
  private DurationCounterManage counterManage;

  @Autowired
  private List<CounterOfUrl> counterOfUrls;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                           Object object) {
    String requestUUID = (String) request.getAttribute(ATTRIBUTE_ID);
    if (requestUUID == null) {
      requestUUID = Long.toString(MessageIdMaker.getMessageIdAsInteger());
      request.setAttribute(ATTRIBUTE_ID, requestUUID);
    } else {
      logger.info("#{} request comes again", requestUUID);
    }

    Long timestamp = (Long) request.getAttribute(ATTRIBUTE_TEIMSTAMP);
    if (timestamp == null || timestamp.longValue() == 0) {
      timestamp = Instant.now(Clock.systemUTC()).toEpochMilli();
      request.setAttribute(ATTRIBUTE_TEIMSTAMP, timestamp);
    }

    HttpServletRequest requestCached = new ContentCachingRequestWrapper(request);
    logger.info("[request] {} {} {} HEAD {}, uuid {}",
                requestCached.getMethod(),
                requestCached.getRequestURI(),
                requestCached.getPathInfo(),
                getHeadersInfo(request),
                requestUUID);
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                              Object object, Exception e) {
    Optional<String> requestUUID = Optional.ofNullable((String) request.getAttribute(ATTRIBUTE_ID));
    Optional<Long> requestTimestamp =
        Optional.ofNullable((Long) request.getAttribute(ATTRIBUTE_TEIMSTAMP));

    HttpServletRequest requestCached = new ContentCachingRequestWrapper(request);
    HttpServletResponse responseCached = new ContentCachingResponseWrapper(response);
    String responseInfo = String.format("[response] %s %s %s",
                                        responseCached.getStatus(),
                                        responseCached.getContentType(),
                                        getHeadersInfo(response));

    long now = Instant.now(Clock.systemUTC()).toEpochMilli();
    long timeCost = now - requestTimestamp.orElse(now + 1);

    logger.info(String.format(" %s, uuid %s, time cost %d",
                              responseInfo,
                              requestUUID.orElse("EMPTY_UUID"),
                              timeCost));

    addCount(requestCached.getRequestURI(), timeCost);
    return;
  }

  private String getHeadersInfo(HttpServletRequest request) {
    StringBuilder sb = new StringBuilder();
    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String k = headerNames.nextElement();
      String v = request.getHeader(k);
      sb.append(String.format("%s : %s,", k, v));
    }
    return sb.toString();
  }

  private String getHeadersInfo(HttpServletResponse response) {
    StringBuilder sb = new StringBuilder();
    Iterable<String> headerNames = response.getHeaderNames();
    for (String k : headerNames) {
      String v = response.getHeader(k);
      sb.append(String.format("%s : %s,", k, v));
    }
    return sb.toString();
  }

  private void addCount(String requestUri, long timeCost) {
    if (requestUri != null && !requestUri.isEmpty()) {
      String shortestMatch = null;
      int shortestMatchLength = Integer.MAX_VALUE;
      for (CounterOfUrl counter : counterOfUrls) {
        String curCounterUrl = counter.getUrl();
        if (requestUri.startsWith(curCounterUrl) && curCounterUrl.length() < shortestMatchLength) {
          shortestMatch = curCounterUrl;
        }
      }
      if (shortestMatch != null) {
        DurationCounter counter = counterManage.getCounter(shortestMatch);
        if (counter != null) {
          counter.count(timeCost);
        }
      }
    }
  }
}
