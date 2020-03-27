/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.service;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.projectcenter.repository.GatewayRepository;
import com.openiot.cloud.projectcenter.repository.document.Gateway;
import com.openiot.cloud.projectcenter.service.dto.GatewayDTO;
import com.openiot.cloud.projectcenter.service.dto.ProjectDTO;
import com.openiot.cloud.sdk.event.TaskOperations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GatewayService {
  @Autowired
  private GatewayRepository gatewayRepository;

  @Autowired
  private TaskOperations taskOperations;

  private static void multipleWarning(List<Gateway> gateways, String queryCondition) {
    if (gateways.size() > 1) {
      log.warn("found multiple({}) gateways with {} ", gateways.size(), queryCondition);
    }
  }

  private void notifyProjectChange(List<Gateway> gateways, String newProjectId) {
    // notify with tasks
    for (Gateway gateway : gateways) {
      // to RD
      taskOperations.createTask(ConstDef.EVENT_MONITOR_RD_DEVICE_PROJECT,
                                ConstDef.EVENT_TYPE_DEVICE_PROJECT,
                                "modification of project information of a device",
                                ConstDef.EVENT_TARGET_TYPE_DEVICE,
                                gateway.getIAgentId(),
                                ConstDef.DAY_SECONDS,
                                newProjectId == null ? null : newProjectId.getBytes(),
                                null,
                                null);

      // to AMS
      taskOperations.createTask(ConstDef.EVENT_MONITOR_AMS_DEVICE_PROJECT,
                                ConstDef.EVENT_TYPE_CFG_SYNC,
                                "modification of project information of a device",
                                ConstDef.EVENT_TARGET_TYPE_DEVICE,
                                gateway.getIAgentId(),
                                ConstDef.DAY_SECONDS,
                                newProjectId == null ? null : newProjectId.getBytes(),
                                null,
                                null);
    }
  }

  public GatewayDTO findBySerialNumber(String serialNumber, boolean newSerialNumber) {
    Objects.requireNonNull(serialNumber);

    List<Gateway> gateways = newSerialNumber ? gatewayRepository.findByNewHwSn(serialNumber)
        : gatewayRepository.findByHwSn(serialNumber);
    if (gateways.isEmpty()) {
      log.debug("there is no such a gateway with a {} {}",
                newSerialNumber ? "new serial number" : "serial number",
                serialNumber);
      return null;
    }

    multipleWarning(gateways,
                    String.format("%s=%s",
                                  newSerialNumber ? "new serial number" : "serial number",
                                  serialNumber));

    GatewayDTO gatewayDTO = new GatewayDTO();
    BeanUtils.copyProperties(gateways.get(0), gatewayDTO);
    return gatewayDTO;
  }

  public GatewayDTO findByIAgentId(String iAgentId) {
    Objects.requireNonNull(iAgentId);

    List<Gateway> gateways = gatewayRepository.findByIAgentId(iAgentId);
    if (gateways.isEmpty()) {
      log.debug("there is no such a gateway with a iAgentId {}", iAgentId);
      return null;
    }

    multipleWarning(gateways, String.format("%s=%s", "iAgentId", iAgentId));

    GatewayDTO gatewayDTO = new GatewayDTO();
    BeanUtils.copyProperties(gateways.get(0), gatewayDTO);
    return gatewayDTO;
  }

  public GatewayDTO findByIAgentIdAndProjectId(String iAgentId, String projectId) {
    Objects.requireNonNull(iAgentId);
    Objects.requireNonNull(projectId);

    List<Gateway> gateways = gatewayRepository.findByIAgentIdAndProjectId(iAgentId, projectId);
    if (gateways.isEmpty()) {
      log.debug("there is no such a gateway with a iAgentId {} and a project id {}",
                iAgentId,
                projectId);
      return null;
    }

    multipleWarning(gateways, String.format("iAgentId=%s && projectId=%s", iAgentId, projectId));

    GatewayDTO gatewayDTO = new GatewayDTO();
    BeanUtils.copyProperties(gateways.get(0), gatewayDTO);
    return gatewayDTO;
  }

  public List<GatewayDTO> findByProjectId(String projectId) {
    Objects.requireNonNull(projectId);

    return gatewayRepository.findByProjectId(projectId).stream().map(gateway -> {
      GatewayDTO gatewayDTO = new GatewayDTO();
      BeanUtils.copyProperties(gateway, gatewayDTO);
      return gatewayDTO;
    }).collect(Collectors.toList());
  }

  // insert if new, update if existed, mongo save will keep tracking the ID
  public void save(GatewayDTO gatewayDTO) {
    Objects.requireNonNull(gatewayDTO);

    List<Gateway> gateways = gatewayRepository.findByIAgentId(gatewayDTO.getIAgentId());
    if (gateways.isEmpty()) {
      // it is a new one
      Gateway gateway = new Gateway();
      BeanUtils.copyProperties(gatewayDTO, gateway);
      gateways.add(gateway);
    } else {
      multipleWarning(gateways, String.format("%s=%s", "iAgentId", gatewayDTO.getIAgentId()));

      gateways.forEach(gateway -> {
        BeanUtils.copyProperties(gatewayDTO, gateway);
      });
    }
    gatewayRepository.saveAll(gateways);
  }

  public void removeAll() {
    gatewayRepository.deleteAll();
  }

  public void removeByIAgentId(String iAgentId) {
    Objects.requireNonNull(iAgentId);

    Optional.of(gatewayRepository.findByIAgentId(iAgentId)).ifPresent(gateways -> {
      multipleWarning(gateways, String.format("%s=%s", "iAgentId", iAgentId));
      gateways.forEach(gateway -> gatewayRepository.deleteById(gateway.getId()));
    });
  }

  public void resetGateways(String[] iAgentIds, boolean resetFlag) {
    Objects.requireNonNull(iAgentIds);

    log.debug("to mark those gateways {} to reset={}", Arrays.toString(iAgentIds), resetFlag);
    List<Gateway> gateways = gatewayRepository.findByIAgentIdIn(iAgentIds);
    for (Gateway gateway : gateways) {
      gateway.setReset(resetFlag);
    }
    gatewayRepository.saveAll(gateways);
  }

  public boolean replaceGateway(String iAgentId, String newSerialNumber) {
    Objects.requireNonNull(iAgentId);
    Objects.requireNonNull(newSerialNumber);

    if (newSerialNumber.isEmpty()) {
      log.warn("need a not empty serial number");
      return false;
    }

    log.debug("to replace a gateway {} with a new one with a serial number {}",
              iAgentId,
              newSerialNumber);

    List<Gateway> gateways = gatewayRepository.findByIAgentId(iAgentId);
    if (gateways.isEmpty()) {
      log.warn("there is no such a gateway with iAgentId {}", iAgentId);
      return false;
    }

    multipleWarning(gateways, String.format("%s=%s", "iAgentId", iAgentId));

    for (Gateway gateway : gateways) {
      if (Objects.equals(gateway.getHwSn(), newSerialNumber)) {
        log.warn("can not replace {} with a same serial number", gateway);
        return false;
      }

      gateway.setNewHwSn(newSerialNumber);
    }
    gatewayRepository.saveAll(gateways);
    return true;
  }

  public void setProject(String[] iAgentIds, ProjectDTO projectDTO) {
    Objects.requireNonNull(iAgentIds);

    // modify database
    List<Gateway> gateways = gatewayRepository.findByIAgentIdIn(iAgentIds);
    for (Gateway gateway : gateways) {
      gateway.setProjectId(projectDTO == null ? null : projectDTO.getId());
    }
    gatewayRepository.saveAll(gateways);

    // notifyAll
    notifyProjectChange(gateways, projectDTO == null ? null : projectDTO.getId());
  }

  public List<GatewayDTO> findAll() {
    return gatewayRepository.findAll().stream().map(gateway -> {
      GatewayDTO gatewayDTO = new GatewayDTO();
      BeanUtils.copyProperties(gateway, gatewayDTO);
      return gatewayDTO;
    }).collect(Collectors.toList());
  }

  public List<GatewayDTO> findUnassigned() {
    return gatewayRepository.findByProjectIdExists(false).stream().map(gateway -> {
      GatewayDTO gatewayDTO = new GatewayDTO();
      BeanUtils.copyProperties(gateway, gatewayDTO);
      return gatewayDTO;
    }).collect(Collectors.toList());
  }
}
