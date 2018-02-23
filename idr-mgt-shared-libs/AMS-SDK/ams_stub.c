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

#include "ams_sdk_interface.h"
#include "ams_sdk_internal.h"
#include "ams_path.h"
#include "coap_request.h"



int ams_init(char * product_name)
{
	printf ("stub: not implemented %s\n", __FUNCTION__);
	return 0;
}

int ams_set_product_id(char* product_device_id)
{
	printf ("stub: not implemented %s\n", __FUNCTION__);
	return 0;
}

int ams_add(char* target_type, char* target_id,bool overwrite_target_id)
{
	printf ("stub: not implemented %s\n", __FUNCTION__);
	return 0;
}

int ams_delete(char* target_type, char* target_id)
{
	printf ("stub: not implemented %s\n", __FUNCTION__);
	return 0;
}

int ams_imediate_cfg_check()
{
	printf ("stub: not implemented %s\n", __FUNCTION__);
	return 0;
}

int ams_register_config_status(void *callback)
{
	printf ("stub: not implemented %s\n", __FUNCTION__);
	return 0;
}

int ams_deregister_config_status()
{
	printf ("stub: not implemented %s\n", __FUNCTION__);
	return 0;
}
