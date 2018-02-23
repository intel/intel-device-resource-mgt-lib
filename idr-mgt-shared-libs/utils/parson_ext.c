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

/*
 * parson_ext.c
 *
 *  Created on: Mar 4, 2017
 *      Author: xwang98
 *
 *
 */

#include "parson_ext.h"

int get_json_number_safe(const JSON_Object *object, const char *name)
{
	JSON_Value * jvalue = json_object_get_value(object, name);
	if(jvalue && json_value_get_type(jvalue) == JSONNumber)
	   return (int) json_number(jvalue);

	return -1;
}
