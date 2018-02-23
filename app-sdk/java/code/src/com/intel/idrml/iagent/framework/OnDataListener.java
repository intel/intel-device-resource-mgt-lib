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

import org.eclipse.californium.core.coap.CoAP.ResponseCode;

import com.intel.idrml.iagent.model.ResourceDataGeneral;

/**
 * Interface OnDataListener is used for notification of resource data.<br><br>
 * 
 * @author saigon
 * @version 1.0
 * @since Jan 2017
 */
public interface OnDataListener {
    /**
     * Callback method to notify APP when resource data changed.
     * 
     * @return the return code that will be sent back to iAgent on gateway when parameter DataQueryParam.process is true, which can be<br> 
     * CONTINUE_2_32（95）: the parameter "data" in this method has NOT been changed, iAgent can continue process next monitor<br>
     * CHANGED_2_04（68）: the parameter "data" in this method has been changed, iAgent shall deliver changed data to next monitor<br>
     * FORBIDDEN_4_03(131):  iAgent shall stop delivering data to next monitor<br>
     * OTHER_VALUE(*): the parameter "data" in this method has NOT been changed, iAgent can continue process next monitor
     * @param deviceID ID of device whose resource data is changed,
     * @param resouceUri URI of the resource on the device.
     * @param data The property data, isParsed() shall be used to check if the raw data has been parsed and <br>
     * getFormat() shall be used to check which format this property data to be converted to, the build-in format includes<br>
     * ResourceDataLWM2M and ResourceDataOIC. <br>
     * If return value is CHANGED_2_04（68）, then it means parameter "data" has been modified and will be sent back to iAgent.<br>
     * If return value is CONTINUE_2_31（95）,FORBIDDEN_4_03(131) or OTHER_VALUE(*), then parameter "data" will be NOT be sent to iAgent 
     */
    public ResponseCode onResourceDataChanged(String deviceID, String resouceUri, ResourceDataGeneral data);
}
