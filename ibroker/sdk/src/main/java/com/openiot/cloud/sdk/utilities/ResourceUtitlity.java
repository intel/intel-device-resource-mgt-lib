/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.utilities;

import com.openiot.cloud.base.mongo.dao.ResTypeRepository;
import com.openiot.cloud.base.mongo.model.Resource;
import com.openiot.cloud.base.mongo.model.ResourceType;
import com.openiot.cloud.sdk.service.ApplicationContextProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * The utility to access resources in openiot cloud.<br>
 * <br>
 *
 * @author saigon
 * @version 1.0
 * @since Oct 2017
 */
@Component
public class ResourceUtitlity {
  @Autowired
  ResTypeRepository rtRepo;

  public static ResourceUtitlity getInstance() {
    return ApplicationContextProvider.getBean(ResourceUtitlity.class);
  }

  /**
   * Method to get Resource with specified resource url and deviceId.
   *
   * @param deviceId The id of the device.
   * @param url The url of the resource.
   * @return the resource searched with specified name.
   * @see com.openiot.cloud.base.mongo.model.Resource
   */
  public Resource getResourceByUrl(String deviceId, String url) {
    return null;
  }

  /**
   * Method to get Resource with specified resource name and deviceId.
   *
   * @param deviceId The id of the device.
   * @param resourceName The name of the resource.
   * @return the resource searched with specified name.
   * @see com.openiot.cloud.base.mongo.model.Resource
   */
  public Resource getResourceByName(String deviceId, String resourceName) {
    return null;
  }

  /**
   * Method to get all resources with specified conditions.<br>
   * [note]:<br>
   * 1. all parameters are optional, which means that any parameter can be set null. If it is null,
   * then this <br>
   * condition take no effect.<br>
   * 2. returned data is the result of all conditions are matched.<br>
   * <br>
   *
   * @param deviceId The id of the devices.
   * @param rtName Resource Type, value can be: light, temperature, humidity... TBD ???.
   * @param groupName The name of group that the resource is belong to.
   * @return List of all resources that match all specified conditions.
   * @see com.openiot.cloud.base.mongo.model.Device
   */
  public List<Resource> getResourceWith(String deviceId, String rtName, String groupName) {
    return null;
  }

  public ResourceType findResourceType(String rt) {
    return rtRepo.findOneByName(rt);
  }

  public void saveResourceType(ResourceType rt) {
    rtRepo.save(rt);
  }
}
