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

class MessageDataMonitor {
    public String deviceID;

    public String resourceURI;

    public int interval;

    public String requesterName;

    public String pushURL;

    public String localPath;
    public String monitorId;

    public int sequence;

    public int process;

    @Override
    public boolean equals(Object obj) {
        super.equals(obj);
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MessageDataMonitor other = (MessageDataMonitor) obj;
        if (other.localPath.equals(localPath)) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return localPath == null ? 0 : localPath.hashCode();
    }

    public String toJsonString() {
        StringBuffer bf = new StringBuffer("{");
        bf.append("\"di\":\"" + deviceID + "\",");
        bf.append("\"ri\":\"" + resourceURI + "\",");
        bf.append("\"interval\":\"" + interval + "\",");
        bf.append("\"purl\":\"" + pushURL + "\",");
        bf.append("\"sequence\":" + sequence + ",");
        bf.append("\"process\":\"" + process + "\"");
        bf.append("}");
        return bf.toString();
    }

}
