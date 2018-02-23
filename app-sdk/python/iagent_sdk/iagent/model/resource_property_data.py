# -*- coding: utf-8 -*-

# Copyright (C) 2017 Intel Corporation.  All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.



# from enum import Enum

class ResourcePropertyData(object):
    def __init__(self):
        self.prop_name = None
        # self.v = None
        # self.t = None
        # self.bv = None
        # self.sv = None
        # self.value_type = None
        self.value = None

    def to_json(self):
        sb = []
        sb.append("{")
        sb.append("\"n\":\"" + self.prop_name + "\",")
        # if self.value_type == ValueType.FLOAT:
        #    sb.append("\"v\":" + str(self.v) )
        # elif self.value_type == ValueType.BOOLEAN:
        #    sb.append("\"bv\":" + str(self.bv))
        # elif self.value_type == ValueType.STRING:
        #    sb.append("\"sv\":\"" + self.sv + "\"")
        # else:
        #    pass
        print "property value =" + str(self.value)
        if isinstance(self.value, (int, float)):
            print "value is a  float"
	    sb.append("\"v\":" + str(self.value))
        elif isinstance(self.value, bool):
	    sb.append("\"bv\":" + str(self.value))
        elif isinstance(self.value, str):
            sb.append("\"sv\":\"" + self.value + "\"")	
        sb.append("}")
        return "".join(sb)


# class ValueType(Enum):
#    FLOAT = 0
#    BOOLEAN = 1
#    STRING = 2
    # def __init__(self,value): #???
    # 	self.value =value

#    def value_of(value):
#        if value == 0:
#            return FLOAT
#        elif value == 1:
#            return BOOLEAN
#        elif value == 2:
#            return STRING
#        else:
#            pass

    # def __init__(self,value):
    #    self.value = value
