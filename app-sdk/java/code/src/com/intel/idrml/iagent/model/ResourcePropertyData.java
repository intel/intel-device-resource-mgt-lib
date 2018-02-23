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

public class ResourcePropertyData {
    public String propName; // for lwm2m, it can be "1", or "1/0" ;  for oic, it can be "settemp"
    public float v;
    public int t;
    public boolean bv;
    public String sv;
    public ValueType valueType;

    public enum ValueType {
        FLOAT(0), BOOLEAN(1), STRING(2);

        public final int value;

        ValueType(int value) {
            this.value = value;
        }

        public static ValueType valueOf(final int value) {
            switch (value) {
            case 0:
                return FLOAT;
            case 1:
                return BOOLEAN;
            case 2:
                return STRING;
            default:
                throw new IllegalArgumentException("Unknown ValueType " + value);
            }
        }
    }

	public String toJson() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"n\":\""+propName+"\",");
		switch(valueType)
		{
		case FLOAT:
			sb.append("\"v\":\""+v+"\"");
			break;
		case BOOLEAN:
			sb.append("\"bv\":\""+bv+"\"");
			break;
		case STRING:
			default:
			sb.append("\"sv\":\""+sv+"\"");
			break;
		}
		sb.append("}");
		return sb.toString();
	}
}