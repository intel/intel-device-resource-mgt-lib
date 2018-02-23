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

package com.intel.imrt.task;

public class AutoResponderEntry {
    public String type;
    public String purl = "";
    public String filename;

    public AutoResponderEntry() {

    }

    public AutoResponderEntry(String type, String purl) {
        this.type = type;
        this.purl = purl;
    }

    public AutoResponderEntry(String type, String purl, String filename) {
        this.type = type;
        this.purl = purl;
        this.filename = filename;
    }

    @Override
    public boolean equals(Object obj) {
        // TODO Auto-generated method stub
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AutoResponderEntry other = (AutoResponderEntry) obj;
        if (other.type.equals(type)) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        int h = 3;
        h += type == null ? 0 : type.hashCode();
        h += purl == null ? 0 : purl.hashCode();
        return h;
    }
}
