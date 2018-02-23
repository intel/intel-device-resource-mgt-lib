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

public class RDQueryParam {
    public String deviceID;
    public String standardType;
    public String deviceType;
    public String groupId;
    public String groupIds[];
    public String resouceType;
    public String resouceUri;
    public String[] withRts;
    public DeviceInfo.DeviceStatus status;

    public String toQueryString() {
        StringBuffer bf = new StringBuffer();
        
        if (deviceID != null) bf.append("&di=" + deviceID);
        if (standardType != null) bf.append("&st=" + standardType);
        if (deviceType != null) bf.append("&dt=" + deviceType);
        if (groupId != null) bf.append("&group=" + groupId);
        if (resouceType != null) bf.append("&rt=" + resouceType);
        if (status != null) bf.append("&status=" + status);
        if (groupIds != null && groupIds.length > 0) {
            bf.append("&groups=");
            StringBuffer ids = new StringBuffer();
            ids.append("[");
            int l = groupIds.length;
            for (int i = 0; i < l; i++) {
                if (i == (l - 1)) {
                    ids.append(groupIds[i] + "]");
                } else {
                    ids.append(groupIds[i] + ",");
                }
            }
            bf.append(ids.toString());
        }

        if (withRts != null && withRts.length > 0) {
            bf.append("&with_rts=");
            StringBuffer ids = new StringBuffer();
            ids.append("[");
            int l = withRts.length;
            for (int i = 0; i < l; i++) {
                if (i == (l - 1)) {
                    ids.append(withRts[i] + "]");
                } else {
                    ids.append(withRts[i] + ",");
                }
            }
            bf.append(ids.toString());
        }
        
        if(bf.length()>0) bf.replace(0, 1, "?"); //replace the first & with ?

        return bf.toString();
    }
}
