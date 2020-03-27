/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.service;

import com.openiot.cloud.projectcenter.repository.FactoryKeyRepository;
import com.openiot.cloud.projectcenter.service.dto.FactoryKeyDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Slf4j
@Service
public class FactoryKeyService {
  @Autowired
  private FactoryKeyRepository factoryKeyRepository;

  public FactoryKeyDTO findByNameAndType(String name, String type) {
    return Optional.of(factoryKeyRepository.findByKeyNameAndKeyType(name, type))
                   .filter(factoryKeys -> !factoryKeys.isEmpty())
                   .map(factoryKeys -> factoryKeys.get(0))
                   .map(factoryKey -> {
                     FactoryKeyDTO factoryKeyDTO = new FactoryKeyDTO();
                     BeanUtils.copyProperties(factoryKey, factoryKeyDTO);
                     return factoryKeyDTO;
                   })
                   .orElse(null);
  }
}
