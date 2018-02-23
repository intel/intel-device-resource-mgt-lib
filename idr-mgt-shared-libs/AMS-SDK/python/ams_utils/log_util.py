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



import datetime

class LogUtil(object):
    # datetime.datetime.now().strftime("%Y/%m/%d %H:%M:%S") 2017/07/11 10:32:13

    def __init__(self):
        pass
    @staticmethod
    def __display_current_stack(num_stack):
        for i in range(4, len(threading.current_thread().get_stack_trace())):
            if i - 4 < num_stack:
                print str(threading.current_thread().get_stack_trace()[i])
 
    @staticmethod
    def __get_code_address():
        # sts = threading.current_thread().get_stack_trace(
        # )  #no get_stack_trace in python
        sts = None
        if sts is None:
            return None
        for st in sts:
            if st.is_native_method() == 0 and st.get_class_name(
            ) != 'threading': 
                return str(st)
        return None
 
    @staticmethod
    def log(info, num_stack=None):
        print datetime.datetime.now().strftime(
            "%Y/%m/%d %H:%M:%S") + ": " + str(LogUtil.__get_code_address()) + " " + info
        if num_stack is not None:
            display_current_stack(num_stack)
