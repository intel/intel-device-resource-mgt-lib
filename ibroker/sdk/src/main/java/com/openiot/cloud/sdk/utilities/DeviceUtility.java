/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.utilities;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.dao.DeviceRepository;
import com.openiot.cloud.base.mongo.model.Device;
import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import com.openiot.cloud.sdk.service.ApplicationContextProvider;
import com.openiot.cloud.sdk.event.TaskOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

/**
 * The utility to access devices in openiot cloud.<br>
 * <br>
 *
 * @author saigon
 * @version 1.0
 * @since Oct 2017
 */
@Component(value = "deviceUtils")
public class DeviceUtility {

  @Autowired
  DeviceRepository deviceRepo;
  // @Autowired
  // TaskTrigger ttrigger;
  @Autowired
  TaskOperations taskOps;

  public static DeviceUtility getInstance() {
    return ApplicationContextProvider.getBean(DeviceUtility.class);
  }

  /**
   * Method to get Device with specified device name.
   *
   * @param name This parameter is the specified name of device.
   * @return the device searched with specified name.
   * @see com.openiot.cloud.base.mongo.model.Device
   */
  public Device getDeviceByName(String name) {
    return deviceRepo.findOneByName(name);
  }

  /**
   * Method to get Device with specified device id.
   *
   * @param id This parameter is the specified id of device.
   * @return the device searched with specified id.
   * @see com.openiot.cloud.base.mongo.model.Device
   */
  public Device getDeviceById(String id) {
    return deviceRepo.findOneById(id);
  }

  /**
   * Method to get all Devices with specified conditions.<br>
   * [note]:<br>
   * 1. all parameters are optional, which means that any parameter can be set null. If it is null,
   * then this <br>
   * condition take no effect.<br>
   * 2. returned data is the result of all conditions are matched.<br>
   * <br>
   *
   * @param standard The standard of the devices, value can be: ocf, lwm2m, modbus, ... TBD ???.
   * @param dtName Device Type, value can be: ... TBD ???.
   * @param groupName The name of group that the device is belong to.
   * @param iAgentId The id of iAgent that the device is belong to.
   * @param isConnected The connection status of the device.
   * @param isEnabled The enabled status of the device.
   * @return List of all devices that match all specified conditions.
   * @see com.openiot.cloud.base.mongo.model.Device
   */
  public List<Device> getDeviceWith(String projectId, String standard, String dtName,
                                    String groupName, String iAgentId, Boolean isConnected,
                                    Boolean isEnabled, AttributeEntity attribute) {
    return deviceRepo.filter(Optional.ofNullable(projectId),
                             Optional.empty(),
                             Optional.empty(),
                             Optional.ofNullable(standard),
                             Optional.ofNullable(dtName),
                             Optional.ofNullable(groupName),
                             Optional.ofNullable(iAgentId),
                             Optional.empty(),
                             Optional.ofNullable(isConnected),
                             Optional.ofNullable(isEnabled).orElse(Boolean.TRUE),
                             Optional.empty(),
                             Optional.ofNullable(attribute),
                             new PageRequest(0, ConstDef.MAX_SIZE));
  }

  public void updateDevice(Device iagent) {
    if (iagent.getId() != null) {
      deviceRepo.save(iagent);
    } else {
      // shall not happen
    }

    taskOps.createTask("CFG_MONITOR",
                       ConstDef.EVENT_TYPE_CFG_SYNC,
                       null,
                       ConstDef.EVENT_TARGET_TYPE_DEVICE,
                       iagent.getId(),
                       ConstDef.DAY_SECONDS,
                       null,
                       null,
                       ConstDef.EVENT_TASK_OPTION_OVERWRITE);
  }
}
