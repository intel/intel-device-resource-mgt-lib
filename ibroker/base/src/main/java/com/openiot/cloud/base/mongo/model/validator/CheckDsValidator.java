/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model.validator;

import com.openiot.cloud.base.mongo.dao.DeviceRepository;
import com.openiot.cloud.base.mongo.dao.GroupRepository;
import com.openiot.cloud.base.mongo.dao.ResourceRepository;
import com.openiot.cloud.base.mongo.model.help.DataSourceEntity;
import org.springframework.beans.factory.annotation.Autowired;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Collections;
import java.util.List;

public class CheckDsValidator implements ConstraintValidator<CheckDs, List<DataSourceEntity>> {
  @Autowired
  DeviceRepository devRepo;
  @Autowired
  ResourceRepository resRepo;

  GroupRepository grpRepo;

  @Override
  public void initialize(CheckDs constraintAnnotation) {}

  @Override
  public boolean isValid(List<DataSourceEntity> ms, ConstraintValidatorContext constrainContext) {
    return (ms == null) || ms.isEmpty()
        || ms.stream().filter(ds -> Collections.frequency(ms, ds) > 1).count() == 0;
  }
}
