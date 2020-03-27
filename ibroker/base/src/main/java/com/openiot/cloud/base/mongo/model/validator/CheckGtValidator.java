/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model.validator;

import com.openiot.cloud.base.mongo.dao.GroupTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Optional;

public class CheckGtValidator implements ConstraintValidator<CheckGt, String> {
  @Autowired
  GroupTypeRepository grptRepo;

  @Override
  public void initialize(CheckGt constraintAnnotation) {}

  @Override
  public boolean isValid(String gtName, ConstraintValidatorContext context) {
    return (gtName == null) || gtName.isEmpty()
        || Optional.ofNullable(grptRepo.findOneByN(gtName)).map(gs -> true).orElse(false);
  }
}
