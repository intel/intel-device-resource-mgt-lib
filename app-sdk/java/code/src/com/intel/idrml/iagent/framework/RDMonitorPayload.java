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

import com.alibaba.fastjson.JSONObject;

class RDMonitorPayload {
    public String dt;
    public String rt;
    public String di;
    public String ri;
    public String st;
    public String mid;
    public String[] with_rts;
    public String[] groups;
    public String purl;
    public transient String localPath;

    public RDMonitorPayload(String di, String dt, String rt, String[] withRts, String st, String[] groups, String mid, String purl, String localPath) {
        this.di = di;
        this.dt = dt;
        this.rt = rt;
        this.with_rts = withRts;
        this.st = st;
        this.groups = groups;
        this.mid = mid;
        this.purl = purl;
        this.localPath = localPath;
    }

    @Override
    public boolean equals(Object obj) {
        super.equals(obj);
        // TODO Auto-generated method stub
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RDMonitorPayload other = (RDMonitorPayload) obj;
        if (other.mid.equals(mid)) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return mid == null ? 0 : mid.hashCode();
    }

    public String toJson() {
        String json = JSONObject.toJSONString(this);
        return json;
    }
}
