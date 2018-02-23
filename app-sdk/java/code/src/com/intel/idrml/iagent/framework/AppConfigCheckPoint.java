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

class AppConfigCheckPoint {
    private AmsRequestAction action;
    private String product_name;
    private String targetType;
    private String targetId;

    public AppConfigCheckPoint(AmsRequestAction action, String product, String targetType, String targetId) {
        this.action = action;
        this.product_name = product;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"action\":\"" + action.toString() + "\",");
        sb.append("\"product\":\"" + product_name + "\",");
        sb.append("\"target_type\":\"" + targetType + "\",");
        sb.append("\"target_id\":\"" + targetId + "\"");
        sb.append("}");
        return sb.toString();
    }

}
