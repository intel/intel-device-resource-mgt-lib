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

package com.intel.idrml.iagent.model;

public class DataQueryParam {

    /**
     * The device ID, which shall be queried by such query API as IAgentManager.DoDeviceQuery()
     */
    public String deviceID;

    /**
     * The resource ID
     */
    public String resouceUri;

    /**
     * The time of interval that iAgent shall send the data notification, the unit is seconds
     */
    public int interval;
    
    /**
     * The sequence of iAgent publish resource data for the data monitor, the bigger "sequence" value, the higher priority<br>
     * default value will be 0
     */
    public int sequence;

    /**
     * Parameter indicating if this monitor is involved in data process, <br>
     * true - involved in data process, the data notified will be changed and returned to iAgent with response code<br>
     * false - NOT involved in data process
     */
    public boolean process;


    public DataQueryParam(String deviceID, String resouceUri) {
    	this(deviceID, resouceUri, 10, 0, false); // 10 is the default
    }
    
    public DataQueryParam(String deviceID, String resouceUri, int interval, int sequence, boolean process) {
        this.deviceID = deviceID;
        this.resouceUri = resouceUri;
        this.interval = interval;
        this.sequence = sequence;
        this.process = process;
    }

}
