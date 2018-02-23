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
 * parson_ext.h
 *
 *  Created on: Mar 4, 2017
 *      Author: xwang98
 */

#ifndef APPS_IAGENT_CORE_LIB_PARSON_EXT_H_
#define APPS_IAGENT_CORE_LIB_PARSON_EXT_H_

#include "parson.h"

#ifdef __cplusplus
extern "C" {
#endif
// return -1 if the item doesn't exist
int get_json_number_safe(const JSON_Object *object, const char *name);


#ifdef __cplusplus
}
#endif
#endif /* APPS_IAGENT_CORE_LIB_PARSON_EXT_H_ */
