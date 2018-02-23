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

class AppConfigWatchPoint {
    public AmsRequestAction action;
    public String product;
    public String ip;
    public int port;
    public String path;

    public static class ConfigNotification {
        public String product;
        public String target_type;
        public String target_id;
        public String config_path;

        public ConfigNotification() {

        }
    }

    public AppConfigWatchPoint(AmsRequestAction action, String product, String addr, int port, String path) {
        this.action = action;
        this.product = product;
        this.ip = addr;
        this.port = port;
        this.path = path;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"action\":\"" + action.toString() + "\",");
        sb.append("\"product\":\"" + product + "\",");
        sb.append("\"ip\":\"" + ip + "\",");
        sb.append("\"path\":\"" + path + "\",");
        sb.append("\"port\":" + port + "");
        sb.append("}");
        return sb.toString();
    }
}
