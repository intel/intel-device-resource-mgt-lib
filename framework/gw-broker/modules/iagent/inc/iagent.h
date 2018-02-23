// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

#ifndef MODULE_IAGENT_H
#define MODULE_IAGENT_H

#include "module.h"

#ifdef __cplusplus
extern "C"
{
#endif


MODULE_EXPORT const MODULE_API* MODULE_STATIC_GETAPI(IAGENT_MODULE)(MODULE_API_VERSION gateway_api_version);

void handle_bus_rd_event(MESSAGE_HANDLE messageHandle);
void handle_bus_rd_get(MESSAGE_HANDLE messageHandle);
void handle_bus_rd_delete(MESSAGE_HANDLE messageHandle);
void handle_bus_rd_monitor_post(MESSAGE_HANDLE messageHandle);
void handle_bus_calibration(MESSAGE_HANDLE messageHandle);
void handle_bus_ibroker(MESSAGE_HANDLE messageHandle, char * url, char * src_module, char *tm );
void handle_bus_endpoint(MESSAGE_HANDLE messageHandle, char * epname, char * url, uint32_t mid, char * src_module);
void handle_refresher_data(MESSAGE_HANDLE messageHandle);
void handle_bus_default(MESSAGE_HANDLE messageHandle);
void handle_data_point(MESSAGE_HANDLE messageHandle);

#ifdef __cplusplus
}
#endif


#endif /*HELLO_WORLD_H*/
