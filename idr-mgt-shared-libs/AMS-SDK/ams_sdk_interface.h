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


#ifndef AMS_CLIENT_INTERFACE_H_
#define AMS_CLIENT_INTERFACE_H_

#include <stdbool.h>

#ifdef __cplusplus
extern "C"
{
#endif
#define AMS_CLIENT_COAP_SERVER_PORT 2233

#define RET_CODE_OK       0
#define RET_CODE_TIMEOUT -1
#define RET_CODE_NOT_INIT -2
#define RET_CODE_AMS_FAIL -3

typedef int (*cfg_change_callback)(const char * product_name, const char * target_type, const char * target_id, const char *cfg_file_name);

typedef int (*ams_client_status_callback)(int result);


typedef enum
{
    AMS_Uninit,
    AMS_Init_Fail,
    AMS_Not_Ready,
    AMS_Ready
}E_AMS_STATUS;

extern void *  ams_init(char * product_name, cfg_change_callback cfg_change_cb, ams_client_status_callback status_cb);
extern int ams_get_status();
extern int ams_set_product_id(char* product_device_id);
extern int ams_add(char* target_type, char* target_id,bool overwrite_target_id);
extern int ams_delete(char* target_type, char* target_id);
extern int ams_imediate_cfg_check();
extern int ams_register_config_status(void *callback);
extern int ams_deregister_config_status();


#ifdef __cplusplus
}
#endif
#endif
