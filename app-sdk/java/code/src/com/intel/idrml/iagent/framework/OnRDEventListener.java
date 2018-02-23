/*
 * Copyright (C) 2017 Intel Corporation.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.intel.idrml.iagent.framework;

import java.util.List;

import com.intel.idrml.iagent.model.DeviceInfo;
import com.intel.idrml.iagent.model.Resource;

/**
 * Interface OnRDEventListener is used for notification of device, group or resource.<br><br>
 * 
 * @author saigon
 * @version 1.0
 * @since Jan 2017
 */
public interface OnRDEventListener
{
    /**
     * Callback method to notify APP when devices changed.
     * 
     * @param devicesChanged list of devices that have been changed
     */
    public void onDeviceChanged(List<DeviceInfo> devicesChanged);
    
    /**
     * Callback method to notify APP when resources changed.
     * 
     * @param resourcesChanged list of resources that have been changed
     * @param isAddedOrRemove indication if the resources are added or removed
     */
    public void onResourceChanged(List<Resource> resourcesChanged, boolean isAddedOrRemove);
}
