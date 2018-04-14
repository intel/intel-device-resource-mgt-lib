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

#include "hello_world.h"

void hello_response_callback(restful_response_t *response, void * user_data)
{
	time_t * req_time = (time_t *)user_data;

	printf("hello_response_callback: req time: %ld, code=%d, seconds used: %ld\n", *req_time, response?response->code:-1, time(NULL) - *req_time);

	free(user_data);

}
