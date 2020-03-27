/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model.validator;

import com.openiot.cloud.base.mongo.dao.GroupRepository;
import com.openiot.cloud.base.mongo.dao.GroupTypeRepository;
import com.openiot.cloud.base.mongo.dao.ResTypeRepository;
import com.openiot.cloud.base.mongo.model.Group;
import com.openiot.cloud.base.mongo.model.GroupType;
import com.openiot.cloud.base.mongo.model.OcfRTDefinition.ResDefEntry;
import org.springframework.beans.factory.annotation.Autowired;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Optional;
// import com.openiot.cloud.base.mongo.dao.TaskSrvRepository;
// import com.openiot.cloud.base.mongo.model.TaskService;

public class CheckNameValidator implements ConstraintValidator<CheckName, String> {

  @Autowired
  GroupRepository grpRepo;
  @Autowired
  GroupTypeRepository grptRepo;
  @Autowired
  ResTypeRepository rtRepo;

  private Class<?> caseMode;

  @Override
  public void initialize(CheckName constraintAnnotation) {
    caseMode = constraintAnnotation.value();
  }

  @Override
  public boolean isValid(String name, ConstraintValidatorContext constrainContext) {
    if (name == null) {
      return true;
    }

    if (caseMode == Group.class) {
      return Optional.ofNullable(grpRepo.findOneByName(name)).map(gs -> false).orElse(true);
    }

    if (caseMode == GroupType.class) {
      return Optional.ofNullable(grptRepo.findOneByN(name)).map(gts -> false).orElse(true);
    }

    if (caseMode == ResDefEntry.class) {
      return Optional.ofNullable(rtRepo.findOneByName(name)).map(rt -> false).orElse(true);
    }

    return false;
  }
}
