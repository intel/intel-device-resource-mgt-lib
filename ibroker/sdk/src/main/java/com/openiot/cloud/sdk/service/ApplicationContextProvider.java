/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.service;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextProvider implements ApplicationContextAware {
  private static ApplicationContext context;

  public static ApplicationContext getApplicaitonContext() {
    return context;
  }

  public static <T> T getBean(Class<T> aClass) {
    return context.getBean(aClass);
  }

  public static <T> T getBean(String name, Class<T> aClass) {
    return context == null ? null : context.getBean(name, aClass);
  }

  @Override
  public void setApplicationContext(ApplicationContext ctx) throws BeansException {
    context = ctx;
  }

  public static void setAppContext(ApplicationContext applicationContext) {
    context = applicationContext;
  }
}
