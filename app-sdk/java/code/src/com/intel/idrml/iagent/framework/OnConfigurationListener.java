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

/**
 * Interface OnConfigurationListener is used for notification of configuration file when changed.<br><br>
 * 
 * @author saigon
 * @version 1.0
 * @since Feb 2017
 */
public interface OnConfigurationListener {
    /**
     * Callback method to notify APP when configuration file changed.
     * 
     * @param configFilePathLocal Local configure file name on the gateway, to read the content of this file,
     * AmsClient.getConfigFilePath() shall be used for the file full path.
     * @param targetType The same as that in API addCheckpoint.
     * @param targetId The same as that in API addCheckpoint. 
     */
    public void onConfigChanged(String configFilePathLocal, String targetType, String targetId);
}
