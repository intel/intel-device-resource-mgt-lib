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

import com.intel.idrml.iagent.utilities.MediaTypeFormat;

public class ResourceDataGeneral {

    private String rawPayload;
    public boolean isParsed = false;
    public String resourceId; // for lwm2m, it can be "1/0"  ;   for oic, it can be "/light/1" 
	private int format = MediaTypeFormat.TEXT_PLAIN;

    public ResourceDataGeneral(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public boolean isParsed() {
        return isParsed;
    }

    public void setParsed(boolean isParsed) {
        this.isParsed = isParsed;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public String setRawPayload(String rawPayload) {
        return this.rawPayload=rawPayload;
    }

    public String toJson() {
        return rawPayload;
    }

    public int getFormat(){ 
    	return format;
    }

	public void setFormat(int format) {
		this.format = format;
	}
}
